package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URISyntaxException;
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

        if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
            try {
                String jdbcUrl = buildJdbcUrl(databaseUrl);

                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", jdbcUrl);

                MapPropertySource propertySource = new MapPropertySource("databaseUrlConversion", props);
                environment.getPropertySources().addFirst(propertySource);

                logger.info("Converted DATABASE_URL to JDBC format");
            } catch (Exception e) {
                logger.error("Failed to convert DATABASE_URL: {}", e.getMessage());
                throw new IllegalStateException("Invalid DATABASE_URL format", e);
            }
        }
    }

    private static String buildJdbcUrl(String databaseUrl) throws URISyntaxException {
        URI uri = new URI(databaseUrl.replace("postgres://", "postgresql://"));

        String host = uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            port = 5432;
        }
        String database = uri.getPath();
        if (database != null && database.startsWith("/")) {
            database = database.substring(1);
        }
        if (database == null || database.isEmpty()) {
            database = "postgres";
        }

        String userInfo = uri.getUserInfo();
        String username = "postgres";
        String password = "";

        if (userInfo != null && !userInfo.isEmpty()) {
            int colonIndex = userInfo.indexOf(':');
            if (colonIndex != -1) {
                username = userInfo.substring(0, colonIndex);
                password = userInfo.substring(colonIndex + 1);
            } else {
                username = userInfo;
            }
        }

        return String.format("jdbc:postgresql://%s:%d/%s?user=%s&password=%s",
                           host, port, database, username, password);
    }
}
