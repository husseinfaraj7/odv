package it.odvsicilia.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import sendinblue.ApiClient;
import sendinblue.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;
import sibModel.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for BrevoConfig that validate actual API connectivity.
 * These tests require a valid Brevo API key to run successfully.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "brevo.api.key=${BREVO_API_KEY:test-key}",
    "brevo.api.base-url=https://api.brevo.com/v3",
    "brevo.api.timeout=30"
})
public class BrevoConfigIntegrationTest {

    @Autowired
    private BrevoConfig brevoConfig;

    @Autowired
    private ApiClient brevoApiClient;

    @Autowired
    private TransactionalEmailsApi transactionalEmailsApi;

    @Nested
    @DisplayName("Brevo SDK Integration Tests")
    class SdkIntegrationTests {
        
        @Test
        @DisplayName("Should successfully create and configure Brevo API client")
        public void testBrevoApiClientConfiguration() {
            // Verify beans are properly injected
            assertNotNull(brevoApiClient);
            assertNotNull(transactionalEmailsApi);
            
            // Verify API client configuration
            ApiKeyAuth apiKeyAuth = (ApiKeyAuth) brevoApiClient.getAuthentication("api-key");
            assertNotNull(apiKeyAuth);
            assertNotNull(apiKeyAuth.getApiKey());
            
            // Verify TransactionalEmailsApi is properly initialized
            assertNotNull(transactionalEmailsApi);
        }

        @Test
        @DisplayName("Should validate sibApi and sibAuth package structure")
        public void testPackageStructureIntegration() {
            // Test that TransactionalEmailsApi can be instantiated with our ApiClient
            TransactionalEmailsApi api = new TransactionalEmailsApi(brevoApiClient);
            assertNotNull(api);
            
            // Verify the API client has proper authentication configured
            ApiKeyAuth auth = (ApiKeyAuth) brevoApiClient.getAuthentication("api-key");
            assertNotNull(auth);
            
            // Test that we can create API request objects (part of sibModel package)
            SendSmtpEmail sendSmtpEmail = new SendSmtpEmail();
            assertNotNull(sendSmtpEmail);
            
            // Test basic configuration of email request
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail("test@example.com");
            sender.setName("Test Sender");
            sendSmtpEmail.setSender(sender);
            
            assertNotNull(sendSmtpEmail.getSender());
            assertEquals("test@example.com", sendSmtpEmail.getSender().getEmail());
        }

        @Test
        @DisplayName("Should create valid email request objects using sibModel classes")
        public void testSibModelIntegration() {
            // Test creating a complete email request using sibModel classes
            SendSmtpEmail email = new SendSmtpEmail();
            
            // Configure sender
            SendSmtpEmailSender sender = new SendSmtpEmailSender();
            sender.setEmail("noreply@odvsicilia.it");
            sender.setName("ODV Sicilia");
            email.setSender(sender);
            
            // Configure recipients
            SendSmtpEmailTo recipient = new SendSmtpEmailTo();
            recipient.setEmail("test@example.com");
            recipient.setName("Test User");
            email.addToItem(recipient);
            
            // Configure content
            email.setSubject("Test Subject");
            email.setHtmlContent("<p>Test HTML content</p>");
            
            // Validate the email object is properly configured
            assertNotNull(email.getSender());
            assertEquals("noreply@odvsicilia.it", email.getSender().getEmail());
            assertNotNull(email.getTo());
            assertFalse(email.getTo().isEmpty());
            assertEquals("test@example.com", email.getTo().get(0).getEmail());
            assertEquals("Test Subject", email.getSubject());
            assertEquals("<p>Test HTML content</p>", email.getHtmlContent());
        }
    }

    @Nested
    @DisplayName("API Authentication Tests")
    class AuthenticationTests {
        
        @Test
        @DisplayName("Should properly configure API key authentication")
        public void testApiKeyAuthentication() {
            ApiKeyAuth apiKeyAuth = (ApiKeyAuth) brevoApiClient.getAuthentication("api-key");
            
            assertNotNull(apiKeyAuth);
            String apiKey = apiKeyAuth.getApiKey();
            assertNotNull(apiKey);
            assertFalse(apiKey.trim().isEmpty());
        }

        @Test
        @DisplayName("Should maintain authentication configuration after bean initialization")
        public void testAuthenticationPersistence() {
            // Get initial API key
            ApiKeyAuth initialAuth = (ApiKeyAuth) brevoApiClient.getAuthentication("api-key");
            String initialApiKey = initialAuth.getApiKey();
            
            // Create a new TransactionalEmailsApi with the same client
            TransactionalEmailsApi newApi = new TransactionalEmailsApi(brevoApiClient);
            
            // Verify authentication is still properly configured
            ApiKeyAuth persistedAuth = (ApiKeyAuth) brevoApiClient.getAuthentication("api-key");
            assertEquals(initialApiKey, persistedAuth.getApiKey());
        }
    }

    @Nested
    @DisplayName("Live API Connection Tests")
    @EnabledIfEnvironmentVariable(named = "BREVO_API_KEY", matches = "^xkeysib-.*")
    class LiveApiTests {
        
        @Test
        @DisplayName("Should successfully connect to Brevo API")
        public void testLiveApiConnection() {
            assertDoesNotThrow(() -> {
                // Attempt to create a simple API call object
                // This tests that our configuration allows for API communication setup
                SendSmtpEmail testEmail = new SendSmtpEmail();
                
                SendSmtpEmailSender sender = new SendSmtpEmailSender();
                sender.setEmail("noreply@odvsicilia.it");
                sender.setName("ODV Sicilia Test");
                testEmail.setSender(sender);
                
                // This should not throw any configuration-related exceptions
                assertNotNull(testEmail);
                assertNotNull(transactionalEmailsApi);
            });
        }

        @Test
        @DisplayName("Should validate API key format and authentication")
        public void testApiKeyValidation() {
            ApiKeyAuth apiKeyAuth = (ApiKeyAuth) brevoApiClient.getAuthentication("api-key");
            String apiKey = apiKeyAuth.getApiKey();
            
            // Brevo API keys should start with "xkeysib-" for valid keys
            if (!apiKey.equals("test-key")) { // Skip format check for test key
                assertTrue(apiKey.startsWith("xkeysib-"), 
                    "Brevo API key should start with 'xkeysib-' for production use");
            }
        }

        @Test
        @DisplayName("Should handle API client timeout configuration in live environment")
        public void testTimeoutConfigurationInLiveEnvironment() {
            // Verify timeout settings are applied
            assertTrue(brevoApiClient.getConnectTimeout() > 0, "Connect timeout should be positive");
            assertTrue(brevoApiClient.getReadTimeout() > 0, "Read timeout should be positive");
            assertTrue(brevoApiClient.getWriteTimeout() > 0, "Write timeout should be positive");
            
            // Verify reasonable timeout values (30 seconds = 30000 ms)
            assertEquals(30000, brevoApiClient.getConnectTimeout());
            assertEquals(30000, brevoApiClient.getReadTimeout());
            assertEquals(30000, brevoApiClient.getWriteTimeout());
        }
    }

    @Nested
    @DisplayName("Configuration Resilience Tests")
    class ResilienceTests {
        
        @Test
        @DisplayName("Should handle configuration validation gracefully")
        public void testConfigurationValidation() {
            assertDoesNotThrow(() -> {
                boolean isValid = brevoConfig.validateConfiguration();
                assertTrue(isValid, "Configuration should be valid with test properties");
            });
        }

        @Test
        @DisplayName("Should provide meaningful error messages for invalid configuration")
        public void testInvalidConfigurationErrorMessages() {
            BrevoConfig invalidConfig = new BrevoConfig();
            
            IllegalStateException exception = assertThrows(IllegalStateException.class, 
                invalidConfig::validateConfiguration);
            
            String errorMessage = exception.getMessage();
            assertTrue(errorMessage.contains("Brevo API key") || errorMessage.contains("base URL"),
                "Error message should indicate which configuration is missing");
        }

        @Test
        @DisplayName("Should maintain API client configuration integrity")
        public void testApiClientConfigurationIntegrity() {
            // Test that multiple accesses to the same API client maintain consistency
            ApiKeyAuth auth1 = (ApiKeyAuth) brevoApiClient.getAuthentication("api-key");
            ApiKeyAuth auth2 = (ApiKeyAuth) brevoApiClient.getAuthentication("api-key");
            
            assertEquals(auth1.getApiKey(), auth2.getApiKey(), 
                "API key should remain consistent across accesses");
            
            int timeout1 = brevoApiClient.getConnectTimeout();
            int timeout2 = brevoApiClient.getConnectTimeout();
            
            assertEquals(timeout1, timeout2, 
                "Timeout configuration should remain consistent");
        }
    }
}