package it.odvsicilia.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseConnectivityValidator Tests")
class DatabaseConnectivityValidatorTest {

    @Test
    @DisplayName("Should validate JDBC URL format successfully")
    void testValidateJdbcUrlFormat() {
        String jdbcUrl = "jdbc:postgresql://localhost:5432/testdb?user=test&password=pass";
        
        DatabaseConnectivityValidator.ValidationResult result = 
            DatabaseConnectivityValidator.validateDatabaseConnectivity(jdbcUrl);
        
        assertNotNull(result);
        assertEquals("localhost", result.getHostname());
        assertEquals(5432, result.getPort());
    }

    @Test
    @DisplayName("Should validate PostgreSQL URL format successfully")
    void testValidatePostgresUrlFormat() {
        String postgresUrl = "postgres://user:pass@localhost:5432/testdb";
        
        DatabaseConnectivityValidator.ValidationResult result = 
            DatabaseConnectivityValidator.validateDatabaseConnectivity(postgresUrl);
        
        assertNotNull(result);
        assertEquals("localhost", result.getHostname());
        assertEquals(5432, result.getPort());
    }

    @Test
    @DisplayName("Should fail validation for null URL")
    void testValidateNullUrl() {
        DatabaseConnectivityValidator.ValidationResult result = 
            DatabaseConnectivityValidator.validateDatabaseConnectivity(null);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("null or empty"));
    }

    @Test
    @DisplayName("Should fail validation for empty URL")
    void testValidateEmptyUrl() {
        DatabaseConnectivityValidator.ValidationResult result = 
            DatabaseConnectivityValidator.validateDatabaseConnectivity("");
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("null or empty"));
    }

    @Test
    @DisplayName("Should fail validation for invalid URL scheme")
    void testValidateInvalidUrlScheme() {
        String invalidUrl = "mysql://localhost:3306/testdb";
        
        DatabaseConnectivityValidator.ValidationResult result = 
            DatabaseConnectivityValidator.validateDatabaseConnectivity(invalidUrl);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Unsupported"));
    }

    @Test
    @DisplayName("Should fail validation for malformed JDBC URL")
    void testValidateMalformedJdbcUrl() {
        String malformedUrl = "jdbc:postgresql://invalid";
        
        DatabaseConnectivityValidator.ValidationResult result = 
            DatabaseConnectivityValidator.validateDatabaseConnectivity(malformedUrl);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("Malformed") || 
                   result.getErrorMessage().contains("could not extract"));
    }

    @Test
    @DisplayName("Should extract hostname and port from complex URL")
    void testExtractHostnameAndPort() {
        String complexUrl = "jdbc:postgresql://db.example.com:5433/production?ssl=true&user=admin&password=secret";
        
        DatabaseConnectivityValidator.ValidationResult result = 
            DatabaseConnectivityValidator.validateDatabaseConnectivity(complexUrl);
        
        assertNotNull(result);
        assertEquals("db.example.com", result.getHostname());
        assertEquals(5433, result.getPort());
    }

    @Test
    @DisplayName("Should handle postgresql:// scheme")
    void testPostgresqlScheme() {
        String postgresqlUrl = "postgresql://user:pass@example.com:5432/mydb";
        
        DatabaseConnectivityValidator.ValidationResult result = 
            DatabaseConnectivityValidator.validateDatabaseConnectivity(postgresqlUrl);
        
        assertNotNull(result);
        assertEquals("example.com", result.getHostname());
        assertEquals(5432, result.getPort());
    }

    @Test
    @DisplayName("Should fail validation for invalid port")
    void testInvalidPort() {
        String invalidPortUrl = "jdbc:postgresql://localhost:99999/testdb";
        
        DatabaseConnectivityValidator.ValidationResult result = 
            DatabaseConnectivityValidator.validateDatabaseConnectivity(invalidPortUrl);
        
        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getErrorMessage().contains("port") && 
                   result.getErrorMessage().contains("out of valid range"));
    }

    @Test
    @DisplayName("Should successfully resolve known good hostname")
    void testResolveKnownGoodHostname() {
        String localhostUrl = "jdbc:postgresql://localhost:5432/testdb";
        
        DatabaseConnectivityValidator.ValidationResult result = 
            DatabaseConnectivityValidator.validateDatabaseConnectivity(localhostUrl, 1000);
        
        assertNotNull(result);
        assertEquals("localhost", result.getHostname());
        assertEquals(5432, result.getPort());
    }
}
