package it.odvsicilia.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.ApplicationContext;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("DatabaseConfig Tests")
public class DatabaseConfigTest {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfigTest.class);
    private DatabaseConfig databaseConfig;

    @Mock
    private DataSource mockDataSource;

    @Mock
    private Connection mockConnection;

    @Mock
    private ContextRefreshedEvent mockContextRefreshedEvent;

    @Mock
    private ApplicationContext mockApplicationContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
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

    // Tests for new validation method

    @Test
    @DisplayName("validateDatabaseUrlFormat should return true for valid URL")
    void testValidateDatabaseUrlFormatSuccess() {
        String validUrl = "postgresql://user:password@localhost:5432/testdb";
        
        boolean result = DatabaseConfig.validateDatabaseUrlFormat(validUrl);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("validateDatabaseUrlFormat should return true for URL with special chars that can be encoded")
    void testValidateDatabaseUrlFormatWithEncodableSpecialChars() {
        String urlWithSpecialChars = "postgresql://user:P@ssw0rd#123@localhost:5432/testdb";
        
        boolean result = DatabaseConfig.validateDatabaseUrlFormat(urlWithSpecialChars);
        
        assertTrue(result); // Should return true because encodeDatabaseCredentials can fix it
    }

    @Test
    @DisplayName("validateDatabaseUrlFormat should return false for null URL")
    void testValidateDatabaseUrlFormatNullUrl() {
        boolean result = DatabaseConfig.validateDatabaseUrlFormat(null);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("validateDatabaseUrlFormat should return false for empty URL")
    void testValidateDatabaseUrlFormatEmptyUrl() {
        boolean result = DatabaseConfig.validateDatabaseUrlFormat("");
        
        assertFalse(result);
    }

    @Test
    @DisplayName("validateDatabaseUrlFormat should return false for invalid scheme")
    void testValidateDatabaseUrlFormatInvalidScheme() {
        String invalidSchemeUrl = "mysql://user:password@localhost:3306/testdb";
        
        boolean result = DatabaseConfig.validateDatabaseUrlFormat(invalidSchemeUrl);
        
        assertFalse(result);
    }

    @Test
    @DisplayName("validateDatabaseUrlFormat should return true for postgres scheme")
    void testValidateDatabaseUrlFormatPostgresScheme() {
        String postgresUrl = "postgres://user:password@localhost:5432/testdb";
        
        boolean result = DatabaseConfig.validateDatabaseUrlFormat(postgresUrl);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("validateDatabaseUrlFormat should return true for jdbc postgresql scheme")
    void testValidateDatabaseUrlFormatJdbcPostgresqlScheme() {
        String jdbcUrl = "jdbc:postgresql://user:password@localhost:5432/testdb";
        
        boolean result = DatabaseConfig.validateDatabaseUrlFormat(jdbcUrl);
        
        assertTrue(result);
    }

    @Test
    @DisplayName("validateDatabaseUrlFormat should return true for jdbc postgres scheme")
    void testValidateDatabaseUrlFormatJdbcPostgresScheme() {
        String jdbcUrl = "jdbc:postgres://user:password@localhost:5432/testdb";
        
        boolean result = DatabaseConfig.validateDatabaseUrlFormat(jdbcUrl);
        
        assertTrue(result);
    }

    // Tests for existing utility methods

    @Test
    @DisplayName("validateDatabaseUrl should return success for valid URL")
    void testValidateDatabaseUrlSuccess() {
        String validUrl = "postgresql://user:password@localhost:5432/testdb";
        
        DatabaseConfig.ValidationResult result = DatabaseConfig.validateDatabaseUrl(validUrl);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getErrorMessages().isEmpty());
    }

    @Test
    @DisplayName("validateDatabaseUrl should detect special characters in credentials")
    void testValidateDatabaseUrlDetectsSpecialChars() {
        String urlWithSpecialChars = "postgresql://user:P@ssw0rd#123@localhost:5432/testdb";
        
        DatabaseConfig.ValidationResult result = DatabaseConfig.validateDatabaseUrl(urlWithSpecialChars);
        
        assertFalse(result.isSuccess());
        assertFalse(result.getErrorMessages().isEmpty());
        assertTrue(result.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("special characters") && msg.contains("Password")));
    }

    @Test
    @DisplayName("validateDatabaseUrl should detect missing components")
    void testValidateDatabaseUrlMissingComponents() {
        String urlMissingDatabase = "postgresql://user:password@localhost:5432/";
        
        DatabaseConfig.ValidationResult result = DatabaseConfig.validateDatabaseUrl(urlMissingDatabase);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("Database name is missing")));
    }

    @Test
    @DisplayName("validateDatabaseUrl should handle null input")
    void testValidateDatabaseUrlNullInput() {
        DatabaseConfig.ValidationResult result = DatabaseConfig.validateDatabaseUrl(null);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessages().stream()
                .anyMatch(msg -> msg.contains("cannot be null or empty")));
    }

    @Test
    @DisplayName("encodeDatabaseCredentials should properly encode special characters")
    void testEncodeDatabaseCredentials() {
        String urlWithSpecialChars = "postgresql://user@domain:P@ssw0rd#123@localhost:5432/testdb";
        
        String encoded = DatabaseConfig.encodeDatabaseCredentials(urlWithSpecialChars);
        
        assertEquals("jdbc:postgresql://user%40domain:P%40ssw0rd%23123@localhost:5432/testdb", encoded);
    }

    @Test
    @DisplayName("encodeDatabaseCredentials should preserve URL structure")
    void testEncodeDatabaseCredentialsPreservesStructure() {
        String urlWithQuery = "postgresql://user:pass@localhost:5432/db?ssl=true";
        
        String encoded = DatabaseConfig.encodeDatabaseCredentials(urlWithQuery);
        
        assertEquals("jdbc:postgresql://user:pass@localhost:5432/db?ssl=true", encoded);
    }

    @Test
    @DisplayName("encodeDatabaseCredentials should handle null input")
    void testEncodeDatabaseCredentialsNullInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            DatabaseConfig.encodeDatabaseCredentials(null);
        });
    }

    @Test
    @DisplayName("suggestUrlFixes should provide specific encoding suggestions")
    void testSuggestUrlFixes() {
        String urlWithSpecialChars = "postgresql://user:P@ssw0rd#123@localhost:5432/testdb";
        
        java.util.List<String> suggestions = DatabaseConfig.suggestUrlFixes(urlWithSpecialChars);
        
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.stream()
                .anyMatch(msg -> msg.contains("@") && msg.contains("%40")));
        assertTrue(suggestions.stream()
                .anyMatch(msg -> msg.contains("#") && msg.contains("%23")));
    }

    @Test
    @DisplayName("suggestUrlFixes should handle valid URL")
    void testSuggestUrlFixesValidUrl() {
        String validUrl = "postgresql://user:password@localhost:5432/testdb";
        
        java.util.List<String> suggestions = DatabaseConfig.suggestUrlFixes(validUrl);
        
        assertTrue(suggestions.stream()
                .anyMatch(msg -> msg.contains("properly formatted")));
    }

    @Test
    @DisplayName("suggestUrlFixes should handle null input")
    void testSuggestUrlFixesNullInput() {
        java.util.List<String> suggestions = DatabaseConfig.suggestUrlFixes(null);
        
        assertFalse(suggestions.isEmpty());
        assertTrue(suggestions.stream()
                .anyMatch(msg -> msg.contains("Provide a valid DATABASE_URL")));
    }

    @Test
    @DisplayName("ValidationResult should properly store success status and error messages")
    void testValidationResult() {
        java.util.List<String> errors = java.util.Arrays.asList("Error 1", "Error 2");
        DatabaseConfig.ValidationResult result = new DatabaseConfig.ValidationResult(false, errors);
        
        assertFalse(result.isSuccess());
        assertEquals(2, result.getErrorMessages().size());
        assertTrue(result.getErrorMessages().contains("Error 1"));
        assertTrue(result.getErrorMessages().contains("Error 2"));
        
        // Test immutability
        java.util.List<String> returnedErrors = result.getErrorMessages();
        assertThrows(UnsupportedOperationException.class, () -> {
            returnedErrors.add("New Error");
        });
    }

    @Test
    @DisplayName("ValidationResult toString should provide meaningful output")
    void testValidationResultToString() {
        java.util.List<String> errors = java.util.Arrays.asList("Test error");
        DatabaseConfig.ValidationResult result = new DatabaseConfig.ValidationResult(false, errors);
        
        String toString = result.toString();
        assertTrue(toString.contains("success=false"));
        assertTrue(toString.contains("Test error"));
    }

    @Test
    @DisplayName("Test database connection validation with valid connection")
    void testDatabaseConnectionValidation_Success() throws Exception {
        // Setup mock connection to simulate successful database connection
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(10)).thenReturn(true);
        when(mockConnection.getMetaData()).thenReturn(mock(java.sql.DatabaseMetaData.class));
        when(mockConnection.getMetaData().getDatabaseProductName()).thenReturn("PostgreSQL");
        when(mockConnection.getMetaData().getDatabaseProductVersion()).thenReturn("13.7");
        when(mockConnection.getMetaData().getDriverName()).thenReturn("PostgreSQL JDBC Driver");
        when(mockConnection.getMetaData().getDriverVersion()).thenReturn("42.3.0");
        when(mockConnection.getMetaData().getURL()).thenReturn("jdbc:postgresql://localhost:5432/testdb");
        
        // Mock prepared statement for SELECT 1 test
        java.sql.PreparedStatement mockStatement = mock(java.sql.PreparedStatement.class);
        java.sql.ResultSet mockResultSet = mock(java.sql.ResultSet.class);
        when(mockConnection.prepareStatement("SELECT 1 as test_value")).thenReturn(mockStatement);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getInt("test_value")).thenReturn(1);

        // Test should complete without throwing exception
        Method testConnectionMethod = DatabaseConfig.class.getDeclaredMethod("testDatabaseConnection", DataSource.class, String.class);
        testConnectionMethod.setAccessible(true);
        
        // This should not throw any exception
        assertDoesNotThrow(() -> {
            try {
                testConnectionMethod.invoke(databaseConfig, mockDataSource, "postgresql://user:pass@localhost:5432/testdb");
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });

        // Verify interactions
        verify(mockDataSource).getConnection();
        verify(mockConnection).isValid(10);
        verify(mockConnection).close();
        verify(mockStatement).executeQuery();
        verify(mockResultSet).next();
        verify(mockResultSet).getInt("test_value");
    }

    @Test
    @DisplayName("Test database connection validation with failed connection")
    void testDatabaseConnectionValidation_Failure() throws Exception {
        // Setup mock to simulate connection failure
        SQLException testException = new SQLException("Connection refused", "08001", 1);
        when(mockDataSource.getConnection()).thenThrow(testException);

        // Test should throw RuntimeException with the original SQLException as cause
        Method testConnectionMethod = DatabaseConfig.class.getDeclaredMethod("testDatabaseConnection", DataSource.class, String.class);
        testConnectionMethod.setAccessible(true);
        
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            try {
                testConnectionMethod.invoke(databaseConfig, mockDataSource, "postgresql://user:pass@localhost:5432/testdb");
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });

        // Verify the error message and cause
        assertTrue(thrown.getMessage().contains("Database connection test failed during application startup"));
        assertTrue(thrown.getCause() instanceof SQLException);
        assertEquals("Connection refused", thrown.getCause().getMessage());

        // Verify interactions
        verify(mockDataSource).getConnection();
    }

    @Test
    @DisplayName("Test database connection validation with invalid connection")
    void testDatabaseConnectionValidation_InvalidConnection() throws Exception {
        // Setup mock connection that fails validation
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(10)).thenReturn(false);

        // Test should throw SQLException for invalid connection
        Method testConnectionMethod = DatabaseConfig.class.getDeclaredMethod("testDatabaseConnection", DataSource.class, String.class);
        testConnectionMethod.setAccessible(true);
        
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            try {
                testConnectionMethod.invoke(databaseConfig, mockDataSource, "postgresql://user:pass@localhost:5432/testdb");
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });

        // Verify the error details
        assertTrue(thrown.getMessage().contains("Database connection test failed during application startup"));
        assertTrue(thrown.getCause() instanceof SQLException);
        assertTrue(thrown.getCause().getMessage().contains("connection.isValid() returned false"));

        // Verify interactions
        verify(mockDataSource).getConnection();
        verify(mockConnection).isValid(10);
        verify(mockConnection).close();
    }

    @Test
    @DisplayName("Test connection failure analysis for authentication errors")
    void testConnectionFailureAnalysis_AuthenticationError() throws Exception {
        // Create authentication failure SQLException
        SQLException authException = new SQLException("password authentication failed for user 'testuser'", "28P01", 0);
        when(mockDataSource.getConnection()).thenThrow(authException);

        Method testConnectionMethod = DatabaseConfig.class.getDeclaredMethod("testDatabaseConnection", DataSource.class, String.class);
        testConnectionMethod.setAccessible(true);
        
        // The method should still throw but with proper analysis logging
        assertThrows(RuntimeException.class, () -> {
            try {
                testConnectionMethod.invoke(databaseConfig, mockDataSource, "postgresql://user:P@ssw0rd@localhost:5432/testdb");
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });

        // Verify the connection attempt was made
        verify(mockDataSource).getConnection();
    }

    @Test
    @DisplayName("Test connection failure analysis for network connectivity errors")
    void testConnectionFailureAnalysis_NetworkError() throws Exception {
        // Create network connectivity SQLException
        SQLException networkException = new SQLException("Connection refused: connect", "08001", 1);
        when(mockDataSource.getConnection()).thenThrow(networkException);

        Method testConnectionMethod = DatabaseConfig.class.getDeclaredMethod("testDatabaseConnection", DataSource.class, String.class);
        testConnectionMethod.setAccessible(true);
        
        assertThrows(RuntimeException.class, () -> {
            try {
                testConnectionMethod.invoke(databaseConfig, mockDataSource, "postgresql://user:pass@localhost:5432/testdb");
            } catch (Exception e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new RuntimeException(e);
            }
        });

        verify(mockDataSource).getConnection();
    }

    @Test
    @DisplayName("Test post-startup event listener")
    void testPostStartupValidation() throws Exception {
        // Setup environment variable
        System.setProperty("DATABASE_URL", "postgresql://user:pass@localhost:5432/testdb");
        
        // Setup mocks
        when(mockContextRefreshedEvent.getApplicationContext()).thenReturn(mockApplicationContext);
        when(mockApplicationContext.getBean(DataSource.class)).thenReturn(mockDataSource);
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(5)).thenReturn(true);

        // Test should complete without exception
        assertDoesNotThrow(() -> {
            databaseConfig.onApplicationStartup(mockContextRefreshedEvent);
        });

        // Verify interactions
        verify(mockContextRefreshedEvent).getApplicationContext();
        verify(mockApplicationContext).getBean(DataSource.class);
        verify(mockDataSource).getConnection();
        verify(mockConnection).isValid(5);
        verify(mockConnection).close();
        
        // Cleanup
        System.clearProperty("DATABASE_URL");
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