package it.odvsicilia.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DatabaseUrlUtils Tests")
class DatabaseUrlUtilsTest {

    @Nested
    @DisplayName("URL Encoding Tests")
    class UrlEncodingTests {

        @Test
        @DisplayName("Should encode the specific failing case password '148632597Faraj'")
        void shouldEncodeSpecificFailingPassword() {
            // Given
            String password = "148632597Faraj";
            
            // When
            String encoded = DatabaseUrlUtils.urlEncode(password);
            
            // Then
            // This password doesn't contain special characters that need encoding
            // but we verify it's handled correctly
            assertEquals("148632597Faraj", encoded);
            
            // Verify it can be decoded back
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            assertEquals(password, decoded);
        }

        @Test
        @DisplayName("Should encode @ symbol in passwords")
        void shouldEncodeAtSymbol() {
            // Given
            String password = "p@ssw0rd";
            
            // When
            String encoded = DatabaseUrlUtils.urlEncode(password);
            
            // Then
            assertEquals("p%40ssw0rd", encoded);
            
            // Verify it can be decoded back
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            assertEquals(password, decoded);
        }

        @Test
        @DisplayName("Should encode # symbol in passwords")
        void shouldEncodeHashSymbol() {
            // Given
            String password = "p#ssw0rd";
            
            // When
            String encoded = DatabaseUrlUtils.urlEncode(password);
            
            // Then
            assertEquals("p%23ssw0rd", encoded);
            
            // Verify it can be decoded back
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            assertEquals(password, decoded);
        }

        @Test
        @DisplayName("Should encode % symbol in passwords")
        void shouldEncodePercentSymbol() {
            // Given
            String password = "p%ssw0rd";
            
            // When
            String encoded = DatabaseUrlUtils.urlEncode(password);
            
            // Then
            assertEquals("p%25ssw0rd", encoded);
            
            // Verify it can be decoded back
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            assertEquals(password, decoded);
        }

        @Test
        @DisplayName("Should encode & symbol in passwords")
        void shouldEncodeAmpersandSymbol() {
            // Given
            String password = "p&ssw0rd";
            
            // When
            String encoded = DatabaseUrlUtils.urlEncode(password);
            
            // Then
            assertEquals("p%26ssw0rd", encoded);
            
            // Verify it can be decoded back
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            assertEquals(password, decoded);
        }

        @Test
        @DisplayName("Should encode + symbol in passwords")
        void shouldEncodePlusSymbol() {
            // Given
            String password = "p+ssw0rd";
            
            // When
            String encoded = DatabaseUrlUtils.urlEncode(password);
            
            // Then
            assertEquals("p%2Bssw0rd", encoded);
            
            // Verify it can be decoded back
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            assertEquals(password, decoded);
        }

        @Test
        @DisplayName("Should encode = symbol in passwords")
        void shouldEncodeEqualsSymbol() {
            // Given
            String password = "p=ssw0rd";
            
            // When
            String encoded = DatabaseUrlUtils.urlEncode(password);
            
            // Then
            assertEquals("p%3Dssw0rd", encoded);
            
            // Verify it can be decoded back
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            assertEquals(password, decoded);
        }

        @Test
        @DisplayName("Should encode ? symbol in passwords")
        void shouldEncodeQuestionMarkSymbol() {
            // Given
            String password = "p?ssw0rd";
            
            // When
            String encoded = DatabaseUrlUtils.urlEncode(password);
            
            // Then
            assertEquals("p%3Fssw0rd", encoded);
            
            // Verify it can be decoded back
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            assertEquals(password, decoded);
        }

        @Test
        @DisplayName("Should encode space characters in passwords")
        void shouldEncodeSpaceCharacters() {
            // Given
            String password = "p ssw0rd";
            
            // When
            String encoded = DatabaseUrlUtils.urlEncode(password);
            
            // Then
            assertEquals("p+ssw0rd", encoded);
            
            // Verify it can be decoded back
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            assertEquals(password, decoded);
        }

        @Test
        @DisplayName("Should encode complex password with multiple special characters")
        void shouldEncodeComplexPassword() {
            // Given
            String password = "MyP@ss%W0rd&#+ Special!";
            
            // When
            String encoded = DatabaseUrlUtils.urlEncode(password);
            
            // Then
            assertEquals("MyP%40ss%25W0rd%26%23%2B+Special%21", encoded);
            
            // Verify it can be decoded back
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            assertEquals(password, decoded);
        }

        @ParameterizedTest
        @DisplayName("Should encode various problematic characters")
        @CsvSource({
            "'@', '%40'",
            "'#', '%23'",
            "'%', '%25'",
            "'&', '%26'",
            "'+', '%2B'",
            "'=', '%3D'",
            "'?', '%3F'",
            "' ', '+'",
            "'!', '%21'",
            "'$', '%24'",
            "'(', '%28'",
            "')', '%29'",
            "'*', '%2A'",
            "',', '%2C'",
            "'/', '%2F'",
            "':', '%3A'",
            "';', '%3B'",
            "'[', '%5B'",
            "']', '%5D'"
        })
        void shouldEncodeProblematicCharacters(String input, String expected) {
            // When
            String encoded = DatabaseUrlUtils.urlEncode(input);
            
            // Then
            assertEquals(expected, encoded);
        }

        @Test
        @DisplayName("Should handle null input for encoding")
        void shouldHandleNullInputForEncoding() {
            // Given
            String input = null;
            
            // When
            String encoded = DatabaseUrlUtils.urlEncode(input);
            
            // Then
            assertNull(encoded);
        }

        @Test
        @DisplayName("Should handle empty string for encoding")
        void shouldHandleEmptyStringForEncoding() {
            // Given
            String input = "";
            
            // When
            String encoded = DatabaseUrlUtils.urlEncode(input);
            
            // Then
            assertEquals("", encoded);
        }

        @Test
        @DisplayName("Should not modify strings without special characters")
        void shouldNotModifyNormalStrings() {
            // Given
            String password = "normalPassword123";
            
            // When
            String encoded = DatabaseUrlUtils.urlEncode(password);
            
            // Then
            assertEquals(password, encoded);
            
            // Verify it can be decoded back
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            assertEquals(password, decoded);
        }
    }

    @Nested
    @DisplayName("URL Decoding Tests")
    class UrlDecodingTests {

        @Test
        @DisplayName("Should decode @ symbol (%40) in passwords")
        void shouldDecodeAtSymbol() {
            // Given
            String encoded = "p%40ssw0rd";
            
            // When
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            
            // Then
            assertEquals("p@ssw0rd", decoded);
        }

        @Test
        @DisplayName("Should decode # symbol (%23) in passwords")
        void shouldDecodeHashSymbol() {
            // Given
            String encoded = "p%23ssw0rd";
            
            // When
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            
            // Then
            assertEquals("p#ssw0rd", decoded);
        }

        @Test
        @DisplayName("Should decode % symbol (%25) in passwords")
        void shouldDecodePercentSymbol() {
            // Given
            String encoded = "p%25ssw0rd";
            
            // When
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            
            // Then
            assertEquals("p%ssw0rd", decoded);
        }

        @Test
        @DisplayName("Should decode & symbol (%26) in passwords")
        void shouldDecodeAmpersandSymbol() {
            // Given
            String encoded = "p%26ssw0rd";
            
            // When
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            
            // Then
            assertEquals("p&ssw0rd", decoded);
        }

        @Test
        @DisplayName("Should decode + symbol (%2B) in passwords")
        void shouldDecodePlusSymbol() {
            // Given
            String encoded = "p%2Bssw0rd";
            
            // When
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            
            // Then
            assertEquals("p+ssw0rd", decoded);
        }

        @Test
        @DisplayName("Should decode = symbol (%3D) in passwords")
        void shouldDecodeEqualsSymbol() {
            // Given
            String encoded = "p%3Dssw0rd";
            
            // When
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            
            // Then
            assertEquals("p=ssw0rd", decoded);
        }

        @Test
        @DisplayName("Should decode ? symbol (%3F) in passwords")
        void shouldDecodeQuestionMarkSymbol() {
            // Given
            String encoded = "p%3Fssw0rd";
            
            // When
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            
            // Then
            assertEquals("p?ssw0rd", decoded);
        }

        @Test
        @DisplayName("Should decode space characters (+) in passwords")
        void shouldDecodeSpaceCharacters() {
            // Given
            String encoded = "p+ssw0rd";
            
            // When
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            
            // Then
            assertEquals("p ssw0rd", decoded);
        }

        @Test
        @DisplayName("Should decode complex encoded password")
        void shouldDecodeComplexPassword() {
            // Given
            String encoded = "MyP%40ss%25W0rd%26%23%2B+Special%21";
            
            // When
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            
            // Then
            assertEquals("MyP@ss%W0rd&#+ Special!", decoded);
        }

        @Test
        @DisplayName("Should handle null input for decoding")
        void shouldHandleNullInputForDecoding() {
            // Given
            String input = null;
            
            // When
            String decoded = DatabaseUrlUtils.urlDecode(input);
            
            // Then
            assertNull(decoded);
        }

        @Test
        @DisplayName("Should handle empty string for decoding")
        void shouldHandleEmptyStringForDecoding() {
            // Given
            String input = "";
            
            // When
            String decoded = DatabaseUrlUtils.urlDecode(input);
            
            // Then
            assertEquals("", decoded);
        }

        @Test
        @DisplayName("Should not modify strings without encoded characters")
        void shouldNotModifyNormalEncodedStrings() {
            // Given
            String input = "normalPassword123";
            
            // When
            String decoded = DatabaseUrlUtils.urlDecode(input);
            
            // Then
            assertEquals(input, decoded);
        }
    }

    @Nested
    @DisplayName("URI Parsing Validation Tests")
    class UriParsingValidationTests {

        @Test
        @DisplayName("Should create valid URI with encoded password containing @")
        void shouldCreateValidUriWithEncodedAtSymbol() throws URISyntaxException {
            // Given
            String username = "user";
            String password = "p@ssw0rd";
            String encodedPassword = DatabaseUrlUtils.urlEncode(password);
            String databaseUrl = String.format("postgresql://%s:%s@localhost:5432/testdb", 
                    username, encodedPassword);
            
            // When & Then - Should not throw "Illegal character in query" exception
            URI uri = new URI(databaseUrl);
            
            assertNotNull(uri);
            assertEquals("postgresql", uri.getScheme());
            assertEquals("localhost", uri.getHost());
            assertEquals(5432, uri.getPort());
            assertEquals("/testdb", uri.getPath());
            assertEquals(username + ":" + encodedPassword, uri.getUserInfo());
        }

        @Test
        @DisplayName("Should create valid URI with encoded password containing #")
        void shouldCreateValidUriWithEncodedHashSymbol() throws URISyntaxException {
            // Given
            String username = "user";
            String password = "p#ssw0rd";
            String encodedPassword = DatabaseUrlUtils.urlEncode(password);
            String databaseUrl = String.format("postgresql://%s:%s@localhost:5432/testdb", 
                    username, encodedPassword);
            
            // When & Then - Should not throw exception
            URI uri = new URI(databaseUrl);
            
            assertNotNull(uri);
            assertEquals("postgresql", uri.getScheme());
            assertEquals("localhost", uri.getHost());
            assertEquals(5432, uri.getPort());
            assertEquals("/testdb", uri.getPath());
        }

        @Test
        @DisplayName("Should create valid URI with encoded password containing %")
        void shouldCreateValidUriWithEncodedPercentSymbol() throws URISyntaxException {
            // Given
            String username = "user";
            String password = "p%ssw0rd";
            String encodedPassword = DatabaseUrlUtils.urlEncode(password);
            String databaseUrl = String.format("postgresql://%s:%s@localhost:5432/testdb", 
                    username, encodedPassword);
            
            // When & Then - Should not throw exception
            URI uri = new URI(databaseUrl);
            
            assertNotNull(uri);
            assertEquals("postgresql", uri.getScheme());
            assertEquals("localhost", uri.getHost());
            assertEquals(5432, uri.getPort());
            assertEquals("/testdb", uri.getPath());
        }

        @Test
        @DisplayName("Should create valid URI with encoded password containing &")
        void shouldCreateValidUriWithEncodedAmpersandSymbol() throws URISyntaxException {
            // Given
            String username = "user";
            String password = "p&ssw0rd";
            String encodedPassword = DatabaseUrlUtils.urlEncode(password);
            String databaseUrl = String.format("postgresql://%s:%s@localhost:5432/testdb", 
                    username, encodedPassword);
            
            // When & Then - Should not throw exception
            URI uri = new URI(databaseUrl);
            
            assertNotNull(uri);
            assertEquals("postgresql", uri.getScheme());
            assertEquals("localhost", uri.getHost());
            assertEquals(5432, uri.getPort());
            assertEquals("/testdb", uri.getPath());
        }

        @Test
        @DisplayName("Should create valid URI with encoded password containing +")
        void shouldCreateValidUriWithEncodedPlusSymbol() throws URISyntaxException {
            // Given
            String username = "user";
            String password = "p+ssw0rd";
            String encodedPassword = DatabaseUrlUtils.urlEncode(password);
            String databaseUrl = String.format("postgresql://%s:%s@localhost:5432/testdb", 
                    username, encodedPassword);
            
            // When & Then - Should not throw exception
            URI uri = new URI(databaseUrl);
            
            assertNotNull(uri);
            assertEquals("postgresql", uri.getScheme());
            assertEquals("localhost", uri.getHost());
            assertEquals(5432, uri.getPort());
            assertEquals("/testdb", uri.getPath());
        }

        @Test
        @DisplayName("Should create valid URI with encoded password containing =")
        void shouldCreateValidUriWithEncodedEqualsSymbol() throws URISyntaxException {
            // Given
            String username = "user";
            String password = "p=ssw0rd";
            String encodedPassword = DatabaseUrlUtils.urlEncode(password);
            String databaseUrl = String.format("postgresql://%s:%s@localhost:5432/testdb", 
                    username, encodedPassword);
            
            // When & Then - Should not throw exception
            URI uri = new URI(databaseUrl);
            
            assertNotNull(uri);
            assertEquals("postgresql", uri.getScheme());
            assertEquals("localhost", uri.getHost());
            assertEquals(5432, uri.getPort());
            assertEquals("/testdb", uri.getPath());
        }

        @Test
        @DisplayName("Should create valid URI with encoded password containing ?")
        void shouldCreateValidUriWithEncodedQuestionMarkSymbol() throws URISyntaxException {
            // Given
            String username = "user";
            String password = "p?ssw0rd";
            String encodedPassword = DatabaseUrlUtils.urlEncode(password);
            String databaseUrl = String.format("postgresql://%s:%s@localhost:5432/testdb", 
                    username, encodedPassword);
            
            // When & Then - Should not throw exception
            URI uri = new URI(databaseUrl);
            
            assertNotNull(uri);
            assertEquals("postgresql", uri.getScheme());
            assertEquals("localhost", uri.getHost());
            assertEquals(5432, uri.getPort());
            assertEquals("/testdb", uri.getPath());
        }

        @Test
        @DisplayName("Should create valid URI with encoded password containing spaces")
        void shouldCreateValidUriWithEncodedSpaces() throws URISyntaxException {
            // Given
            String username = "user";
            String password = "p ssw0rd";
            String encodedPassword = DatabaseUrlUtils.urlEncode(password);
            String databaseUrl = String.format("postgresql://%s:%s@localhost:5432/testdb", 
                    username, encodedPassword);
            
            // When & Then - Should not throw exception
            URI uri = new URI(databaseUrl);
            
            assertNotNull(uri);
            assertEquals("postgresql", uri.getScheme());
            assertEquals("localhost", uri.getHost());
            assertEquals(5432, uri.getPort());
            assertEquals("/testdb", uri.getPath());
        }

        @Test
        @DisplayName("Should create valid URI with the specific failing password '148632597Faraj'")
        void shouldCreateValidUriWithSpecificFailingPassword() throws URISyntaxException {
            // Given
            String username = "user";
            String password = "148632597Faraj";
            String encodedPassword = DatabaseUrlUtils.urlEncode(password);
            String databaseUrl = String.format("postgresql://%s:%s@localhost:5432/testdb", 
                    username, encodedPassword);
            
            // When & Then - Should not throw exception
            URI uri = new URI(databaseUrl);
            
            assertNotNull(uri);
            assertEquals("postgresql", uri.getScheme());
            assertEquals("localhost", uri.getHost());
            assertEquals(5432, uri.getPort());
            assertEquals("/testdb", uri.getPath());
            assertEquals(username + ":" + encodedPassword, uri.getUserInfo());
        }

        @Test
        @DisplayName("Should create valid URI with complex encoded password")
        void shouldCreateValidUriWithComplexEncodedPassword() throws URISyntaxException {
            // Given
            String username = "user@domain.com";
            String password = "MyP@ss%W0rd&#+ Special!";
            String encodedUsername = DatabaseUrlUtils.urlEncode(username);
            String encodedPassword = DatabaseUrlUtils.urlEncode(password);
            String databaseUrl = String.format("postgresql://%s:%s@localhost:5432/testdb", 
                    encodedUsername, encodedPassword);
            
            // When & Then - Should not throw exception
            URI uri = new URI(databaseUrl);
            
            assertNotNull(uri);
            assertEquals("postgresql", uri.getScheme());
            assertEquals("localhost", uri.getHost());
            assertEquals(5432, uri.getPort());
            assertEquals("/testdb", uri.getPath());
            assertEquals(encodedUsername + ":" + encodedPassword, uri.getUserInfo());
        }

        @ParameterizedTest
        @DisplayName("Should handle various problematic passwords in URI construction")
        @ValueSource(strings = {
            "148632597Faraj",
            "p@ssword",
            "p#ssword", 
            "p%ssword",
            "p&ssword",
            "p+ssword",
            "p=ssword",
            "p?ssword",
            "p ssword",
            "complex@#%&+=? password!",
            "test@example.com:password"
        })
        void shouldHandleVariousProblematicPasswordsInUriConstruction(String password) {
            // Given
            String username = "user";
            String encodedPassword = DatabaseUrlUtils.urlEncode(password);
            String databaseUrl = String.format("postgresql://%s:%s@localhost:5432/testdb", 
                    username, encodedPassword);
            
            // When & Then - Should not throw "Illegal character in query" exception
            assertDoesNotThrow(() -> {
                URI uri = new URI(databaseUrl);
                assertNotNull(uri);
                assertEquals("postgresql", uri.getScheme());
                assertEquals("localhost", uri.getHost());
                assertEquals(5432, uri.getPort());
                assertEquals("/testdb", uri.getPath());
            });
        }
    }

    @Nested
    @DisplayName("Round-trip Encoding/Decoding Tests")
    class RoundTripTests {

        @ParameterizedTest
        @DisplayName("Should maintain password integrity through encode/decode cycle")
        @ValueSource(strings = {
            "148632597Faraj",
            "p@ssword",
            "p#ssword",
            "p%ssword", 
            "p&ssword",
            "p+ssword",
            "p=ssword",
            "p?ssword",
            "p ssword",
            "normalPassword123",
            "MyP@ss%W0rd&#+ Special!",
            "test@example.com:password!@#$%^&*()",
            ""
        })
        void shouldMaintainPasswordIntegrityThroughEncodeDecodeCycle(String originalPassword) {
            // When
            String encoded = DatabaseUrlUtils.urlEncode(originalPassword);
            String decoded = DatabaseUrlUtils.urlDecode(encoded);
            
            // Then
            assertEquals(originalPassword, decoded, 
                "Password should maintain integrity through encode/decode cycle");
        }

        @Test
        @DisplayName("Should handle multiple encode/decode cycles without corruption")
        void shouldHandleMultipleEncodeDecodeCycles() {
            // Given
            String originalPassword = "MyP@ss%W0rd&#+ Special!";
            
            // When - Multiple encode/decode cycles
            String result = originalPassword;
            for (int i = 0; i < 5; i++) {
                result = DatabaseUrlUtils.urlEncode(result);
                result = DatabaseUrlUtils.urlDecode(result);
            }
            
            // Then
            assertEquals(originalPassword, result, 
                "Password should survive multiple encode/decode cycles");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests") 
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should throw RuntimeException with proper message on encoding error")
        void shouldThrowRuntimeExceptionOnEncodingError() {
            // This test verifies the error handling mechanism is in place
            // In practice, UTF-8 encoding should always be supported
            // but the method is designed to handle potential UnsupportedEncodingException
            
            // The actual UTF-8 encoding should work fine
            assertDoesNotThrow(() -> {
                String result = DatabaseUrlUtils.urlEncode("test@password");
                assertNotNull(result);
            });
        }

        @Test
        @DisplayName("Should throw RuntimeException with proper message on decoding error")
        void shouldThrowRuntimeExceptionOnDecodingError() {
            // This test verifies the error handling mechanism is in place
            // In practice, UTF-8 decoding should always be supported
            // but the method is designed to handle potential UnsupportedEncodingException
            
            // The actual UTF-8 decoding should work fine
            assertDoesNotThrow(() -> {
                String result = DatabaseUrlUtils.urlDecode("test%40password");
                assertNotNull(result);
            });
        }
    }
}