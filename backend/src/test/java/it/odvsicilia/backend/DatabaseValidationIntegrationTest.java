package it.odvsicilia.backend;

import it.odvsicilia.backend.config.DatabaseValidationConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "DATABASE_URL=jdbc:postgresql://localhost:5432/testdb?user=testuser&password=testpass"
})
class DatabaseValidationIntegrationTest {

    @Test
    void contextLoads() {
        // This test ensures that the application context starts up correctly
        // with a valid DATABASE_URL, which means our validation passed
    }
}