package it.odvsicilia.backend;

import it.odvsicilia.backend.config.DatabaseValidationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseValidationConfigTest {

    private DatabaseValidationConfig validationConfig;
    private DataSource mockDataSource;
    private Connection mockConnection;
    private DatabaseMetaData mockMetaData;

    @BeforeEach
    void setUp() throws Exception {
        mockDataSource = mock(DataSource.class);
        mockConnection = mock(Connection.class);
        mockMetaData = mock(DatabaseMetaData.class);
        
        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isValid(anyInt())).thenReturn(true);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(mockMetaData.getDatabaseProductVersion()).thenReturn("14.0");
        
        validationConfig = new DatabaseValidationConfig(mockDataSource);
    }

    @Test
    void testValidDatabaseUrl() throws Exception {
        // Given
        String validUrl = "jdbc:postgresql://localhost:5432/testdb?user=testuser&password=testpass";
        ReflectionTestUtils.setField(validationConfig, "databaseUrl", validUrl);

        // When & Then
        assertDoesNotThrow(() -> validationConfig.afterPropertiesSet());
    }

    @Test
    void testMissingDatabaseUrl() {
        // Given
        ReflectionTestUtils.setField(validationConfig, "databaseUrl", "");

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> validationConfig.afterPropertiesSet());
        
        assertTrue(exception.getMessage().contains("DATABASE_URL environment variable is missing or empty"));
    }

    @Test
    void testNullDatabaseUrl() {
        // Given
        ReflectionTestUtils.setField(validationConfig, "databaseUrl", null);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> validationConfig.afterPropertiesSet());
        
        assertTrue(exception.getMessage().contains("DATABASE_URL environment variable is missing or empty"));
    }

    @Test
    void testInvalidPrefix() {
        // Given
        String invalidUrl = "jdbc:mysql://localhost:3306/testdb";
        ReflectionTestUtils.setField(validationConfig, "databaseUrl", invalidUrl);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> validationConfig.afterPropertiesSet());
        
        assertTrue(exception.getMessage().contains("DATABASE_URL has unsupported format"));
    }

    @Test
    void testStandardPostgresUrlConversion() throws Exception {
        // Given
        String standardUrl = "postgres://testuser:testpass@localhost:5432/testdb";
        ReflectionTestUtils.setField(validationConfig, "databaseUrl", standardUrl);

        // When & Then
        assertDoesNotThrow(() -> validationConfig.afterPropertiesSet());
    }

    @Test
    void testMissingHost() {
        // Given
        String invalidUrl = "jdbc:postgresql:///testdb";
        ReflectionTestUtils.setField(validationConfig, "databaseUrl", invalidUrl);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> validationConfig.afterPropertiesSet());
        
        assertTrue(exception.getMessage().contains("DATABASE_URL is missing hostname") ||
                  exception.getMessage().contains("DATABASE_URL validation failed"));
    }

    @Test
    void testMissingDatabaseName() {
        // Given
        String invalidUrl = "jdbc:postgresql://localhost:5432/";
        ReflectionTestUtils.setField(validationConfig, "databaseUrl", invalidUrl);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> validationConfig.afterPropertiesSet());
        
        assertTrue(exception.getMessage().contains("DATABASE_URL is missing database name") ||
                  exception.getMessage().contains("DATABASE_URL validation failed"));
    }

    @Test
    void testValidUrlWithoutPort() throws Exception {
        // Given
        String validUrl = "jdbc:postgresql://localhost/testdb?user=testuser&password=testpass";
        ReflectionTestUtils.setField(validationConfig, "databaseUrl", validUrl);

        // When & Then
        assertDoesNotThrow(() -> validationConfig.afterPropertiesSet());
    }

    @Test
    void testValidUrlWithComplexParameters() throws Exception {
        // Given
        String validUrl = "jdbc:postgresql://db.example.com:5432/mydb?user=myuser&password=mypass&ssl=true&sslmode=require";
        ReflectionTestUtils.setField(validationConfig, "databaseUrl", validUrl);

        // When & Then
        assertDoesNotThrow(() -> validationConfig.afterPropertiesSet());
    }

    @Test
    void testInvalidUriSyntax() {
        // Given
        String invalidUrl = "jdbc:postgresql://[invalid-uri-syntax";
        ReflectionTestUtils.setField(validationConfig, "databaseUrl", invalidUrl);

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> validationConfig.afterPropertiesSet());
        
        assertTrue(exception.getMessage().contains("DATABASE_URL has invalid URI syntax"));
    }

    @Test
    void testStandardPostgreSqlUrlConversion() throws Exception {
        // Given
        String standardUrl = "postgresql://localhost:5432/testdb?user=testuser&password=testpass";
        ReflectionTestUtils.setField(validationConfig, "databaseUrl", standardUrl);

        // When & Then
        assertDoesNotThrow(() -> validationConfig.afterPropertiesSet());
    }

    @Test
    void testStandardPostgreSqlUrlWithoutPort() throws Exception {
        // Given
        String standardUrl = "postgresql://localhost/testdb?user=testuser&password=testpass";
        ReflectionTestUtils.setField(validationConfig, "databaseUrl", standardUrl);

        // When & Then
        assertDoesNotThrow(() -> validationConfig.afterPropertiesSet());
    }

    @Test
    void testStandardPostgreSqlUrlWithComplexParameters() throws Exception {
        // Given
        String standardUrl = "postgresql://db.example.com:5432/mydb?user=myuser&password=mypass&ssl=true&sslmode=require";
        ReflectionTestUtils.setField(validationConfig, "databaseUrl", standardUrl);

        // When & Then
        assertDoesNotThrow(() -> validationConfig.afterPropertiesSet());
    }
}