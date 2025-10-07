package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;

@Component
@ConditionalOnProperty(name = "database.validation.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseValidationConfig implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseValidationConfig.class);

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    private final DataSource dataSource;
    private final Environment environment;

    public DatabaseValidationConfig(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isDevOrTest = Arrays.stream(activeProfiles)
                .anyMatch(profile -> "dev".equals(profile) || "test".equals(profile));
        
        if (isDevOrTest) {
            logger.info("=== DATABASE_URL validation skipped for dev/test profile ===");
            logger.info("Active profiles: {}", Arrays.toString(activeProfiles));
            return;
        }
        
        logger.info("=== Starting DATABASE_URL validation ===");
        String validatedUrl = validateDatabaseUrl(databaseUrl);
        validateDatabaseConnection(validatedUrl);
        logger.info("=== DATABASE_URL validation completed successfully ===");
        logger.info("Application is using JDBC URL format: {}", maskCredentials(validatedUrl));
    }

    private String validateDatabaseUrl(String jdbcUrl) throws Exception {
        logger.info("Validating DATABASE_URL format...");

        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            String errorMessage = "DATABASE_URL environment variable is missing or empty. " +
                    "Expected format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>";
            logger.error("URL validation failed: {}", errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        logger.info("Validating DATABASE_URL: {}", maskCredentials(jdbcUrl));

        if (!jdbcUrl.startsWith("jdbc:postgresql://")) {
            String errorMessage = String.format(
                    "DATABASE_URL has unsupported format. Current value: '%s'\n" +
                    "Expected JDBC format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>",
                    maskCredentials(jdbcUrl)
            );
            logger.error("URL validation failed: {}", errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        URI uri = parseAndValidateJdbcUrl(jdbcUrl);
        
        if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
            String errorMessage = "DATABASE_URL is missing hostname. " +
                    "Expected format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>";
            logger.error("URL validation failed: {}", errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        String path = uri.getPath();
        if (path == null || path.length() <= 1) {
            String errorMessage = "DATABASE_URL is missing database name in path. " +
                    "Expected format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>";
            logger.error("URL validation failed: {}", errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        logger.info("DATABASE_URL format validation passed successfully");
        logger.info("Database connection details - Host: {}, Port: {}, Database: {}",
                uri.getHost(),
                uri.getPort() != -1 ? uri.getPort() : "5432 (default)",
                path.substring(1));

        return jdbcUrl;
    }
    
    private URI parseAndValidateJdbcUrl(String jdbcUrl) {
        try {
            String uriPart = jdbcUrl.substring("jdbc:postgresql://".length());
            return new URI("postgresql://" + uriPart);
        } catch (URISyntaxException e) {
            handleUrlParsingException(jdbcUrl, e, "JDBC URL validation");
            return null;
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
            "1. URL format is correct: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>\n" +
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
