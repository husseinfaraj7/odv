package it.odvsicilia.backend;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple validation test for render.yaml configuration without external dependencies.
 * Uses only standard Java libraries to validate environment variables.
 */
public class RenderYamlSimpleValidationTest {

    private static final Set<String> REQUIRED_ENV_VARS = Set.of(
            "DATABASE_URL",
            "SUPABASE_URL", 
            "SUPABASE_ANON_KEY",
            "SUPABASE_ROLE_KEY",
            "ADMIN_EMAIL",
            "BREVO_API_KEY",
            "PORT"
    );

    public static void main(String[] args) {
        RenderYamlSimpleValidationTest test = new RenderYamlSimpleValidationTest();
        try {
            test.validateRenderYaml();
            System.out.println("✅ All render.yaml validation tests passed!");
        } catch (Exception e) {
            System.err.println("❌ Validation failed: " + e.getMessage());
            System.exit(1);
        }
    }

    public void validateRenderYaml() throws Exception {
        testRenderYamlContainsAllRequiredEnvironmentVariables();
        testRenderYamlContainsPortVariable();
        testWebServiceConfiguration();
        testDatabaseUrlConfiguration();
        testSecretEnvironmentVariablesConfiguration();
        testRenderYamlStructure();
    }

    public void testRenderYamlContainsAllRequiredEnvironmentVariables() throws Exception {
        Set<String> definedEnvVars = parseRenderYamlEnvironmentVariables();
        
        Set<String> missingRequired = new HashSet<>(REQUIRED_ENV_VARS);
        missingRequired.removeAll(definedEnvVars);
        
        if (!missingRequired.isEmpty()) {
            throw new AssertionError("Missing required environment variables in render.yaml: " + missingRequired);
        }
    }

    public void testRenderYamlContainsPortVariable() throws Exception {
        Set<String> definedEnvVars = parseRenderYamlEnvironmentVariables();
        
        if (!definedEnvVars.contains("PORT")) {
            throw new AssertionError("render.yaml should contain PORT environment variable for Render.com deployment");
        }
    }
 
    public void testWebServiceConfiguration() throws Exception {
        String renderContent = readRenderYaml();
        
        if (!renderContent.contains("type: web")) {
            throw new AssertionError("Service should be of type 'web'");
        }
        
        if (!renderContent.contains("env: docker")) {
            throw new AssertionError("Service should use docker environment");
        }
        
        if (!renderContent.contains("healthCheckPath: /actuator/health")) {
            throw new AssertionError("Health check path should be /actuator/health");
        }
        
        if (!renderContent.contains("dockerfilePath:")) {
            throw new AssertionError("Service should have dockerfilePath specified");
        }
    }

    public void testDatabaseUrlConfiguration() throws Exception {
        String renderContent = readRenderYaml();
        
        if (!renderContent.contains("key: DATABASE_URL")) {
            throw new AssertionError("DATABASE_URL should be defined in render.yaml");
        }
        
        Pattern databaseUrlPattern = Pattern.compile("- key: DATABASE_URL\\s+sync: false");
        if (!databaseUrlPattern.matcher(renderContent).find()) {
            throw new AssertionError("DATABASE_URL should have sync: false (managed externally)");
        }
        
        Pattern valuePattern = Pattern.compile("- key: DATABASE_URL\\s+value:");
        if (valuePattern.matcher(renderContent).find()) {
            throw new AssertionError("DATABASE_URL should not have a hardcoded value");
        }
    }

    public void testSecretEnvironmentVariablesConfiguration() throws Exception {
        String renderContent = readRenderYaml();
        
        Set<String> secretVars = Set.of("DATABASE_URL", "SUPABASE_ANON_KEY", "SUPABASE_ROLE_KEY", "ADMIN_EMAIL", "BREVO_API_KEY");
        
        for (String secretVar : secretVars) {
            if (!renderContent.contains("key: " + secretVar)) {
                throw new AssertionError(secretVar + " should be defined in render.yaml");
            }
            
            Pattern secretPattern = Pattern.compile("- key: " + Pattern.quote(secretVar) + "\\s+sync: false");
            if (!secretPattern.matcher(renderContent).find()) {
                throw new AssertionError(secretVar + " should have sync=false (managed externally)");
            }
            
            Pattern valuePattern = Pattern.compile("- key: " + Pattern.quote(secretVar) + "\\s+value:");
            if (valuePattern.matcher(renderContent).find()) {
                throw new AssertionError(secretVar + " should not have a hardcoded value for security");
            }
        }
    }

    public void testRenderYamlStructure() throws Exception {
        String renderContent = readRenderYaml();
        
        if (!renderContent.contains("services:")) {
            throw new AssertionError("render.yaml should have 'services' section");
        }
        
        if (!renderContent.contains("type: web")) {
            throw new AssertionError("Should have a web service");
        }
        
        Pattern webServicePattern = Pattern.compile("type:\\s*web");
        Matcher matcher = webServicePattern.matcher(renderContent);
        int webServiceCount = 0;
        while (matcher.find()) {
            webServiceCount++;
        }
        
        if (webServiceCount != 1) {
            throw new AssertionError("Should have exactly one web service, found: " + webServiceCount);
        }
    }

    private String readRenderYaml() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader("render.yaml"))) {
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
            return content.toString();
        }
    }

    private Set<String> parseRenderYamlEnvironmentVariables() throws IOException {
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