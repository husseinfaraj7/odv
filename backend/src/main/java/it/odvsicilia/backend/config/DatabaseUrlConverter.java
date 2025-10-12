package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
                String jdbcUrl = convertToJdbcUrl(databaseUrl);
                
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

    private String convertToJdbcUrl(String databaseUrl) {
        String url = databaseUrl.replace("postgres://", "jdbc:postgresql://")
                                 .replace("postgresql://", "jdbc:postgresql://");
        
        int atIndex = url.indexOf('@');
        if (atIndex == -1) {
            return url;
        }
        
        int schemeEnd = url.indexOf("://") + 3;
        String userInfo = url.substring(schemeEnd, atIndex);
        String afterAuth = url.substring(atIndex + 1);
        
        String username = "";
        String password = "";
        
        int colonIndex = userInfo.indexOf(':');
        if (colonIndex != -1) {
            username = userInfo.substring(0, colonIndex);
            password = userInfo.substring(colonIndex + 1);
        } else {
            username = userInfo;
        }
        
        String baseUrl = "jdbc:postgresql://" + afterAuth;
        
        String encodedUsername = URLEncoder.encode(username, StandardCharsets.UTF_8);
        String encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8);
        
        String separator = baseUrl.contains("?") ? "&" : "?";
        return baseUrl + separator + "user=" + encodedUsername + "&password=" + encodedPassword;
    }
}
