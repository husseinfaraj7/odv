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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    // URL encoding patterns and constants
    private static final Pattern JDBC_URL_PATTERN = Pattern.compile(
        "^(jdbc:)?([a-z]+)://([^:/@]+)(?::([^/@]+))?@([^:/?]+)(?::(\\d+))?/([^?]+)(?:\\?(.+))?$"
    );
    
    private static final Pattern URL_ENCODE_REQUIRED_PATTERN = Pattern.compile("[^A-Za-z0-9\\-_.~]");
    private static final String UTF8_CHARSET = "UTF-8";
    
    // Common problematic characters mapping for URL encoding
    private static final Map<Character, String> COMMON_ENCODING_MAP = new HashMap<>();
    static {
        COMMON_ENCODING_MAP.put('@', "%40");
        COMMON_ENCODING_MAP.put('#', "%23");
        COMMON_ENCODING_MAP.put('$', "%24");
        COMMON_ENCODING_MAP.put('%', "%25");
        COMMON_ENCODING_MAP.put('^', "%5E");
        COMMON_ENCODING_MAP.put('&', "%26");
        COMMON_ENCODING_MAP.put('*', "%2A");
        COMMON_ENCODING_MAP.put(' ', "%20");
        COMMON_ENCODING_MAP.put('+', "%2B");
        COMMON_ENCODING_MAP.put('/', "%2F");
        COMMON_ENCODING_MAP.put('?', "%3F");
        COMMON_ENCODING_MAP.put('=', "%3D");
        COMMON_ENCODING_MAP.put(':', "%3A");
        COMMON_ENCODING_MAP.put('!', "%21");
        COMMON_ENCODING_MAP.put('(', "%28");
        COMMON_ENCODING_MAP.put(')', "%29");
        COMMON_ENCODING_MAP.put('[', "%5B");
        COMMON_ENCODING_MAP.put(']', "%5D");
        COMMON_ENCODING_MAP.put('{', "%7B");
        COMMON_ENCODING_MAP.put('}', "%7D");
        COMMON_ENCODING_MAP.put(';', "%3B");
        COMMON_ENCODING_MAP.put('\'', "%27");
        COMMON_ENCODING_MAP.put('"', "%22");
        COMMON_ENCODING_MAP.put('<', "%3C");
        COMMON_ENCODING_MAP.put('>', "%3E");
        COMMON_ENCODING_MAP.put('|', "%7C");
        COMMON_ENCODING_MAP.put('\\', "%5C");
        COMMON_ENCODING_MAP.put('`', "%60");
        COMMON_ENCODING_MAP.put('~', "%7E");
    }

    /**
     * Gets the transformed database URL from environment with proper JDBC formatting.
     * This method consolidates URL transformation logic previously in DatabaseUrlConfig.
     * 
     * @return the transformed database URL in JDBC format, or H2 default if not set
     */
    @Bean
    @Primary
    public String transformedDatabaseUrl() {
        String rawUrl = System.getenv("DATABASE_URL");
        
        if (rawUrl == null || rawUrl.isEmpty()) {
            // Return default H2 URL if DATABASE_URL is not set (development)
            return "jdbc:h2:mem:testdb";
        }
        
        // Check if the URL already has the jdbc: prefix
        if (rawUrl.startsWith("jdbc:")) {
            return rawUrl;
        }
        
        // Prepend jdbc: to the PostgreSQL URL
        return "jdbc:" + rawUrl;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            logger.warn("DATABASE_URL environment variable is not set or is empty. Using default H2 database configuration.");
            return createH2DataSource();
        }

        try {
            DataSource dataSource = createSupabaseDataSource(databaseUrl);
            
            // Test the connection during bean creation to validate URL encoding fixes
            testDatabaseConnection(dataSource, databaseUrl);
            
            return dataSource;
        } catch (Exception e) {
            logger.error("Failed to create Supabase DataSource, falling back to H2: {}", e.getMessage(), e);
            return createH2DataSource();
        }
    }

    /**
     * Tests database connectivity during application startup to validate PostgreSQL connection
     * and URL encoding fixes. This method performs a connection test and logs detailed results.
     * 
     * @param dataSource the DataSource to test
     * @param databaseUrl the original DATABASE_URL for logging context
     */
    private void testDatabaseConnection(DataSource dataSource, String databaseUrl) {
        logger.info("=== DATABASE CONNECTION VALIDATION ===");
        logger.info("Testing PostgreSQL connectivity with URL-encoded DATABASE_URL during application startup...");
        logger.info("Database URL (masked): {}", maskCredentials(databaseUrl));
        
        try (Connection connection = dataSource.getConnection()) {
            // Test basic connectivity
            if (connection.isValid(10)) {
                logger.info("✓ Database connection test SUCCESSFUL");
                logger.info("✓ PostgreSQL connectivity validated");
                logger.info("✓ URL encoding fixes are working correctly");
                
                // Log connection metadata for verification
                String databaseProductName = connection.getMetaData().getDatabaseProductName();
                String databaseProductVersion = connection.getMetaData().getDatabaseProductVersion();
                String driverName = connection.getMetaData().getDriverName();
                String driverVersion = connection.getMetaData().getDriverVersion();
                String url = connection.getMetaData().getURL();
                
                logger.info("Database Product: {} {}", databaseProductName, databaseProductVersion);
                logger.info("JDBC Driver: {} {}", driverName, driverVersion);
                logger.info("Connected URL (masked): {}", maskCredentials(url));
                
                // Test a simple query to validate full functionality
                try (var statement = connection.prepareStatement("SELECT 1 as test_value");
                     var resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        int testValue = resultSet.getInt("test_value");
                        if (testValue == 1) {
                            logger.info("✓ Database query execution test SUCCESSFUL");
                        } else {
                            logger.warn("⚠ Database query execution test returned unexpected value: {}", testValue);
                        }
                    }
                }
                
            } else {
                logger.error("✗ Database connection test FAILED - connection is not valid");
                throw new SQLException("Database connection validation failed - connection.isValid() returned false");
            }
            
        } catch (SQLException e) {
            logger.error("=== DATABASE CONNECTION TEST FAILED ===");
            logger.error("✗ PostgreSQL connection test FAILED during application startup");
            logger.error("Error Type: {}", e.getClass().getSimpleName());
            logger.error("SQL State: {}", e.getSQLState());
            logger.error("Error Code: {}", e.getErrorCode());
            logger.error("Error Message: {}", e.getMessage());
            
            // Analyze and log specific error categories
            analyzeConnectionFailure(e, databaseUrl);
            
            throw new RuntimeException("Database connection test failed during application startup. " +
                    "This indicates issues with the DATABASE_URL, network connectivity, or authentication. " +
                    "Check the error details above for specific failure reasons.", e);
        }
        
        logger.info("=== DATABASE CONNECTION VALIDATION COMPLETE ===");
    }

    /**
     * Analyzes SQL exceptions to provide specific guidance on connection failures,
     * distinguishing between encoding issues and other database problems.
     * 
     * @param e the SQLException to analyze
     * @param databaseUrl the DATABASE_URL being tested
     */
    private void analyzeConnectionFailure(SQLException e, String databaseUrl) {
        String sqlState = e.getSQLState();
        String errorMessage = e.getMessage().toLowerCase();
        int errorCode = e.getErrorCode();
        
        logger.error("=== DETAILED CONNECTION FAILURE ANALYSIS ===");
        
        // Authentication-related errors
        if (isAuthenticationError(sqlState, errorCode, errorMessage)) {
            logger.error("🔐 AUTHENTICATION FAILURE DETECTED:");
            logger.error("   • SQL State: {} indicates authentication/authorization issues", sqlState);
            logger.error("   • This suggests problems with username/password in DATABASE_URL");
            logger.error("   • Possible causes:");
            logger.error("     - Incorrect username or password");
            logger.error("     - Username/password not properly URL-encoded");
            logger.error("     - Special characters in credentials not encoded");
            logger.error("     - Database user permissions issues");
            
            if (containsSpecialCharacters(databaseUrl)) {
                logger.error("   • CRITICAL: DATABASE_URL contains special characters that may require encoding");
                logger.error("   • Consider using DatabaseConfig.validateAndFixDatabaseUrl() to auto-encode credentials");
            }
        }
        
        // URL parsing/format errors  
        else if (isUrlParsingError(sqlState, errorCode, errorMessage)) {
            logger.error("🔗 URL PARSING/FORMAT ERROR DETECTED:");
            logger.error("   • SQL State: {} indicates URL format or parsing issues", sqlState);
            logger.error("   • Error Code: {} suggests malformed connection string", errorCode);
            logger.error("   • Possible causes:");
            logger.error("     - Malformed DATABASE_URL structure");
            logger.error("     - Invalid characters in URL components");
            logger.error("     - Encoding issues with special characters");
            logger.error("     - Missing required URL components (host, port, database)");
            
            logger.error("   • SOLUTION: Verify DATABASE_URL format and encoding");
            logger.error("     Expected format: postgresql://username:password@host:port/database");
        }
        
        // Network connectivity errors
        else if (isNetworkConnectivityError(sqlState, errorCode, errorMessage)) {
            logger.error("🌐 NETWORK CONNECTIVITY ERROR DETECTED:");
            logger.error("   • SQL State: {} indicates network connection issues", sqlState);
            logger.error("   • Error Code: {} suggests host unreachable or timeout", errorCode);
            logger.error("   • Possible causes:");
            logger.error("     - Database host is unreachable");
            logger.error("     - Network firewall blocking connection");
            logger.error("     - Incorrect host or port in DATABASE_URL");
            logger.error("     - Database server is down or not accepting connections");
            logger.error("     - DNS resolution failure for database host");
            
            logger.error("   • TROUBLESHOOTING:");
            logger.error("     - Verify host and port are correct");
            logger.error("     - Check network connectivity to database host");
            logger.error("     - Confirm database server is running and accessible");
        }
        
        // SSL/TLS errors
        else if (isSslError(sqlState, errorCode, errorMessage)) {
            logger.error("🔒 SSL/TLS CONNECTION ERROR DETECTED:");
            logger.error("   • SSL/TLS configuration or certificate issues");
            logger.error("   • Possible causes:");
            logger.error("     - SSL certificate validation failure");
            logger.error("     - SSL/TLS protocol mismatch");
            logger.error("     - Missing SSL configuration parameters");
        }
        
        // Database-specific errors
        else if (isDatabaseError(sqlState, errorCode, errorMessage)) {
            logger.error("🗃️  DATABASE-SPECIFIC ERROR DETECTED:");
            logger.error("   • SQL State: {} indicates database-level issues", sqlState);
            logger.error("   • Possible causes:");
            logger.error("     - Database does not exist");
            logger.error("     - Database is not accessible");
            logger.error("     - PostgreSQL server configuration issues");
            logger.error("     - Database user lacks connection privileges");
        }
        
        // General/unknown errors
        else {
            logger.error("❓ UNCLASSIFIED CONNECTION ERROR:");
            logger.error("   • SQL State: {} (unrecognized error category)", sqlState);
            logger.error("   • Error Code: {} (refer to PostgreSQL documentation)", errorCode);
            logger.error("   • Review the full error message above for additional details");
        }
        
        // Always provide encoding guidance for URLs with special characters
        if (containsSpecialCharacters(databaseUrl)) {
            logger.error("");
            logger.error("⚠️  ENCODING NOTICE:");
            logger.error("   Your DATABASE_URL contains special characters that may need proper encoding.");
            logger.error("   Use DatabaseConfig.validateAndFixDatabaseUrl() for automatic encoding.");
            logger.error("   Example: @ should be %40, # should be %23, etc.");
        }
        
        logger.error("=== END CONNECTION FAILURE ANALYSIS ===");
    }

    private boolean isAuthenticationError(String sqlState, int errorCode, String errorMessage) {
        // PostgreSQL authentication error SQL states and patterns
        return "28000".equals(sqlState) || // Invalid authorization specification
               "28P01".equals(sqlState) || // Invalid password
               "28001".equals(sqlState) || // Invalid authorization
               errorMessage.contains("authentication failed") ||
               errorMessage.contains("password authentication failed") ||
               errorMessage.contains("role") && errorMessage.contains("does not exist") ||
               errorMessage.contains("invalid user") ||
               errorMessage.contains("access denied");
    }

    private boolean isUrlParsingError(String sqlState, int errorCode, String errorMessage) {
        return errorMessage.contains("invalid connection url") ||
               errorMessage.contains("malformed url") ||
               errorMessage.contains("illegal character") ||
               errorMessage.contains("invalid port") ||
               errorMessage.contains("connection string") ||
               errorMessage.contains("url format");
    }

    private boolean isNetworkConnectivityError(String sqlState, int errorCode, String errorMessage) {
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

    private boolean containsSpecialCharacters(String url) {
        if (url == null) return false;
        return URL_ENCODE_REQUIRED_PATTERN.matcher(url).find();
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
        
        parseAndValidateUrl(databaseUrl, "validation");
    }
    
    private URI parseAndValidateUrl(String databaseUrl, String operation) {
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
            
            return uri;
            
        } catch (URISyntaxException e) {
            handleUrlParsingException(databaseUrl, e, operation);
            // This line will never be reached due to the exception being thrown above
            return null;
        }
    }
    
    private void handleUrlParsingException(String databaseUrl, URISyntaxException e, String operation) {
        String errorMessage = e.getMessage();
        String reason = e.getReason();
        int errorIndex = e.getIndex();
        
        // Detect URL encoding issues
        if (isEncodingRelatedError(errorMessage, reason, databaseUrl)) {
            handleEncodingError(databaseUrl, e, operation, errorIndex);
        } else if (isMalformedUrlStructure(errorMessage, reason)) {
            handleMalformedUrlError(databaseUrl, e, operation);
        } else {
            handleGenericUrlParsingError(databaseUrl, e, operation);
        }
    }
    
    private boolean isEncodingRelatedError(String errorMessage, String reason, String databaseUrl) {
        // Check for encoding-related error patterns
        Pattern encodingErrorPattern = Pattern.compile(
            "Illegal character|Invalid character|Malformed escape pair|" +
            "Expected digit|URLDecoder|percent|encoding|%[^0-9A-Fa-f]", 
            Pattern.CASE_INSENSITIVE
        );
        
        if (encodingErrorPattern.matcher(errorMessage).find() || 
            (reason != null && encodingErrorPattern.matcher(reason).find())) {
            return true;
        }
        
        // Check for unencoded special characters in credentials
        if (databaseUrl.contains("@") && databaseUrl.contains(":")) {
            String credentialsPart = DatabaseConfig.extractCredentialsPart(databaseUrl);
            if (credentialsPart != null && containsUnencodedSpecialChars(credentialsPart)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean isMalformedUrlStructure(String errorMessage, String reason) {
        Pattern structuralErrorPattern = Pattern.compile(
            "Expected authority|Illegal character in authority|Illegal character in path|" +
            "Illegal character in query|Invalid port|scheme|authority", 
            Pattern.CASE_INSENSITIVE
        );
        
        return structuralErrorPattern.matcher(errorMessage).find() || 
               (reason != null && structuralErrorPattern.matcher(reason).find());
    }
    
    private static String extractCredentialsPart(String databaseUrl) {
        try {
            int atIndex = databaseUrl.indexOf('@');
            int schemeEndIndex = databaseUrl.indexOf("://");
            
            if (schemeEndIndex != -1 && atIndex > schemeEndIndex) {
                return databaseUrl.substring(schemeEndIndex + 3, atIndex);
            }
        } catch (Exception e) {
            LoggerFactory.getLogger(DatabaseConfig.class).debug("Failed to extract credentials part for encoding analysis", e);
        }
        return null;
    }
    
    private boolean containsUnencodedSpecialChars(String credentialsPart) {
        // Common unencoded special characters that should be URL-encoded
        Pattern unEncodedCharsPattern = Pattern.compile("[@ #$%^&*()+=\\[\\]{}|\\\\;:\"'<>?,./`~]");
        return unEncodedCharsPattern.matcher(credentialsPart).find();
    }
    
    private void handleEncodingError(String databaseUrl, URISyntaxException e, String operation, int errorIndex) {
        logger.error("URL encoding error during {} at position {}: {}", operation, errorIndex, e.getMessage());
        
        String problematicPart = "";
        if (errorIndex >= 0 && errorIndex < databaseUrl.length()) {
            int start = Math.max(0, errorIndex - 5);
            int end = Math.min(databaseUrl.length(), errorIndex + 10);
            problematicPart = databaseUrl.substring(start, end);
        }
        
        String detailedMessage = String.format(
            "DATABASE_URL contains improperly encoded characters during %s.\n" +
            "Error at position %d: %s\n" +
            "Problematic portion: '%s'\n\n" +
            "CRITICAL: Special characters in database credentials must be properly URL-encoded!\n\n" +
            "Common encoding issues (especially in passwords):\n" +
            "• '@' characters must be encoded as '%%40'\n" +
            "• '#' characters must be encoded as '%%23'\n" +
            "• '$' characters must be encoded as '%%24'\n" +
            "• '%%' characters must be encoded as '%%25'\n" +
            "• '^' characters must be encoded as '%%5E'\n" +
            "• '&' characters must be encoded as '%%26'\n" +
            "• '*' characters must be encoded as '%%2A'\n" +
            "• Space characters must be encoded as '%%20'\n" +
            "• '+' characters must be encoded as '%%2B'\n" +
            "• '/' characters must be encoded as '%%2F'\n" +
            "• '?' characters must be encoded as '%%3F'\n" +
            "• '=' characters must be encoded as '%%3D'\n\n" +
            "SOLUTION: Use the DatabaseConfig utility methods for automatic URL encoding!\n" +
            "Example usage:\n" +
            "  String encodedPassword = DatabaseConfig.urlEncode(\"P@ssw0rd#123\");\n" +
            "  // Result: \"P%%40ssw0rd%%23123\"\n\n" +
            "Example: If your password is 'P@ssw0rd#123', it should be encoded as 'P%%40ssw0rd%%23123'\n" +
            "Full example URL: postgresql://user:P%%40ssw0rd%%23123@host:5432/database\n" +
            "Or use DatabaseConfig.urlEncode() to handle encoding automatically.\n\n" +
            "Password encoding is the most common cause of this error. Ensure your password\n" +
            "is properly URL-encoded before constructing the DATABASE_URL.\n\n" +
            "Original parsing error: %s",
            operation, errorIndex, e.getReason() != null ? e.getReason() : "URI syntax error",
            problematicPart.isEmpty() ? "Unable to identify" : problematicPart,
            e.getMessage()
        );
        
        logger.error("Detailed encoding error: {}", detailedMessage);
        throw new IllegalArgumentException(detailedMessage, e);
    }
    
    private void handleMalformedUrlError(String databaseUrl, URISyntaxException e, String operation) {
        logger.error("Malformed URL structure during {}: {}", operation, e.getMessage());
        
        String detailedMessage = String.format(
            "DATABASE_URL has malformed structure during %s.\n" +
            "Error: %s\n\n" +
            "Expected URL format:\n" +
            "postgresql://username:password@host:port/database\n" +
            "or\n" +
            "postgres://username:password@host:port/database\n\n" +
            "Ensure your URL includes:\n" +
            "• Valid scheme (postgresql:// or postgres://)\n" +
            "• Username and password separated by ':'\n" +
            "• Host and port (port optional, defaults to 5432)\n" +
            "• Database name after the final '/'\n\n" +
            "Original error: %s",
            operation, e.getReason() != null ? e.getReason() : "Structural error",
            e.getMessage()
        );
        
        logger.error("Detailed malformed URL error: {}", detailedMessage);
        throw new IllegalArgumentException(detailedMessage, e);
    }
    
    private void handleGenericUrlParsingError(String databaseUrl, URISyntaxException e, String operation) {
        logger.error("Generic URL parsing error during {}: {}", operation, e.getMessage());
        
        String detailedMessage = String.format(
            "DATABASE_URL parsing failed during %s.\n" +
            "Error: %s\n" +
            "Error position: %d\n\n" +
            "Please verify:\n" +
            "1. URL format is correct: postgresql://username:password@host:port/database\n" +
            "2. Special characters in credentials are properly URL-encoded\n" +
            "3. No illegal characters are present in the URL\n" +
            "4. All required components (host, database name) are included\n\n" +
            "For encoding help, refer to URL encoding guidelines or use online URL encoders.\n" +
            "Original error: %s",
            operation, 
            e.getReason() != null ? e.getReason() : "Unknown parsing error",
            e.getIndex(),
            e.getMessage()
        );
        
        logger.error("Detailed generic URL parsing error: {}", detailedMessage);
        throw new IllegalArgumentException(detailedMessage, e);
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
        logger.info("Starting DATABASE_URL parsing process during application startup");
        
        // Log the sanitized URL structure for debugging
        String maskedUrl = maskCredentials(databaseUrl);
        logger.info("DATABASE_URL structure: {}", maskedUrl);
        
        // Analyze for special characters before processing
        SpecialCharacterAnalysis specialCharAnalysis = analyzeSpecialCharacters(databaseUrl);
        if (specialCharAnalysis.hasSpecialCharacters()) {
            logger.warn("Special characters detected in DATABASE_URL that may require encoding:");
            for (String detectedChar : specialCharAnalysis.getDetectedCharacters()) {
                logger.warn("  - Character '{}' found at {} section", detectedChar, specialCharAnalysis.getCharacterLocation(detectedChar));
            }
            logger.warn("Total special characters detected: {}", specialCharAnalysis.getTotalCount());
        } else {
            logger.info("No problematic special characters detected in DATABASE_URL");
        }
        
        // Validate URL before parsing
        ValidationResult validationResult = validateDatabaseUrl(databaseUrl);
        if (!validationResult.isSuccess()) {
            // Log validation issues with specific error messages
            logger.error("DATABASE_URL validation failed during application startup:");
            for (String errorMessage : validationResult.getErrorMessages()) {
                logger.error("  - {}", errorMessage);
            }
            
            // Attempt to provide helpful suggestions
            List<String> suggestions = suggestUrlFixes(databaseUrl);
            if (!suggestions.isEmpty()) {
                logger.info("Suggested fixes for DATABASE_URL encoding issues:");
                for (String suggestion : suggestions) {
                    logger.info("  • {}", suggestion);
                }
            }
            
            // If there are encoding issues, provide the fixed URL
            try {
                String encodedUrl = encodeDatabaseCredentials(databaseUrl);
                logger.info("Automatically encoded DATABASE_URL: {}", maskCredentials(encodedUrl));
                logger.info("Consider using the encoded URL above to prevent parsing failures");
            } catch (Exception e) {
                logger.warn("Could not provide automatic encoding suggestion: {}", e.getMessage());
            }
            
            throw new IllegalArgumentException("DATABASE_URL validation failed: " + 
                String.join(", ", validationResult.getErrorMessages()));
        } else {
            logger.info("DATABASE_URL format validation passed successfully");
        }
        
        URI uri;
        boolean encodingApplied = false;
        String originalUrl = databaseUrl;
        
        try {
            // First attempt: try parsing without encoding
            String targetUrl = databaseUrl.startsWith("jdbc:") ? databaseUrl : "jdbc:" + databaseUrl;
            uri = new URI(targetUrl);
            logger.info("Successfully parsed DATABASE_URL without encoding on first attempt");
        } catch (URISyntaxException originalException) {
            logger.warn("Initial DATABASE_URL parsing failed: {}", originalException.getMessage());
            logger.info("Attempting automatic credential encoding to handle special characters");
            
            try {
                // Second attempt: encode the URL to handle special characters in credentials
                String encodedUrl = encodeUrl(databaseUrl);
                uri = new URI(encodedUrl);
                encodingApplied = true;
                logger.warn("URL encoding was applied to DATABASE_URL due to special characters");
                logger.info("Successfully parsed DATABASE_URL after applying automatic credential encoding");
                
                // Log encoding details
                String originalMasked = maskCredentials(originalUrl);
                String encodedMasked = maskCredentials(encodedUrl);
                logger.info("URL transformation applied:");
                logger.info("  Original: {}", originalMasked);
                logger.info("  Encoded:  {}", encodedMasked);
            } catch (URISyntaxException retryException) {
                logger.error("DATABASE_URL parsing failed even after automatic encoding");
                logger.error("Original parsing error: {}", originalException.getMessage());
                logger.error("Encoding retry error: {}", retryException.getMessage());
                
                // Enhanced error handling with character-specific recommendations
                handleUrlParsingWithEncodingSuggestions(databaseUrl, originalException, retryException);
                
                // This line will never be reached due to the exception being thrown above
                return null;
            }
        }
        
        // Log encoding status
        if (encodingApplied) {
            logger.warn("ENCODING STATUS: Automatic URL encoding was applied during startup");
            logger.warn("This indicates the DATABASE_URL contained unencoded special characters");
            logger.warn("Consider pre-encoding your DATABASE_URL to avoid runtime encoding overhead");
        } else {
            logger.info("ENCODING STATUS: No encoding was required - DATABASE_URL was properly formatted");
        }
        
        // Perform startup health check on parsed URL
        boolean healthCheckPassed = performStartupHealthCheck(uri, encodingApplied);
        if (healthCheckPassed) {
            logger.info("Startup health check: DATABASE_URL format validation PASSED");
        } else {
            logger.warn("Startup health check: DATABASE_URL format validation FAILED - potential future encoding issues");
        }
        
        if (uri.getUserInfo() == null) {
            logger.error("No user authentication information found in DATABASE_URL");
            throw new IllegalArgumentException("No user information found in DATABASE_URL");
        }
        
        String[] userInfo = uri.getUserInfo().split(":");
        
        if (userInfo.length != 2) {
            logger.error("Invalid user information format in DATABASE_URL - expected username:password");
            throw new IllegalArgumentException("Invalid user information format in DATABASE_URL. Expected format: username:password");
        }
        
        String username, password;
        
        if (encodingApplied) {
            // If encoding was applied, decode the credentials
            try {
                username = urlDecode(userInfo[0]);
                password = urlDecode(userInfo[1]);
                logger.info("Successfully decoded username and password after automatic encoding");
            } catch (Exception e) {
                logger.error("Failed to decode credentials after encoding: {}", e.getMessage());
                throw new IllegalArgumentException("Failed to decode database credentials", e);
            }
        } else {
            // If no encoding was needed, use credentials as-is
            username = userInfo[0];
            password = userInfo[1];
            logger.info("Using credentials as-is (no decoding required)");
        }
        
        if (username.trim().isEmpty()) {
            logger.error("Username is empty in DATABASE_URL");
            throw new IllegalArgumentException("Username is empty in DATABASE_URL");
        }
        
        if (password.trim().isEmpty()) {
            logger.error("Password is empty in DATABASE_URL");
            throw new IllegalArgumentException("Password is empty in DATABASE_URL");
        }
        
        // Log successful parsing summary
        logger.info("DATABASE_URL parsing completed successfully:");
        logger.info("  - Host: {}", uri.getHost());
        logger.info("  - Port: {}", uri.getPort() != -1 ? uri.getPort() : "default");
        logger.info("  - Database: {}", uri.getPath().substring(1));
        logger.info("  - Username: {}", username);
        logger.info("  - Password: [MASKED]");
        logger.info("  - Encoding applied: {}", encodingApplied ? "YES" : "NO");
        
        return new DatabaseConnectionDetails(username, password);
    }
    
    /**
     * Encodes a database connection URL by properly URL-encoding the username and password
     * segments while preserving the overall URL structure.
     * 
     * @param connectionUrl the database connection URL to encode
     * @return the properly encoded database URL
     * @throws IllegalArgumentException if the URL format is invalid or unsupported
     */
    public static String encodeUrl(String connectionUrl) {
        if (connectionUrl == null || connectionUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Connection URL cannot be null or empty");
        }

        String normalizedUrl = normalizeUrl(connectionUrl);
        
        // Check if encoding is needed
        if (!requiresEncoding(normalizedUrl)) {
            return normalizedUrl;
        }

        return applyUrlEncoding(normalizedUrl);
    }

    /**
     * URL encodes a string using UTF-8 charset.
     * 
     * @param value the string to encode
     * @return the encoded string
     * @throws RuntimeException if UTF-8 encoding is not supported or encoding fails
     */
    public static String urlEncode(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            return URLEncoder.encode(value, UTF8_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to URL encode value due to unsupported UTF-8 encoding: " + 
                    e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to URL encode value: " + e.getMessage(), e);
        }
    }

    /**
     * URL decodes a string using UTF-8 charset.
     * 
     * @param value the string to decode
     * @return the decoded string
     * @throws RuntimeException if UTF-8 encoding is not supported or decoding fails
     */
    public static String urlDecode(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            return URLDecoder.decode(value, UTF8_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to URL decode value due to unsupported UTF-8 encoding: " + 
                    e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("URLDecoder: " + e.getMessage(), e);
        }
    }

    /**
     * Checks if the given URL contains characters that require URL encoding.
     * 
     * @param url the URL to check
     * @return true if encoding is required, false otherwise
     */
    private static boolean requiresEncoding(String url) {
        Matcher matcher = JDBC_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            // If we can't parse it, assume encoding might be needed
            return true;
        }

        String username = matcher.group(3);
        String password = matcher.group(4);

        if (username != null && URL_ENCODE_REQUIRED_PATTERN.matcher(username).find()) {
            return true;
        }

        if (password != null && URL_ENCODE_REQUIRED_PATTERN.matcher(password).find()) {
            return true;
        }

        return false;
    }

    /**
     * Applies URL encoding to username and password segments in the database URL.
     * 
     * @param url the URL to encode
     * @return the encoded URL
     */
    private static String applyUrlEncoding(String url) {
        Matcher matcher = JDBC_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid database URL format: " + maskCredentials(url));
        }

        String jdbcPrefix = matcher.group(1) != null ? matcher.group(1) : "";
        String protocol = matcher.group(2);
        String username = matcher.group(3);
        String password = matcher.group(4);
        String host = matcher.group(5);
        String port = matcher.group(6);
        String database = matcher.group(7);
        String queryParams = matcher.group(8);

        // Encode username and password
        String encodedUsername = username != null ? urlEncode(username) : null;
        String encodedPassword = password != null ? urlEncode(password) : null;

        // Reconstruct the URL
        StringBuilder encodedUrl = new StringBuilder();
        encodedUrl.append(jdbcPrefix).append(protocol).append("://");
        
        if (encodedUsername != null) {
            encodedUrl.append(encodedUsername);
            if (encodedPassword != null) {
                encodedUrl.append(":").append(encodedPassword);
            }
            encodedUrl.append("@");
        }
        
        encodedUrl.append(host);
        
        if (port != null) {
            encodedUrl.append(":").append(port);
        }
        
        encodedUrl.append("/").append(database);
        
        if (queryParams != null) {
            encodedUrl.append("?").append(queryParams);
        }

        return encodedUrl.toString();
    }

    /**
     * Normalizes the URL format by ensuring it has the proper JDBC prefix if it's a JDBC URL.
     * 
     * @param url the URL to normalize
     * @return the normalized URL
     */
    /**
     * Normalizes the URL format by ensuring it has the proper JDBC prefix if it's a JDBC URL.
     *
     * @param url the URL to normalize
     * @return the normalized URL
     */
    private static String normalizeUrl(String url) {
        String trimmed = url.trim();

        // If it starts with jdbc:, return as-is
        if (trimmed.startsWith("jdbc:")) {
            return trimmed;
        }

        // If it looks like a PostgreSQL URL without jdbc: prefix, add it
        if (trimmed.startsWith("postgresql://") || trimmed.startsWith("postgres://")) {
            String normalizedProtocol = trimmed.replace("postgres://", "postgresql://");
            return "jdbc:" + normalizedProtocol;
        }

        return trimmed;
    }

    /**
     * Validates and fixes a DATABASE_URL by automatically detecting and encoding special characters
     * in the username and password portions. This method parses the existing DATABASE_URL,
     * extracts credentials, encodes any special characters, and reconstructs a properly encoded URL.
     * 
     * @param databaseUrl the database URL to validate and fix
     * @return a properly encoded DATABASE_URL string
     * @throws IllegalArgumentException if the URL format is invalid
     */
    public static String validateAndFixDatabaseUrl(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("DATABASE_URL cannot be null or empty");
        }

        String normalizedUrl = normalizeUrl(databaseUrl.trim());
        
        // Parse the URL to extract components
        Matcher matcher = JDBC_URL_PATTERN.matcher(normalizedUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid DATABASE_URL format: " + maskCredentials(normalizedUrl));
        }

        String jdbcPrefix = matcher.group(1) != null ? matcher.group(1) : "";
        String protocol = matcher.group(2);
        String username = matcher.group(3);
        String password = matcher.group(4);
        String host = matcher.group(5);
        String port = matcher.group(6);
        String database = matcher.group(7);
        String queryParams = matcher.group(8);

        // Check if special character encoding is needed
        boolean usernameHasSpecialChars = hasSpecialCharacters(username);
        boolean passwordHasSpecialChars = hasSpecialCharacters(password);

        if (!usernameHasSpecialChars && !passwordHasSpecialChars) {
            // No encoding needed, return original URL
            return normalizedUrl;
        }

        // Encode credentials that contain special characters
        String encodedUsername = usernameHasSpecialChars ? 
            URLEncoder.encode(username, StandardCharsets.UTF_8) : username;
        String encodedPassword = passwordHasSpecialChars ? 
            URLEncoder.encode(password, StandardCharsets.UTF_8) : password;

        // Reconstruct the URL with properly encoded credentials
        StringBuilder fixedUrl = new StringBuilder();
        fixedUrl.append(jdbcPrefix).append(protocol).append("://");
        
        if (encodedUsername != null) {
            fixedUrl.append(encodedUsername);
            if (encodedPassword != null) {
                fixedUrl.append(":").append(encodedPassword);
            }
            fixedUrl.append("@");
        }
        
        fixedUrl.append(host);
        
        if (port != null) {
            fixedUrl.append(":").append(port);
        }
        
        fixedUrl.append("/").append(database);
        
        if (queryParams != null) {
            fixedUrl.append("?").append(queryParams);
        }

        return fixedUrl.toString();
    }

    /**
     * Enhanced URL parsing exception handler that provides specific character encoding recommendations.
     * 
     * @param rawUrl the original URL that failed to parse
     * @param originalException the first parsing exception
     * @param retryException the second parsing exception after encoding attempt
     */
    private void handleUrlParsingWithEncodingSuggestions(String rawUrl, URISyntaxException originalException, URISyntaxException retryException) {
        logger.error("Enhanced URL parsing error analysis for DATABASE_URL");
        
        // Analyze the raw URL for problematic characters
        String encodingRecommendations = analyzeAndGenerateEncodingRecommendations(rawUrl);
        
        String detailedMessage = String.format(
            "DATABASE_URL parsing failed with enhanced character analysis.\n\n" +
            "ORIGINAL ERROR: %s\n" +
            "RETRY ERROR AFTER ENCODING: %s\n\n" +
            "CHARACTER ENCODING ANALYSIS:\n" +
            "%s\n\n" +
            "SOLUTION STEPS:\n" +
            "1. Identify problematic characters in your DATABASE_URL\n" +
            "2. Apply the specific encoding recommendations above\n" +
            "3. Reconstruct your DATABASE_URL with encoded characters\n" +
            "4. Alternatively, use DatabaseConfig.urlEncode() for automatic encoding\n\n" +
            "EXAMPLE:\n" +
            "Original: postgresql://user:P@ssw0rd#123:special@host:5432/db\n" +
            "Encoded:  postgresql://user:P%%40ssw0rd%%23123%%3Aspecial@host:5432/db\n\n" +
            "For automatic encoding, use:\n" +
            "String encoded = DatabaseConfig.urlEncode(\"your-password-here\");\n",
            originalException.getMessage(),
            retryException.getMessage(),
            encodingRecommendations.isEmpty() ? "No specific problematic characters detected in standard positions." : encodingRecommendations
        );
        
        logger.error("Enhanced URL parsing error details: {}", detailedMessage);
        throw new IllegalArgumentException(detailedMessage, originalException);
    }

    /**
     * Analyzes a URL string for problematic characters and generates specific encoding recommendations.
     * 
     * @param url the URL to analyze
     * @return a formatted string with character-specific encoding recommendations
     */
    private String analyzeAndGenerateEncodingRecommendations(String url) {
        if (url == null || url.isEmpty()) {
            return "URL is null or empty.";
        }
        
        StringBuilder recommendations = new StringBuilder();
        ProblematicCharacterAnalysis analysis = scanUrlForProblematicCharacters(url);
        
        if (analysis.hasProblematicCharacters()) {
            recommendations.append("DETECTED PROBLEMATIC CHARACTERS:\n");
            
            for (ProblematicCharacter problematicChar : analysis.getProblematicCharacters()) {
                recommendations.append(String.format(
                    "• Character '%s' at position %d should be encoded as '%s'\n",
                    problematicChar.character,
                    problematicChar.position,
                    problematicChar.encodedForm
                ));
            }
            
            recommendations.append("\nCOMMON ENCODING REFERENCE:\n");
            recommendations.append("@ → %40    # → %23    $ → %24    % → %25\n");
            recommendations.append("^ → %5E    & → %26    * → %2A    + → %2B\n");
            recommendations.append("space → %20    : → %3A    / → %2F    ? → %3F\n");
            recommendations.append("= → %3D    [ → %5B    ] → %5D    { → %7B\n");
            recommendations.append("} → %7D    | → %7C    \\ → %5C    ; → %3B\n");
            recommendations.append("\" → %22    ' → %27    < → %3C    > → %3E\n");
            recommendations.append(", → %2C    . → %2E    ` → %60    ~ → %7E\n");
        } else {
            recommendations.append("No obvious problematic characters detected in standard credential positions.\n");
            recommendations.append("The error may be due to overall URL structure issues or complex character combinations.\n");
            recommendations.append("Consider using DatabaseConfig.urlEncode() for comprehensive encoding.\n");
        }
        
        return recommendations.toString();
    }

    /**
     * Scans the URL for problematic characters that require encoding, focusing on credential sections.
     * 
     * @param url the URL to scan
     * @return analysis results containing detected problematic characters
     */
    private ProblematicCharacterAnalysis scanUrlForProblematicCharacters(String url) {
        ProblematicCharacterAnalysis analysis = new ProblematicCharacterAnalysis();
        
        // Define characters that commonly cause URL parsing issues
        String problematicChars = "@#$%^&*+=[]{}|\\;:\"'<>?,./`~ ";
        
        // Try to identify credential section for focused analysis
        String credentialSection = extractCredentialSection(url);
        
        if (credentialSection != null) {
            // Analyze credential section specifically
            analyzeStringForProblematicCharacters(credentialSection, analysis, getCredentialSectionOffset(url));
        } else {
            // Fall back to analyzing the entire URL
            analyzeStringForProblematicCharacters(url, analysis, 0);
        }
        
        return analysis;
    }

    /**
     * Analyzes a string for problematic characters and adds them to the analysis.
     * 
     * @param text the text to analyze
     * @param analysis the analysis object to populate
     * @param baseOffset the base offset for position calculations
     */
    private void analyzeStringForProblematicCharacters(String text, ProblematicCharacterAnalysis analysis, int baseOffset) {
        // Characters that commonly require URL encoding
        char[] problematicChars = {'@', '#', '$', '%', '^', '&', '*', '+', '=', '[', ']', '{', '}', '|', '\\', ';', ':', '"', '\'', '<', '>', '?', ',', '.', '/', '`', '~', ' '};
        
        for (int i = 0; i < text.length(); i++) {
            char currentChar = text.charAt(i);
            
            for (char problemChar : problematicChars) {
                if (currentChar == problemChar) {
                    String encodedForm = getUrlEncodingForCharacter(currentChar);
                    analysis.addProblematicCharacter(new ProblematicCharacter(
                        String.valueOf(currentChar),
                        baseOffset + i,
                        encodedForm
                    ));
                    break;
                }
            }
        }
    }

    /**
     * Gets the URL encoding for a specific character.
     * 
     * @param character the character to encode
     * @return the URL-encoded form
     */
    private String getUrlEncodingForCharacter(char character) {
        switch (character) {
            case '@': return "%40";
            case '#': return "%23";
            case '$': return "%24";
            case '%': return "%25";
            case '^': return "%5E";
            case '&': return "%26";
            case '*': return "%2A";
            case '+': return "%2B";
            case '=': return "%3D";
            case '[': return "%5B";
            case ']': return "%5D";
            case '{': return "%7B";
            case '}': return "%7D";
            case '|': return "%7C";
            case '\\': return "%5C";
            case ';': return "%3B";
            case ':': return "%3A";
            case '"': return "%22";
            case '\'': return "%27";
            case '<': return "%3C";
            case '>': return "%3E";
            case '?': return "%3F";
            case ',': return "%2C";
            case '.': return "%2E";
            case '/': return "%2F";
            case '`': return "%60";
            case '~': return "%7E";
            case ' ': return "%20";
            default:
                // For any other character, use URLEncoder
                try {
                    return URLEncoder.encode(String.valueOf(character), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return "%" + Integer.toHexString(character).toUpperCase();
                }
        }
    }

    /**
     * Extracts the credential section from a database URL.
     * 
     * @param url the URL to analyze
     * @return the credential section (username:password) or null if not found
     */
    private String extractCredentialSection(String url) {
        try {
            // Look for the pattern: scheme://username:password@host
            int schemeEnd = url.indexOf("://");
            if (schemeEnd == -1) return null;
            
            int atIndex = url.indexOf('@', schemeEnd + 3);
            if (atIndex == -1) return null;
            
            return url.substring(schemeEnd + 3, atIndex);
        } catch (Exception e) {
            logger.debug("Failed to extract credential section: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets the offset of the credential section within the URL.
     * 
     * @param url the URL to analyze
     * @return the offset position
     */
    private int getCredentialSectionOffset(String url) {
        try {
            int schemeEnd = url.indexOf("://");
            return schemeEnd == -1 ? 0 : schemeEnd + 3;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Analyzes DATABASE_URL for special characters that may cause encoding issues.
     * 
     * @param databaseUrl the database URL to analyze
     * @return analysis results containing detected special characters
     */
    private SpecialCharacterAnalysis analyzeSpecialCharacters(String databaseUrl) {
        SpecialCharacterAnalysis analysis = new SpecialCharacterAnalysis();
        
        if (databaseUrl == null || databaseUrl.isEmpty()) {
            return analysis;
        }
        
        // Extract credential section for focused analysis
        String credentialSection = extractCredentialSection(databaseUrl);
        
        if (credentialSection != null) {
            // Analyze credentials specifically
            analyzeCredentialCharacters(credentialSection, analysis);
        }
        
        return analysis;
    }
    
    /**
     * Analyzes credential section for special characters.
     * 
     * @param credentials the credential string to analyze
     * @param analysis the analysis object to populate
     */
    private void analyzeCredentialCharacters(String credentials, SpecialCharacterAnalysis analysis) {
        // Characters that commonly cause URL parsing issues
        char[] problematicChars = {'@', '#', '$', '%', '^', '&', '*', '+', '=', ':', '/', '?', ' ', '[', ']', '{', '}', '|', '\\', ';', '"', '\'', '<', '>', ',', '.', '`', '~'};
        
        int colonIndex = credentials.indexOf(':');
        String username = colonIndex > 0 ? credentials.substring(0, colonIndex) : credentials;
        String password = colonIndex > 0 && colonIndex < credentials.length() - 1 ? 
                         credentials.substring(colonIndex + 1) : "";
        
        // Analyze username
        for (char problematicChar : problematicChars) {
            if (username.indexOf(problematicChar) >= 0) {
                analysis.addCharacter(String.valueOf(problematicChar), "username");
            }
        }
        
        // Analyze password
        for (char problematicChar : problematicChars) {
            if (password.indexOf(problematicChar) >= 0) {
                analysis.addCharacter(String.valueOf(problematicChar), "password");
            }
        }
    }
    
    /**
     * Performs a startup health check on the parsed DATABASE_URL to validate format
     * and identify potential future encoding issues.
     * 
     * @param uri the parsed URI
     * @param encodingWasApplied whether encoding was applied during parsing
     * @return true if health check passed, false otherwise
     */
    private boolean performStartupHealthCheck(URI uri, boolean encodingWasApplied) {
        logger.info("Performing startup health check on DATABASE_URL format");
        
        boolean healthCheckPassed = true;
        List<String> healthIssues = new ArrayList<>();
        
        // Check 1: Verify essential components are present
        if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
            healthIssues.add("Missing or empty host component");
            healthCheckPassed = false;
        } else {
            logger.info("✓ Host component validation passed: {}", uri.getHost());
        }
        
        if (uri.getPath() == null || uri.getPath().length() <= 1) {
            healthIssues.add("Missing or empty database name in path");
            healthCheckPassed = false;
        } else {
            logger.info("✓ Database path validation passed: {}", uri.getPath());
        }
        
        if (uri.getUserInfo() == null || !uri.getUserInfo().contains(":")) {
            healthIssues.add("Missing or malformed user credentials");
            healthCheckPassed = false;
        } else {
            logger.info("✓ User credentials format validation passed");
        }
        
        // Check 2: Port validation
        if (uri.getPort() == -1) {
            logger.info("✓ Using default PostgreSQL port (5432)");
        } else if (uri.getPort() < 1 || uri.getPort() > 65535) {
            healthIssues.add("Invalid port number: " + uri.getPort());
            healthCheckPassed = false;
        } else {
            logger.info("✓ Port validation passed: {}", uri.getPort());
        }
        
        // Check 3: Scheme validation
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("jdbc:postgresql") && !scheme.equals("postgresql") && !scheme.equals("postgres"))) {
            healthIssues.add("Invalid or missing scheme - expected postgresql:// or postgres://");
            healthCheckPassed = false;
        } else {
            logger.info("✓ Scheme validation passed: {}", scheme);
        }
        
        // Check 4: Encoding consistency validation
        if (encodingWasApplied) {
            logger.warn("⚠ Encoding was applied during parsing - this may indicate future encoding issues");
            logger.warn("⚠ Consider pre-encoding your DATABASE_URL to ensure consistency");
            // This is a warning, not a failure
        }
        
        // Check 5: URL structure integrity
        try {
            String reconstructedUrl = reconstructUrlFromUri(uri);
            logger.info("✓ URL structure integrity check passed");
        } catch (Exception e) {
            healthIssues.add("URL structure integrity check failed: " + e.getMessage());
            healthCheckPassed = false;
        }
        
        // Log health check results
        if (healthCheckPassed) {
            logger.info("DATABASE_URL startup health check: ALL CHECKS PASSED");
            logger.info("URL format is valid and should not cause parsing issues");
        } else {
            logger.error("DATABASE_URL startup health check: ISSUES DETECTED");
            for (String issue : healthIssues) {
                logger.error("  ✗ {}", issue);
            }
            logger.error("These issues may cause database connection failures");
        }
        
        // Additional early detection logging
        if (encodingWasApplied) {
            logger.warn("EARLY DETECTION ALERT: URL encoding was required during startup");
            logger.warn("This suggests the original DATABASE_URL had encoding issues");
            logger.warn("Future recommendation: Use pre-encoded DATABASE_URL values");
        }
        
        return healthCheckPassed;
    }
    
    /**
     * Reconstructs a URL string from a parsed URI for integrity checking.
     * 
     * @param uri the URI to reconstruct
     * @return reconstructed URL string
     */
    private String reconstructUrlFromUri(URI uri) {
        StringBuilder reconstructed = new StringBuilder();
        
        if (uri.getScheme() != null) {
            reconstructed.append(uri.getScheme()).append(":");
        }
        
        if (uri.getHost() != null) {
            reconstructed.append("//");
            if (uri.getUserInfo() != null) {
                reconstructed.append(uri.getUserInfo()).append("@");
            }
            reconstructed.append(uri.getHost());
            if (uri.getPort() != -1) {
                reconstructed.append(":").append(uri.getPort());
            }
        }
        
        if (uri.getPath() != null) {
            reconstructed.append(uri.getPath());
        }
        
        if (uri.getQuery() != null) {
            reconstructed.append("?").append(uri.getQuery());
        }
        
        if (uri.getFragment() != null) {
            reconstructed.append("#").append(uri.getFragment());
        }
        
        return reconstructed.toString();
    }
    
    /**
     * Container class for special character analysis results.
     */
    private static class SpecialCharacterAnalysis {
        private final Map<String, String> detectedCharacters = new HashMap<>();
        
        void addCharacter(String character, String location) {
            detectedCharacters.put(character, location);
        }
        
        boolean hasSpecialCharacters() {
            return !detectedCharacters.isEmpty();
        }
        
        java.util.Set<String> getDetectedCharacters() {
            return detectedCharacters.keySet();
        }
        
        String getCharacterLocation(String character) {
            return detectedCharacters.getOrDefault(character, "unknown");
        }
        
        int getTotalCount() {
            return detectedCharacters.size();
        }
    }

    /**
     * Represents a problematic character found during URL analysis.
     */
    private static class ProblematicCharacter {
        final String character;
        final int position;
        final String encodedForm;
        
        ProblematicCharacter(String character, int position, String encodedForm) {
            this.character = character;
            this.position = position;
            this.encodedForm = encodedForm;
        }
    }

    /**
     * Container for problematic character analysis results.
     */
    private static class ProblematicCharacterAnalysis {
        private final java.util.List<ProblematicCharacter> problematicCharacters = new java.util.ArrayList<>();
        
        void addProblematicCharacter(ProblematicCharacter character) {
            problematicCharacters.add(character);
        }
        
        boolean hasProblematicCharacters() {
            return !problematicCharacters.isEmpty();
        }
        
        java.util.List<ProblematicCharacter> getProblematicCharacters() {
            return problematicCharacters;
        }
    }

    /**
     * Checks whether the given string contains special characters that require URL encoding.
     * This method specifically looks for characters like @, #, $, %, &, +, =, spaces,
     * and other characters that have special meaning in URLs.
     * 
     * @param credentials the username or password string to check
     * @return true if the string contains characters requiring URL encoding, false otherwise
     */
    public static boolean hasSpecialCharacters(String credentials) {
        if (credentials == null || credentials.isEmpty()) {
            return false;
        }

        // Pattern matches characters that require URL encoding in credentials:
        // @, #, $, %, &, +, =, spaces, and other URL-unsafe characters
        Pattern specialCharsPattern = Pattern.compile("[@ #$%&+=\\[\\]{}|\\\\;:\"'<>?,./`~^*()!]");
        return specialCharsPattern.matcher(credentials).find();
    }

    /**
     * Validates a database URL and returns detailed validation results including 
     * success status and specific error messages.
     * 
     * @param databaseUrl the database URL to validate
     * @return ValidationResult containing success status and error messages
     */
    public static ValidationResult validateDatabaseUrl(String databaseUrl) {
        List<String> errorMessages = new ArrayList<>();
        
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            errorMessages.add("DATABASE_URL cannot be null or empty");
            return new ValidationResult(false, errorMessages);
        }
        
        String normalizedUrl = normalizeUrl(databaseUrl.trim());
        
        // Basic URL structure validation
        if (!normalizedUrl.startsWith("jdbc:postgresql://") && 
            !normalizedUrl.startsWith("jdbc:postgres://") &&
            !normalizedUrl.startsWith("postgresql://") && 
            !normalizedUrl.startsWith("postgres://")) {
            errorMessages.add("DATABASE_URL must start with 'postgresql://', 'postgres://', " +
                            "'jdbc:postgresql://', or 'jdbc:postgres://'");
        }
        
        // Parse URL components
        Matcher matcher = JDBC_URL_PATTERN.matcher(normalizedUrl);
        if (!matcher.matches()) {
            errorMessages.add("DATABASE_URL format is invalid. Expected format: " +
                            "postgresql://username:password@host:port/database");
            return new ValidationResult(false, errorMessages);
        }
        
        String username = matcher.group(3);
        String password = matcher.group(4);
        String host = matcher.group(5);
        String database = matcher.group(7);
        
        // Validate required components
        if (username == null || username.trim().isEmpty()) {
            errorMessages.add("Username is missing or empty in DATABASE_URL");
        }
        
        if (password == null || password.trim().isEmpty()) {
            errorMessages.add("Password is missing or empty in DATABASE_URL");
        }
        
        if (host == null || host.trim().isEmpty()) {
            errorMessages.add("Host is missing or empty in DATABASE_URL");
        }
        
        if (database == null || database.trim().isEmpty()) {
            errorMessages.add("Database name is missing or empty in DATABASE_URL");
        }
        
        // Check for encoding issues
        if (username != null && hasSpecialCharacters(username)) {
            errorMessages.add("Username contains special characters that require URL encoding: '" + 
                            username + "'. Use encodeDatabaseCredentials() to fix this.");
        }
        
        if (password != null && hasSpecialCharacters(password)) {
            errorMessages.add("Password contains special characters that require URL encoding. " +
                            "Use encodeDatabaseCredentials() to fix this.");
        }
        
        // Test URI parsing
        try {
            new URI(normalizedUrl);
        } catch (URISyntaxException e) {
            errorMessages.add("DATABASE_URL syntax is invalid: " + e.getMessage() + 
                            ". This often indicates encoding issues with special characters.");
        }
        
        return new ValidationResult(errorMessages.isEmpty(), errorMessages);
    }
    
    /**
     * Validates a DATABASE_URL string parameter and uses the existing encodeDatabaseCredentials() 
     * utility method to verify the URL format is correctly encoded for PostgreSQL connection.
     * 
     * @param databaseUrl the DATABASE_URL string to validate
     * @return boolean indicating whether the URL is properly formatted for PostgreSQL connection
     */
    public static boolean validateDatabaseUrlFormat(String databaseUrl) {
        Logger validationLogger = LoggerFactory.getLogger(DatabaseConfig.class);
        
        validationLogger.info("Starting DATABASE_URL format validation");
        
        // Initial null/empty checks
        if (databaseUrl == null) {
            validationLogger.error("DATABASE_URL validation failed: URL is null");
            validationLogger.info("Required format: postgresql://username:password@host:port/database");
            return false;
        }
        
        if (databaseUrl.trim().isEmpty()) {
            validationLogger.error("DATABASE_URL validation failed: URL is empty");
            validationLogger.info("Required format: postgresql://username:password@host:port/database");
            return false;
        }
        
        String trimmedUrl = databaseUrl.trim();
        validationLogger.debug("Validating DATABASE_URL: {}", maskCredentials(trimmedUrl));
        
        // Check PostgreSQL scheme requirement
        if (!trimmedUrl.startsWith("postgresql://") && !trimmedUrl.startsWith("postgres://") && 
            !trimmedUrl.startsWith("jdbc:postgresql://") && !trimmedUrl.startsWith("jdbc:postgres://")) {
            validationLogger.error("DATABASE_URL validation failed: Invalid scheme. Must start with 'postgresql://', 'postgres://', 'jdbc:postgresql://', or 'jdbc:postgres://'");
            validationLogger.error("Current scheme: {}", 
                trimmedUrl.contains("://") ? trimmedUrl.substring(0, trimmedUrl.indexOf("://") + 3) : "No scheme found");
            validationLogger.info("PostgreSQL schemes accepted: postgresql://, postgres://, jdbc:postgresql://, jdbc:postgres://");
            return false;
        }
        
        try {
            // Test initial URL parsing to detect encoding issues
            validationLogger.debug("Testing initial URL parsing...");
            URI initialUri = new URI(trimmedUrl);
            validationLogger.debug("Initial URL parsing successful");
            
            // Validate that all required components are present
            if (initialUri.getHost() == null || initialUri.getHost().trim().isEmpty()) {
                validationLogger.error("DATABASE_URL validation failed: Missing host information");
                validationLogger.info("Ensure URL includes host: postgresql://username:password@HOST:port/database");
                return false;
            }
            
            if (initialUri.getPath() == null || initialUri.getPath().length() <= 1) {
                validationLogger.error("DATABASE_URL validation failed: Missing database name");
                validationLogger.info("Ensure URL includes database name: postgresql://username:password@host:port/DATABASE");
                return false;
            }
            
            if (initialUri.getUserInfo() == null) {
                validationLogger.error("DATABASE_URL validation failed: Missing credentials");
                validationLogger.info("Ensure URL includes credentials: postgresql://USERNAME:PASSWORD@host:port/database");
                return false;
            }
            
            if (!initialUri.getUserInfo().contains(":")) {
                validationLogger.error("DATABASE_URL validation failed: Invalid credential format");
                validationLogger.error("Credentials format: {}", initialUri.getUserInfo().replaceAll(".", "*"));
                validationLogger.info("Expected credential format: username:password");
                return false;
            }
            
        } catch (URISyntaxException initialException) {
            validationLogger.warn("Initial URL parsing failed, checking for encoding issues: {}", initialException.getMessage());
            validationLogger.debug("Parsing error reason: {}", initialException.getReason());
            validationLogger.debug("Error position: {}", initialException.getIndex());
            
            // Analyze the specific encoding issues
            if (analyzeEncodingIssues(trimmedUrl, initialException, validationLogger)) {
                try {
                    // Test if encodeDatabaseCredentials() can fix the URL
                    validationLogger.info("Attempting automatic URL encoding to fix validation issues...");
                    String encodedUrl = encodeDatabaseCredentials(trimmedUrl);
                    validationLogger.info("Automatic encoding successful");
                    validationLogger.debug("Encoded URL: {}", maskCredentials(encodedUrl));
                    
                    // Test parsing the encoded URL
                    URI encodedUri = new URI(encodedUrl);
                    validationLogger.info("Encoded URL parsing successful - URL format is valid after encoding");
                    
                    // Log the encoding differences for user information
                    logEncodingDifferences(trimmedUrl, encodedUrl, validationLogger);
                    
                    validationLogger.info("DATABASE_URL validation result: VALID (after encoding)");
                    validationLogger.info("Recommendation: Use the encodeDatabaseCredentials() method or manually encode special characters");
                    return true;
                    
                } catch (URISyntaxException encodedParsingException) {
                    validationLogger.error("DATABASE_URL validation failed: URL cannot be parsed even after encoding");
                    validationLogger.error("Original error: {}", initialException.getMessage());
                    validationLogger.error("Encoded URL parsing error: {}", encodedParsingException.getMessage());
                    validationLogger.info("The URL structure itself may be invalid beyond encoding issues");
                    return false;
                } catch (Exception encodingException) {
                    validationLogger.error("DATABASE_URL validation failed: Error during encoding process");
                    validationLogger.error("Encoding error: {}", encodingException.getMessage());
                    return false;
                }
            } else {
                validationLogger.error("DATABASE_URL validation failed: URL parsing failed due to structural issues");
                validationLogger.error("Parsing error: {}", initialException.getMessage());
                return false;
            }
        } catch (Exception unexpectedException) {
            validationLogger.error("DATABASE_URL validation failed: Unexpected error during validation");
            validationLogger.error("Unexpected error: {}", unexpectedException.getMessage(), unexpectedException);
            return false;
        }
        
        validationLogger.info("DATABASE_URL validation result: VALID (no encoding needed)");
        return true;
    }
    
    /**
     * Analyzes specific encoding issues in the DATABASE_URL and logs detailed information
     * about which characters need encoding and their proper percent-encoded equivalents.
     * 
     * @param databaseUrl the URL to analyze
     * @param exception the URISyntaxException that occurred during parsing
     * @param logger the logger to use for output
     * @return true if encoding issues were detected and can be fixed, false otherwise
     */
    private static boolean analyzeEncodingIssues(String databaseUrl, URISyntaxException exception, Logger logger) {
        logger.info("Analyzing DATABASE_URL for encoding issues...");
        
        boolean hasEncodingIssues = false;
        String errorMessage = exception.getMessage();
        String reason = exception.getReason();
        int errorIndex = exception.getIndex();
        
        // Check for encoding-related error patterns
        Pattern encodingErrorPattern = Pattern.compile(
            "Illegal character|Invalid character|Malformed escape pair|Expected digit|URLDecoder|percent|encoding|%[^0-9A-Fa-f]", 
            Pattern.CASE_INSENSITIVE
        );
        
        if (encodingErrorPattern.matcher(errorMessage).find() || 
            (reason != null && encodingErrorPattern.matcher(reason).find())) {
            
            logger.warn("Detected encoding-related parsing error");
            hasEncodingIssues = true;
            
            if (errorIndex >= 0 && errorIndex < databaseUrl.length()) {
                int contextStart = Math.max(0, errorIndex - 10);
                int contextEnd = Math.min(databaseUrl.length(), errorIndex + 10);
                String problematicContext = databaseUrl.substring(contextStart, contextEnd);
                char problematicChar = databaseUrl.charAt(errorIndex);
                
                logger.error("Encoding issue at position {}: character '{}'", errorIndex, problematicChar);
                logger.error("Context: '{}' (error at position {} in this excerpt)", problematicContext, errorIndex - contextStart);
                
                if (COMMON_ENCODING_MAP.containsKey(problematicChar)) {
                    String encodedValue = COMMON_ENCODING_MAP.get(problematicChar);
                    logger.info("Character '{}' should be encoded as '{}'", problematicChar, encodedValue);
                } else {
                    try {
                        String encodedChar = URLEncoder.encode(String.valueOf(problematicChar), StandardCharsets.UTF_8.name());
                        logger.info("Character '{}' should be encoded as '{}'", problematicChar, encodedChar);
                    } catch (UnsupportedEncodingException e) {
                        logger.warn("Could not determine encoding for character '{}'", problematicChar);
                    }
                }
            }
        }
        
        // Analyze credentials section for unencoded special characters
        String credentialsPart = extractCredentialsPart(databaseUrl);
        if (credentialsPart != null) {
            logger.debug("Analyzing credentials section for encoding issues");
            
            List<Character> problematicChars = new ArrayList<>();
            for (int i = 0; i < credentialsPart.length(); i++) {
                char c = credentialsPart.charAt(i);
                if (COMMON_ENCODING_MAP.containsKey(c)) {
                    problematicChars.add(c);
                }
            }
            
            if (!problematicChars.isEmpty()) {
                hasEncodingIssues = true;
                logger.warn("Found {} problematic characters in credentials that require encoding", problematicChars.size());
                
                for (char c : problematicChars) {
                    String encoded = COMMON_ENCODING_MAP.get(c);
                    logger.info("Character '{}' found in credentials - should be encoded as '{}'", c, encoded);
                }
                
                // Provide specific encoding recommendations
                logger.info("Encoding recommendations for credentials:");
                String[] credentials = credentialsPart.split(":");
                if (credentials.length == 2) {
                    String username = credentials[0];
                    String password = credentials[1];
                    
                    if (containsSpecialCharacters(username)) {
                        try {
                            String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8.name());
                            logger.info("Username '{}' should be encoded as '{}'", maskString(username), encodedUsername);
                        } catch (UnsupportedEncodingException e) {
                            logger.warn("Could not encode username");
                        }
                    }
                    
                    if (containsSpecialCharacters(password)) {
                        try {
                            String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8.name());
                            logger.info("Password '{}' should be encoded as '{}'", maskString(password), encodedPassword);
                        } catch (UnsupportedEncodingException e) {
                            logger.warn("Could not encode password");
                        }
                    }
                } else {
                    logger.warn("Could not parse username and password from credentials for individual encoding recommendations");
                }
            }
        }
        
        if (hasEncodingIssues) {
            logger.info("Summary: DATABASE_URL contains characters that require URL encoding");
            logger.info("Solution: Use DatabaseConfig.encodeDatabaseCredentials() to automatically fix these issues");
            logger.info("Common characters requiring encoding: @ # $ % ^ & * + / ? = : ! ( ) [ ] { } ; ' \" < > | \\ ` ~");
        } else {
            logger.debug("No obvious encoding issues detected in credentials section");
        }
        
        return hasEncodingIssues;
    }
    
    /**
     * Logs the differences between original and encoded URLs to help users understand
     * what encoding changes were applied.
     * 
     * @param originalUrl the original URL
     * @param encodedUrl the encoded URL
     * @param logger the logger to use for output
     */
    private static void logEncodingDifferences(String originalUrl, String encodedUrl, Logger logger) {
        if (!originalUrl.equals(encodedUrl)) {
            logger.info("URL encoding differences detected:");
            logger.debug("Original: {}", maskCredentials(originalUrl));
            logger.debug("Encoded:  {}", maskCredentials(encodedUrl));
            
            // Extract and compare credentials
            String originalCredentials = extractCredentialsPart(originalUrl);
            String encodedCredentials = extractCredentialsPart(encodedUrl);
            
            if (originalCredentials != null && encodedCredentials != null && !originalCredentials.equals(encodedCredentials)) {
                logger.info("Credentials encoding changes:");
                logger.info("Original credentials: {}", maskString(originalCredentials));
                logger.info("Encoded credentials:  {}", maskString(encodedCredentials));
                
                // Show specific character changes
                Map<String, String> changes = findCharacterChanges(originalCredentials, encodedCredentials);
                if (!changes.isEmpty()) {
                    logger.info("Specific character encodings applied:");
                    for (Map.Entry<String, String> change : changes.entrySet()) {
                        logger.info("  '{}' → '{}'", change.getKey(), change.getValue());
                    }
                }
            }
        } else {
            logger.debug("No encoding changes were necessary");
        }
    }
    
    /**
     * Finds specific character encoding changes between original and encoded strings.
     * 
     * @param original the original string
     * @param encoded the encoded string
     * @return map of original characters to their encoded equivalents
     */
    private static Map<String, String> findCharacterChanges(String original, String encoded) {
        Map<String, String> changes = new HashMap<>();
        
        for (Map.Entry<Character, String> entry : COMMON_ENCODING_MAP.entrySet()) {
            char originalChar = entry.getKey();
            String encodedSequence = entry.getValue();
            
            if (original.contains(String.valueOf(originalChar)) && encoded.contains(encodedSequence)) {
                changes.put(String.valueOf(originalChar), encodedSequence);
            }
        }
        
        return changes;
    }
    
    /**
     * Checks if a string contains characters that require URL encoding.
     * 
     * @param str the string to check
     * @return true if the string contains special characters, false otherwise
     */
    private static boolean containsSpecialCharacters(String str) {
        if (str == null) return false;
        return URL_ENCODE_REQUIRED_PATTERN.matcher(str).find();
    }
    
    /**
     * Masks a string for safe logging by replacing characters with asterisks,
     * keeping only the first and last characters visible.
     * 
     * @param str the string to mask
     * @return the masked string
     */
    private static String maskString(String str) {
        if (str == null || str.length() <= 2) {
            return "***";
        }
        return str.charAt(0) + "***" + str.charAt(str.length() - 1);
    }

    /**
     * Properly URL-encodes username and password components while preserving 
     * the JDBC URL structure.
     * 
     * @param databaseUrl the database URL to encode
     * @return properly encoded database URL
     * @throws IllegalArgumentException if URL format is invalid
     */
    public static String encodeDatabaseCredentials(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("DATABASE_URL cannot be null or empty");
        }
        
        String normalizedUrl = normalizeUrl(databaseUrl.trim());
        
        // Parse the URL to extract components
        Matcher matcher = JDBC_URL_PATTERN.matcher(normalizedUrl);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid DATABASE_URL format: " + maskCredentials(normalizedUrl));
        }
        
        String jdbcPrefix = matcher.group(1) != null ? matcher.group(1) : "";
        String protocol = matcher.group(2);
        String username = matcher.group(3);
        String password = matcher.group(4);
        String host = matcher.group(5);
        String port = matcher.group(6);
        String database = matcher.group(7);
        String queryParams = matcher.group(8);
        
        // URL encode only the credentials, preserving URL structure
        String encodedUsername = username != null ? urlEncode(username) : "";
        String encodedPassword = password != null ? urlEncode(password) : "";
        
        // Reconstruct the URL with properly encoded credentials
        StringBuilder encodedUrl = new StringBuilder();
        encodedUrl.append(jdbcPrefix).append(protocol).append("://");
        
        if (!encodedUsername.isEmpty()) {
            encodedUrl.append(encodedUsername);
            if (!encodedPassword.isEmpty()) {
                encodedUrl.append(":").append(encodedPassword);
            }
            encodedUrl.append("@");
        }
        
        encodedUrl.append(host);
        
        if (port != null && !port.isEmpty()) {
            encodedUrl.append(":").append(port);
        }
        
        encodedUrl.append("/").append(database);
        
        if (queryParams != null && !queryParams.isEmpty()) {
            encodedUrl.append("?").append(queryParams);
        }
        
        return encodedUrl.toString();
    }
    
    /**
     * Analyzes common problematic characters in the database URL and provides 
     * specific encoding suggestions.
     * 
     * @param databaseUrl the database URL to analyze
     * @return list of specific encoding suggestions
     */
    public static List<String> suggestUrlFixes(String databaseUrl) {
        List<String> suggestions = new ArrayList<>();
        
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            suggestions.add("Provide a valid DATABASE_URL in format: postgresql://username:password@host:port/database");
            return suggestions;
        }
        
        String normalizedUrl = normalizeUrl(databaseUrl.trim());
        
        // Extract credentials part for analysis
        DatabaseConfig config = new DatabaseConfig();
        String credentialsPart = config.extractCredentialsPart(normalizedUrl);
        if (credentialsPart != null) {
            // Analyze specific problematic characters
            for (char c : credentialsPart.toCharArray()) {
                if (COMMON_ENCODING_MAP.containsKey(c)) {
                    String encodedValue = COMMON_ENCODING_MAP.get(c);
                    suggestions.add(String.format("Character '%c' should be encoded as '%s'", c, encodedValue));
                }
            }
        }
        
        // Parse URL and check components
        try {
            Matcher matcher = JDBC_URL_PATTERN.matcher(normalizedUrl);
            if (matcher.matches()) {
                String username = matcher.group(3);
                String password = matcher.group(4);
                
                if (username != null && hasSpecialCharacters(username)) {
                    suggestions.add("Username contains special characters. Encoded username: '" + 
                                  urlEncode(username) + "'");
                }
                
                if (password != null && hasSpecialCharacters(password)) {
                    suggestions.add("Password contains special characters. Use encodeDatabaseCredentials() " +
                                  "method or manually encode special characters");
                    
                    // Count problematic characters in password
                    long problematicChars = password.chars()
                        .mapToObj(ch -> (char) ch)
                        .filter(COMMON_ENCODING_MAP::containsKey)
                        .count();
                    
                    if (problematicChars > 0) {
                        suggestions.add(String.format("Found %d special character(s) in password that need encoding", 
                                      problematicChars));
                    }
                }
                
                // Provide complete encoded URL example
                try {
                    String encodedUrl = encodeDatabaseCredentials(databaseUrl);
                    suggestions.add("Complete encoded URL: " + maskCredentials(encodedUrl));
                } catch (Exception e) {
                    suggestions.add("Could not generate complete encoded URL: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            suggestions.add("URL structure analysis failed: " + e.getMessage());
        }
        
        // General encoding guidance
        if (suggestions.isEmpty()) {
            suggestions.add("URL appears to be properly formatted");
        } else {
            suggestions.add("Use DatabaseConfig.encodeDatabaseCredentials() to automatically fix encoding issues");
        }
        
        return suggestions;
    }

    /**
     * Masks credentials in a URL for safe logging.
     * 
     * @param url the URL to mask
     * @return the URL with masked credentials
     */
    private static String maskCredentials(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("://[^:/@]+(?::[^/@]+)?@", "://***:***@");
    }
    
    /**
     * Represents the result of database URL validation with success status and error messages.
     */
    public static class ValidationResult {
        private final boolean success;
        private final List<String> errorMessages;
        
        public ValidationResult(boolean success, List<String> errorMessages) {
            this.success = success;
            this.errorMessages = new ArrayList<>(errorMessages != null ? errorMessages : new ArrayList<>());
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public List<String> getErrorMessages() {
            return new ArrayList<>(errorMessages);
        }
        
        @Override
        public String toString() {
            return "ValidationResult{" +
                   "success=" + success +
                   ", errorMessages=" + errorMessages +
                   '}';
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