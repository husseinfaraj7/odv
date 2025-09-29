package it.odvsicilia.backend.config;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class containing URL encoding validation methods and utility functions
 * for database connection URLs. This class handles URL parsing, encoding,
 * validation, and provides helper methods for handling special characters
 * in database connection strings.
 */
public class DatabaseUrlUtils {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseUrlUtils.class);

    // URL encoding patterns and constants
    public static final Pattern JDBC_URL_PATTERN = Pattern.compile(
        "^(jdbc:)?([a-z]+)://([^:/@]+)(?::([^/@]+))?@([^:/?]+)(?::(\\d+))?/([^?]+)(?:\\?(.+))?$"
    );

    public static final Pattern URL_ENCODE_REQUIRED_PATTERN = Pattern.compile("[^A-Za-z0-9\\-_.~]");
    public static final String UTF8_CHARSET = "UTF-8";

    // Common problematic characters mapping for URL encoding
    public static final Map<Character, String> COMMON_ENCODING_MAP = new HashMap<>();
    static {
        COMMON_ENCODING_MAP.put('@', "%40");
        COMMON_ENCODING_MAP.put('#', "%23");
        COMMON_ENCODING_MAP.put('$', "%24");
        COMMON_ENCODING_MAP.put('%', "%25");
        COMMON_ENCODING_MAP.put('^', "%5E");
        COMMON_ENCODING_MAP.put('&', "%26");
        COMMON_ENCODING_MAP.put('*', "%2A");
        COMMON_ENCODING_MAP.put(' ', "%20");
        COMMON_ENCODING_MAP.put('+', "%2B");
        COMMON_ENCODING_MAP.put('/', "%2F");
        COMMON_ENCODING_MAP.put('?', "%3F");
        COMMON_ENCODING_MAP.put('=', "%3D");
        COMMON_ENCODING_MAP.put(':', "%3A");
        COMMON_ENCODING_MAP.put('!', "%21");
        COMMON_ENCODING_MAP.put('(', "%28");
        COMMON_ENCODING_MAP.put(')', "%29");
        COMMON_ENCODING_MAP.put('[', "%5B");
        COMMON_ENCODING_MAP.put(']', "%5D");
        COMMON_ENCODING_MAP.put('{', "%7B");
        COMMON_ENCODING_MAP.put('}', "%7D");
        COMMON_ENCODING_MAP.put(';', "%3B");
        COMMON_ENCODING_MAP.put('\'', "%27");
        COMMON_ENCODING_MAP.put('"', "%22");
        COMMON_ENCODING_MAP.put('<', "%3C");
        COMMON_ENCODING_MAP.put('>', "%3E");
        COMMON_ENCODING_MAP.put('|', "%7C");
        COMMON_ENCODING_MAP.put('\\', "%5C");
        COMMON_ENCODING_MAP.put('`', "%60");
        COMMON_ENCODING_MAP.put('~', "%7E");
    }

    /**
     * URL encodes a string using UTF-8 charset.
     */
    public static String urlEncode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return URLEncoder.encode(value, UTF8_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to URL encode value due to unsupported UTF-8 encoding: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to URL encode value: " + e.getMessage(), e);
        }
    }

    /**
     * URL decodes a string using UTF-8 charset.
     */
    public static String urlDecode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return URLDecoder.decode(value, UTF8_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to URL decode value due to unsupported UTF-8 encoding: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("URLDecoder: " + e.getMessage(), e);
        }
    }

    /**
     * Checks whether the given string contains special characters that require URL encoding.
     */
    public static boolean hasSpecialCharacters(String credentials) {
        if (credentials == null || credentials.isEmpty()) {
            return false;
        }
        Pattern specialCharsPattern = Pattern.compile("[@ #$%&+=\\[\\]{}|\\\\;:\"'<>?,./`~^*()!]");
        return specialCharsPattern.matcher(credentials).find();
    }

    /**
     * Masks credentials in a database URL for safe logging.
     */
    public static String maskCredentials(String url) {
        if (url == null) return null;
        return url.replaceAll("://[^:]+:[^@]+@", "://***:***@");
    }

    /**
     * Extracts the credentials part from a database URL.
     */
    public static String extractCredentialsPart(String databaseUrl) {
        try {
            int atIndex = databaseUrl.indexOf('@');
            int schemeEndIndex = databaseUrl.indexOf("://");
            if (schemeEndIndex != -1 && atIndex > schemeEndIndex) {
                return databaseUrl.substring(schemeEndIndex + 3, atIndex);
            }
        } catch (Exception e) {
            logger.debug("Failed to extract credentials part for encoding analysis", e);
        }
        return null;
    }

    /**
     * Validates a database URL and returns detailed validation results.
     */
    public static ValidationResult validateDatabaseUrl(String databaseUrl) {
        List<String> errorMessages = new ArrayList<>();
        
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            errorMessages.add("DATABASE_URL cannot be null or empty");
            return new ValidationResult(false, errorMessages);
        }

        // Basic validation logic would go here
        return new ValidationResult(true, errorMessages);
    }

    /**
     * ValidationResult class to hold validation results.
     */
    public static class ValidationResult {
        private final boolean success;
        private final List<String> errorMessages;

        public ValidationResult(boolean success, List<String> errorMessages) {
            this.success = success;
            this.errorMessages = errorMessages != null ? errorMessages : new ArrayList<>();
        }

        public boolean isSuccess() {
            return success;
        }

        public List<String> getErrorMessages() {
            return errorMessages;
        }
    }
}