package it.odvsicilia.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import sendinblue.ApiClient;
import sendinblue.auth.ApiKeyAuth;
import sendinblue.api.TransactionalEmailsApi;

import java.time.Duration;

/**
 * Configuration class for Brevo (formerly Sendinblue) API settings.
 * This handles API-specific configurations separate from SMTP settings for better flexibility.
 */
@org.springframework.context.annotation.Configuration
public class BrevoConfig {

    @org.springframework.beans.factory.annotation.Value("${brevo.api.key}")
    private String apiKey;

    @org.springframework.beans.factory.annotation.Value("${brevo.api.base-url:https://api.brevo.com/v3}")
    private String baseUrl;

    @org.springframework.beans.factory.annotation.Value("${brevo.api.timeout:30}")
    private int timeoutSeconds;

    @org.springframework.beans.factory.annotation.Value("${brevo.api.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @org.springframework.beans.factory.annotation.Value("${brevo.api.retry.delay-seconds:2}")
    private int retryDelaySeconds;

    @org.springframework.beans.factory.annotation.Value("${brevo.api.connection-pool.max-total:20}")
    private int connectionPoolMaxTotal;

    @org.springframework.beans.factory.annotation.Value("${brevo.api.connection-pool.max-per-route:10}")
    private int connectionPoolMaxPerRoute;

    /**
     * Creates the main Brevo API client instance.
     * 
     * @return configured Brevo ApiClient object
     */
    @org.springframework.context.annotation.Bean
    public ApiClient brevoApiClient() {
        ApiClient apiClient = new ApiClient();
        
        // Set custom base URL if different from default
        if (baseUrl != null && !baseUrl.equals("https://api.brevo.com/v3")) {
            apiClient.setBasePath(baseUrl);
        }
        
        // Configure API key authentication
        ApiKeyAuth apiKeyAuth = (ApiKeyAuth) apiClient.getAuthentication("api-key");
        if (apiKeyAuth != null) {
            apiKeyAuth.setApiKey(this.apiKey);
        }
        
        // Configure timeouts
        if (apiClient.getHttpClient() != null) {
            try {
                apiClient.setConnectTimeout(Duration.ofSeconds(timeoutSeconds).toMillis());
                apiClient.setReadTimeout(Duration.ofSeconds(timeoutSeconds).toMillis());
                apiClient.setWriteTimeout(Duration.ofSeconds(timeoutSeconds).toMillis());
            } catch (Exception e) {
                // Fallback if timeout configuration methods are not available
                System.err.println("Warning: Could not configure timeouts for Brevo API client: " + e.getMessage());
            }
        }
        
        return apiClient;
    }

    /**
     * Creates the Brevo Transactional Emails API client.
     * 
     * @param apiClient the Brevo API client
     * @return configured TransactionalEmailsApi instance
     */
    @org.springframework.context.annotation.Bean
    public TransactionalEmailsApi transactionalEmailsApi(ApiClient apiClient) {
        return new TransactionalEmailsApi(apiClient);
    }

    // Getters for configuration properties
    
    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }

    public int getRetryDelaySeconds() {
        return retryDelaySeconds;
    }

    public Duration getRetryDelay() {
        return Duration.ofSeconds(retryDelaySeconds);
    }

    public int getConnectionPoolMaxTotal() {
        return connectionPoolMaxTotal;
    }

    public int getConnectionPoolMaxPerRoute() {
        return connectionPoolMaxPerRoute;
    }

    /**
     * Validates that required API configuration is present.
     * 
     * @return true if configuration is valid
     * @throws IllegalStateException if required configuration is missing
     */
    public boolean validateConfiguration() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("Brevo API key is required but not configured. Set the 'brevo.api.key' property.");
        }
        
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            throw new IllegalStateException("Brevo API base URL is required but not configured. Set the 'brevo.api.base-url' property.");
        }
        
        return true;
    }
}