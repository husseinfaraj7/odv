package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(name = "database.validation.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseValidationConfig implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseValidationConfig.class);

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    private final DataSource dataSource;

    public DatabaseValidationConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("=== Starting DATABASE_URL validation ===");
        String validatedUrl = validateDatabaseUrl();
        validateDatabaseConnection(validatedUrl);
        logger.info("=== DATABASE_URL validation completed successfully ===");
        logger.info("Application is using JDBC URL format: {}", maskCredentials(validatedUrl));
    }

    private String validateDatabaseUrl() throws Exception {
        logger.info("Validating DATABASE_URL format...");
        
        // Check if DATABASE_URL is present
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            String errorMessage = "DATABASE_URL environment variable is missing or empty. " +
                    "Acceptable URL formats:\n" +
                    "  - JDBC format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>\n" +
                    "  - Standard format: postgres://<username>:<password>@<host>:<port>/<database> (will be converted)\n" +
                    "  - PostgreSQL format: postgresql://<host>:<port>/<database>?user=<username>&password=<password> (will be converted)";
            logger.error("URL validation failed: {}", errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        String originalUrl = databaseUrl;
        String finalUrl = databaseUrl;
        boolean urlConverted = false;

        logger.info("Original DATABASE_URL format being validated: {}", maskCredentials(originalUrl));

        // Check if it's a standard postgres:// URL that needs conversion
        if (databaseUrl.startsWith("postgres://")) {
            logger.info("Standard postgres:// URL detected, converting to JDBC format...");
            finalUrl = convertStandardToJdbcUrl(databaseUrl);
            urlConverted = true;
            logger.info("URL conversion completed: {} -> {}", 
                    maskCredentials(originalUrl), maskCredentials(finalUrl));
        } else if (databaseUrl.startsWith("postgresql://")) {
            logger.info("PostgreSQL format detected, converting to JDBC format...");
            finalUrl = convertPostgreSqlToJdbcFormat(databaseUrl);
            urlConverted = true;
            logger.info("URL conversion completed: {} -> {}", 
                    maskCredentials(originalUrl), maskCredentials(finalUrl));
        } else if (!databaseUrl.startsWith("jdbc:postgresql://")) {
            String errorMessage = String.format(
                    "DATABASE_URL has unsupported format. Current value: '%s'\n" +
                    "Acceptable URL formats:\n" +
                    "  - JDBC format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>\n" +
                    "  - Standard format: postgres://<username>:<password>@<host>:<port>/<database> (will be converted to JDBC)\n" +
                    "  - PostgreSQL format: postgresql://<host>:<port>/<database>?user=<username>&password=<password> (will be converted to JDBC)",
                    maskCredentials(databaseUrl)
            );
            logger.error("URL validation failed: {}", errorMessage);
            throw new IllegalStateException(errorMessage);
        } else {
            logger.info("JDBC PostgreSQL URL format detected, no conversion needed");
        }

        // Validate URL format by parsing
        URI uri = parseAndValidateJdbcUrl(finalUrl);
        
        // Check if host is present
        if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
            String errorMessage = "DATABASE_URL is missing hostname. " +
                    "Expected format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>";
            logger.error("URL validation failed: {}", errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        // Check if database name is present
        String path = uri.getPath();
        if (path == null || path.length() <= 1) { // path starts with '/', so length <= 1 means no database name
            String errorMessage = "DATABASE_URL is missing database name in path. " +
                    "Expected format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>";
            logger.error("URL validation failed: {}", errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        logger.info("DATABASE_URL format validation passed successfully");
        logger.info("Database connection details - Host: {}, Port: {}, Database: {}",
                uri.getHost(),
                uri.getPort() != -1 ? uri.getPort() : "5432 (default)",
                path.substring(1)); // remove leading '/'

        if (urlConverted) {
            logger.info("URL conversion summary: Standard format converted to JDBC format successfully");
        }

        return finalUrl;
    }

    private String convertPostgreSqlToJdbcFormat(String postgresqlUrl) {
        String trimmed = postgresqlUrl.trim();
        String jdbcUrl = "jdbc:" + trimmed;
        logger.info("Converted PostgreSQL URL format to JDBC format");
        return jdbcUrl;
    }

    private String convertStandardToJdbcUrl(String standardUrl) {
        URI uri = parseAndValidateStandardUrl(standardUrl, "standard URL conversion");
        String userInfo = uri.getUserInfo();
        String host = uri.getHost();
        int port = uri.getPort() != -1 ? uri.getPort() : 5432;
        String database = uri.getPath().substring(1); // remove leading '/'

        if (userInfo == null) {
            throw new IllegalArgumentException("Missing credentials in standard URL format");
        }

        String[] credentials = userInfo.split(":", 2);
        if (credentials.length != 2) {
            throw new IllegalArgumentException("Invalid credentials format in standard URL");
        }

        // URL decode credentials with proper error handling
        String username;
        String password;
        try {
            username = DatabaseUrlUtils.urlDecode(credentials[0]);
            password = DatabaseUrlUtils.urlDecode(credentials[1]);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                "Failed to decode URL-encoded credentials in standard URL format. " +
                "Ensure special characters in username and password are properly URL-encoded. " +
                "Original error: " + e.getMessage(), e);
        }

        // URL encode credentials for JDBC URL with proper error handling
        String encodedUsername;
        String encodedPassword;
        try {
            encodedUsername = DatabaseUrlUtils.urlEncode(username);
            encodedPassword = DatabaseUrlUtils.urlEncode(password);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                "Failed to encode credentials for JDBC URL format. " +
                "This indicates an issue with the credential values or encoding process. " +
                "Original error: " + e.getMessage(), e);
        }

        return String.format("jdbc:postgresql://%s:%d/%s?user=%s&password=%s",
                host, port, database, encodedUsername, encodedPassword);
    }
    
    private URI parseAndValidateJdbcUrl(String jdbcUrl) {
        try {
            // Remove jdbc:postgresql:// prefix to get the URI part
            String uriPart = jdbcUrl.substring("jdbc:postgresql://".length());
            return new URI("postgresql://" + uriPart);
        } catch (URISyntaxException e) {
            handleUrlParsingException(jdbcUrl, e, "JDBC URL validation");
            return null; // Never reached
        }
    }
    
    private URI parseAndValidateStandardUrl(String standardUrl, String operation) {
        try {
            return new URI(standardUrl);
        } catch (URISyntaxException e) {
            handleUrlParsingException(standardUrl, e, operation);
            return null; // Never reached
        }
    }
    
    private void handleUrlParsingException(String databaseUrl, URISyntaxException e, String operation) {
        String errorMessage = e.getMessage();
        String reason = e.getReason();
        int errorIndex = e.getIndex();
        
        logger.error("URL parsing exception during {} at position {}: {}", operation, errorIndex, errorMessage);
        
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
            "URL encoding is required for special characters in database credentials.\n" +
            "This error indicates that special characters in the username or password are not properly URL-encoded.\n\n" +
            "Common encoding issues and solutions:\n" +
            "• '@' in passwords must be encoded as '%%40'\n" +
            "• '#' characters must be encoded as '%%23'\n" +
            "• '$' characters must be encoded as '%%24'\n" +
            "• '%%' characters must be encoded as '%%25'\n" +
            "• '^' characters must be encoded as '%%5E'\n" +
            "• '&' characters must be encoded as '%%26'\n" +
            "• '*' characters must be encoded as '%%2A'\n" +
            "• Space characters must be encoded as '%%20'\n" +
            "• '+' characters must be encoded as '%%2B'\n" +
            "• ':' characters must be encoded as '%%3A'\n" +
            "• '/' characters must be encoded as '%%2F'\n" +
            "• '?' characters must be encoded as '%%3F'\n" +
            "• '=' characters must be encoded as '%%3D'\n\n" +
            "SOLUTION: Use the DatabaseUrlUtils utility class for automatic URL encoding!\n" +
            "Example usage:\n" +
            "  String encodedPassword = DatabaseUrlUtils.urlEncode(\"P@ssw0rd#123\");\n" +
            "  // Result: \"P%%40ssw0rd%%23123\"\n\n" +
            "Example: If your password is 'P@ssw0rd#123', it should be encoded as 'P%%40ssw0rd%%23123'\n" +
            "Full example URL: postgresql://user:P%%40ssw0rd%%23123@host:5432/database\n\n" +
            "To fix this issue:\n" +
            "1. Identify special characters in your username and password\n" +
            "2. URL-encode them using the mappings above or use DatabaseUrlUtils.urlEncode()\n" +
            "3. Update your DATABASE_URL environment variable with the encoded values\n\n" +
            "Original error: %s",
            operation, errorIndex, e.getReason() != null ? e.getReason() : "URI syntax error",
            problematicPart.isEmpty() ? "Unable to identify" : problematicPart,
            e.getMessage()
        );
        
        logger.error("Detailed encoding error: {}", detailedMessage);
        throw new IllegalStateException(detailedMessage, e);
    }
    
    private void handleMalformedUrlError(String databaseUrl, URISyntaxException e, String operation) {
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
        throw new IllegalStateException(detailedMessage, e);
    }
    
    private void handleGenericUrlParsingError(String databaseUrl, URISyntaxException e, String operation) {
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
        throw new IllegalStateException(detailedMessage, e);
    }

    private void validateDatabaseConnection(String jdbcUrl) {
        logger.info("Validating database connection...");
        try {
            logger.debug("Attempting database connection with URL: {}", maskCredentials(jdbcUrl));
            
            try (Connection connection = dataSource.getConnection()) {
                if (connection.isValid(10)) { // 10 second timeout
                    logger.info("Database connection validation successful");
                    logger.info("Database product: {}, Version: {}", 
                            connection.getMetaData().getDatabaseProductName(),
                            connection.getMetaData().getDatabaseProductVersion());
                } else {
                    String errorMessage = "Database connection is not valid (connection.isValid() returned false)";
                    logger.error("Database connection validation failed: {}", errorMessage);
                    logger.error("Failed connection URL: {}", maskCredentials(jdbcUrl));
                    throw new SQLException(errorMessage);
                }
            }
        } catch (SQLException e) {
            String errorMessage = String.format(
                    "Database connection validation failed. Unable to connect to database.\n" +
                    "Connection URL: %s\n" +
                    "Error: %s\n" +
                    "Possible causes:\n" +
                    "  - Database server is not running or unreachable\n" +
                    "  - Invalid credentials (username/password)\n" +
                    "  - Database does not exist\n" +
                    "  - Network connectivity issues\n" +
                    "  - Firewall blocking connection",
                    maskCredentials(jdbcUrl), e.getMessage()
            );
            logger.error("Database connection validation failed: {}", errorMessage, e);
            logger.error("Full exception details:", e);
            throw new IllegalStateException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = String.format(
                    "Unexpected error during database connection validation.\n" +
                    "Connection URL: %s\n" +
                    "Error: %s",
                    maskCredentials(jdbcUrl), e.getMessage()
            );
            logger.error("Database connection validation failed: {}", errorMessage, e);
            logger.error("Full exception details:", e);
            throw new IllegalStateException(errorMessage, e);
        }
    }

    private String maskCredentials(String url) {
        if (url == null) {
            return "null";
        }
        
        // Mask passwords in JDBC URLs (password=xxxx)
        String masked = url.replaceAll("password=[^&]*", "password=***");
        
        // Mask credentials in standard postgres:// URLs (username:password@)
        masked = masked.replaceAll("://[^@:]*:[^@]*@", "://***:***@");
        
        return masked;
    }
}
