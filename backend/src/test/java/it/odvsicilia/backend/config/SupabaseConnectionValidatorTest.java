package it.odvsicilia.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SupabaseConnectionValidator Tests")
class SupabaseConnectionValidatorTest {

    @Test
    @DisplayName("Should return correct connection mode when both available")
    void testConnectionModeBoth() {
        SupabaseConnectionValidator.SupabaseValidationResult result = 
            SupabaseConnectionValidator.SupabaseValidationResult.of(true, true, "hostname", null, null);

        assertTrue(result.isPoolerAvailable());
        assertTrue(result.isDirectAvailable());
        assertTrue(result.isAnyAvailable());
        assertTrue(result.isBothAvailable());
        assertEquals(SupabaseConnectionValidator.ConnectionMode.BOTH, result.getAvailableConnectionMode());
    }

    @Test
    @DisplayName("Should return correct connection mode when only pooler available")
    void testConnectionModePoolerOnly() {
        SupabaseConnectionValidator.SupabaseValidationResult result = 
            SupabaseConnectionValidator.SupabaseValidationResult.of(true, false, "hostname", null, "direct error");

        assertTrue(result.isPoolerAvailable());
        assertFalse(result.isDirectAvailable());
        assertTrue(result.isAnyAvailable());
        assertFalse(result.isBothAvailable());
        assertEquals(SupabaseConnectionValidator.ConnectionMode.POOLER, result.getAvailableConnectionMode());
    }

    @Test
    @DisplayName("Should return correct connection mode when only direct available")
    void testConnectionModeDirectOnly() {
        SupabaseConnectionValidator.SupabaseValidationResult result = 
            SupabaseConnectionValidator.SupabaseValidationResult.of(false, true, "hostname", "pooler error", null);

        assertFalse(result.isPoolerAvailable());
        assertTrue(result.isDirectAvailable());
        assertTrue(result.isAnyAvailable());
        assertFalse(result.isBothAvailable());
        assertEquals(SupabaseConnectionValidator.ConnectionMode.DIRECT, result.getAvailableConnectionMode());
    }

    @Test
    @DisplayName("Should return correct connection mode when neither available")
    void testConnectionModeNeither() {
        SupabaseConnectionValidator.SupabaseValidationResult result = 
            SupabaseConnectionValidator.SupabaseValidationResult.of(false, false, "hostname", "pooler error", "direct error");

        assertFalse(result.isPoolerAvailable());
        assertFalse(result.isDirectAvailable());
        assertFalse(result.isAnyAvailable());
        assertFalse(result.isBothAvailable());
        assertEquals(SupabaseConnectionValidator.ConnectionMode.NEITHER, result.getAvailableConnectionMode());
    }

    @Test
    @DisplayName("Should provide error messages when connections fail")
    void testErrorMessages() {
        SupabaseConnectionValidator.SupabaseValidationResult result = 
            SupabaseConnectionValidator.SupabaseValidationResult.of(
                false, false, "db.example.com", "Pooler timeout", "Direct refused");

        assertEquals("Pooler timeout", result.getPoolerError());
        assertEquals("Direct refused", result.getDirectError());
    }

    @Test
    @DisplayName("Should provide null error messages when connections succeed")
    void testNoErrorMessages() {
        SupabaseConnectionValidator.SupabaseValidationResult result = 
            SupabaseConnectionValidator.SupabaseValidationResult.of(
                true, true, "db.example.com", null, null);

        assertNull(result.getPoolerError());
        assertNull(result.getDirectError());
    }

    @Test
    @DisplayName("Should store hostname correctly")
    void testHostnameStorage() {
        String expectedHostname = "db.example.supabase.co";
        SupabaseConnectionValidator.SupabaseValidationResult result = 
            SupabaseConnectionValidator.SupabaseValidationResult.of(
                true, true, expectedHostname, null, null);

        assertEquals(expectedHostname, result.getHostname());
    }

    @Test
    @DisplayName("ConnectionMode enum should have correct descriptions")
    void testConnectionModeDescriptions() {
        assertEquals("Transaction Pooler (port 6543)", 
            SupabaseConnectionValidator.ConnectionMode.POOLER.getDescription());
        assertEquals("Direct Connection (port 5432)", 
            SupabaseConnectionValidator.ConnectionMode.DIRECT.getDescription());
        assertEquals("Both Pooler and Direct", 
            SupabaseConnectionValidator.ConnectionMode.BOTH.getDescription());
        assertEquals("Neither Pooler nor Direct", 
            SupabaseConnectionValidator.ConnectionMode.NEITHER.getDescription());
    }

    @Test
    @DisplayName("Should handle mixed success scenarios")
    void testMixedScenarios() {
        SupabaseConnectionValidator.SupabaseValidationResult resultPoolerOnly = 
            SupabaseConnectionValidator.SupabaseValidationResult.of(true, false, "host1", null, "Direct failed");
        
        assertFalse(resultPoolerOnly.isBothAvailable());
        assertTrue(resultPoolerOnly.isAnyAvailable());
        
        SupabaseConnectionValidator.SupabaseValidationResult resultDirectOnly = 
            SupabaseConnectionValidator.SupabaseValidationResult.of(false, true, "host2", "Pooler failed", null);
        
        assertFalse(resultDirectOnly.isBothAvailable());
        assertTrue(resultDirectOnly.isAnyAvailable());
    }
}
