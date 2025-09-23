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
        try {
            // Remove jdbc:postgresql:// prefix to get the URI part
            String uriPart = finalUrl.substring("jdbc:postgresql://".length());
            URI uri = new URI("postgresql://" + uriPart);

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
            
        } catch (URISyntaxException e) {
            String errorMessage = String.format(
                    "DATABASE_URL has invalid URI syntax: '%s'. Error: %s\n" +
                    "Acceptable URL formats:\n" +
                    "  - JDBC format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>\n" +
                    "  - Standard format: postgres://<username>:<password>@<host>:<port>/<database> (will be converted)\n" +
                    "  - PostgreSQL format: postgresql://<host>:<port>/<database>?user=<username>&password=<password> (will be converted)",
                    maskCredentials(finalUrl), e.getMessage()
            );
            logger.error("URL validation failed: {}", errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = String.format(
                    "DATABASE_URL validation failed: '%s'. Error: %s\n" +
                    "Acceptable URL formats:\n" +
                    "  - JDBC format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>\n" +
                    "  - Standard format: postgres://<username>:<password>@<host>:<port>/<database> (will be converted)\n" +
                    "  - PostgreSQL format: postgresql://<host>:<port>/<database>?user=<username>&password=<password> (will be converted)",
                    maskCredentials(finalUrl), e.getMessage()
            );
            logger.error("URL validation failed: {}", errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        }
    }

    private String convertPostgreSqlToJdbcFormat(String postgresqlUrl) {
        String trimmed = postgresqlUrl.trim();
        String jdbcUrl = "jdbc:" + trimmed;
        logger.info("Converted PostgreSQL URL format to JDBC format");
        return jdbcUrl;
    }
    }

    private String convertStandardToJdbcUrl(String standardUrl) {
        try {
            URI uri = new URI(standardUrl);
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

            String username = credentials[0];
            String password = credentials[1];

            return String.format("jdbc:postgresql://%s:%d/%s?user=%s&password=%s",
                    host, port, database, username, password);
        } catch (Exception e) {
            logger.error("Failed to convert standard URL to JDBC format: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to convert standard postgres:// URL to JDBC format: " + e.getMessage(), e);
        }
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
