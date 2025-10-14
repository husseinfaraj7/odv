package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

public class DatabaseUrlConverter implements EnvironmentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseUrlConverter.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");

        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            logger.warn("DATABASE_URL environment variable is not set or empty - skipping URL conversion");
            return;
        }

        logger.info("DATABASE_URL environment variable found: {}", databaseUrl);

        if (databaseUrl.startsWith("jdbc:")) {
            logger.info("Database URL is already in JDBC format, no conversion performed: {}", databaseUrl);
            return;
        }

        String scheme = detectScheme(databaseUrl);
        logger.info("Detected URL scheme: {}", scheme);

        String convertedUrl = null;
        if (databaseUrl.startsWith("postgres://")) {
            convertedUrl = "jdbc:postgresql://" + databaseUrl.substring("postgres://".length());
            logger.info("Converting postgres:// URL to JDBC format");
        } else if (databaseUrl.startsWith("postgresql://")) {
            convertedUrl = "jdbc:postgresql://" + databaseUrl.substring("postgresql://".length());
            logger.info("Converting postgresql:// URL to JDBC format");
        } else if (databaseUrl.startsWith("jdbc:postgresql://")) {
            logger.warn("DATABASE_URL is already in JDBC format (jdbc:postgresql://) - skipping conversion");
            return;
        } else {
            logger.warn("DATABASE_URL has unrecognized scheme: {} - skipping conversion", scheme);
            return;
        }
        
        if (convertedUrl != null) {
            logger.info("URL conversion performed successfully");
            logger.info("Original URL: {}", databaseUrl);
            logger.info("Converted URL: {}", convertedUrl);
            
            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", convertedUrl);
            
            MapPropertySource propertySource = new MapPropertySource("databaseUrlConversion", props);
            environment.getPropertySources().addFirst(propertySource);
            
            logger.info("Final spring.datasource.url set to: {}", convertedUrl);
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
}
