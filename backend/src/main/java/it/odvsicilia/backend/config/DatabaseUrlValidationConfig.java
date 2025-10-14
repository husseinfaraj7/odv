package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

@Component
public class DatabaseUrlValidationConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseUrlValidationConfig.class);
    
    private static final Pattern CREDENTIAL_PATTERN = Pattern.compile(".*:.*@");
    
    private final Environment environment;

    public DatabaseUrlValidationConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateDatabaseUrl() {
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        
        if (datasourceUrl == null || datasourceUrl.trim().isEmpty()) {
            logger.debug("spring.datasource.url is not set - skipping validation");
            return;
        }

        logger.debug("Validating spring.datasource.url: {}", datasourceUrl);

        try {
            String hostname = extractHostname(datasourceUrl);
            
            if (hostname != null && CREDENTIAL_PATTERN.matcher(hostname).matches()) {
                logger.error("=== DATABASE URL VALIDATION ERROR ===");
                logger.error("Malformed database URL detected: {}", datasourceUrl);
                logger.error("Credentials are being incorrectly interpreted as part of the hostname.");
                logger.error("The hostname extracted is: {}", hostname);
                logger.error("");
                logger.error("This typically indicates that the DATABASE_URL was not properly converted to JDBC format.");
                logger.error("");
                logger.error("REMEDIATION STEPS:");
                logger.error("1. Verify your Render environment variables are set correctly:");
                logger.error("   - Check that DATABASE_URL is set to the PostgreSQL connection URL");
                logger.error("   - Ensure no manual jdbc: prefix is added to DATABASE_URL in Render");
                logger.error("2. Check application startup logs for DatabaseUrlConverter execution:");
                logger.error("   - Look for 'URL conversion performed successfully' message");
                logger.error("   - Verify 'Final spring.datasource.url set to:' shows proper JDBC format");
                logger.error("3. If the issue persists, check that:");
                logger.error("   - META-INF/spring.factories contains the DatabaseUrlConverter entry");
                logger.error("   - No other configuration is overriding spring.datasource.url");
                logger.error("=====================================");
            } else {
                logger.debug("Database URL hostname validation passed: {}", hostname);
            }
        } catch (Exception e) {
            logger.warn("Failed to parse database URL for validation: {}", e.getMessage());
        }
    }

    private String extractHostname(String jdbcUrl) throws URISyntaxException {
        String urlWithoutJdbc = jdbcUrl;
        if (jdbcUrl.startsWith("jdbc:")) {
            urlWithoutJdbc = jdbcUrl.substring(5);
        }
        
        URI uri = new URI(urlWithoutJdbc);
        return uri.getHost();
    }
}
