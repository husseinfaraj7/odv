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

    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long INITIAL_RETRY_DELAY_MS = 2000; // 2 seconds
    private static final double RETRY_BACKOFF_MULTIPLIER = 1.5;

    /**
     * Gets the transformed database URL from environment with proper JDBC formatting.
     * Supports both DATABASE_URL and separate DATABASE_HOST/PORT/NAME variables.
     * 
     * Supported DATABASE_URL formats:
     * - postgresql://user:password@host:port/database
     * - jdbc:postgresql://user:password@host:port/database
     * - postgres://user:password@host:port/database (auto-converted to postgresql)
     * - jdbc:postgres://user:password@host:port/database (auto-converted)
     */
    @Bean
    @Primary
    public String transformedDatabaseUrl() {
        // Check if separate environment variables are provided
        String host = System.getenv("DATABASE_HOST");
        String port = System.getenv("DATABASE_PORT");
        String name = System.getenv("DATABASE_NAME");
        
        if (host != null && !host.isEmpty()) {
            String dbPort = (port != null && !port.isEmpty()) ? port : "5432";
            String dbName = (name != null && !name.isEmpty()) ? name : "postgres";
            return String.format("jdbc:postgresql://%s:%s/%s", host, dbPort, dbName);
        }
        
        // Fall back to DATABASE_URL
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
        String host = System.getenv("DATABASE_HOST");
        String user = System.getenv("DATABASE_USER");
        String password = System.getenv("DATABASE_PASSWORD");
        
        // If separate variables are provided, use them directly
        if (host != null && !host.isEmpty() && user != null && password != null) {
            logger.info("Using separate DATABASE_HOST/USER/PASSWORD environment variables");
            try {
                DataSource dataSource = createDataSourceWithRetry(() -> 
                    createSupabaseDataSourceFromEnvVars(host, user, password)
                );
                testDatabaseConnection(dataSource);
                return dataSource;
            } catch (Exception e) {
                logger.error("Failed to create DataSource from env vars after retries, falling back to H2: {}", e.getMessage());
                return createH2DataSource();
            }
        }
        
        // Fall back to DATABASE_URL parsing
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            logger.warn("DATABASE_URL environment variable is not set. Falling back to H2 database.");
            return createH2DataSource();
        }

        try {
            DataSource dataSource = createDataSourceWithRetry(() -> 
                createSupabaseDataSource(databaseUrl)
            );
            testDatabaseConnection(dataSource);
            return dataSource;
        } catch (Exception e) {
            logger.error("Failed to create Supabase DataSource after retries, falling back to H2: {}", e.getMessage());
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

    private boolean isAuthenticationError(Exception e) {
        if (e instanceof SQLException) {
            SQLException sqlEx = (SQLException) e;
            String sqlState = sqlEx.getSQLState();
            String errorMessage = sqlEx.getMessage().toLowerCase();
            return "28000".equals(sqlState) || // Invalid authorization specification
                   "28P01".equals(sqlState) || // Invalid password
                   "28001".equals(sqlState) || // Invalid authorization
                   errorMessage.contains("authentication failed") ||
                   errorMessage.contains("password authentication failed");
        }
        String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return errorMessage.contains("authentication failed") ||
               errorMessage.contains("password authentication failed");
    }

    private boolean isNetworkConnectivityError(Exception e) {
        if (e instanceof SQLException) {
            SQLException sqlEx = (SQLException) e;
            String sqlState = sqlEx.getSQLState();
            String errorMessage = sqlEx.getMessage().toLowerCase();
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
        String errorMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return errorMessage.contains("connection refused") ||
               errorMessage.contains("unreachable") ||
               errorMessage.contains("timeout") ||
               errorMessage.contains("no route to host") ||
               errorMessage.contains("network is unreachable") ||
               errorMessage.contains("connection timed out") ||
               errorMessage.contains("failed to initialize pool");
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

    private DataSource createSupabaseDataSourceFromEnvVars(String host, String user, String password) {
        String port = System.getenv("DATABASE_PORT");
        String name = System.getenv("DATABASE_NAME");
        
        String dbPort = (port != null && !port.isEmpty()) ? port : "5432";
        String dbName = (name != null && !name.isEmpty()) ? name : "postgres";
        
        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s?sslmode=require&ssl=true", host, dbPort, dbName);
        
        logger.info("Creating DataSource with JDBC URL: {}", jdbcUrl);
        
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        
        config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        config.setConnectionTimeout(CONNECTION_TIMEOUT.toMillis());
        
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(Duration.ofSeconds(5).toMillis());
        config.setPoolName("SupabaseHikariPool");
        
        config.addDataSourceProperty("ssl", "true");
        config.addDataSourceProperty("sslmode", "require");
        
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

    private DataSource createSupabaseDataSource(String databaseUrl) {
        DatabaseConnectionDetails connectionDetails = parseConnectionDetails(databaseUrl);
        
        String jdbcUrl = connectionDetails.jdbcUrl;
        
        if (!jdbcUrl.contains("sslmode=")) {
            jdbcUrl += (jdbcUrl.contains("?") ? "&" : "?") + "sslmode=require&ssl=true";
        }
        
        logger.info("Final JDBC URL with SSL: {}", jdbcUrl);
        logger.info("Attempting to connect to database...");
        
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
        
        config.addDataSourceProperty("ssl", "true");
        config.addDataSourceProperty("sslmode", "require");
        
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
            String urlToParse = databaseUrl;
            
            logger.info("Original DATABASE_URL: {}", databaseUrl);
            
            // Remove jdbc: prefix if present for URI parsing
            if (urlToParse.startsWith("jdbc:")) {
                urlToParse = urlToParse.substring(5); // Remove "jdbc:"
                logger.info("After removing jdbc: prefix: {}", urlToParse);
            }
            
            // Also normalize postgres:// to postgresql://
            if (urlToParse.startsWith("postgres://")) {
                urlToParse = urlToParse.replace("postgres://", "postgresql://");
                logger.info("After normalizing to postgresql://: {}", urlToParse);
            }
            
            logger.info("Attempting to parse URI: {}", urlToParse);
            uri = new URI(urlToParse);
            logger.info("Successfully parsed URI. UserInfo: {}", uri.getUserInfo());
        } catch (URISyntaxException e) {
            logger.error("Failed to parse DATABASE_URL. Original: {}, Error: {}", databaseUrl, e.getMessage());
            logger.error("Make sure your DATABASE_URL format is: postgresql://username:password@host:port/database");
            logger.error("Or use separate environment variables: DATABASE_HOST, DATABASE_USER, DATABASE_PASSWORD");
            throw new IllegalArgumentException("Invalid DATABASE_URL format", e);
        }
        
        if (uri.getUserInfo() == null) {
            logger.error("No user authentication information found in DATABASE_URL");
            logger.error("Expected format: postgresql://username:password@host:port/database");
            throw new IllegalArgumentException("No user authentication information found in DATABASE_URL");
        }
        
        String[] userInfo = uri.getUserInfo().split(":", 2);
        if (userInfo.length != 2) {
            logger.error("Invalid user information format. Expected username:password, got: {}", uri.getUserInfo());
            throw new IllegalArgumentException("Invalid user information format in DATABASE_URL");
        }
        
        // Use raw credentials directly without any encoding/decoding
        String username = userInfo[0];
        String password = userInfo[1];
        
        logger.info("Parsed username: {}, password length: {}", username, password.length());
        
        if (username.trim().isEmpty() || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Username or password is empty in DATABASE_URL");
        }
        
        String host = uri.getHost();
        int port = uri.getPort() != -1 ? uri.getPort() : 5432;
        String database = uri.getPath().startsWith("/") ? uri.getPath().substring(1) : uri.getPath();
        
        logger.info("=== PARSED DATABASE CONNECTION DETAILS ===");
        logger.info("Host: {}", host);
        logger.info("Port: {}", port);
        logger.info("Database: {}", database);
        logger.info("Username: {}", username);
        logger.info("==========================================");
        
        // Reconstruct as jdbc:postgresql://host:port/database (no credentials in URL)
        String cleanJdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
        
        if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
            cleanJdbcUrl += "?" + uri.getQuery();
        }
        
        logger.info("Reconstructed clean JDBC URL: {}", cleanJdbcUrl);
        
        return new DatabaseConnectionDetails(username, password, cleanJdbcUrl);
    }
    
    private static class DatabaseConnectionDetails {
        public final String username;
        public final String password;
        public final String jdbcUrl;
        
        public DatabaseConnectionDetails(String username, String password, String jdbcUrl) {
            this.username = username;
            this.password = password;
            this.jdbcUrl = jdbcUrl;
        }
    }

    /**
     * Creates a DataSource with retry logic to handle Supabase cold starts.
     * Supabase databases can pause after inactivity and take time to wake up.
     * This method retries connection attempts with exponential backoff.
     */
    private DataSource createDataSourceWithRetry(DataSourceSupplier supplier) {
        Exception lastException = null;
        long retryDelay = INITIAL_RETRY_DELAY_MS;
        
        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                if (attempt > 1) {
                    logger.info("Retry attempt {}/{} after {}ms delay...", attempt, MAX_RETRY_ATTEMPTS, retryDelay);
                    Thread.sleep(retryDelay);
                    retryDelay = (long) (retryDelay * RETRY_BACKOFF_MULTIPLIER);
                } else {
                    logger.info("Initial connection attempt to database...");
                }
                
                DataSource dataSource = supplier.get();
                
                // Test the connection immediately
                try (Connection conn = dataSource.getConnection()) {
                    if (conn.isValid(5)) {
                        logger.info("✓ Successfully connected to database on attempt {}/{}", attempt, MAX_RETRY_ATTEMPTS);
                        return dataSource;
                    }
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Retry interrupted", e);
            } catch (Exception e) {
                lastException = e;
                
                if (isNetworkConnectivityError(e)) {
                    logger.warn("Network connectivity issue on attempt {}/{}: {} - Database may be waking up from pause", 
                        attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                } else if (isAuthenticationError(e)) {
                    logger.error("Authentication error - no point in retrying: {}", e.getMessage());
                    throw new RuntimeException("Authentication failed", e);
                } else {
                    logger.warn("Connection attempt {}/{} failed: {}", attempt, MAX_RETRY_ATTEMPTS, e.getMessage());
                }
                
                if (attempt == MAX_RETRY_ATTEMPTS) {
                    logger.error("All {} connection attempts failed. Database may be paused or unreachable.", MAX_RETRY_ATTEMPTS);
                }
            }
        }
        
        throw new RuntimeException("Failed to connect to database after " + MAX_RETRY_ATTEMPTS + " attempts", lastException);
    }

    @FunctionalInterface
    private interface DataSourceSupplier {
        DataSource get() throws Exception;
    }
}
