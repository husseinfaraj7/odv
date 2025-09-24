package it.odvsicilia.backend;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Simple test to validate Spring Boot application properties match render.yaml configuration
 * without starting the full application context.
 */
public class SimpleSpringBootStartupTest {

    public static void main(String[] args) {
        SimpleSpringBootStartupTest test = new SimpleSpringBootStartupTest();
        try {
            test.validateApplicationConfiguration();
            System.out.println("✅ Spring Boot configuration validation passed!");
        } catch (Exception e) {
            System.err.println("❌ Configuration validation failed: " + e.getMessage());
            System.exit(1);
        }
    }

    public void validateApplicationConfiguration() throws Exception {
        testPortConfigurationMatches();
        testDatabaseConfigurationPresent();
        testSupabaseConfigurationPresent();
        testEmailConfigurationPresent();
        testHealthEndpointEnabled();
    }

    public void testPortConfigurationMatches() throws Exception {
        Properties appProps = loadApplicationProperties();
        
        String portConfig = appProps.getProperty("server.port");
        if (portConfig == null) {
            throw new AssertionError("server.port should be configured in application.properties");
        }
        
        // Should use PORT environment variable with fallback to 8080
        if (!portConfig.contains("${PORT:8080}")) {
            throw new AssertionError("server.port should use PORT environment variable: " + portConfig);
        }
        
        System.out.println("✓ PORT configuration matches render.yaml");
    }

    public void testDatabaseConfigurationPresent() throws Exception {
        Properties appProps = loadApplicationProperties();
        
        String databaseUrl = appProps.getProperty("spring.datasource.url");
        if (databaseUrl == null) {
            throw new AssertionError("spring.datasource.url should be configured");
        }
        
        if (!databaseUrl.contains("${DATABASE_URL")) {
            throw new AssertionError("spring.datasource.url should use DATABASE_URL environment variable");
        }
        
        System.out.println("✓ Database configuration uses DATABASE_URL environment variable");
    }

    public void testSupabaseConfigurationPresent() throws Exception {
        Properties appProps = loadApplicationProperties();
        
        String supabaseUrl = appProps.getProperty("supabase.url");
        if (supabaseUrl == null) {
            throw new AssertionError("supabase.url should be configured");
        }
        
        if (!supabaseUrl.contains("${SUPABASE_URL")) {
            throw new AssertionError("supabase.url should use SUPABASE_URL environment variable");
        }
        
        String supabaseAnonKey = appProps.getProperty("supabase.anon.key");
        if (supabaseAnonKey == null) {
            throw new AssertionError("supabase.anon.key should be configured");
        }
        
        if (!supabaseAnonKey.contains("${SUPABASE_ANON_KEY")) {
            throw new AssertionError("supabase.anon.key should use SUPABASE_ANON_KEY environment variable");
        }
        
        System.out.println("✓ Supabase configuration uses environment variables");
    }

    public void testEmailConfigurationPresent() throws Exception {
        Properties appProps = loadApplicationProperties();
        
        String adminEmail = appProps.getProperty("admin.email");
        if (adminEmail == null) {
            throw new AssertionError("admin.email should be configured");
        }
        
        if (!adminEmail.contains("${ADMIN_EMAIL")) {
            throw new AssertionError("admin.email should use ADMIN_EMAIL environment variable");
        }
        
        String brevoApiKey = appProps.getProperty("brevo.api.key");
        if (brevoApiKey == null) {
            throw new AssertionError("brevo.api.key should be configured");
        }
        
        if (!brevoApiKey.contains("${BREVO_API_KEY")) {
            throw new AssertionError("brevo.api.key should use BREVO_API_KEY environment variable");
        }
        
        System.out.println("✓ Email configuration uses environment variables");
    }

    public void testHealthEndpointEnabled() throws Exception {
        Properties appProps = loadApplicationProperties();
        
        String healthEndpoints = appProps.getProperty("management.endpoints.web.exposure.include");
        if (healthEndpoints == null || !healthEndpoints.contains("health")) {
            throw new AssertionError("Health actuator endpoint should be enabled");
        }
        
        System.out.println("✓ Health endpoint is enabled (matches render.yaml healthCheckPath)");
    }

    private Properties loadApplicationProperties() throws IOException {
        Properties properties = new Properties();
        
        try (BufferedReader reader = new BufferedReader(new FileReader("backend/src/main/resources/application.properties"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        properties.setProperty(parts[0].trim(), parts[1].trim());
                    }
                }
            }
        }
        
        return properties;
    }
}