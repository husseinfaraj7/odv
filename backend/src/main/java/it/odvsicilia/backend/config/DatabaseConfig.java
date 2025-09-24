package it.odvsicilia.backend.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
            String credentialsPart = extractCredentialsPart(databaseUrl);
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
    
    private String extractCredentialsPart(String databaseUrl) {
        try {
            int atIndex = databaseUrl.indexOf('@');
            int schemeEndIndex = databaseUrl.indexOf("://");
            
            if (schemeEndIndex != -1 && atIndex > schemeEndIndex) {
                return databaseUrl.substring(schemeEndIndex + 3, atIndex);
            }
        } catch (Exception e) {
            logger.debug("Failed to extract credentials part for encoding analysis", e);
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
            "SOLUTION: Use the DatabaseUrlUtils utility class for automatic URL encoding!\n" +
            "Example usage:\n" +
            "  String encodedPassword = DatabaseUrlUtils.urlEncode(\"P@ssw0rd#123\");\n" +
            "  // Result: \"P%%40ssw0rd%%23123\"\n\n" +
            "Example: If your password is 'P@ssw0rd#123', it should be encoded as 'P%%40ssw0rd%%23123'\n" +
            "Full example URL: postgresql://user:P%%40ssw0rd%%23123@host:5432/database\n" +
            "Or use DatabaseUrlUtils.urlEncode() to handle encoding automatically.\n\n" +
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
        // Encode the URL to handle special characters in credentials
        String encodedUrl;
        try {
            encodedUrl = DatabaseUrlEncoder.encodeUrl(databaseUrl);
        } catch (DatabaseUrlEncoder.DatabaseEncodingException e) {
            throw new IllegalArgumentException("Failed to encode database URL: " + e.getMessage(), e);
        }
        
        // Use the encoded URL for all subsequent operations
        URI uri = parseAndValidateUrl(encodedUrl, "connection details parsing");
        
        if (uri.getUserInfo() == null) {
            throw new IllegalArgumentException("No user information found in DATABASE_URL");
        }
        
        String[] userInfo = uri.getUserInfo().split(":");
        
        if (userInfo.length != 2) {
            throw new IllegalArgumentException("Invalid user information format in DATABASE_URL. Expected format: username:password");
        }
        
        String username = DatabaseUrlUtils.urlDecode(userInfo[0]);
        String password = DatabaseUrlUtils.urlDecode(userInfo[1]);
        
        if (username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is empty in DATABASE_URL");
        }
        
        if (password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is empty in DATABASE_URL");
        }
        
        return new DatabaseConnectionDetails(username, password);
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