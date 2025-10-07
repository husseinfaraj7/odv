package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseUrlEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            logger.debug("DATABASE_URL not found - skipping conversion");
            return;
        }

        if (databaseUrl.startsWith("jdbc:")) {
            logger.debug("DATABASE_URL is already in JDBC format - skipping conversion");
            return;
        }

        if (!databaseUrl.startsWith("postgres://") && !databaseUrl.startsWith("postgresql://")) {
            logger.debug("DATABASE_URL is not in PostgreSQL format - skipping conversion");
            return;
        }

        try {
            String jdbcUrl = convertToJdbcUrl(databaseUrl);
            
            Map<String, Object> propertySource = new HashMap<>();
            propertySource.put("spring.datasource.url", jdbcUrl);
            
            environment.getPropertySources().addFirst(
                new MapPropertySource("databaseUrlConversion", propertySource)
            );
            
            logger.info("Successfully converted DATABASE_URL to JDBC format");
        } catch (Exception e) {
            logger.error("Failed to convert DATABASE_URL to JDBC format: {}", e.getMessage());
        }
    }

    private String convertToJdbcUrl(String databaseUrl) throws URISyntaxException, UnsupportedEncodingException {
        String normalizedUrl = databaseUrl
            .replace("postgres://", "http://")
            .replace("postgresql://", "http://");
        
        URI uri = new URI(normalizedUrl);
        
        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("Invalid DATABASE_URL: missing host");
        }
        
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        
        String path = uri.getPath();
        if (path == null || path.length() <= 1) {
            throw new IllegalArgumentException("Invalid DATABASE_URL: missing database name");
        }
        String database = path.substring(1);
        
        String userInfo = uri.getUserInfo();
        if (userInfo == null || !userInfo.contains(":")) {
            throw new IllegalArgumentException("Invalid DATABASE_URL: missing credentials");
        }
        
        String[] credentials = userInfo.split(":", 2);
        String user = credentials[0];
        String password = credentials[1];
        
        String encodedUser = URLEncoder.encode(user, StandardCharsets.UTF_8.toString());
        String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8.toString());
        
        return String.format(
            "jdbc:postgresql://%s:%d/%s?user=%s&password=%s",
            host, port, database, encodedUser, encodedPassword
        );
    }
}
