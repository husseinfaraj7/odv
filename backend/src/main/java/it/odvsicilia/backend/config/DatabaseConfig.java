package it.odvsicilia.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

@Configuration
public class DatabaseConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    
    // Supabase connection pool optimization constants
    private static final int MAXIMUM_POOL_SIZE = 8; // Supabase free tier has 60 concurrent connections max
    private static final int MINIMUM_IDLE = 2;
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration MAX_LIFETIME = Duration.ofMinutes(30);
    private static final Duration LEAK_DETECTION_THRESHOLD = Duration.ofSeconds(60);

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            logger.warn("DATABASE_URL environment variable is not set or is empty. Using default H2 database configuration.");
            return createH2DataSource();
        }

        try {
            return createSupabaseDataSource(databaseUrl);
        } catch (Exception e) {
            logger.error("Failed to create Supabase DataSource, falling back to H2: {}", e.getMessage(), e);
            return createH2DataSource();
        }
    }

    private DataSource createSupabaseDataSource(String databaseUrl) {
        validateSupabaseUrl(databaseUrl);
        
        String jdbcUrl = convertToJdbcFormat(databaseUrl);
        DatabaseConnectionDetails connectionDetails = parseConnectionDetails(databaseUrl);
        
        HikariConfig config = new HikariConfig();
        
        // Connection settings
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(connectionDetails.username);
        config.setPassword(connectionDetails.password);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Connection pool optimization for Supabase
        config.setMaximumPoolSize(MAXIMUM_POOL_SIZE);
        config.setMinimumIdle(MINIMUM_IDLE);
        config.setConnectionTimeout(CONNECTION_TIMEOUT.toMillis());
        config.setIdleTimeout(IDLE_TIMEOUT.toMillis());
        config.setMaxLifetime(MAX_LIFETIME.toMillis());
        config.setLeakDetectionThreshold(LEAK_DETECTION_THRESHOLD.toMillis());
        
        // Connection validation
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(Duration.ofSeconds(5).toMillis());
        
        // Pool name for monitoring
        config.setPoolName("SupabaseHikariPool");
        
        // Additional PostgreSQL optimizations
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
        
        logger.info("Created Supabase DataSource with optimized connection pool settings");
        return new HikariDataSource(config);
    }
    
    private DataSource createH2DataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");
        config.setMaximumPoolSize(10);
        config.setPoolName("H2HikariPool");
        
        return new HikariDataSource(config);
    }
    
    private void validateSupabaseUrl(String databaseUrl) {
        if (!databaseUrl.startsWith("postgresql://") && !databaseUrl.startsWith("postgres://")) {
            throw new IllegalArgumentException(
                "DATABASE_URL must start with 'postgresql://' or 'postgres://'. " +
                "Current format: " + (databaseUrl.length() > 20 ? databaseUrl.substring(0, 20) + "..." : databaseUrl)
            );
        }
        
        try {
            URI uri = new URI(databaseUrl);
            
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                throw new IllegalArgumentException("DATABASE_URL is missing host information");
            }
            
            if (uri.getPath() == null || uri.getPath().length() <= 1) {
                throw new IllegalArgumentException("DATABASE_URL is missing database name");
            }
            
            if (uri.getUserInfo() == null || !uri.getUserInfo().contains(":")) {
                throw new IllegalArgumentException("DATABASE_URL is missing username or password information");
            }
            
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("DATABASE_URL is malformed: " + e.getMessage(), e);
        }
    }
    
    private String convertToJdbcFormat(String databaseUrl) {
        if (databaseUrl.startsWith("jdbc:")) {
            logger.info("DATABASE_URL is already in JDBC format");
            return databaseUrl;
        }
        
        String jdbcUrl = "jdbc:" + databaseUrl;
        
        // Handle postgres:// vs postgresql:// schemes
        if (databaseUrl.startsWith("postgres://")) {
            jdbcUrl = jdbcUrl.replace("jdbc:postgres://", "jdbc:postgresql://");
        }
        
        logger.info("Converted DATABASE_URL to JDBC format: {}",
            jdbcUrl.replaceAll("://[^:]+:[^@]+@", "://***:***@")); // Log safely without credentials
        
        return jdbcUrl;
    }
    
    private DatabaseConnectionDetails parseConnectionDetails(String databaseUrl) {
        try {
            URI uri = new URI(databaseUrl);
            
            if (uri.getUserInfo() == null) {
                throw new IllegalArgumentException("No user information found in DATABASE_URL");
            }
            
            String[] userInfo = uri.getUserInfo().split(":");
            
            if (userInfo.length != 2) {
                throw new IllegalArgumentException("Invalid user information format in DATABASE_URL. Expected format: username:password");
            }
            
            String username = userInfo[0];
            String password = userInfo[1];
            
            if (username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username is empty in DATABASE_URL");
            }
            
            if (password.trim().isEmpty()) {
                throw new IllegalArgumentException("Password is empty in DATABASE_URL");
            }
            
            return new DatabaseConnectionDetails(username, password);
            
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Failed to parse connection details from DATABASE_URL: " + e.getMessage(), e);
        }
    }
    
    private static class DatabaseConnectionDetails {
        final String username;
        final String password;
        
        DatabaseConnectionDetails(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}