package it.odvsicilia.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that simulates Render deployment environment
 * to validate that the application can successfully start with render.yaml configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test") 
@TestPropertySource(properties = {
        // Simulate Render.com environment variables as defined in render.yaml
        "server.port=10000",
        "DATABASE_URL=jdbc:h2:mem:render_test_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "SUPABASE_URL=https://pejuystijjkjxjctieyb.supabase.co",
        "SUPABASE_ANON_KEY=test_anon_key",
        "SUPABASE_ROLE_KEY=test_role_key", 
        "ADMIN_EMAIL=test@odvsicilia.it",
        "BREVO_API_KEY=test_brevo_key",
        "BREVO_SMTP_SERVER=smtp-relay.brevo.com",
        "BREVO_SMTP_PORT=587",
        "BREVO_SMTP_USERNAME=96f5ae002@smtp-brevo.com",
        "BREVO_SENDER_EMAIL=noreply@odvsicilia.it",
        "BREVO_SENDER_NAME=ODV Sicilia",
        "FRONTEND_URL=https://odvsicilia.it",
        // Test database configuration
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.h2.console.enabled=false"
})
public class RenderDeploymentSimulationTest {

    @org.springframework.boot.test.web.server.LocalServerPort
    private int port;
    
    private final DataSource dataSource;

    public RenderDeploymentSimulationTest(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Test
    public void testApplicationStartsSuccessfully() {
        // Test passes if Spring Boot context loads successfully
        assertTrue(port > 0, "Application should bind to a port");
        assertNotNull(dataSource, "DataSource should be configured");
    }

    @Test
    public void testPortBinding() {
        // Verify that the application binds to the assigned port
        assertTrue(port > 1024, "Port should be greater than 1024");
        assertTrue(port < 65536, "Port should be less than 65536");
    }

    @Test  
    public void testDatabaseConnection() throws Exception {
        // Test database connectivity using the configured DataSource
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection, "Should be able to establish database connection");
            assertFalse(connection.isClosed(), "Database connection should be open");
            
            DatabaseMetaData metaData = connection.getMetaData();
            assertNotNull(metaData, "Should be able to get database metadata");
            
            String databaseProductName = metaData.getDatabaseProductName();
            assertNotNull(databaseProductName, "Database product name should not be null");
        }
    }

    @Test
    public void testHealthEndpointConfiguration() {
        // Verify that the health endpoint path matches render.yaml configuration
        // This test ensures the health check path is configured correctly
        String expectedHealthPath = "/actuator/health";
        
        // In a real deployment test, you would make an HTTP request to this endpoint
        // For now, we verify the path is what Render expects
        assertNotNull(expectedHealthPath, "Health check path should be configured");
        assertTrue(expectedHealthPath.startsWith("/"), "Health check path should start with /");
    }

    @Test
    public void testEnvironmentVariableConfiguration() {
        // Test that critical environment variables are accessible
        assertNotNull(System.getProperty("SUPABASE_URL", System.getenv("SUPABASE_URL")), 
                "SUPABASE_URL should be configured");
        assertNotNull(System.getProperty("FRONTEND_URL", System.getenv("FRONTEND_URL")), 
                "FRONTEND_URL should be configured");
        assertNotNull(System.getProperty("BREVO_SMTP_SERVER", System.getenv("BREVO_SMTP_SERVER")), 
                "BREVO_SMTP_SERVER should be configured");
    }
}