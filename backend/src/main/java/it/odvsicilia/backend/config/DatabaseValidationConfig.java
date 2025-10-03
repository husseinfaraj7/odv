package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;

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
        String validatedUrl = validateDatabaseUrl(databaseUrl);
        validateDatabaseConnection(validatedUrl);
        logger.info("=== DATABASE_URL validation completed successfully ===");
        logger.info("Application is using JDBC URL format: {}", maskCredentials(validatedUrl));
    }

    private String validateDatabaseUrl(String processedUrl) throws Exception {
        logger.info("Validating DATABASE_URL format...");

        // Check if DATABASE_URL is present
        if (processedUrl == null || processedUrl.trim().isEmpty()) {
            String errorMessage = "DATABASE_URL environment variable is missing or empty. " +
                    "Acceptable URL formats:\n" +
                    "  - JDBC format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>\n" +
                    "  - Standard format: postgres://<username>:<password>@<host>:<port>/<database> (will be converted)\n" +
                    "  - PostgreSQL format: postgresql://<host>:<port>/<database>?user=<username>&password=<password> (will be converted)";
            logger.error("URL validation failed: {}", errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        String finalUrl = processedUrl;
        boolean urlConverted = false;

        logger.info("Original DATABASE_URL: {}", maskCredentials(processedUrl));

        // Check if it's a standard postgres:// URL that needs conversion
        if (processedUrl.startsWith("postgres://")) {
            logger.info("Standard postgres:// URL detected, converting to JDBC format...");
            finalUrl = convertStandardToJdbcUrl(processedUrl);
            urlConverted = true;
            logger.info("URL conversion completed: {} -> {}", 
                    maskCredentials(processedUrl), maskCredentials(finalUrl));
        } else if (processedUrl.startsWith("postgresql://")) {
            logger.info("PostgreSQL format detected, converting to JDBC format...");
            finalUrl = convertPostgreSqlToJdbcFormat(processedUrl);
            urlConverted = true;
            logger.info("URL conversion completed: {} -> {}", 
                    maskCredentials(processedUrl), maskCredentials(finalUrl));
        } else if (!processedUrl.startsWith("jdbc:postgresql://")) {
            String errorMessage = String.format(
                    "DATABASE_URL has unsupported format. Current value: '%s'\n" +
                    "Acceptable URL formats:\n" +
                    "  - JDBC format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>\n" +
                    "  - Standard format: postgres://<username>:<password>@<host>:<port>/<database> (will be converted to JDBC)\n" +
                    "  - PostgreSQL format: postgresql://<host>:<port>/<database>?user=<username>&password=<password> (will be converted to JDBC)",
                    maskCredentials(processedUrl)
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

        // Use raw credentials directly without any encoding/decoding
        String username = credentials[0];
        String password = credentials[1];

        return String.format("jdbc:postgresql://%s:%d/%s?user=%s&password=%s",
                host, port, database, username, password);
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
        
        String problematicPart = "";
        if (errorIndex >= 0 && errorIndex < databaseUrl.length()) {
            int start = Math.max(0, errorIndex - 5);
            int end = Math.min(databaseUrl.length(), errorIndex + 10);
            problematicPart = databaseUrl.substring(start, end);
        }
        
        String detailedMessage = String.format(
            "DATABASE_URL parsing failed during %s.\n" +
            "Error: %s\n" +
            "Error position: %d\n" +
            "Problematic portion: '%s'\n\n" +
            "Please verify:\n" +
            "1. URL format is correct: postgresql://username:password@host:port/database\n" +
            "2. All required components (host, database name, credentials) are included\n" +
            "3. No illegal characters are present in the URL structure\n\n" +
            "Original error: %s",
            operation, 
            reason != null ? reason : "Unknown parsing error",
            errorIndex,
            problematicPart.isEmpty() ? "Unable to identify" : problematicPart,
            errorMessage
        );
        
        logger.error("Detailed URL parsing error: {}", detailedMessage);
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
