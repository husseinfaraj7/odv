package it.odvsicilia.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    
    // Supabase connection pool optimization constants
    private static final int MAXIMUM_POOL_SIZE = 8;
    private static final int MINIMUM_IDLE = 2;
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration MAX_LIFETIME = Duration.ofMinutes(30);
    private static final Duration LEAK_DETECTION_THRESHOLD = Duration.ofSeconds(60);

    /**
     * Gets the transformed database URL from environment with proper JDBC formatting.
     */
    @Bean
    @Primary
    public String transformedDatabaseUrl() {
        String rawUrl = System.getenv("DATABASE_URL");
        
        if (rawUrl == null || rawUrl.isEmpty()) {
            return "jdbc:h2:mem:testdb";
        }
        
        if (rawUrl.startsWith("jdbc:")) {
            return rawUrl;
        }
        
        return "jdbc:" + rawUrl;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            logger.warn("DATABASE_URL environment variable is not set. Falling back to H2 database.");
            return createH2DataSource();
        }

        try {
            DataSource dataSource = createSupabaseDataSource(databaseUrl);
            testDatabaseConnection(dataSource);
            return dataSource;
        } catch (Exception e) {
            logger.error("Failed to create Supabase DataSource, falling back to H2: {}", e.getMessage());
            return createH2DataSource();
        }
    }

    /**
     * Tests database connectivity during application startup.
     */
    private void testDatabaseConnection(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.isValid(10)) {
                throw new SQLException("Database connection validation failed");
            }
        } catch (SQLException e) {
            logger.error("Database connection test failed: {}", e.getMessage());
            
            // Log essential error details for actual connectivity problems
            if (isAuthenticationError(e)) {
                logger.error("Authentication failure - check username/password in DATABASE_URL");
            } else if (isNetworkConnectivityError(e)) {
                logger.error("Network connectivity error - database host unreachable");
            }
            
            throw new RuntimeException("Database connection test failed during application startup", e);
        }
    }

    private boolean isAuthenticationError(SQLException e) {
        String sqlState = e.getSQLState();
        String errorMessage = e.getMessage().toLowerCase();
        return "28000".equals(sqlState) || // Invalid authorization specification
               "28P01".equals(sqlState) || // Invalid password
               "28001".equals(sqlState) || // Invalid authorization
               errorMessage.contains("authentication failed") ||
               errorMessage.contains("password authentication failed");
    }

    private boolean isNetworkConnectivityError(SQLException e) {
        String sqlState = e.getSQLState();
        String errorMessage = e.getMessage().toLowerCase();
        return "08000".equals(sqlState) || // Connection exception
               "08001".equals(sqlState) || // Cannot establish connection
               "08006".equals(sqlState) || // Connection failure
               errorMessage.contains("connection refused") ||
               errorMessage.contains("host unreachable") ||
               errorMessage.contains("timeout") ||
               errorMessage.contains("no route to host") ||
               errorMessage.contains("network is unreachable") ||
               errorMessage.contains("connection timed out");
    }

    private boolean isSslError(String sqlState, int errorCode, String errorMessage) {
        return errorMessage.contains("ssl") ||
               errorMessage.contains("certificate") ||
               errorMessage.contains("tls") ||
               errorMessage.contains("secure connection");
    }

    private boolean isDatabaseError(String sqlState, int errorCode, String errorMessage) {
        return "3D000".equals(sqlState) || // Invalid catalog/database name
               errorMessage.contains("database") && errorMessage.contains("does not exist") ||
               errorMessage.contains("invalid database") ||
               errorMessage.contains("database connection");
    }

    private static boolean containsSpecialCharacters(String url) {
        if (url == null) return false;
        return url.matches(".*[^A-Za-z0-9\\-_.~].*");
    }

    /**
     * Event listener that performs additional database connection validation
     * after the application context is fully initialized.
     */
    @EventListener
    public void onApplicationStartup(ContextRefreshedEvent event) {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl != null && !databaseUrl.trim().isEmpty()) {
            logger.info("=== POST-STARTUP DATABASE VALIDATION ===");
            logger.info("Performing additional database connectivity validation after application startup...");
            
            try {
                DataSource dataSource = event.getApplicationContext().getBean(DataSource.class);
                
                // Re-test connection to ensure everything is still working after full app initialization
                try (Connection connection = dataSource.getConnection()) {
                    if (connection.isValid(5)) {
                        logger.info("✓ Post-startup database connection validation SUCCESSFUL");
                        logger.info("✓ Application is ready with validated database connectivity");
                    } else {
                        logger.error("✗ Post-startup database connection validation FAILED");
                    }
                }
                
            } catch (Exception e) {
                logger.error("Post-startup database validation failed: {}", e.getMessage());
                logger.warn("Application started but database connectivity issues detected");
            }
            
            logger.info("=== POST-STARTUP DATABASE VALIDATION COMPLETE ===");
        }
    }

    private DataSource createSupabaseDataSource(String databaseUrl) {
        String jdbcUrl = convertToJdbcFormat(databaseUrl);
        DatabaseConnectionDetails connectionDetails = parseConnectionDetails(databaseUrl);
        
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(connectionDetails.username);
        config.setPassword(connectionDetails.password);
        config.setDriverClassName("org.postgresql.Driver");
        
        config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        config.setConnectionTimeout(CONNECTION_TIMEOUT.toMillis());
        
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(Duration.ofSeconds(5).toMillis());
        config.setPoolName("SupabaseHikariPool");
        
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        return new HikariDataSource(config);
    }
    
    private DataSource createH2DataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        config.setConnectionTimeout(CONNECTION_TIMEOUT.toMillis());
        config.setConnectionTestQuery("SELECT 1");
        config.setPoolName("H2HikariPool");
        
        logger.info("Created H2 in-memory DataSource");
        return new HikariDataSource(config);
    }
    
    private String convertToJdbcFormat(String databaseUrl) {
        if (databaseUrl.startsWith("jdbc:")) {
            return databaseUrl;
        }
        
        String jdbcUrl = "jdbc:" + databaseUrl;
        
        if (databaseUrl.startsWith("postgres://")) {
            jdbcUrl = jdbcUrl.replace("jdbc:postgres://", "jdbc:postgresql://");
        }
        
        return jdbcUrl;
    }
    
    private DatabaseConnectionDetails parseConnectionDetails(String databaseUrl) {
        URI uri;
        try {
            String targetUrl = databaseUrl.startsWith("jdbc:") ? databaseUrl : "jdbc:" + databaseUrl;
            uri = new URI(targetUrl);
        } catch (URISyntaxException e) {
            logger.error("Failed to parse DATABASE_URL: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid DATABASE_URL format", e);
        }
        
        if (uri.getUserInfo() == null) {
            throw new IllegalArgumentException("No user authentication information found in DATABASE_URL");
        }
        
        String[] userInfo = uri.getUserInfo().split(":", 2);
        if (userInfo.length != 2) {
            throw new IllegalArgumentException("Invalid user information format in DATABASE_URL");
        }
        
        // Use raw credentials directly without any encoding/decoding
        String username = userInfo[0];
        String password = userInfo[1];
        
        if (username.trim().isEmpty() || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Username or password is empty in DATABASE_URL");
        }
        
        return new DatabaseConnectionDetails(username, password);
    }
    
    private static class DatabaseConnectionDetails {
        public final String username;
        public final String password;
        
        public DatabaseConnectionDetails(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
