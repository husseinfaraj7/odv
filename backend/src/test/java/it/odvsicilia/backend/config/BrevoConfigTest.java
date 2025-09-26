package it.odvsicilia.backend.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import sendinblue.ApiClient;
import sibApi.TransactionalEmailsApi;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "brevo.api.key=test-key",
    "brevo.api.base-url=https://api.brevo.com/v3",
    "brevo.api.timeout=30"
})
public class BrevoConfigTest {

    @Test
    public void testImportsAreCorrect() {
        // Test that the imports compile correctly
        assertNotNull(ApiClient.class);
        assertNotNull(TransactionalEmailsApi.class);
    }

    @Test 
    public void testBrevoConfigCanBeInstantiated() {
        // Test that BrevoConfig can be instantiated
        BrevoConfig config = new BrevoConfig();
        assertNotNull(config);
    }
}