package it.odvsicilia.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@EnableRetry
public class DatabaseConnectionRetryConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionRetryConfig.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_INTERVAL = 2000L;
    private static final double MULTIPLIER = 2.0;

    @Bean
    public RetryTemplate databaseRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(INITIAL_INTERVAL);
        backOffPolicy.setMultiplier(MULTIPLIER);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        Map<Class<? extends Throwable>, Boolean> retryableExceptions = new HashMap<>();
        retryableExceptions.put(CannotGetJdbcConnectionException.class, true);
        retryableExceptions.put(SQLException.class, true);
        
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(MAX_ATTEMPTS, retryableExceptions);
        retryTemplate.setRetryPolicy(retryPolicy);

        retryTemplate.registerListener(new DatabaseConnectionRetryListener());

        return retryTemplate;
    }

    @Bean
    public BeanPostProcessor dataSourceRetryPostProcessor(RetryTemplate databaseRetryTemplate) {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                if (bean instanceof HikariDataSource) {
                    HikariDataSource dataSource = (HikariDataSource) bean;
                    logger.info("Wrapping HikariDataSource with retry logic for connection pool initialization");
                    
                    try {
                        return databaseRetryTemplate.execute(context -> {
                            try {
                                dataSource.getConnection().close();
                                logger.info("Database connection pool initialized successfully");
                                return dataSource;
                            } catch (SQLException | CannotGetJdbcConnectionException e) {
                                logger.error("Failed to initialize connection pool on attempt {}", 
                                    context.getRetryCount() + 1, e);
                                throw e;
                            }
                        });
                    } catch (Exception e) {
                        logger.error("All retry attempts exhausted. Unable to initialize database connection pool", e);
                        throw new IllegalStateException("Failed to initialize database connection after " + 
                            MAX_ATTEMPTS + " attempts", e);
                    }
                }
                return bean;
            }
        };
    }

    private static class DatabaseConnectionRetryListener implements RetryListener {
        private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionRetryListener.class);
        private static final Pattern HOST_PATTERN = Pattern.compile("jdbc:postgresql://([^:/]+)(?::(\\d+))?");
        private static final Pattern AUTH_ERROR_PATTERN = Pattern.compile("(authentication failed|password authentication failed|role .* does not exist)", Pattern.CASE_INSENSITIVE);
        private static final Pattern NETWORK_ERROR_PATTERN = Pattern.compile("(Connection refused|Connection timed out|Network is unreachable|No route to host|UnknownHostException)", Pattern.CASE_INSENSITIVE);
        private static final Pattern TIMEOUT_ERROR_PATTERN = Pattern.compile("(timeout|timed out)", Pattern.CASE_INSENSITIVE);

        @Override
        public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
            logger.info("Database connection retry mechanism initialized");
            return true;
        }

        @Override
        public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            if (throwable != null) {
                logger.error("Database connection retry mechanism exhausted all attempts. Final error: {}", 
                    throwable.getMessage());
            } else {
                logger.info("Database connection established successfully");
            }
        }

        @Override
        public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
            int attemptNumber = context.getRetryCount() + 1;
            String exceptionMessage = throwable.getMessage();
            
            String diagnosticDetails = extractDiagnosticDetails(throwable, exceptionMessage);
            
            logger.warn("Database connection retry attempt {}/{} failed. Exception: {}. Diagnostic details: {}", 
                attemptNumber, 
                MAX_ATTEMPTS, 
                exceptionMessage,
                diagnosticDetails);
            
            if (attemptNumber < MAX_ATTEMPTS) {
                long nextRetryDelay = calculateNextRetryDelay(attemptNumber);
                logger.info("Retrying database connection in {} ms...", nextRetryDelay);
            }
        }

        private String extractDiagnosticDetails(Throwable throwable, String exceptionMessage) {
            StringBuilder details = new StringBuilder();
            
            String databaseHost = extractDatabaseHost(throwable);
            if (databaseHost != null) {
                details.append("Database host: ").append(databaseHost).append("; ");
            }
            
            String failureReason = extractFailureReason(exceptionMessage, throwable);
            details.append("Failure reason: ").append(failureReason);
            
            return details.toString();
        }

        private String extractDatabaseHost(Throwable throwable) {
            Throwable current = throwable;
            while (current != null) {
                String message = current.getMessage();
                if (message != null) {
                    Matcher matcher = HOST_PATTERN.matcher(message);
                    if (matcher.find()) {
                        String host = matcher.group(1);
                        String port = matcher.group(2);
                        return port != null ? host + ":" + port : host + ":5432";
                    }
                }
                current = current.getCause();
            }
            
            String jdbcUrl = System.getProperty("spring.datasource.url");
            if (jdbcUrl == null) {
                jdbcUrl = System.getenv("DATABASE_URL");
            }
            if (jdbcUrl != null) {
                Matcher matcher = HOST_PATTERN.matcher(jdbcUrl);
                if (matcher.find()) {
                    String host = matcher.group(1);
                    String port = matcher.group(2);
                    return port != null ? host + ":" + port : host + ":5432";
                }
            }
            
            return "Unknown";
        }

        private String extractFailureReason(String exceptionMessage, Throwable throwable) {
            if (exceptionMessage == null) {
                exceptionMessage = "";
            }
            
            String fullStackTrace = getFullExceptionChain(throwable);
            
            if (AUTH_ERROR_PATTERN.matcher(fullStackTrace).find()) {
                return "Authentication failure - invalid username or password";
            }
            
            if (NETWORK_ERROR_PATTERN.matcher(fullStackTrace).find()) {
                return "Network connectivity issue - host unreachable or connection refused";
            }
            
            if (TIMEOUT_ERROR_PATTERN.matcher(fullStackTrace).find()) {
                return "Connection timeout - database server not responding in time";
            }
            
            if (fullStackTrace.contains("database") && fullStackTrace.contains("does not exist")) {
                return "Database does not exist";
            }
            
            if (fullStackTrace.contains("SSL")) {
                return "SSL/TLS connection issue";
            }
            
            if (exceptionMessage.length() > 100) {
                return exceptionMessage.substring(0, 97) + "...";
            }
            
            return exceptionMessage.isEmpty() ? "Unknown error" : exceptionMessage;
        }

        private String getFullExceptionChain(Throwable throwable) {
            StringBuilder chain = new StringBuilder();
            Throwable current = throwable;
            while (current != null) {
                if (current.getMessage() != null) {
                    chain.append(current.getMessage()).append(" ");
                }
                chain.append(current.getClass().getSimpleName()).append(" ");
                current = current.getCause();
            }
            return chain.toString();
        }

        private long calculateNextRetryDelay(int attemptNumber) {
            return (long) (INITIAL_INTERVAL * Math.pow(MULTIPLIER, attemptNumber - 1));
        }
    }
}
