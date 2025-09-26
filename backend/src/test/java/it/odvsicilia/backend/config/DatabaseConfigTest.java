package it.odvsicilia.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseConfig Tests")
public class DatabaseConfigTest {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfigTest.class);
    private DatabaseConfig databaseConfig;

    @BeforeEach
    void setUp() {
        databaseConfig = new DatabaseConfig();
    }

    // Tests from upstream for hasSpecialCharacters method
    @Test
    void testHasSpecialCharacters_WithNoSpecialChars() {
        assertFalse(DatabaseConfig.hasSpecialCharacters("simplepassword"));
        assertFalse(DatabaseConfig.hasSpecialCharacters("user123"));
        assertFalse(DatabaseConfig.hasSpecialCharacters("simple_user"));
        assertFalse(DatabaseConfig.hasSpecialCharacters("password123"));
    }

    @Test
    void testHasSpecialCharacters_WithSpecialChars() {
        assertTrue(DatabaseConfig.hasSpecialCharacters("user@domain"));
        assertTrue(DatabaseConfig.hasSpecialCharacters("P@ssw0rd"));
        assertTrue(DatabaseConfig.hasSpecialCharacters("pass#word"));
        assertTrue(DatabaseConfig.hasSpecialCharacters("user$name"));
        assertTrue(DatabaseConfig.hasSpecialCharacters("pass%word"));
        assertTrue(DatabaseConfig.hasSpecialCharacters("user&name"));
        assertTrue(DatabaseConfig.hasSpecialCharacters("pass+word"));
        assertTrue(DatabaseConfig.hasSpecialCharacters("user=name"));
        assertTrue(DatabaseConfig.hasSpecialCharacters("pass word"));
        assertTrue(DatabaseConfig.hasSpecialCharacters("P@ssw0rd#123"));
    }

    @Test
    void testHasSpecialCharacters_WithNullOrEmpty() {
        assertFalse(DatabaseConfig.hasSpecialCharacters(null));
        assertFalse(DatabaseConfig.hasSpecialCharacters(""));
    }

    // Tests from upstream for validateAndFixDatabaseUrl method
    @Test
    void testValidateAndFixDatabaseUrl_NoEncodingNeeded() {
        String simpleUrl = "postgresql://user:password@localhost:5432/testdb";
        String result = DatabaseConfig.validateAndFixDatabaseUrl(simpleUrl);
        assertEquals("jdbc:postgresql://user:password@localhost:5432/testdb", result);
    }

    @Test
    void testValidateAndFixDatabaseUrl_WithSpecialCharsInPassword() {
        String urlWithSpecialChars = "postgresql://user:P@ssw0rd#123@localhost:5432/testdb";
        String result = DatabaseConfig.validateAndFixDatabaseUrl(urlWithSpecialChars);
        assertEquals("jdbc:postgresql://user:P%40ssw0rd%23123@localhost:5432/testdb", result);
    }

    @Test
    void testValidateAndFixDatabaseUrl_WithSpecialCharsInUsername() {
        String urlWithSpecialChars = "postgresql://user@domain:password@localhost:5432/testdb";
        String result = DatabaseConfig.validateAndFixDatabaseUrl(urlWithSpecialChars);
        assertEquals("jdbc:postgresql://user%40domain:password@localhost:5432/testdb", result);
    }

    @Test
    void testValidateAndFixDatabaseUrl_WithSpecialCharsInBoth() {
        String urlWithSpecialChars = "postgresql://user@domain:P@ssw0rd#123@localhost:5432/testdb";
        String result = DatabaseConfig.validateAndFixDatabaseUrl(urlWithSpecialChars);
        assertEquals("jdbc:postgresql://user%40domain:P%40ssw0rd%23123@localhost:5432/testdb", result);
    }

    @Test
    void testValidateAndFixDatabaseUrl_AlreadyJdbcFormat() {
        String jdbcUrl = "jdbc:postgresql://user:P@ssw0rd#123@localhost:5432/testdb";
        String result = DatabaseConfig.validateAndFixDatabaseUrl(jdbcUrl);
        assertEquals("jdbc:postgresql://user:P%40ssw0rd%23123@localhost:5432/testdb", result);
    }

    @Test
    void testValidateAndFixDatabaseUrl_WithPort() {
        String urlWithPort = "postgresql://user:P@ssw0rd@localhost:5432/testdb";
        String result = DatabaseConfig.validateAndFixDatabaseUrl(urlWithPort);
        assertEquals("jdbc:postgresql://user:P%40ssw0rd@localhost:5432/testdb", result);
    }

    @Test
    void testValidateAndFixDatabaseUrl_WithQueryParams() {
        String urlWithQuery = "postgresql://user:P@ssw0rd@localhost:5432/testdb?ssl=true";
        String result = DatabaseConfig.validateAndFixDatabaseUrl(urlWithQuery);
        assertEquals("jdbc:postgresql://user:P%40ssw0rd@localhost:5432/testdb?ssl=true", result);
    }

    @Test
    void testValidateAndFixDatabaseUrl_WithNullInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            DatabaseConfig.validateAndFixDatabaseUrl(null);
        });
    }

    @Test
    void testValidateAndFixDatabaseUrl_WithEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            DatabaseConfig.validateAndFixDatabaseUrl("");
        });
    }

    @Test
    void testValidateAndFixDatabaseUrl_WithInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            DatabaseConfig.validateAndFixDatabaseUrl("invalid-url");
        });
    }

    @Test
    void testValidateAndFixDatabaseUrl_PostgresSchema() {
        String postgresUrl = "postgres://user:P@ssw0rd@localhost:5432/testdb";
        String result = DatabaseConfig.validateAndFixDatabaseUrl(postgresUrl);
        assertEquals("jdbc:postgresql://user:P%40ssw0rd@localhost:5432/testdb", result);
    }

    // Tests from my version for parseConnectionDetails method
    @Test
    @DisplayName("parseConnectionDetails should handle valid URL without encoding")
    void testParseConnectionDetailsValidUrl() throws Exception {
        String validUrl = "postgresql://testuser:testpass@localhost:5432/testdb";
        
        Object result = invokeParseConnectionDetails(validUrl);
        
        assertNotNull(result);
        assertEquals("testuser", getFieldValue(result, "username"));
        assertEquals("testpass", getFieldValue(result, "password"));
    }

    @Test
    @DisplayName("parseConnectionDetails should automatically encode special characters")
    void testParseConnectionDetailsWithSpecialCharacters() throws Exception {
        String urlWithSpecialChars = "postgresql://user123:P@ssw0rd#123@localhost:5432/testdb";
        
        Object result = invokeParseConnectionDetails(urlWithSpecialChars);
        
        assertNotNull(result);
        assertEquals("user123", getFieldValue(result, "username"));
        assertEquals("P@ssw0rd#123", getFieldValue(result, "password"));
    }

    @Test
    @DisplayName("parseConnectionDetails should handle percent characters in password")
    void testParseConnectionDetailsWithPercentInPassword() throws Exception {
        String urlWithPercent = "postgresql://user:pass%word@localhost:5432/testdb";
        
        Object result = invokeParseConnectionDetails(urlWithPercent);
        
        assertNotNull(result);
        assertEquals("user", getFieldValue(result, "username"));
        assertEquals("pass%word", getFieldValue(result, "password"));
    }

    @Test
    @DisplayName("parseConnectionDetails should handle colon in password")
    void testParseConnectionDetailsWithColonInPassword() throws Exception {
        String urlWithColon = "postgresql://user:pass:word@localhost:5432/testdb";
        
        Object result = invokeParseConnectionDetails(urlWithColon);
        
        assertNotNull(result);
        assertEquals("user", getFieldValue(result, "username"));
        assertEquals("pass:word", getFieldValue(result, "password"));
    }

    @Test
    @DisplayName("parseConnectionDetails should handle at symbol in username")
    void testParseConnectionDetailsWithAtInUsername() throws Exception {
        String urlWithAt = "postgresql://user@domain:password@localhost:5432/testdb";
        
        Object result = invokeParseConnectionDetails(urlWithAt);
        
        assertNotNull(result);
        assertEquals("user@domain", getFieldValue(result, "username"));
        assertEquals("password", getFieldValue(result, "password"));
    }

    @Test
    @DisplayName("parseConnectionDetails should handle complex special characters")
    void testParseConnectionDetailsWithComplexSpecialCharacters() throws Exception {
        String complexUrl = "postgresql://user@domain:P@ssw0rd!#$%^&*()@localhost:5432/testdb";
        
        Object result = invokeParseConnectionDetails(complexUrl);
        
        assertNotNull(result);
        assertEquals("user@domain", getFieldValue(result, "username"));
        assertEquals("P@ssw0rd!#$%^&*()", getFieldValue(result, "password"));
    }

    @Test
    @DisplayName("parseConnectionDetails should handle already encoded URLs")
    void testParseConnectionDetailsWithAlreadyEncodedUrl() throws Exception {
        String encodedUrl = "postgresql://user%40domain:P%40ssw0rd%23123@localhost:5432/testdb";
        
        Object result = invokeParseConnectionDetails(encodedUrl);
        
        assertNotNull(result);
        assertEquals("user@domain", getFieldValue(result, "username"));
        assertEquals("P@ssw0rd#123", getFieldValue(result, "password"));
    }

    @Test
    @DisplayName("parseConnectionDetails should throw exception for empty username")
    void testParseConnectionDetailsEmptyUsername() {
        String urlWithEmptyUsername = "postgresql://:password@localhost:5432/testdb";
        
        assertThrows(IllegalArgumentException.class, () -> {
            invokeParseConnectionDetails(urlWithEmptyUsername);
        });
    }

    @Test
    @DisplayName("parseConnectionDetails should throw exception for empty password")
    void testParseConnectionDetailsEmptyPassword() {
        String urlWithEmptyPassword = "postgresql://username:@localhost:5432/testdb";
        
        assertThrows(IllegalArgumentException.class, () -> {
            invokeParseConnectionDetails(urlWithEmptyPassword);
        });
    }

    @Test
    @DisplayName("parseConnectionDetails should throw exception for malformed URL")
    void testParseConnectionDetailsMalformedUrl() {
        String malformedUrl = "invalid://url/format";
        
        assertThrows(IllegalArgumentException.class, () -> {
            invokeParseConnectionDetails(malformedUrl);
        });
    }

    // Helper methods to invoke private methods and access private fields using reflection
    private Object invokeParseConnectionDetails(String url) throws Exception {
        Method method = DatabaseConfig.class.getDeclaredMethod("parseConnectionDetails", String.class);
        method.setAccessible(true);
        return method.invoke(databaseConfig, url);
    }

    private String getFieldValue(Object obj, String fieldName) throws Exception {
        Class<?> clazz = obj.getClass();
        java.lang.reflect.Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(obj);
    }
}