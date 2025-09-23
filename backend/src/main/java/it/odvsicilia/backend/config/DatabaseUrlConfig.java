package it.odvsicilia.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

@Configuration
public class DatabaseUrlConfig {

    private final Environment environment;

    public DatabaseUrlConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    @Primary
    public String transformedDatabaseUrl() {
        String rawUrl = environment.getProperty("DATABASE_URL");
        
        if (rawUrl == null || rawUrl.isEmpty()) {
            // Return default H2 URL if DATABASE_URL is not set (development)
            return "jdbc:h2:mem:testdb";
        }
        
        // Check if the URL already has the jdbc: prefix
        if (rawUrl.startsWith("jdbc:")) {
            return rawUrl;
        }
        
        // Prepend jdbc: to the PostgreSQL URL
        return "jdbc:" + rawUrl;
    }
}