package it.odvsicilia.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
public class DatabaseUrlConfigTest {

    private DatabaseUrlConfig databaseUrlConfig;
    private MockEnvironment environment;
    private DatabaseConfig databaseConfig;

    @BeforeEach
    public void setUp() {
        environment = new MockEnvironment();
        databaseUrlConfig = new DatabaseUrlConfig(environment);
        databaseConfig = new DatabaseConfig();
    }

    @Test
    public void testTransformDatabaseUrl_WithoutJdbcPrefix() {
        // Given
        environment.setProperty("DATABASE_URL", "postgresql://user:pass@localhost:5432/testdb");
        
        // When
        String result = databaseUrlConfig.transformedDatabaseUrl();
        
        // Then
        assertEquals("jdbc:postgresql://user:pass@localhost:5432/testdb", result);
    }

    @Test
    public void testTransformDatabaseUrl_WithJdbcPrefix() {
        // Given
        environment.setProperty("DATABASE_URL", "jdbc:postgresql://user:pass@localhost:5432/testdb");
        
        // When
        String result = databaseUrlConfig.transformedDatabaseUrl();
        
        // Then
        assertEquals("jdbc:postgresql://user:pass@localhost:5432/testdb", result);
    }

    @Test
    public void testTransformDatabaseUrl_NullUrl() {
        // Given - no DATABASE_URL set
        
        // When
        String result = databaseUrlConfig.transformedDatabaseUrl();
        
        // Then
        assertEquals("jdbc:h2:mem:testdb", result);
    }

    @Test
    public void testTransformDatabaseUrl_EmptyUrl() {
        // Given
        environment.setProperty("DATABASE_URL", "");
        
        // When
        String result = databaseUrlConfig.transformedDatabaseUrl();
        
        // Then
        assertEquals("jdbc:h2:mem:testdb", result);
    }

    @Test
    public void testParseConnectionDetails_WithUrlEncodedCharacters() throws Exception {
        // Test URL decoding with @ symbol (%40) and + symbol (%2B)
        String encodedUrl = "postgresql://user%40domain:pass%2Bword@localhost:5432/testdb";
        
        // Use reflection to access private method for testing
        java.lang.reflect.Method parseMethod = DatabaseConfig.class.getDeclaredMethod("parseConnectionDetails", String.class);
        parseMethod.setAccessible(true);
        
        Object connectionDetails = parseMethod.invoke(databaseConfig, encodedUrl);
        
        // Use reflection to access the username and password fields
        java.lang.reflect.Field usernameField = connectionDetails.getClass().getDeclaredField("username");
        java.lang.reflect.Field passwordField = connectionDetails.getClass().getDeclaredField("password");
        usernameField.setAccessible(true);
        passwordField.setAccessible(true);
        
        String username = (String) usernameField.get(connectionDetails);
        String password = (String) passwordField.get(connectionDetails);
        
        assertEquals("user@domain", username);
        assertEquals("pass+word", password);
    }

    @Test
    public void testParseConnectionDetails_WithSpecialCharacters() throws Exception {
        // Test URL decoding with various special characters
        String encodedUrl = "postgresql://user%21%40%23:pass%24%25%5E%26%2A@localhost:5432/testdb";
        
        java.lang.reflect.Method parseMethod = DatabaseConfig.class.getDeclaredMethod("parseConnectionDetails", String.class);
        parseMethod.setAccessible(true);
        
        Object connectionDetails = parseMethod.invoke(databaseConfig, encodedUrl);
        
        java.lang.reflect.Field usernameField = connectionDetails.getClass().getDeclaredField("username");
        java.lang.reflect.Field passwordField = connectionDetails.getClass().getDeclaredField("password");
        usernameField.setAccessible(true);
        passwordField.setAccessible(true);
        
        String username = (String) usernameField.get(connectionDetails);
        String password = (String) passwordField.get(connectionDetails);
        
        assertEquals("user!@#", username);
        assertEquals("pass$%^&*", password);
    }

    @Test
    public void testParseConnectionDetails_WithoutEncoding() throws Exception {
        // Test that regular strings without encoding still work
        String normalUrl = "postgresql://normaluser:normalpass@localhost:5432/testdb";
        
        java.lang.reflect.Method parseMethod = DatabaseConfig.class.getDeclaredMethod("parseConnectionDetails", String.class);
        parseMethod.setAccessible(true);
        
        Object connectionDetails = parseMethod.invoke(databaseConfig, normalUrl);
        
        java.lang.reflect.Field usernameField = connectionDetails.getClass().getDeclaredField("username");
        java.lang.reflect.Field passwordField = connectionDetails.getClass().getDeclaredField("password");
        usernameField.setAccessible(true);
        passwordField.setAccessible(true);
        
        String username = (String) usernameField.get(connectionDetails);
        String password = (String) passwordField.get(connectionDetails);
        
        assertEquals("normaluser", username);
        assertEquals("normalpass", password);
    }
}