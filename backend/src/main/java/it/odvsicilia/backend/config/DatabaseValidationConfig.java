package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
@ConditionalOnProperty(name = "database.validation.enabled", havingValue = "true", matchIfMissing = true)
public class DatabaseValidationConfig implements InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseValidationConfig.class);

    @Value("${DATABASE_URL:}")
    private String databaseUrl;

    @Override
    public void afterPropertiesSet() throws Exception {
        logger.info("Starting DATABASE_URL validation...");
        validateDatabaseUrl();
        logger.info("DATABASE_URL validation completed successfully");
    }

    private void validateDatabaseUrl() throws Exception {
        // Check if DATABASE_URL is present
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            String errorMessage = "DATABASE_URL environment variable is missing or empty. " +
                    "Please provide a valid PostgreSQL JDBC URL in the format: " +
                    "jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>";
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        // Check for correct JDBC PostgreSQL prefix
        if (!databaseUrl.startsWith("jdbc:postgresql://")) {
            String errorMessage = String.format(
                    "DATABASE_URL must start with 'jdbc:postgresql://'. Current value: '%s'. " +
                    "Expected format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>",
                    databaseUrl
            );
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        // Validate URL format by parsing
        try {
            // Remove jdbc:postgresql:// prefix to get the URI part
            String uriPart = databaseUrl.substring("jdbc:postgresql://".length());
            URI uri = new URI("postgresql://" + uriPart);

            // Check if host is present
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                throw new IllegalStateException("DATABASE_URL is missing hostname");
            }

            // Check if database name is present
            String path = uri.getPath();
            if (path == null || path.length() <= 1) { // path starts with '/', so length <= 1 means no database name
                throw new IllegalStateException("DATABASE_URL is missing database name in path");
            }

            logger.info("DATABASE_URL validation passed. Host: {}, Port: {}, Database: {}",
                    uri.getHost(),
                    uri.getPort() != -1 ? uri.getPort() : "default",
                    path.substring(1)); // remove leading '/'
        } catch (URISyntaxException e) {
            String errorMessage = String.format(
                    "DATABASE_URL has invalid URI syntax: '%s'. Error: %s. " +
                    "Expected format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>",
                    databaseUrl, e.getMessage()
            );
            logger.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        } catch (Exception e) {
            String errorMessage = String.format(
                    "DATABASE_URL validation failed: '%s'. Error: %s. " +
                    "Expected format: jdbc:postgresql://<host>:<port>/<database>?user=<username>&password=<password>",
                    databaseUrl, e.getMessage()
            );
            logger.error(errorMessage, e);
            throw new IllegalStateException(errorMessage, e);
        }
    }
}
