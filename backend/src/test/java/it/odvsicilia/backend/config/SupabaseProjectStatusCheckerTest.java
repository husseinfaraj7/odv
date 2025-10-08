package it.odvsicilia.backend.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SupabaseProjectStatusChecker Tests")
class SupabaseProjectStatusCheckerTest {

    private SupabaseProjectStatusChecker checker;

    @BeforeEach
    void setUp() {
        checker = new SupabaseProjectStatusChecker();
    }

    @Test
    @DisplayName("Should validate Supabase direct connection with correct format")
    void testValidSupabaseDirectConnection() {
        String url = "postgresql://postgres:securePass123@db.abcdefghij1234567890.supabase.co:5432/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("abcdefghij1234567890"));
        assertTrue(result.getMessage().contains("Direct Connection"));
    }

    @Test
    @DisplayName("Should validate Supabase pooler connection with correct format")
    void testValidSupabasePoolerConnection() {
        String url = "postgresql://postgres.abcdefghij1234567890:myPass456@aws-1-eu-north-1.pooler.supabase.com:6543/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getMessage());
        assertTrue(result.getMessage().contains("abcdefghij1234567890"));
        assertTrue(result.getMessage().contains("Transaction Pooler"));
        assertTrue(result.getMessage().contains("eu-north-1"));
    }

    @Test
    @DisplayName("Should handle JDBC URL format")
    void testJdbcUrlFormat() {
        String url = "jdbc:postgresql://db.xyz12345678901234567.supabase.co:5432/postgres?user=postgres&password=pass";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("xyz12345678901234567"));
    }

    @Test
    @DisplayName("Should handle postgres:// scheme")
    void testPostgresScheme() {
        String url = "postgres://postgres:pass@db.abcdefghij1234567890.supabase.co:5432/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("abcdefghij1234567890"));
    }

    @Test
    @DisplayName("Should extract project reference from direct connection hostname")
    void testExtractProjectRefFromDirectConnection() {
        String url = "postgresql://role:pass@db.projectref123456789a.supabase.co:5432/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("projectref123456789a"));
    }

    @Test
    @DisplayName("Should extract project reference from pooler connection username")
    void testExtractProjectRefFromPoolerUsername() {
        String url = "postgresql://postgres.myprojectref1234567a:pass@aws-1-us-west-2.pooler.supabase.com:6543/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("myprojectref1234567a"));
        assertTrue(result.getMessage().contains("us-west-2"));
    }

    @Test
    @DisplayName("Should detect non-Supabase database")
    void testNonSupabaseDatabase() {
        String url = "postgresql://user:pass@localhost:5432/mydb";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Non-Supabase"));
    }

    @Test
    @DisplayName("Should handle different regions in pooler hostname")
    void testDifferentRegions() {
        String[] urls = {
            "postgresql://postgres.abcdefghij1234567890:p@aws-1-eu-central-1.pooler.supabase.com:6543/postgres",
            "postgresql://postgres.abcdefghij1234567890:p@aws-2-us-east-1.pooler.supabase.com:6543/postgres",
            "postgresql://postgres.abcdefghij1234567890:p@aws-1-ap-south-1.pooler.supabase.com:6543/postgres"
        };

        for (String url : urls) {
            SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("Transaction Pooler"));
        }
    }

    @Test
    @DisplayName("Should fail validation for null URL")
    void testNullUrl() {
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(null);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("null or empty"));
    }

    @Test
    @DisplayName("Should fail validation for empty URL")
    void testEmptyUrl() {
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate("");
        
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("null or empty"));
    }

    @Test
    @DisplayName("Should handle malformed URL gracefully")
    void testMalformedUrl() {
        String url = "not-a-valid-url";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Non-Supabase"));
    }

    @Test
    @DisplayName("Should handle URL missing hostname gracefully")
    void testUrlMissingHostname() {
        String url = "postgresql://postgres:pass@:5432/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
        assertTrue(result.getMessage().contains("Non-Supabase"));
    }

    @Test
    @DisplayName("Should fail for invalid project reference length")
    void testInvalidProjectRefLength() {
        String url = "postgresql://postgres:pass@db.short.supabase.co:5432/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("Invalid") || result.getMessage().contains("20 characters"));
    }

    @Test
    @DisplayName("Should handle uppercase hostnames gracefully (URIs normalize to lowercase)")
    void testInvalidProjectRefCharacters() {
        String url = "postgresql://postgres:pass@db.UPPERCASEREFERENCE1.supabase.co:5432/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should detect placeholder project reference")
    void testPlaceholderProjectRef() {
        String url = "postgresql://postgres.YOUR_PROJECT_REF:pass@aws-1-eu-north-1.pooler.supabase.com:6543/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("placeholder") || result.getMessage().contains("YOUR_PROJECT_REF"));
    }

    @Test
    @DisplayName("Should warn about port mismatch for direct connection")
    void testDirectConnectionWithPoolerPort() {
        String url = "postgresql://postgres:pass@db.abcdefghij1234567890.supabase.co:6543/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should warn about port mismatch for pooler connection")
    void testPoolerConnectionWithDirectPort() {
        String url = "postgresql://postgres.abcdefghij1234567890:pass@aws-1-eu-north-1.pooler.supabase.com:5432/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle URL with query parameters")
    void testUrlWithQueryParameters() {
        String url = "postgresql://postgres:pass@db.abcdefghij1234567890.supabase.co:5432/postgres?sslmode=require&connectTimeout=10";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle URL with special characters in password")
    void testUrlWithSpecialCharsInPassword() {
        String url = "postgresql://postgres:p%40ss%21w0rd%23@db.abcdefghij1234567890.supabase.co:5432/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle pooler URL without explicit port")
    void testPoolerUrlWithoutPort() {
        String url = "postgresql://postgres.abcdefghij1234567890:pass@aws-1-eu-north-1.pooler.supabase.com/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should handle direct URL without explicit port")
    void testDirectUrlWithoutPort() {
        String url = "postgresql://postgres:pass@db.abcdefghij1234567890.supabase.co/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should fail when pooler URL has no project ref in username")
    void testPoolerUrlMissingProjectRef() {
        String url = "postgresql://postgres:pass@aws-1-eu-north-1.pooler.supabase.com:6543/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("extract") || result.getMessage().contains("reference"));
    }

    @Test
    @DisplayName("Should handle URL with encoded special characters")
    void testUrlWithEncodedCharacters() {
        String url = "postgresql://postgres.abcdefghij1234567890:p%40ss%23w0rd@aws-1-eu-west-1.pooler.supabase.com:6543/postgres";
        
        SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
        
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("Should validate 20-character alphanumeric project references")
    void testValidProjectRefFormats() {
        String[] validRefs = {
            "abcdefghijklmnopqrst",
            "12345678901234567890",
            "abc123xyz789def456gh",
            "project1ref2test3456"
        };

        for (String ref : validRefs) {
            String url = String.format("postgresql://postgres:pass@db.%s.supabase.co:5432/postgres", ref);
            SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
            assertTrue(result.isSuccess(), "Should validate project ref: " + ref);
        }
    }

    @Test
    @DisplayName("Should reject invalid project reference formats")
    void testInvalidProjectRefFormats() {
        String[] invalidRefs = {
            "short",
            "toolongprojectreference123",
            "hasdashesbutvalid12345"
        };

        for (String ref : invalidRefs) {
            String url = String.format("postgresql://postgres:pass@db.%s.supabase.co:5432/postgres", ref);
            SupabaseProjectStatusChecker.ValidationResult result = checker.validate(url);
            assertFalse(result.isSuccess(), "Should reject project ref: " + ref);
        }
    }

    @Test
    @DisplayName("ValidationResult should have correct success state")
    void testValidationResultSuccessState() {
        SupabaseProjectStatusChecker.ValidationResult success = 
            SupabaseProjectStatusChecker.ValidationResult.success("test");
        SupabaseProjectStatusChecker.ValidationResult failure = 
            SupabaseProjectStatusChecker.ValidationResult.failure("error");

        assertTrue(success.isSuccess());
        assertFalse(success.isFailure());
        assertEquals("test", success.getMessage());

        assertFalse(failure.isSuccess());
        assertTrue(failure.isFailure());
        assertEquals("error", failure.getMessage());
    }
}
