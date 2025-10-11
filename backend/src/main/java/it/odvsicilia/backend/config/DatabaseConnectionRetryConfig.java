package it.odvsicilia.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
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
    public BeanPostProcessor dataSourceRetryPostProcessor(RetryTemplate databaseRetryTemplate, 
                                                         ConfigurableEnvironment environment) {
        return new BeanPostProcessor() {
            private boolean poolerFallbackAttempted = false;
            private boolean directConnectionFailed = false;
            
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
                                String currentUrl = dataSource.getJdbcUrl();
                                boolean isPoolerError = isSupabasePoolerConnectionError(e, currentUrl);
                                
                                if (isPoolerError && !poolerFallbackAttempted) {
                                    logger.error("SUPABASE POOLER CONNECTION FAILED on attempt {}: Connection refused to pooler endpoint", 
                                        context.getRetryCount() + 1);
                                    logger.error("Pooler error details: {}", e.getMessage());
                                    logger.warn("Attempting automatic fallback from pooler to direct database connection...");
                                    
                                    String directUrl = convertPoolerToDirectUrl(currentUrl);
                                    if (directUrl != null && !directUrl.equals(currentUrl)) {
                                        poolerFallbackAttempted = true;
                                        updateDatasourceUrl(environment, directUrl);
                                        dataSource.setJdbcUrl(directUrl);
                                        logger.info("Switched datasource URL from pooler endpoint to direct connection: {}", 
                                            maskPassword(directUrl));
                                        
                                        dataSource.getConnection().close();
                                        logger.info("Direct database connection established successfully after pooler failure");
                                        return dataSource;
                                    } else {
                                        logger.error("Failed to convert pooler URL to direct connection URL");
                                    }
                                } else if (poolerFallbackAttempted) {
                                    directConnectionFailed = true;
                                    logger.error("DIRECT DATABASE CONNECTION FAILED on attempt {}: {}", 
                                        context.getRetryCount() + 1, e.getMessage());
                                } else {
                                    logger.error("DATABASE CONNECTION FAILED on attempt {} (general connectivity issue): {}", 
                                        context.getRetryCount() + 1, e.getMessage());
                                }
                                
                                throw e;
                            }
                        });
                    } catch (Exception e) {
                        if (poolerFallbackAttempted && directConnectionFailed) {
                            String errorMsg = buildFailFastErrorMessage(dataSource.getJdbcUrl());
                            logger.error(errorMsg);
                            throw new IllegalStateException(errorMsg, e);
                        } else {
                            logger.error("All retry attempts exhausted. Unable to initialize database connection pool", e);
                            throw new IllegalStateException("Failed to initialize database connection after " + 
                                MAX_ATTEMPTS + " attempts. Check database connectivity and credentials.", e);
                        }
                    }
                }
                return bean;
            }
            
            private boolean isSupabasePoolerConnectionError(Throwable throwable, String jdbcUrl) {
                if (jdbcUrl == null || !isSupabasePoolerUrl(jdbcUrl)) {
                    return false;
                }
                
                String fullMessage = getFullExceptionChain(throwable);
                
                boolean hasConnectionRefused = fullMessage.contains("Connection refused") || 
                                              fullMessage.contains("Connection timed out") ||
                                              fullMessage.contains("ConnectException");
                
                boolean hasPoolerPort = jdbcUrl.contains(":6543") || jdbcUrl.contains(":5432");
                
                return hasConnectionRefused && hasPoolerPort;
            }
            
            private boolean isSupabasePoolerUrl(String url) {
                return url != null && 
                       (url.contains(".pooler.supabase.com") || 
                        (url.contains(".supabase.com") && url.contains(":6543")) ||
                        (url.contains(".supabase.co") && url.contains(":6543")));
            }
            
            private String convertPoolerToDirectUrl(String poolerUrl) {
                if (poolerUrl == null) {
                    return null;
                }
                
                Pattern poolerPattern = Pattern.compile(
                    "(jdbc:postgresql://)(aws-\\d+-[a-z]+-[a-z]+-\\d+)\\.pooler\\.supabase\\.com:6543"
                );
                Matcher matcher = poolerPattern.matcher(poolerUrl);
                
                if (matcher.find()) {
                    String region = matcher.group(2);
                    String directUrl = poolerUrl.replace(
                        region + ".pooler.supabase.com:6543",
                        "db." + region + ".supabase.co:5432"
                    );
                    return directUrl;
                }
                
                if (poolerUrl.contains(":6543")) {
                    return poolerUrl.replace(":6543", ":5432");
                }
                
                return null;
            }
            
            private void updateDatasourceUrl(ConfigurableEnvironment environment, String newUrl) {
                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", newUrl);
                
                MapPropertySource propertySource = new MapPropertySource(
                    "poolerFallbackDatasource", props
                );
                
                environment.getPropertySources().addFirst(propertySource);
            }
            
            private String buildFailFastErrorMessage(String currentUrl) {
                StringBuilder msg = new StringBuilder();
                msg.append("\n");
                msg.append("================================================================================\n");
                msg.append("FAIL-FAST: DATABASE CONNECTION COMPLETELY FAILED\n");
                msg.append("================================================================================\n");
                msg.append("Both Supabase pooler and direct database connection attempts have failed.\n\n");
                msg.append("Connection attempts made:\n");
                msg.append("  1. Supabase Pooler endpoint (port 6543) - FAILED: Connection refused\n");
                msg.append("  2. Direct database endpoint (port 5432) - FAILED: Connection refused\n\n");
                msg.append("Possible causes:\n");
                msg.append("  • Supabase project is paused or suspended\n");
                msg.append("  • Database instance is not running\n");
                msg.append("  • Network connectivity issues to Supabase infrastructure\n");
                msg.append("  • Firewall blocking outbound connections on ports 6543 and 5432\n");
                msg.append("  • Invalid database credentials or URL\n\n");
                msg.append("Recommended actions:\n");
                msg.append("  1. Check Supabase project status: https://app.supabase.com/project/[project-id]/settings/general\n");
                msg.append("  2. Verify project is not paused (free tier projects pause after inactivity)\n");
                msg.append("  3. Check pooler configuration: https://app.supabase.com/project/[project-id]/settings/database\n");
                msg.append("  4. Verify DATABASE_URL environment variable is correct\n");
                msg.append("  5. Test connectivity: telnet [hostname] 6543\n");
                msg.append("  6. Review Supabase status page: https://status.supabase.com\n\n");
                msg.append("Current URL: ").append(maskPassword(currentUrl)).append("\n");
                msg.append("================================================================================\n");
                return msg.toString();
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
            
            private String maskPassword(String url) {
                if (url == null) return null;
                return url.replaceAll("password=[^&\\s]+", "password=****");
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
