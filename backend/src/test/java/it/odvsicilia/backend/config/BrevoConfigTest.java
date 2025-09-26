package it.odvsicilia.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import sibApi.ApiClient;
import sibApi.auth.ApiKeyAuth;
import sibApi.TransactionalEmailsApi;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "brevo.api.key=test-api-key-12345",
    "brevo.api.base-url=https://api.brevo.com/v3",
    "brevo.api.timeout=30",
    "brevo.api.retry.max-attempts=3",
    "brevo.api.retry.delay-seconds=2",
    "brevo.api.connection-pool.max-total=20",
    "brevo.api.connection-pool.max-per-route=10"
})
public class BrevoConfigTest {

    @Autowired
    private BrevoConfig brevoConfig;

    @Nested
    @DisplayName("Package Import Tests")
    class ImportTests {
        
        @Test
        @DisplayName("Should successfully import sibApi.ApiClient")
        public void testSibApiApiClientImport() {
            assertNotNull(ApiClient.class);
            ApiClient apiClient = new ApiClient();
            assertNotNull(apiClient);
        }

        @Test
        @DisplayName("Should successfully import sibApi.auth.ApiKeyAuth")
        public void testApiKeyAuthImport() {
            assertNotNull(ApiKeyAuth.class);
            ApiClient apiClient = new ApiClient();
            ApiKeyAuth auth = (ApiKeyAuth) apiClient.getAuthentication("api-key");
            assertNotNull(auth);
        }

        @Test
        @DisplayName("Should successfully import sibApi.TransactionalEmailsApi")
        public void testTransactionalEmailsApiImport() {
            assertNotNull(TransactionalEmailsApi.class);
            ApiClient apiClient = new ApiClient();
            TransactionalEmailsApi api = new TransactionalEmailsApi(apiClient);
            assertNotNull(api);
        }
    }

    @Nested
    @DisplayName("Bean Creation Tests")
    class BeanCreationTests {
        
        @Test
        @DisplayName("Should create BrevoConfig bean with injected properties")
        public void testBrevoConfigBeanCreation() {
            assertNotNull(brevoConfig);
            assertEquals("test-api-key-12345", brevoConfig.getApiKey());
            assertEquals("https://api.brevo.com/v3", brevoConfig.getBaseUrl());
            assertEquals(30, brevoConfig.getTimeoutSeconds());
        }

        @Test
        @DisplayName("Should create brevoApiClient bean with proper configuration")
        public void testBrevoApiClientBeanCreation() {
            ApiClient apiClient = brevoConfig.brevoApiClient();
            
            assertNotNull(apiClient);
            
            // Test API key authentication is configured
            ApiKeyAuth apiKeyAuth = (ApiKeyAuth) apiClient.getAuthentication("api-key");
            assertNotNull(apiKeyAuth);
            assertEquals("test-api-key-12345", apiKeyAuth.getApiKey());
            
            // Test timeouts are set (in milliseconds)
            assertEquals(30000, apiClient.getConnectTimeout());
            assertEquals(30000, apiClient.getReadTimeout());
            assertEquals(30000, apiClient.getWriteTimeout());
        }

        @Test
        @DisplayName("Should create transactionalEmailsApi bean")
        public void testTransactionalEmailsApiBeanCreation() {
            ApiClient apiClient = brevoConfig.brevoApiClient();
            TransactionalEmailsApi transactionalApi = brevoConfig.transactionalEmailsApi(apiClient);
            
            assertNotNull(transactionalApi);
            assertEquals(apiClient, ReflectionTestUtils.getField(transactionalApi, "apiClient"));
        }

        @Test
        @DisplayName("Should create beans with custom base URL")
        public void testCustomBaseUrlConfiguration() {
            // Create config with custom base URL
            BrevoConfig customConfig = new BrevoConfig();
            ReflectionTestUtils.setField(customConfig, "apiKey", "custom-key");
            ReflectionTestUtils.setField(customConfig, "baseUrl", "https://custom.brevo.com/v3");
            ReflectionTestUtils.setField(customConfig, "timeoutSeconds", 45);
            
            ApiClient apiClient = customConfig.brevoApiClient();
            
            assertNotNull(apiClient);
            assertEquals("https://custom.brevo.com/v3", apiClient.getBasePath());
            
            ApiKeyAuth auth = (ApiKeyAuth) apiClient.getAuthentication("api-key");
            assertEquals("custom-key", auth.getApiKey());
        }
    }

    @Nested
    @DisplayName("Configuration Property Tests")
    class ConfigurationPropertyTests {
        
        @Test
        @DisplayName("Should have correct default values from properties")
        public void testConfigurationProperties() {
            assertEquals("test-api-key-12345", brevoConfig.getApiKey());
            assertEquals("https://api.brevo.com/v3", brevoConfig.getBaseUrl());
            assertEquals(30, brevoConfig.getTimeoutSeconds());
            assertEquals(3, brevoConfig.getMaxRetryAttempts());
            assertEquals(2, brevoConfig.getRetryDelaySeconds());
            assertEquals(20, brevoConfig.getConnectionPoolMaxTotal());
            assertEquals(10, brevoConfig.getConnectionPoolMaxPerRoute());
        }

        @Test
        @DisplayName("Should convert retry delay to Duration correctly")
        public void testRetryDelayConversion() {
            Duration expected = Duration.ofSeconds(2);
            assertEquals(expected, brevoConfig.getRetryDelay());
        }
    }

    @Nested
    @DisplayName("Configuration Validation Tests")
    class ValidationTests {
        
        @Test
        @DisplayName("Should validate configuration successfully with valid properties")
        public void testValidConfigurationValidation() {
            assertTrue(brevoConfig.validateConfiguration());
        }

        @Test
        @DisplayName("Should throw exception when API key is null")
        public void testValidationWithNullApiKey() {
            BrevoConfig invalidConfig = new BrevoConfig();
            ReflectionTestUtils.setField(invalidConfig, "apiKey", null);
            ReflectionTestUtils.setField(invalidConfig, "baseUrl", "https://api.brevo.com/v3");
            
            IllegalStateException exception = assertThrows(IllegalStateException.class, 
                invalidConfig::validateConfiguration);
            assertTrue(exception.getMessage().contains("Brevo API key is required"));
        }

        @Test
        @DisplayName("Should throw exception when API key is empty")
        public void testValidationWithEmptyApiKey() {
            BrevoConfig invalidConfig = new BrevoConfig();
            ReflectionTestUtils.setField(invalidConfig, "apiKey", "   ");
            ReflectionTestUtils.setField(invalidConfig, "baseUrl", "https://api.brevo.com/v3");
            
            IllegalStateException exception = assertThrows(IllegalStateException.class, 
                invalidConfig::validateConfiguration);
            assertTrue(exception.getMessage().contains("Brevo API key is required"));
        }

        @Test
        @DisplayName("Should throw exception when base URL is null")
        public void testValidationWithNullBaseUrl() {
            BrevoConfig invalidConfig = new BrevoConfig();
            ReflectionTestUtils.setField(invalidConfig, "apiKey", "valid-key");
            ReflectionTestUtils.setField(invalidConfig, "baseUrl", null);
            
            IllegalStateException exception = assertThrows(IllegalStateException.class, 
                invalidConfig::validateConfiguration);
            assertTrue(exception.getMessage().contains("Brevo API base URL is required"));
        }

        @Test
        @DisplayName("Should throw exception when base URL is empty")
        public void testValidationWithEmptyBaseUrl() {
            BrevoConfig invalidConfig = new BrevoConfig();
            ReflectionTestUtils.setField(invalidConfig, "apiKey", "valid-key");
            ReflectionTestUtils.setField(invalidConfig, "baseUrl", "   ");
            
            IllegalStateException exception = assertThrows(IllegalStateException.class, 
                invalidConfig::validateConfiguration);
            assertTrue(exception.getMessage().contains("Brevo API base URL is required"));
        }
    }

    @Nested
    @DisplayName("API Client Configuration Tests")
    class ApiClientConfigurationTests {
        
        @Test
        @DisplayName("Should configure API client with default base URL when standard URL provided")
        public void testDefaultBaseUrlHandling() {
            BrevoConfig configWithDefaultUrl = new BrevoConfig();
            ReflectionTestUtils.setField(configWithDefaultUrl, "apiKey", "test-key");
            ReflectionTestUtils.setField(configWithDefaultUrl, "baseUrl", "https://api.brevo.com/v3");
            ReflectionTestUtils.setField(configWithDefaultUrl, "timeoutSeconds", 30);
            
            ApiClient apiClient = configWithDefaultUrl.brevoApiClient();
            
            // When using default URL, base path should remain as configured in SDK
            assertNotNull(apiClient);
            assertNotNull(apiClient.getBasePath());
        }

        @Test
        @DisplayName("Should properly configure timeout values")
        public void testTimeoutConfiguration() {
            BrevoConfig configWithCustomTimeout = new BrevoConfig();
            ReflectionTestUtils.setField(configWithCustomTimeout, "apiKey", "test-key");
            ReflectionTestUtils.setField(configWithCustomTimeout, "baseUrl", "https://api.brevo.com/v3");
            ReflectionTestUtils.setField(configWithCustomTimeout, "timeoutSeconds", 60);
            
            ApiClient apiClient = configWithCustomTimeout.brevoApiClient();
            
            assertEquals(60000, apiClient.getConnectTimeout()); // 60 seconds in milliseconds
            assertEquals(60000, apiClient.getReadTimeout());
            assertEquals(60000, apiClient.getWriteTimeout());
        }

        @Test
        @DisplayName("Should configure authentication correctly")
        public void testAuthenticationConfiguration() {
            String testApiKey = "test-authentication-key-12345";
            BrevoConfig configWithAuth = new BrevoConfig();
            ReflectionTestUtils.setField(configWithAuth, "apiKey", testApiKey);
            ReflectionTestUtils.setField(configWithAuth, "baseUrl", "https://api.brevo.com/v3");
            ReflectionTestUtils.setField(configWithAuth, "timeoutSeconds", 30);
            
            ApiClient apiClient = configWithAuth.brevoApiClient();
            ApiKeyAuth apiKeyAuth = (ApiKeyAuth) apiClient.getAuthentication("api-key");
            
            assertNotNull(apiKeyAuth);
            assertEquals(testApiKey, apiKeyAuth.getApiKey());
        }
    }
}