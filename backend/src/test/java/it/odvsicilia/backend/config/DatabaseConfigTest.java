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

    // Note: hasSpecialCharacters method removed from DatabaseConfig

    // Note: validateAndFixDatabaseUrl method removed from DatabaseConfig

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

    // Note: validateDatabaseUrlFormat method removed from DatabaseConfig

    // Tests for existing utility methods

    // Note: validateDatabaseUrl method removed from DatabaseConfig

    // Note: encodeDatabaseCredentials and suggestUrlFixes methods removed from DatabaseConfig

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