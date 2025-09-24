package it.odvsicilia.backend.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for encoding database connection URLs to handle special characters
 * in usernames and passwords that can cause "Illegal character in query" exceptions.
 */
public final class DatabaseUrlEncoder {

    private static final Pattern JDBC_URL_PATTERN = Pattern.compile(
        "^(jdbc:)?([a-z]+)://([^:/@]+)(?::([^/@]+))?@([^:/?]+)(?::(\\d+))?/([^?]+)(?:\\?(.+))?$"
    );
    
    private static final Pattern URL_ENCODE_REQUIRED_PATTERN = Pattern.compile("[^A-Za-z0-9\\-_.~]");

    private DatabaseUrlEncoder() {
        // Utility class - prevent instantiation
    }

    /**
     * Encodes a database connection URL by properly URL-encoding the username and password
     * segments while preserving the overall URL structure.
     * 
     * @param connectionUrl the database connection URL to encode
     * @return the properly encoded database URL
     * @throws IllegalArgumentException if the URL format is invalid or unsupported
     */
    public static String encodeUrl(String connectionUrl) {
        if (connectionUrl == null || connectionUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Connection URL cannot be null or empty");
        }

        String normalizedUrl = normalizeUrl(connectionUrl);
        
        // Check if encoding is needed
        if (!requiresEncoding(normalizedUrl)) {
            return normalizedUrl;
        }

        return applyUrlEncoding(normalizedUrl);
    }

    /**
     * Checks if the given URL contains characters that require URL encoding.
     * 
     * @param url the URL to check
     * @return true if encoding is required, false otherwise
     */
    private static boolean requiresEncoding(String url) {
        Matcher matcher = JDBC_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            // If we can't parse it, assume encoding might be needed
            return true;
        }

        String username = matcher.group(3);
        String password = matcher.group(4);

        if (username != null && URL_ENCODE_REQUIRED_PATTERN.matcher(username).find()) {
            return true;
        }

        if (password != null && URL_ENCODE_REQUIRED_PATTERN.matcher(password).find()) {
            return true;
        }

        return false;
    }

    /**
     * Applies URL encoding to username and password segments in the database URL.
     * 
     * @param url the URL to encode
     * @return the encoded URL
     */
    private static String applyUrlEncoding(String url) {
        Matcher matcher = JDBC_URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid database URL format: " + maskCredentials(url));
        }

        String jdbcPrefix = matcher.group(1) != null ? matcher.group(1) : "";
        String protocol = matcher.group(2);
        String username = matcher.group(3);
        String password = matcher.group(4);
        String host = matcher.group(5);
        String port = matcher.group(6);
        String database = matcher.group(7);
        String queryParams = matcher.group(8);

        // Encode username and password
        String encodedUsername = username != null ? DatabaseUrlUtils.urlEncode(username) : null;
        String encodedPassword = password != null ? DatabaseUrlUtils.urlEncode(password) : null;

        // Reconstruct the URL
        StringBuilder encodedUrl = new StringBuilder();
        encodedUrl.append(jdbcPrefix).append(protocol).append("://");
        
        if (encodedUsername != null) {
            encodedUrl.append(encodedUsername);
            if (encodedPassword != null) {
                encodedUrl.append(":").append(encodedPassword);
            }
            encodedUrl.append("@");
        }
        
        encodedUrl.append(host);
        
        if (port != null) {
            encodedUrl.append(":").append(port);
        }
        
        encodedUrl.append("/").append(database);
        
        if (queryParams != null) {
            encodedUrl.append("?").append(queryParams);
        }

        return encodedUrl.toString();
    }

    /**
     * Normalizes the URL format by ensuring it has the proper JDBC prefix if it's a JDBC URL.
     * 
     * @param url the URL to normalize
     * @return the normalized URL
     */
    private static String normalizeUrl(String url) {
        String trimmed = url.trim();
        
        // If it starts with jdbc:, return as-is
        if (trimmed.startsWith("jdbc:")) {
            return trimmed;
        }
        
        // If it looks like a PostgreSQL URL without jdbc: prefix, add it
        if (trimmed.startsWith("postgresql://") || trimmed.startsWith("postgres://")) {
            String normalizedProtocol = trimmed.replace("postgres://", "postgresql://");
            return "jdbc:" + normalizedProtocol;
        }
        
        return trimmed;
    }

    /**
     * Masks credentials in a URL for safe logging.
     * 
     * @param url the URL to mask
     * @return the URL with masked credentials
     */
    private static String maskCredentials(String url) {
        if (url == null) {
            return null;
        }
        return url.replaceAll("://[^:/@]+(?::[^/@]+)?@", "://***:***@");
    }
}