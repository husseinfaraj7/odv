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
        }
        
        URI uri;
        boolean encodingApplied = false;
        
        try {
            // First attempt: try parsing without encoding
            uri = new URI(databaseUrl.startsWith("jdbc:") ? databaseUrl : "jdbc:" + databaseUrl);
            logger.debug("Successfully parsed DATABASE_URL without encoding");
        } catch (URISyntaxException originalException) {
            logger.info("Initial URL parsing failed, attempting with automatic credential encoding: {}", 
                       originalException.getMessage());
            
            try {
                // Second attempt: encode the URL to handle special characters in credentials
                String encodedUrl = encodeUrl(databaseUrl);
                uri = new URI(encodedUrl);
                encodingApplied = true;
                logger.info("Successfully parsed DATABASE_URL after applying automatic credential encoding");
            } catch (URISyntaxException retryException) {
                logger.error("URL parsing failed even after automatic encoding. Original error: {}, Retry error: {}", 
                           originalException.getMessage(), retryException.getMessage());
                
                // Enhanced error handling with character-specific recommendations
                handleUrlParsingWithEncodingSuggestions(databaseUrl, originalException, retryException);
                
                // This line will never be reached due to the exception being thrown above
                return null;
            }
        }
        
        if (encodingApplied) {
            logger.info("Automatic URL encoding was applied to handle special characters (@, :, %, etc.) in database credentials. " +
                       "This indicates that the original DATABASE_URL contained unencoded special characters.");
        }
        
        if (uri.getUserInfo() == null) {
            throw new IllegalArgumentException("No user information found in DATABASE_URL");
        }
        
        String[] userInfo = uri.getUserInfo().split(":");
        
        if (userInfo.length != 2) {
            throw new IllegalArgumentException("Invalid user information format in DATABASE_URL. Expected format: username:password");
        }
        
        String username, password;
        
        if (encodingApplied) {
            // If encoding was applied, decode the credentials
            username = urlDecode(userInfo[0]);
            password = urlDecode(userInfo[1]);
            logger.debug("Decoded username and password after automatic encoding");
        } else {
            // If no encoding was needed, use credentials as-is
            username = userInfo[0];
            password = userInfo[1];
        }
        
        if (username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username is empty in DATABASE_URL");
        }
        
        if (password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is empty in DATABASE_URL");
        }
        
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
        String credentialsPart = extractCredentialsPart(normalizedUrl);
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