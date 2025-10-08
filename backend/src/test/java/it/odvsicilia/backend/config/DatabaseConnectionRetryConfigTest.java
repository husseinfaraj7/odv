package it.odvsicilia.backend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.retry.support.RetryTemplate;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseConnectionRetryConfigTest {

    private DatabaseConnectionRetryConfig config;
    private ConfigurableEnvironment environment;

    @BeforeEach
    void setUp() {
        config = new DatabaseConnectionRetryConfig();
        environment = new StandardEnvironment();
    }

    @Test
    void testDatabaseRetryTemplateCreation() {
        RetryTemplate retryTemplate = config.databaseRetryTemplate();
        
        assertNotNull(retryTemplate, "RetryTemplate should not be null");
    }

    @Test
    void testDataSourceRetryPostProcessorCreation() {
        RetryTemplate retryTemplate = config.databaseRetryTemplate();
        BeanPostProcessor postProcessor = config.dataSourceRetryPostProcessor(retryTemplate, environment);
        
        assertNotNull(postProcessor, "BeanPostProcessor should not be null");
    }

    @Test
    void testNonDataSourceBeanPassesThrough() {
        RetryTemplate retryTemplate = config.databaseRetryTemplate();
        BeanPostProcessor postProcessor = config.dataSourceRetryPostProcessor(retryTemplate, environment);
        
        String testBean = "test";
        Object result = postProcessor.postProcessAfterInitialization(testBean, "testBean");
        
        assertEquals(testBean, result, "Non-DataSource beans should pass through unchanged");
    }
}
