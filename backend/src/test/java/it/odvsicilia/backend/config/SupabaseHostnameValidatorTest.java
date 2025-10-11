package it.odvsicilia.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SupabaseHostnameValidator Tests")
class SupabaseHostnameValidatorTest {

    @Test
    @DisplayName("Should accept valid Supabase direct connection hostname")
    void testValidDirectHostname() {
        String url = "postgres://postgres.project:pass@db.abcdefghijklmnop.supabase.co:5432/postgres";
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(url);
        
        assertTrue(result.isValid());
        assertEquals("Hostname is valid", result.getMessage());
    }

    @Test
    @DisplayName("Should accept valid Supabase pooler connection hostname")
    void testValidPoolerHostname() {
        String url = "postgres://postgres.project:pass@aws-0-eu-north-1.pooler.supabase.com:6543/postgres";
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(url);
        
        assertTrue(result.isValid());
        assertEquals("Hostname is valid", result.getMessage());
    }

    @Test
    @DisplayName("Should reject malformed hostname mixing direct and pooler formats (db.aws-*)")
    void testMalformedHostnameMixingFormats() {
        String url = "postgres://postgres.project:pass@db.aws-0-eu-north-1.pooler.supabase.com:6543/postgres";
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(url);
        
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Malformed Supabase hostname"));
        assertTrue(result.getMessage().contains("db.aws-0-eu-north-1.pooler.supabase.com"));
        assertTrue(result.getMessage().contains("incorrectly mixes direct and pooler formats"));
        assertTrue(result.getMessage().contains("db.{PROJECT_REF}.supabase.co"));
        assertTrue(result.getMessage().contains("aws-0-{REGION}.pooler.supabase.com"));
    }

    @Test
    @DisplayName("Should reject invalid Supabase hostname that doesn't match any valid pattern")
    void testInvalidSupabaseHostname() {
        String url = "postgres://user:pass@invalid.supabase.com:5432/postgres";
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(url);
        
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Invalid Supabase hostname format"));
        assertTrue(result.getMessage().contains("invalid.supabase.com"));
        assertTrue(result.getMessage().contains("db.{PROJECT_REF}.supabase.co"));
        assertTrue(result.getMessage().contains("aws-0-{REGION}.pooler.supabase.com"));
    }

    @Test
    @DisplayName("Should accept non-Supabase hostnames without validation")
    void testNonSupabaseHostname() {
        String url = "postgres://user:pass@localhost:5432/testdb";
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(url);
        
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should accept non-Supabase production hostname")
    void testNonSupabaseProductionHostname() {
        String url = "postgres://user:pass@dpg-12345678-a.oregon-postgres.render.com:5432/mydb";
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(url);
        
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should handle null DATABASE_URL")
    void testNullDatabaseUrl() {
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(null);
        
        assertFalse(result.isValid());
        assertEquals("DATABASE_URL is null or empty", result.getMessage());
    }

    @Test
    @DisplayName("Should handle empty DATABASE_URL")
    void testEmptyDatabaseUrl() {
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate("");
        
        assertFalse(result.isValid());
        assertEquals("DATABASE_URL is null or empty", result.getMessage());
    }

    @Test
    @DisplayName("Should handle malformed URL without scheme")
    void testMalformedUrlWithoutScheme() {
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate("localhost:5432/db");
        
        assertFalse(result.isValid());
        assertEquals("Could not extract hostname from DATABASE_URL", result.getMessage());
    }

    @Test
    @DisplayName("Should accept valid direct hostname with different project reference")
    void testValidDirectHostnameVariation() {
        String url = "postgres://user:pass@db.xyz123abc456.supabase.co:5432/postgres";
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(url);
        
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should accept valid pooler hostname with different region")
    void testValidPoolerHostnameVariation() {
        String url = "postgres://user:pass@aws-0-us-west-1.pooler.supabase.com:6543/postgres";
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(url);
        
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should reject malformed hostname db.aws- with any suffix")
    void testMalformedHostnameWithAwsPrefix() {
        String url = "postgres://user:pass@db.aws-test.supabase.com:5432/postgres";
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(url);
        
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Malformed Supabase hostname"));
        assertTrue(result.getMessage().contains("db.aws-test.supabase.com"));
    }

    @Test
    @DisplayName("Should handle JDBC URL format")
    void testJdbcUrlFormat() {
        String url = "jdbc:postgresql://db.abcdefghijklmnop.supabase.co:5432/postgres?user=test&password=pass";
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(url);
        
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should handle URL with query parameters")
    void testUrlWithQueryParameters() {
        String url = "postgres://user:pass@db.project123.supabase.co:5432/postgres?sslmode=require";
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(url);
        
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("Should reject pooler hostname without aws-0 prefix")
    void testInvalidPoolerHostnameWithoutAwsPrefix() {
        String url = "postgres://user:pass@eu-north-1.pooler.supabase.com:6543/postgres";
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(url);
        
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Invalid Supabase hostname format"));
    }

    @Test
    @DisplayName("Should reject direct hostname without db prefix")
    void testInvalidDirectHostnameWithoutDbPrefix() {
        String url = "postgres://user:pass@project123.supabase.co:5432/postgres";
        SupabaseHostnameValidator.ValidationResult result = SupabaseHostnameValidator.validate(url);
        
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Invalid Supabase hostname format"));
    }
}
