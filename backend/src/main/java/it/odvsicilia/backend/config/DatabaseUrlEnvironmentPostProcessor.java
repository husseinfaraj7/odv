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

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseUrlEnvironmentPostProcessor.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            logger.warn("DATABASE_URL environment variable is missing or empty. " +
                       "Application startup will fail unless spring.datasource.url is configured " +
                       "or a dev profile with H2 is active. " +
                       "For deployment environments like Render, ensure DATABASE_URL is set correctly.");
            return;
        }

        logger.info("Original DATABASE_URL detected: {}", maskPassword(databaseUrl));

        if (databaseUrl.startsWith("jdbc:")) {
            logger.info("DATABASE_URL is already in JDBC format, no conversion needed");
            String existingUrl = environment.getProperty("spring.datasource.url");
            if (existingUrl != null) {
                logger.info("Resulting spring.datasource.url: {}", maskPassword(existingUrl));
            } else {
                logger.info("spring.datasource.url will be used from other configuration sources");
            }
            return;
        }

        if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
            try {
                String jdbcUrl = convertToJdbcUrl(databaseUrl);
                
                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", jdbcUrl);
                
                MapPropertySource propertySource = new MapPropertySource(
                    "databaseUrlConversion", props
                );
                environment.getPropertySources().addFirst(propertySource);
                
                logger.info("DATABASE_URL conversion occurred: Standard PostgreSQL URL converted to JDBC format");
                logger.info("Resulting spring.datasource.url: {}", maskPassword(jdbcUrl));
            } catch (Exception e) {
                logger.error("Failed to convert DATABASE_URL to JDBC format. " +
                           "Original DATABASE_URL format: {}. Error: {}", 
                           maskPassword(databaseUrl), e.getMessage());
                throw new IllegalStateException("Invalid DATABASE_URL format", e);
            }
        } else {
            logger.warn("DATABASE_URL has unrecognized format (not postgres://, postgresql://, or jdbc:). " +
                       "Value starts with: {}. Application may fail to connect to database.",
                       databaseUrl.substring(0, Math.min(20, databaseUrl.length())));
        }
    }

    private String maskPassword(String url) {
        if (url == null) {
            return null;
        }
        
        String masked = url.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
        masked = masked.replaceAll("[&?]password=([^&]+)", "&password=****");
        
        return masked;
    }

    private String convertToJdbcUrl(String databaseUrl) throws Exception {
        int schemeEnd = databaseUrl.indexOf("://");
        if (schemeEnd == -1) {
            throw new IllegalArgumentException("Invalid DATABASE_URL: missing scheme");
        }
        
        String afterScheme = databaseUrl.substring(schemeEnd + 3);
        
        String userInfo = null;
        String user = "postgres";
        String password = "";
        String hostAndRest;
        
        int atIndex = afterScheme.indexOf('@');
        if (atIndex != -1) {
            userInfo = afterScheme.substring(0, atIndex);
            hostAndRest = afterScheme.substring(atIndex + 1);
            
            int colonIndex = userInfo.indexOf(':');
            if (colonIndex != -1) {
                user = userInfo.substring(0, colonIndex);
                password = URLEncoder.encode(userInfo.substring(colonIndex + 1), StandardCharsets.UTF_8);
            } else {
                user = userInfo;
            }
        } else {
            hostAndRest = afterScheme;
        }
        
        String host;
        int port = 5432;
        String database = "postgres";
        String query = null;
        
        int slashIndex = hostAndRest.indexOf('/');
        if (slashIndex != -1) {
            String hostPart = hostAndRest.substring(0, slashIndex);
            String pathPart = hostAndRest.substring(slashIndex + 1);
            
            int colonIndex = hostPart.indexOf(':');
            if (colonIndex != -1) {
                host = hostPart.substring(0, colonIndex);
                try {
                    port = Integer.parseInt(hostPart.substring(colonIndex + 1));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid port number in DATABASE_URL");
                }
            } else {
                host = hostPart;
            }
            
            int queryIndex = pathPart.indexOf('?');
            if (queryIndex != -1) {
                database = pathPart.substring(0, queryIndex);
                query = pathPart.substring(queryIndex + 1);
            } else {
                database = pathPart;
            }
        } else {
            int colonIndex = hostAndRest.indexOf(':');
            int queryIndex = hostAndRest.indexOf('?');
            
            if (colonIndex != -1 && (queryIndex == -1 || colonIndex < queryIndex)) {
                host = hostAndRest.substring(0, colonIndex);
                String portPart = queryIndex != -1 ? hostAndRest.substring(colonIndex + 1, queryIndex) : hostAndRest.substring(colonIndex + 1);
                try {
                    port = Integer.parseInt(portPart);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid port number in DATABASE_URL");
                }
                if (queryIndex != -1) {
                    query = hostAndRest.substring(queryIndex + 1);
                }
            } else if (queryIndex != -1) {
                host = hostAndRest.substring(0, queryIndex);
                query = hostAndRest.substring(queryIndex + 1);
            } else {
                host = hostAndRest;
            }
        }
        
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid DATABASE_URL: missing host");
        }
        
        StringBuilder jdbcUrl = new StringBuilder();
        jdbcUrl.append("jdbc:postgresql://")
               .append(host)
               .append(":")
               .append(port)
               .append("/")
               .append(database);
        
        StringBuilder params = new StringBuilder();
        params.append("user=").append(user);
        params.append("&password=").append(password);
        
        if (query != null && !query.isEmpty()) {
            params.append("&").append(query);
        }
        
        jdbcUrl.append("?").append(params);
        
        return jdbcUrl.toString();
    }
}
