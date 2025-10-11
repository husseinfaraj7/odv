package it.odvsicilia.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("SupabaseHostnameValidator Integration Tests")
class SupabaseHostnameValidatorIntegrationTest {

    @Autowired
    private SupabaseHostnameValidator validator;

    // ============================================================================
    // MALFORMED HOSTNAME TESTS - Specific patterns from requirements
    // ============================================================================

    @Test
    @DisplayName("Should reject malformed hostname: db.aws-1-eu-north-1.supabase.co")
    void testMalformedHostname_MixedDirectPoolerFormat1() {
        String hostname = "db.aws-1-eu-north-1.supabase.co";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid(), "Should reject mixed direct/pooler format");
        assertTrue(result.isInvalid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("malformed") || result.getErrorMessage().contains("Malformed"),
                "Error message should indicate malformed hostname");
        assertTrue(result.getErrorMessage().contains("DIRECT") || result.getErrorMessage().contains("direct"),
                "Error message should mention direct connection format");
        assertEquals(hostname, result.getHostname());
    }

    @Test
    @DisplayName("Should reject malformed hostname: db.pooler.supabase.com")
    void testMalformedHostname_MixedDirectPoolerFormat2() {
        String hostname = "db.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid(), "Should reject mixed direct/pooler format");
        assertTrue(result.isInvalid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("malformed") || result.getErrorMessage().contains("Malformed"),
                "Error message should indicate malformed hostname");
        assertTrue(result.getErrorMessage().contains("POOLER") || result.getErrorMessage().contains("pooler"),
                "Error message should mention pooler connection format");
        assertEquals(hostname, result.getHostname());
    }

    @Test
    @DisplayName("Should reject malformed hostname: db.aws-0-us-east-1.pooler.supabase.com")
    void testMalformedHostname_DirectPrefixWithPoolerSuffix() {
        String hostname = "db.aws-0-us-east-1.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid(), "Should reject hostname with both 'db.' prefix and pooler suffix");
        assertTrue(result.getErrorMessage().contains("Mixed format") || 
                   result.getErrorMessage().contains("should not start with 'db.'"),
                "Error message should indicate mixed format error");
        assertTrue(result.getErrorMessage().toLowerCase().contains("pooler"),
                "Error message should mention pooler");
    }

    @Test
    @DisplayName("Should reject malformed hostname: aws-1-eu-north-1.supabase.co")
    void testMalformedHostname_PoolerFormatWithDirectDomain() {
        String hostname = "aws-1-eu-north-1.supabase.co";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid(), "Should reject pooler format with .supabase.co domain");
        assertTrue(result.getErrorMessage().contains(".pooler.supabase.com"),
                "Error message should indicate correct pooler domain");
    }

    @Test
    @DisplayName("Should reject malformed hostname: eu-north-1.pooler.supabase.com")
    void testMalformedHostname_MissingPoolerIndex() {
        String hostname = "eu-north-1.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid(), "Should reject pooler hostname without aws-INDEX- prefix");
        assertTrue(result.getErrorMessage().contains("aws-0-") || result.getErrorMessage().contains("aws-1-"),
                "Error message should indicate required aws-INDEX- prefix");
    }

    @Test
    @DisplayName("Should reject malformed hostname: aws-0.pooler.supabase.com")
    void testMalformedHostname_MissingRegion() {
        String hostname = "aws-0.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid(), "Should reject pooler hostname without region");
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should reject malformed hostname: aws-0-us-east.pooler.supabase.com")
    void testMalformedHostname_InvalidRegionFormat() {
        String hostname = "aws-0-us-east.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid(), "Should reject hostname with invalid region format");
        assertTrue(result.getErrorMessage().contains("region") || result.getErrorMessage().contains("REGION"),
                "Error message should mention region format");
    }

    @Test
    @DisplayName("Should reject malformed hostname: projectref.supabase.co")
    void testMalformedHostname_MissingDbPrefix() {
        String hostname = "abcdefghij1234567890.supabase.co";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid(), "Should reject hostname without 'db.' prefix");
        assertTrue(result.getErrorMessage().contains("db."),
                "Error message should mention required 'db.' prefix");
    }

    @Test
    @DisplayName("Should reject malformed hostname: db.short.supabase.co")
    void testMalformedHostname_InvalidProjectRefLength() {
        String hostname = "db.short.supabase.co";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid(), "Should reject hostname with invalid project ref length");
        assertTrue(result.getErrorMessage().contains("20"),
                "Error message should mention required 20-character length");
    }

    @Test
    @DisplayName("Should reject malformed hostname: db.UPPERCASEREFERENCE1.supabase.co")
    void testMalformedHostname_UppercaseProjectRef() {
        String hostname = "db.UPPERCASEREFERENCE1.supabase.co";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid(), "Should reject hostname with uppercase characters");
    }

    // ============================================================================
    // VALID DIRECT CONNECTION HOSTNAMES
    // ============================================================================

    @Test
    @DisplayName("Should accept valid direct hostname: db.abcdefghij1234567890.supabase.co")
    void testValidDirectHostname_Standard() {
        String hostname = "db.abcdefghij1234567890.supabase.co";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertTrue(result.isValid(), "Should accept valid direct connection hostname");
        assertFalse(result.isInvalid());
        assertEquals(SupabaseHostnameValidator.HostnameType.DIRECT, result.getType());
        assertEquals("abcdefghij1234567890", result.getProjectRef());
        assertTrue(result.isDirectConnection());
        assertFalse(result.isPoolerConnection());
        assertNull(result.getErrorMessage());
        assertNull(result.getRegion());
        assertNull(result.getPoolerIndex());
    }

    @Test
    @DisplayName("Should accept valid direct hostname with different project ref")
    void testValidDirectHostname_DifferentProjectRef() {
        String hostname = "db.xyz123abc456def78901.supabase.co";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertTrue(result.isValid());
        assertEquals(SupabaseHostnameValidator.HostnameType.DIRECT, result.getType());
        assertEquals("xyz123abc456def78901", result.getProjectRef());
    }

    @Test
    @DisplayName("Should accept valid direct hostname with all lowercase letters")
    void testValidDirectHostname_AllLowercase() {
        String hostname = "db.abcdefghijklmnopqrst.supabase.co";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertTrue(result.isValid());
        assertEquals("abcdefghijklmnopqrst", result.getProjectRef());
    }

    @Test
    @DisplayName("Should accept valid direct hostname with all numbers")
    void testValidDirectHostname_AllNumbers() {
        String hostname = "db.12345678901234567890.supabase.co";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertTrue(result.isValid());
        assertEquals("12345678901234567890", result.getProjectRef());
    }

    // ============================================================================
    // VALID POOLER CONNECTION HOSTNAMES - Multiple Regions
    // ============================================================================

    @Test
    @DisplayName("Should accept valid pooler hostname: aws-0-eu-north-1.pooler.supabase.com")
    void testValidPoolerHostname_EuNorth1_Index0() {
        String hostname = "aws-0-eu-north-1.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertTrue(result.isValid(), "Should accept valid pooler connection hostname");
        assertFalse(result.isInvalid());
        assertEquals(SupabaseHostnameValidator.HostnameType.POOLER, result.getType());
        assertEquals("eu-north-1", result.getRegion());
        assertEquals("0", result.getPoolerIndex());
        assertTrue(result.isPoolerConnection());
        assertFalse(result.isDirectConnection());
        assertNull(result.getErrorMessage());
        assertNull(result.getProjectRef());
    }

    @Test
    @DisplayName("Should accept valid pooler hostname: aws-1-eu-north-1.pooler.supabase.com")
    void testValidPoolerHostname_EuNorth1_Index1() {
        String hostname = "aws-1-eu-north-1.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertTrue(result.isValid());
        assertEquals(SupabaseHostnameValidator.HostnameType.POOLER, result.getType());
        assertEquals("eu-north-1", result.getRegion());
        assertEquals("1", result.getPoolerIndex());
    }

    @Test
    @DisplayName("Should accept valid pooler hostname: aws-0-us-east-1.pooler.supabase.com")
    void testValidPoolerHostname_UsEast1_Index0() {
        String hostname = "aws-0-us-east-1.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertTrue(result.isValid());
        assertEquals(SupabaseHostnameValidator.HostnameType.POOLER, result.getType());
        assertEquals("us-east-1", result.getRegion());
        assertEquals("0", result.getPoolerIndex());
    }

    @Test
    @DisplayName("Should accept valid pooler hostname: aws-1-us-east-1.pooler.supabase.com")
    void testValidPoolerHostname_UsEast1_Index1() {
        String hostname = "aws-1-us-east-1.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertTrue(result.isValid());
        assertEquals("us-east-1", result.getRegion());
        assertEquals("1", result.getPoolerIndex());
    }

    @Test
    @DisplayName("Should accept valid pooler hostname: aws-0-ap-southeast-1.pooler.supabase.com")
    void testValidPoolerHostname_ApSoutheast1_Index0() {
        String hostname = "aws-0-ap-southeast-1.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertTrue(result.isValid());
        assertEquals(SupabaseHostnameValidator.HostnameType.POOLER, result.getType());
        assertEquals("ap-southeast-1", result.getRegion());
        assertEquals("0", result.getPoolerIndex());
    }

    @Test
    @DisplayName("Should accept valid pooler hostname: aws-1-ap-southeast-1.pooler.supabase.com")
    void testValidPoolerHostname_ApSoutheast1_Index1() {
        String hostname = "aws-1-ap-southeast-1.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertTrue(result.isValid());
        assertEquals("ap-southeast-1", result.getRegion());
        assertEquals("1", result.getPoolerIndex());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "aws-0-eu-west-1.pooler.supabase.com",
        "aws-1-eu-west-1.pooler.supabase.com",
        "aws-0-us-west-1.pooler.supabase.com",
        "aws-1-us-west-2.pooler.supabase.com",
        "aws-0-ap-south-1.pooler.supabase.com",
        "aws-1-eu-central-1.pooler.supabase.com",
        "aws-0-ca-central-1.pooler.supabase.com",
        "aws-1-sa-east-1.pooler.supabase.com"
    })
    @DisplayName("Should accept valid pooler hostnames for various AWS regions")
    void testValidPoolerHostname_VariousRegions(String hostname) {
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertTrue(result.isValid(), "Should accept valid pooler hostname: " + hostname);
        assertEquals(SupabaseHostnameValidator.HostnameType.POOLER, result.getType());
        assertTrue(result.isPoolerConnection());
        assertNotNull(result.getRegion());
        assertNotNull(result.getPoolerIndex());
    }

    // ============================================================================
    // EDGE CASES - Null, Empty, Whitespace
    // ============================================================================

    @Test
    @DisplayName("Should reject null hostname")
    void testEdgeCase_NullHostname() {
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(null);

        assertFalse(result.isValid());
        assertTrue(result.isInvalid());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("null") || result.getErrorMessage().contains("empty"));
    }

    @Test
    @DisplayName("Should reject empty hostname")
    void testEdgeCase_EmptyHostname() {
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate("");

        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should reject whitespace-only hostname")
    void testEdgeCase_WhitespaceHostname() {
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate("   ");

        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should handle hostname with leading/trailing whitespace")
    void testEdgeCase_HostnameWithWhitespace() {
        String hostname = "  db.abcdefghij1234567890.supabase.co  ";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertTrue(result.isValid(), "Should accept valid hostname after trimming whitespace");
        assertEquals("db.abcdefghij1234567890.supabase.co", result.getHostname());
    }

    @Test
    @DisplayName("Should handle uppercase hostname (normalize to lowercase)")
    void testEdgeCase_UppercaseHostname() {
        String hostname = "DB.ABCDEFGHIJ1234567890.SUPABASE.CO";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertTrue(result.isValid(), "Should accept valid hostname after normalizing to lowercase");
        assertEquals("db.abcdefghij1234567890.supabase.co", result.getHostname());
    }

    // ============================================================================
    // EDGE CASES - Wrong Port in Hostname (port should not be in hostname)
    // ============================================================================

    @Test
    @DisplayName("Should reject hostname with port included: db.abc.supabase.co:5432")
    void testEdgeCase_HostnameWithPort() {
        String hostname = "db.abcdefghij1234567890.supabase.co:5432";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid(), "Hostname should not include port specification");
    }

    // ============================================================================
    // ERROR MESSAGE QUALITY TESTS
    // ============================================================================

    @Test
    @DisplayName("Should provide clear error message distinguishing direct vs pooler for mixed format")
    void testErrorMessage_MixedFormatClarification() {
        String hostname = "db.aws-0-eu-north-1.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid());
        String errorMsg = result.getErrorMessage();
        assertTrue(errorMsg.contains("Direct") || errorMsg.contains("direct") ||
                   errorMsg.contains("Pooler") || errorMsg.contains("pooler"),
                "Error message should distinguish between direct and pooler formats");
        assertTrue(errorMsg.contains("db.") && errorMsg.contains("aws-"),
                "Error message should mention both prefixes");
    }

    @Test
    @DisplayName("Should provide error message with valid format examples")
    void testErrorMessage_ContainsExamples() {
        String hostname = "invalid.supabase.co";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid());
        String errorMsg = result.getErrorMessage();
        assertTrue(errorMsg.contains("db.") || errorMsg.contains("aws-"),
                "Error message should contain examples of valid formats");
    }

    @Test
    @DisplayName("Should provide error message mentioning region requirements for pooler")
    void testErrorMessage_PoolerRegionRequirements() {
        String hostname = "aws-0.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid());
        String errorMsg = result.getErrorMessage();
        assertTrue(errorMsg.toLowerCase().contains("region"),
                "Error message should mention region requirement for pooler hostnames");
    }

    @Test
    @DisplayName("Should provide error message mentioning project ref requirements for direct")
    void testErrorMessage_DirectProjectRefRequirements() {
        String hostname = "db.short.supabase.co";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid());
        String errorMsg = result.getErrorMessage();
        assertTrue(errorMsg.contains("20") || errorMsg.toLowerCase().contains("project"),
                "Error message should mention project reference requirements");
    }

    // ============================================================================
    // NON-SUPABASE HOSTNAMES
    // ============================================================================

    @Test
    @DisplayName("Should reject non-Supabase hostname: localhost")
    void testNonSupabaseHostname_Localhost() {
        String hostname = "localhost";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid());
        assertFalse(result.getErrorMessage().contains("malformed") || result.getErrorMessage().contains("Malformed"),
                "Error for non-Supabase hostname should not say 'malformed'");
    }

    @Test
    @DisplayName("Should reject non-Supabase hostname: postgres.example.com")
    void testNonSupabaseHostname_Generic() {
        String hostname = "postgres.example.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }

    // ============================================================================
    // ADDITIONAL MALFORMED PATTERNS
    // ============================================================================

    @Test
    @DisplayName("Should reject malformed hostname: pooler.supabase.com")
    void testMalformedHostname_OnlyPoolerSuffix() {
        String hostname = "pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid());
        assertTrue(result.getErrorMessage().toLowerCase().contains("pooler"));
    }

    @Test
    @DisplayName("Should reject malformed hostname: db.supabase.co")
    void testMalformedHostname_MissingProjectRef() {
        String hostname = "db.supabase.co";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    @DisplayName("Should reject malformed hostname: aws-2-eu-north-1.pooler.supabase.com")
    void testMalformedHostname_InvalidPoolerIndex() {
        String hostname = "aws-2-eu-north-1.pooler.supabase.com";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid(), "Should reject invalid pooler index (only 0 and 1 are valid)");
    }

    @Test
    @DisplayName("Should reject malformed hostname with special characters in project ref")
    void testMalformedHostname_SpecialCharsInProjectRef() {
        String hostname = "db.abc-def_123456789012.supabase.co";
        SupabaseHostnameValidator.HostnameValidationResult result = validator.validate(hostname);

        assertFalse(result.isValid(), "Project ref should only contain alphanumeric characters");
    }

    // ============================================================================
    // BEAN VALIDATION
    // ============================================================================

    @Test
    @DisplayName("SupabaseHostnameValidator bean should be available")
    void testValidatorBeanExists() {
        assertNotNull(validator, "SupabaseHostnameValidator bean should be autowired");
    }

    @Test
    @DisplayName("Validator should be instantiable and functional")
    void testValidatorFunctional() {
        assertNotNull(validator);
        SupabaseHostnameValidator.HostnameValidationResult result = 
            validator.validate("db.abcdefghij1234567890.supabase.co");
        assertNotNull(result);
        assertTrue(result.isValid());
    }

    // ============================================================================
    // ENUM TESTS
    // ============================================================================

    @Test
    @DisplayName("HostnameType enum should have correct descriptions")
    void testHostnameTypeDescriptions() {
        assertEquals("Direct Connection (db.PROJECT_REF.supabase.co)", 
            SupabaseHostnameValidator.HostnameType.DIRECT.getDescription());
        assertEquals("Pooler Connection (aws-INDEX-REGION.pooler.supabase.com)", 
            SupabaseHostnameValidator.HostnameType.POOLER.getDescription());
    }
}
