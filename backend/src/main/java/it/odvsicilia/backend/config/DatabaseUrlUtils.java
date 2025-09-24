package it.odvsicilia.backend.config;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Utility class for URL encoding/decoding database connection string components.
 * Provides safe handling of special characters in usernames and passwords.
 */
public final class DatabaseUrlUtils {

    private static final String UTF8_CHARSET = "UTF-8";

    private DatabaseUrlUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * URL decodes a string using UTF-8 charset.
     * 
     * @param value the string to decode
     * @return the decoded string
     * @throws RuntimeException if UTF-8 encoding is not supported or decoding fails
     */
    public static String urlDecode(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            return URLDecoder.decode(value, UTF8_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to URL decode value due to unsupported UTF-8 encoding: " + 
                    e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("URLDecoder: " + e.getMessage(), e);
        }
    }

    /**
     * URL encodes a string using UTF-8 charset.
     * 
     * @param value the string to encode
     * @return the encoded string
     * @throws RuntimeException if UTF-8 encoding is not supported or encoding fails
     */
    public static String urlEncode(String value) {
        if (value == null) {
            return null;
        }
        
        try {
            return URLEncoder.encode(value, UTF8_CHARSET);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to URL encode value due to unsupported UTF-8 encoding: " + 
                    e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to URL encode value: " + e.getMessage(), e);
        }
    }
}