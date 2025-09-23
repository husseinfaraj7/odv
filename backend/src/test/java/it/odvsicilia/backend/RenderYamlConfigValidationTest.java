package it.odvsicilia.backend;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to validate that render.yaml contains all required environment variables
 * for successful deployment on Render.com platform.
 */
public class RenderYamlConfigValidationTest {

    // Critical environment variables required for Spring Boot application startup
    private static final Set<String> REQUIRED_ENV_VARS = Set.of(
            "DATABASE_URL",
            "SUPABASE_URL", 
            "SUPABASE_ANON_KEY",
            "SUPABASE_ROLE_KEY",
            "ADMIN_EMAIL",
            "BREVO_API_KEY"
    );

    // Environment variables that should be present for full functionality
    private static final Set<String> EXPECTED_ENV_VARS = Set.of(
            "DATABASE_URL",
            "SUPABASE_URL",
            "SUPABASE_ANON_KEY", 
            "SUPABASE_ROLE_KEY",
            "ADMIN_EMAIL",
            "BREVO_API_KEY",
            "BREVO_SMTP_SERVER",
            "BREVO_SMTP_PORT", 
            "BREVO_SMTP_USERNAME",
            "BREVO_SENDER_EMAIL",
            "BREVO_SENDER_NAME",
            "FRONTEND_URL",
            "PORT" // Required for Render.com deployment
    );

    // Environment variables with default values that don't need to be in render.yaml
    private static final Set<String> OPTIONAL_ENV_VARS = Set.of(
            "BREVO_SMTP_SERVER", 
            "BREVO_SMTP_PORT",
            "BREVO_SMTP_USERNAME",
            "BREVO_SENDER_EMAIL",
            "BREVO_SENDER_NAME",
            "FRONTEND_URL"
    );

    @Test
    public void testRenderYamlContainsAllRequiredEnvironmentVariables() {
        Set<String> definedEnvVars = parseRenderYamlEnvironmentVariables();
        
        // Check for all required environment variables
        Set<String> missingRequired = new HashSet<>(REQUIRED_ENV_VARS);
        missingRequired.removeAll(definedEnvVars);
        
        assertTrue(missingRequired.isEmpty(), 
                "Missing required environment variables in render.yaml: " + missingRequired);
    }

    @Test
    public void testRenderYamlContainsPortVariable() {
        Set<String> definedEnvVars = parseRenderYamlEnvironmentVariables();
        
        assertTrue(definedEnvVars.contains("PORT"), 
                "render.yaml should contain PORT environment variable for Render.com deployment");
    }

    @Test 
    public void testWebServiceConfiguration() {
        String renderContent = readRenderYaml();
        
        // Validate service type
        assertTrue(renderContent.contains("type: web"), "Service should be of type 'web'");
        
        // Validate environment
        assertTrue(renderContent.contains("env: docker"), "Service should use docker environment");
        
        // Validate health check path
        assertTrue(renderContent.contains("healthCheckPath: /actuator/health"), 
                "Health check path should be /actuator/health");
        
        // Validate dockerfile path
        assertTrue(renderContent.contains("dockerfilePath:"), "Service should have dockerfilePath specified");
    }

    @Test
    public void testDatabaseUrlConfiguration() {
        String renderContent = readRenderYaml();
        
        assertTrue(renderContent.contains("key: DATABASE_URL"), "DATABASE_URL should be defined in render.yaml");
        
        // DATABASE_URL should be synced from Render environment (not hardcoded)
        Pattern databaseUrlPattern = Pattern.compile("- key: DATABASE_URL\\s+sync: false");
        assertTrue(databaseUrlPattern.matcher(renderContent).find(),
                "DATABASE_URL should have sync: false (managed externally)");
        
        // Should not have a hardcoded value
        Pattern valuePattern = Pattern.compile("- key: DATABASE_URL\\s+value:");
        assertFalse(valuePattern.matcher(renderContent).find(),
                "DATABASE_URL should not have a hardcoded value");
    }

    @Test
    public void testSecretEnvironmentVariablesConfiguration() {
        String renderContent = readRenderYaml();
        
        Set<String> secretVars = Set.of("DATABASE_URL", "SUPABASE_ANON_KEY", "SUPABASE_ROLE_KEY", "ADMIN_EMAIL", "BREVO_API_KEY");
        
        for (String secretVar : secretVars) {
            assertTrue(renderContent.contains("key: " + secretVar), 
                    secretVar + " should be defined in render.yaml");
            
            Pattern secretPattern = Pattern.compile("- key: " + Pattern.quote(secretVar) + "\\s+sync: false");
            assertTrue(secretPattern.matcher(renderContent).find(),
                    secretVar + " should have sync=false (managed externally)");
            
            // Should not have a hardcoded value  
            Pattern valuePattern = Pattern.compile("- key: " + Pattern.quote(secretVar) + "\\s+value:");
            assertFalse(valuePattern.matcher(renderContent).find(),
                    secretVar + " should not have a hardcoded value for security");
        }
    }

    @Test
    public void testRenderYamlStructure() {
        String renderContent = readRenderYaml();
        
        // Validate top-level structure
        assertTrue(renderContent.contains("services:"), "render.yaml should have 'services' section");
        
        // Validate that we have a web service
        assertTrue(renderContent.contains("type: web"), "Should have a web service");
        
        // Count web services (should be exactly one)
        Pattern webServicePattern = Pattern.compile("type:\\s*web");
        Matcher matcher = webServicePattern.matcher(renderContent);
        int webServiceCount = 0;
        while (matcher.find()) {
            webServiceCount++;
        }
        
        assertEquals(1, webServiceCount, "Should have exactly one web service");
    }

    private String readRenderYaml() {
        try (BufferedReader reader = new BufferedReader(new FileReader("render.yaml"))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        } catch (IOException e) {
            fail("Failed to read render.yaml: " + e.getMessage());
            return "";
        }
    }

    private Set<String> parseRenderYamlEnvironmentVariables() {
        String content = readRenderYaml();
        Set<String> envVars = new HashSet<>();
        
        Pattern envVarPattern = Pattern.compile("- key: (\\w+)");
        Matcher matcher = envVarPattern.matcher(content);
        
        while (matcher.find()) {
            envVars.add(matcher.group(1));
        }
        
        return envVars;
    }
}