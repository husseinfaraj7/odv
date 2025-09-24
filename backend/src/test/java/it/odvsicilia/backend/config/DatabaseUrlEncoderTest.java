package it.odvsicilia.backend.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseUrlEncoderTest {

    @Test
    void testEncodeUrlWithAlphanumericPassword() {
        String input = "jdbc:postgresql://user:148632597Faraj@localhost:5432/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        // Alphanumeric password should not require encoding
        assertEquals("jdbc:postgresql://user:148632597Faraj@localhost:5432/testdb", result);
    }

    @Test
    void testEncodeUrlWithAtSymbolInPassword() {
        String input = "jdbc:postgresql://user:pass@word@localhost:5432/testdb";
        String expected = "jdbc:postgresql://user:pass%40word@localhost:5432/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(expected, result);
    }

    @Test
    void testEncodeUrlWithPlusSymbolInPassword() {
        String input = "jdbc:postgresql://user:pass+word@localhost:5432/testdb";
        String expected = "jdbc:postgresql://user:pass%2Bword@localhost:5432/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(expected, result);
    }

    @Test
    void testEncodeUrlWithSpecialCharactersInUsername() {
        String input = "jdbc:postgresql://user@domain:password@localhost:5432/testdb";
        String expected = "jdbc:postgresql://user%40domain:password@localhost:5432/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(expected, result);
    }

    @Test
    void testEncodeUrlWithMultipleSpecialCharacters() {
        String input = "jdbc:postgresql://user!@#:pass$%^&*@localhost:5432/testdb";
        String expected = "jdbc:postgresql://user%21%40%23:pass%24%25%5E%26%2A@localhost:5432/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(expected, result);
    }

    @Test
    void testEncodeUrlWithoutSpecialCharacters() {
        String input = "jdbc:postgresql://user:password@localhost:5432/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(input, result);
    }

    @Test
    void testEncodeUrlWithQueryParameters() {
        String input = "jdbc:postgresql://user:pass@word@localhost:5432/testdb?ssl=true&sslmode=require";
        String expected = "jdbc:postgresql://user:pass%40word@localhost:5432/testdb?ssl=true&sslmode=require";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(expected, result);
    }

    @Test
    void testEncodeUrlWithDefaultPort() {
        String input = "jdbc:postgresql://user:pass@word@localhost/testdb";
        String expected = "jdbc:postgresql://user:pass%40word@localhost/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(expected, result);
    }

    @Test
    void testEncodeUrlWithoutJdbcPrefix() {
        String input = "postgresql://user:pass@word@localhost:5432/testdb";
        String expected = "jdbc:postgresql://user:pass%40word@localhost:5432/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(expected, result);
    }

    @Test
    void testEncodeUrlWithPostgresProtocol() {
        String input = "postgres://user:pass@word@localhost:5432/testdb";
        String expected = "jdbc:postgresql://user:pass%40word@localhost:5432/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(expected, result);
    }

    @Test
    void testEncodeUrlWithSupabaseExample() {
        String input = "jdbc:postgresql://postgres:aBc@123+D#fG&h@db.supabasehost.co:5432/postgres";
        String expected = "jdbc:postgresql://postgres:aBc%40123%2BD%23fG%26h@db.supabasehost.co:5432/postgres";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(expected, result);
    }

    @Test
    void testEncodeUrlWithComplexPassword() {
        String input = "jdbc:postgresql://user:148632597Faraj!@#$%^&*()@localhost:5432/testdb";
        String expected = "jdbc:postgresql://user:148632597Faraj%21%40%23%24%25%5E%26%2A%28%29@localhost:5432/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(expected, result);
    }

    @Test
    void testEncodeUrlNullInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            DatabaseUrlEncoder.encodeUrl(null);
        });
    }

    @Test
    void testEncodeUrlEmptyInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            DatabaseUrlEncoder.encodeUrl("");
        });
    }

    @Test
    void testEncodeUrlWhitespaceInput() {
        assertThrows(IllegalArgumentException.class, () -> {
            DatabaseUrlEncoder.encodeUrl("   ");
        });
    }

    @Test
    void testEncodeUrlWithNoCredentials() {
        String input = "jdbc:postgresql://localhost:5432/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(input, result);
    }

    @Test
    void testEncodeUrlWithOnlyUsername() {
        String input = "jdbc:postgresql://user@localhost:5432/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(input, result);
    }

    @Test
    void testEncodeUrlWithSpecialCharactersInUsernameOnly() {
        String input = "jdbc:postgresql://user@domain@localhost:5432/testdb";
        String expected = "jdbc:postgresql://user%40domain@localhost:5432/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(expected, result);
    }

    @Test
    void testEncodeUrlPreservesAlreadyEncodedUrl() {
        String input = "jdbc:postgresql://user:pass%40word@localhost:5432/testdb";
        String result = DatabaseUrlEncoder.encodeUrl(input);
        
        assertEquals(input, result);
    }
}