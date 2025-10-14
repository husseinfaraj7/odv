package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseUrlConverter implements EnvironmentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseUrlConverter.class);
    private static final Pattern EMBEDDED_CREDENTIALS_PATTERN = Pattern.compile("://([^:]+):([^@]+)@");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        logger.debug("DatabaseUrlConverter.postProcessEnvironment() invoked");
        logger.debug("Active profiles: {}", String.join(", ", environment.getActiveProfiles()));
        logger.debug("Available property sources: ");
        for (PropertySource<?> ps : environment.getPropertySources()) {
            logger.debug("  - {} (type: {})", ps.getName(), ps.getClass().getSimpleName());
        }

        String databaseUrl = environment.getProperty("DATABASE_URL");
        logger.debug("Retrieved DATABASE_URL property: {}", databaseUrl != null ? "[present]" : "[null]");

        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            logger.warn("DATABASE_URL environment variable is not set or empty - skipping URL conversion");
            logger.info("Application will use default datasource configuration from application.properties");
            return;
        }

        logger.info("DATABASE_URL environment variable found, length: {} characters", databaseUrl.length());
        logger.debug("DATABASE_URL raw value: {}", databaseUrl);

        validateAndWarnEmbeddedCredentials(databaseUrl);

        if (databaseUrl.startsWith("jdbc:")) {
            logger.info("Database URL is already in JDBC format, no conversion needed");
            logger.debug("Existing JDBC URL format detected with length: {} characters", databaseUrl.length());
            logger.info("Setting spring.datasource.url directly from DATABASE_URL");
            setDatasourceUrl(environment, databaseUrl);
            return;
        }

        String scheme = detectScheme(databaseUrl);
        logger.info("Detected URL scheme: {}", scheme);

        String convertedUrl = performConversion(databaseUrl, scheme);
        
        if (convertedUrl != null) {
            logger.info("URL conversion performed successfully");
            logger.debug("Original URL: {}", maskCredentials(databaseUrl));
            logger.debug("Converted URL: {}", maskCredentials(convertedUrl));
            
            setDatasourceUrl(environment, convertedUrl);
        }
    }

    private String performConversion(String databaseUrl, String scheme) {
        String convertedUrl = null;
        
        if (databaseUrl.startsWith("postgres://")) {
            logger.debug("Converting postgres:// URL to JDBC format");
            convertedUrl = "jdbc:postgresql://" + databaseUrl.substring("postgres://".length());
            logger.info("Conversion applied: postgres:// -> jdbc:postgresql://");
        } else if (databaseUrl.startsWith("postgresql://")) {
            logger.debug("Converting postgresql:// URL to JDBC format");
            convertedUrl = "jdbc:postgresql://" + databaseUrl.substring("postgresql://".length());
            logger.info("Conversion applied: postgresql:// -> jdbc:postgresql://");
        } else if (databaseUrl.startsWith("jdbc:postgresql://")) {
            logger.warn("DATABASE_URL is already in JDBC format (jdbc:postgresql://) - skipping conversion");
            convertedUrl = databaseUrl;
        } else {
            logger.warn("DATABASE_URL has unrecognized scheme: {} - skipping conversion", scheme);
            logger.warn("Supported schemes: postgres://, postgresql://, jdbc:postgresql://");
            return null;
        }
        
        return convertedUrl;
    }

    private void setDatasourceUrl(ConfigurableEnvironment environment, String url) {
        logger.debug("Setting spring.datasource.url property");
        
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", url);
        
        MapPropertySource propertySource = new MapPropertySource("databaseUrlConversion", props);
        environment.getPropertySources().addFirst(propertySource);
        
        logger.info("Property source 'databaseUrlConversion' added to environment (highest priority)");
        logger.info("Final spring.datasource.url value: {}", maskCredentials(url));
        
        String verifyUrl = environment.getProperty("spring.datasource.url");
        if (verifyUrl != null && verifyUrl.equals(url)) {
            logger.debug("Verification successful: spring.datasource.url matches expected value");
        } else {
            logger.warn("Verification warning: spring.datasource.url does not match expected value");
            logger.warn("Expected: {}", maskCredentials(url));
            logger.warn("Actual: {}", verifyUrl != null ? maskCredentials(verifyUrl) : "[null]");
        }
    }

    private void validateAndWarnEmbeddedCredentials(String databaseUrl) {
        Matcher matcher = EMBEDDED_CREDENTIALS_PATTERN.matcher(databaseUrl);
        
        if (matcher.find()) {
            logger.warn("===================================================================================");
            logger.warn("SECURITY WARNING: Embedded credentials detected in DATABASE_URL");
            logger.warn("Found pattern: '{}:***@' in URL", matcher.group(1));
            logger.warn("RECOMMENDATION: Separate credentials into environment variables:");
            logger.warn("  - DATABASE_USER for username");
            logger.warn("  - DATABASE_PASSWORD for password");
            logger.warn("This improves security and follows best practices for credential management.");
            logger.warn("===================================================================================");
        } else {
            logger.debug("No embedded credentials detected in DATABASE_URL (pattern 'user:pass@' not found)");
        }
    }

    private String detectScheme(String url) {
        if (url.startsWith("postgres://")) {
            return "postgres://";
        } else if (url.startsWith("postgresql://")) {
            return "postgresql://";
        } else if (url.startsWith("jdbc:postgresql://")) {
            return "jdbc:postgresql://";
        } else {
            int colonIndex = url.indexOf(":");
            return colonIndex > 0 ? url.substring(0, colonIndex + 1) : "unknown";
        }
    }

    private String maskCredentials(String url) {
        if (url == null) {
            return null;
        }
        Matcher matcher = EMBEDDED_CREDENTIALS_PATTERN.matcher(url);
        if (matcher.find()) {
            return matcher.replaceAll("://" + matcher.group(1) + ":***@");
        }
        return url;
    }
}
