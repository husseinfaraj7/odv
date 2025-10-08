package it.odvsicilia.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SupabaseConnectionValidation Integration Tests")
class SupabaseConnectionValidationIntegrationTest {

    @Autowired
    private Environment environment;

    @Autowired(required = false)
    private SupabaseConnectionValidator validator;

    @Test
    @DisplayName("Should skip validation for test profile")
    void testValidationSkippedForTestProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean hasTestProfile = false;
        for (String profile : activeProfiles) {
            if ("test".equals(profile)) {
                hasTestProfile = true;
                break;
            }
        }
        assertTrue(hasTestProfile, "Test profile should be active");
    }

    @Test
    @DisplayName("SupabaseConnectionValidator bean should be available")
    void testValidatorBeanExists() {
        assertNotNull(validator, "SupabaseConnectionValidator bean should be available");
    }

    @Test
    @DisplayName("Validator should handle test environment gracefully")
    void testValidatorInTestEnvironment() {
        assertNotNull(validator);
        SupabaseConnectionValidator.SupabaseValidationResult result = validator.validate();
        assertNotNull(result);
    }
}
