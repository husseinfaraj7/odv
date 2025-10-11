package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DatabaseConfig implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private final Environment environment;

    public DatabaseConfig(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        String[] activeProfiles = environment.getActiveProfiles();
        
        // Skip DATABASE_URL validation for dev and test profiles (use H2 instead)
        if (Arrays.asList(activeProfiles).contains("dev") || 
            Arrays.asList(activeProfiles).contains("test")) {
            logger.info("Development/Test profile active - DATABASE_URL validation skipped");
            return;
        }
        
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            String errorMessage = "DATABASE_URL environment variable is required but not set. " +
                                "Application cannot start without database configuration. " +
                                "For local development, use -Dspring-boot.run.profiles=dev to use H2 database.";
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        logger.info("DATABASE_URL environment variable validated successfully");
    }
}
