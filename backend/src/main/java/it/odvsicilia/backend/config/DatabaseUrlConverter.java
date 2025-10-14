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
            return;
        }

        String convertedUrl = null;
        if (databaseUrl.startsWith("postgres://")) {
            convertedUrl = "jdbc:postgresql://" + databaseUrl.substring("postgres://".length());
        } else if (databaseUrl.startsWith("postgresql://")) {
            convertedUrl = "jdbc:postgresql://" + databaseUrl.substring("postgresql://".length());
        }
        
        if (convertedUrl != null) {
            logger.info("Original URL: {}", databaseUrl);
            logger.info("Converted URL: {}", convertedUrl);
            
            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", convertedUrl);
            
            MapPropertySource propertySource = new MapPropertySource("databaseUrlConversion", props);
            environment.getPropertySources().addFirst(propertySource);
        }
    }
}
