package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class DatabaseConfig implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        String databaseUrl = System.getenv("DATABASE_URL");
        
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            String errorMessage = "DATABASE_URL environment variable is required but not set. " +
                                "Application cannot start without database configuration.";
            logger.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }
        
        logger.info("DATABASE_URL environment variable validated successfully");
    }
}
