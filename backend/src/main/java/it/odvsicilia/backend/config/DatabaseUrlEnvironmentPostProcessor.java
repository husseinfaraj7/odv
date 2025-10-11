package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseUrlEnvironmentPostProcessor.class);
    private static final int POOLER_PORT = 6543;
    private static final int DIRECT_PORT = 5432;
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final Pattern SUPABASE_POOLER_PATTERN = Pattern.compile("^(.*\\.)?([a-zA-Z0-9-]+)\\.pooler\\.supabase\\.com");
    private static final Pattern SUPABASE_DIRECT_PATTERN = Pattern.compile("^db\\.([a-zA-Z0-9-]+)\\.supabase\\.co");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");
        
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            logger.warn("DATABASE_URL environment variable is missing or empty. " +
                       "Application startup will fail unless spring.datasource.url is configured " +
                       "or a dev profile with H2 is active. " +
                       "For deployment environments like Render, ensure DATABASE_URL is set correctly.");
            logger.debug("Final configuration state: DATABASE_URL is missing, no URL conversion performed");
            return;
        }

        logger.info("Original DATABASE_URL detected: {}", maskPassword(databaseUrl));
        
        try {
            String[] hostPort = extractHostAndPort(databaseUrl);
            String hostname = hostPort[0];
            
            SupabaseHostnameValidator.HostnameValidationResult validationResult = SupabaseHostnameValidator.validate(hostname);
            if (!validationResult.isValid()) {
                throw new IllegalStateException(validationResult.getMessage());
            }
        } catch (Exception e) {
            logger.error("Failed to extract hostname or validate DATABASE_URL: {}", e.getMessage());
            throw new IllegalStateException("Invalid DATABASE_URL format: " + e.getMessage(), e);
        }
        
        DatabaseConnectivityValidator.validateDatabaseConnectivity(databaseUrl);

        if (databaseUrl.startsWith("jdbc:")) {
            logger.info("DATABASE_URL is already in JDBC format, no conversion needed");
            String existingUrl = environment.getProperty("spring.datasource.url");
            if (existingUrl != null) {
                logger.info("Resulting spring.datasource.url: {}", maskPassword(existingUrl));
                logger.debug("Final spring.datasource.url after processing (already in JDBC format): {}", maskPassword(existingUrl));
            } else {
                logger.info("spring.datasource.url will be used from other configuration sources");
                logger.debug("Final spring.datasource.url after processing (already in JDBC format): will be resolved from other property sources");
            }
            return;
        }

        if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
            try {
                String connectionMode = environment.getProperty("SUPABASE_CONNECTION_MODE", "auto").toLowerCase();
                String finalDatabaseUrl = selectConnectionUrl(databaseUrl, connectionMode);
                String jdbcUrl = convertToJdbcUrl(finalDatabaseUrl);
                
                performNetworkDiagnostics(finalDatabaseUrl);
                
                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", jdbcUrl);
                
                MapPropertySource propertySource = new MapPropertySource(
                    "databaseUrlConversion", props
                );
                environment.getPropertySources().addFirst(propertySource);
                
                logger.info("DATABASE_URL conversion occurred: Standard PostgreSQL URL converted to JDBC format");
                logger.info("Resulting spring.datasource.url: {}", maskPassword(jdbcUrl));
                logger.debug("Property successfully added to environment: property source name='{}', conversion successful=true", 
                    propertySource.getName());
                logger.debug("Final spring.datasource.url after conversion: {}", maskPassword(jdbcUrl));
            } catch (Exception e) {
                logger.error("Failed to convert DATABASE_URL to JDBC format. " +
                           "Original DATABASE_URL format: {}. Error: {}", 
                           maskPassword(databaseUrl), e.getMessage());
                logger.debug("URL conversion failed: conversion successful=false, error={}", e.getMessage());
                throw new IllegalStateException("Invalid DATABASE_URL format", e);
            }
        } else {
            logger.warn("DATABASE_URL has unrecognized format (not postgres://, postgresql://, or jdbc:). " +
                       "Value starts with: {}. Application may fail to connect to database.",
                       databaseUrl.substring(0, Math.min(20, databaseUrl.length())));
            logger.debug("Final configuration state: DATABASE_URL format unrecognized, no URL conversion performed");
        }
    }

    private String selectConnectionUrl(String databaseUrl, String connectionMode) {
        switch (connectionMode) {
            case "direct":
                String directUrl = convertToDirectConnection(databaseUrl);
                logger.info("Connection mode 'direct' selected: using direct database connection");
                return directUrl;
            
            case "pooler":
                logger.info("Connection mode 'pooler' selected: using pooler connection");
                return databaseUrl;
            
            case "auto":
                return autoSelectConnection(databaseUrl);
            
            default:
                logger.warn("Invalid SUPABASE_CONNECTION_MODE value '{}'. Valid values are 'pooler', 'direct', or 'auto'. Defaulting to 'auto'", connectionMode);
                return autoSelectConnection(databaseUrl);
        }
    }

    private String autoSelectConnection(String databaseUrl) {
        try {
            String[] hostPort = extractHostAndPort(databaseUrl);
            String hostname = hostPort[0];
            int port = Integer.parseInt(hostPort[1]);
            
            if (port == POOLER_PORT && testTcpConnectivity(hostname, POOLER_PORT)) {
                logger.info("Connection mode 'auto' selected: pooler connectivity test successful, using pooler connection");
                return databaseUrl;
            } else {
                logger.warn("Connection mode 'auto' selected: pooler connectivity test failed or not using pooler port, falling back to direct database connection");
                return convertToDirectConnection(databaseUrl);
            }
        } catch (Exception e) {
            logger.warn("Connection mode 'auto' selected: failed to test pooler connectivity ({}), falling back to direct database connection", e.getMessage());
            return convertToDirectConnection(databaseUrl);
        }
    }

    private String convertToDirectConnection(String databaseUrl) {
        try {
            String[] hostPort = extractHostAndPort(databaseUrl);
            String hostname = hostPort[0];
            
            String projectRef = extractProjectReference(hostname);
            if (projectRef == null) {
                logger.warn("Could not extract Supabase project reference from hostname '{}', returning original URL", hostname);
                return databaseUrl;
            }
            
            String directHostname = "db." + projectRef + ".supabase.co";
            
            String newUrl = databaseUrl.replace(hostname, directHostname);
            
            if (newUrl.contains(":" + POOLER_PORT)) {
                newUrl = newUrl.replace(":" + POOLER_PORT, ":" + DIRECT_PORT);
            } else {
                int slashAfterHost = newUrl.indexOf('/', newUrl.indexOf("://") + 3);
                int atIndex = newUrl.lastIndexOf('@', slashAfterHost);
                if (atIndex != -1) {
                    String beforeSlash = newUrl.substring(0, slashAfterHost);
                    String afterSlash = newUrl.substring(slashAfterHost);
                    if (!beforeSlash.matches(".*:\\d+$")) {
                        newUrl = beforeSlash + ":" + DIRECT_PORT + afterSlash;
                    }
                }
            }
            
            return newUrl;
        } catch (Exception e) {
            logger.error("Failed to convert to direct connection: {}", e.getMessage());
            return databaseUrl;
        }
    }

    private String extractProjectReference(String hostname) {
        Matcher poolerMatcher = SUPABASE_POOLER_PATTERN.matcher(hostname);
        if (poolerMatcher.matches()) {
            return poolerMatcher.group(2);
        }
        
        Matcher directMatcher = SUPABASE_DIRECT_PATTERN.matcher(hostname);
        if (directMatcher.matches()) {
            return directMatcher.group(1);
        }
        
        return null;
    }

    private boolean testTcpConnectivity(String hostname, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(hostname, port), CONNECTION_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String maskPassword(String url) {
        if (url == null) {
            return null;
        }
        
        String masked = url.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
        masked = masked.replaceAll("[&?]password=([^&]+)", "&password=****");
        
        return masked;
    }

    private void performNetworkDiagnostics(String databaseUrl) {
        logger.info("=== Database Network Diagnostics ===");
        
        try {
            String[] hostPort = extractHostAndPort(databaseUrl);
            String hostname = hostPort[0];
            int port = Integer.parseInt(hostPort[1]);
            
            logger.info("Extracted hostname: {}", hostname);
            logger.info("Extracted port: {}", port);
            
            performDnsResolution(hostname);
            performTcpConnectivityTest(hostname, port);
            
            logger.info("=== Network Diagnostics Complete ===");
        } catch (Exception e) {
            logger.warn("Network diagnostics failed: {}", e.getMessage());
        }
    }

    private void performDnsResolution(String hostname) {
        try {
            logger.info("Attempting DNS resolution for hostname: {}", hostname);
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            
            if (addresses.length > 0) {
                logger.info("DNS resolution successful. Resolved to {} IP address(es):", addresses.length);
                for (InetAddress address : addresses) {
                    logger.info("  - {}", address.getHostAddress());
                }
            } else {
                logger.warn("DNS resolution returned no IP addresses for hostname: {}", hostname);
            }
        } catch (UnknownHostException e) {
            logger.error("DNS resolution failed for hostname: {}. Reason: {}", hostname, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during DNS resolution for hostname: {}. Error: {}", hostname, e.getMessage());
        }
    }

    private void validateHostnameWithDnsResolution(String hostname) {
        logger.info("=== Database Hostname Validation ===");
        logger.info("Validating hostname: {}", hostname);
        
        String hostnamePattern = detectHostnamePattern(hostname);
        logger.info("Detected hostname pattern: {}", hostnamePattern);
        
        if ("MALFORMED".equals(hostnamePattern)) {
            logger.error("Hostname appears to be malformed: {}", hostname);
            logger.error("The hostname '{}' mixes Supabase direct and pooler connection formats incorrectly", hostname);
        }
        
        try {
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            
            if (addresses.length > 0) {
                logger.info("DNS resolution successful for hostname '{}'. Resolved to {} IP address(es)", hostname, addresses.length);
                for (InetAddress address : addresses) {
                    logger.info("  - {}", address.getHostAddress());
                }
            } else {
                logger.warn("DNS resolution returned no IP addresses for hostname: {}", hostname);
            }
            
            logger.info("=== Hostname Validation Complete ===");
        } catch (UnknownHostException e) {
            logger.error("=== DNS RESOLUTION FAILED ===");
            logger.error("Failed to resolve hostname: {}", hostname);
            logger.error("Reason: {}", e.getMessage());
            logger.error("");
            logger.error("Examples of correct Supabase hostname formats:");
            logger.error("  1. Direct connection pattern: db.PROJECT_REF.supabase.co (port 5432)");
            logger.error("     Example: db.abcdefghijklmnop.supabase.co");
            logger.error("");
            logger.error("  2. Transaction pooler pattern: aws-0-REGION.pooler.supabase.com (port 6543)");
            logger.error("     Example: aws-0-us-east-1.pooler.supabase.com");
            logger.error("");
            logger.error("Your hostname that failed to resolve: {}", hostname);
            logger.error("");
            logger.error("Common mistakes:");
            logger.error("  - Mixing formats (e.g., db.aws-0-region.pooler.supabase.com) - INCORRECT");
            logger.error("  - Wrong domain extension (.com vs .co) - check your Supabase project settings");
            logger.error("  - Incorrect project reference - verify in your Supabase dashboard");
            logger.error("=== Application startup will fail due to invalid hostname ===");
            
            throw new IllegalStateException("DNS resolution failed for database hostname: " + hostname + 
                ". The hostname cannot be resolved to an IP address. " +
                "Please verify your DATABASE_URL contains a valid Supabase hostname. " +
                "See logs above for examples of correct hostname formats.", e);
        } catch (Exception e) {
            logger.error("Unexpected error during DNS resolution validation for hostname: {}. Error: {}", hostname, e.getMessage());
            throw new IllegalStateException("DNS resolution validation failed for database hostname: " + hostname, e);
        }
    }

    private String detectHostnamePattern(String hostname) {
        if (hostname.startsWith("db.aws-") || hostname.matches("^db\\.[^.]*aws[^.]*\\..*")) {
            return "MALFORMED (incorrectly mixes direct 'db.' prefix with pooler 'aws-' format)";
        }
        
        Matcher directMatcher = SUPABASE_DIRECT_PATTERN.matcher(hostname);
        if (directMatcher.matches()) {
            return "Supabase direct connection (db.PROJECT_REF.supabase.co)";
        }
        
        Matcher poolerMatcher = SUPABASE_POOLER_PATTERN.matcher(hostname);
        if (poolerMatcher.matches()) {
            return "Supabase transaction pooler (aws-0-REGION.pooler.supabase.com)";
        }
        
        if (hostname.contains("supabase")) {
            return "MALFORMED (contains 'supabase' but doesn't match expected patterns)";
        }
        
        return "Custom or non-Supabase hostname";
    }

    private void performTcpConnectivityTest(String hostname, int port) {
        logger.info("Attempting TCP socket connection to {}:{}", hostname, port);
        
        try (Socket socket = new Socket()) {
            socket.connect(new java.net.InetSocketAddress(hostname, port), 5000);
            logger.info("TCP connection successful to {}:{}", hostname, port);
        } catch (java.net.SocketTimeoutException e) {
            logger.error("TCP connection to {}:{} timed out after 5 seconds. The host may be unreachable or firewall rules may be blocking the connection.", hostname, port);
        } catch (java.net.ConnectException e) {
            logger.error("TCP connection to {}:{} refused. Reason: {}. The database server may not be running or not accepting connections on this port.", hostname, port, e.getMessage());
        } catch (java.io.IOException e) {
            logger.error("TCP connection to {}:{} failed due to I/O error: {}", hostname, port, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during TCP connection to {}:{}. Error: {}", hostname, port, e.getMessage());
        }
    }

    private String[] extractHostAndPort(String databaseUrl) throws Exception {
        int schemeEnd = databaseUrl.indexOf("://");
        if (schemeEnd == -1) {
            throw new IllegalArgumentException("Invalid DATABASE_URL: missing scheme");
        }
        
        String afterScheme = databaseUrl.substring(schemeEnd + 3);
        
        String hostAndRest;
        int atIndex = afterScheme.lastIndexOf('@');
        if (atIndex != -1) {
            hostAndRest = afterScheme.substring(atIndex + 1);
        } else {
            hostAndRest = afterScheme;
        }
        
        String host;
        int port = 5432;
        
        int slashIndex = hostAndRest.indexOf('/');
        int queryIndex = hostAndRest.indexOf('?');
        
        String hostPart;
        if (slashIndex != -1) {
            hostPart = hostAndRest.substring(0, slashIndex);
        } else if (queryIndex != -1) {
            hostPart = hostAndRest.substring(0, queryIndex);
        } else {
            hostPart = hostAndRest;
        }
        
        int colonIndex = hostPart.indexOf(':');
        if (colonIndex != -1) {
            host = hostPart.substring(0, colonIndex);
            try {
                port = Integer.parseInt(hostPart.substring(colonIndex + 1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid port number in DATABASE_URL");
            }
        } else {
            host = hostPart;
        }
        
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid DATABASE_URL: missing host");
        }
        
        return new String[]{host, String.valueOf(port)};
    }

    private String convertToJdbcUrl(String databaseUrl) throws Exception {
        int schemeEnd = databaseUrl.indexOf("://");
        if (schemeEnd == -1) {
            throw new IllegalArgumentException("Invalid DATABASE_URL: missing scheme");
        }
        
        String afterScheme = databaseUrl.substring(schemeEnd + 3);
        
        String userInfo = null;
        String user = "postgres";
        String password = "";
        String hostAndRest;
        
        int atIndex = afterScheme.lastIndexOf('@');
        if (atIndex != -1) {
            userInfo = afterScheme.substring(0, atIndex);
            hostAndRest = afterScheme.substring(atIndex + 1);
            
            int colonIndex = userInfo.indexOf(':');
            if (colonIndex != -1) {
                user = userInfo.substring(0, colonIndex);
                String rawPassword = userInfo.substring(colonIndex + 1);
                
                try {
                    password = URLDecoder.decode(rawPassword, StandardCharsets.UTF_8.name());
                } catch (UnsupportedEncodingException e) {
                    logger.warn("Failed to decode password from DATABASE_URL. Falling back to original password value. Error: {}", e.getMessage());
                    password = rawPassword;
                }
                
                password = URLEncoder.encode(password, StandardCharsets.UTF_8);
            } else {
                user = userInfo;
            }
        } else {
            hostAndRest = afterScheme;
        }
        
        String host;
        int port = 5432;
        String database = "postgres";
        String query = null;
        
        int slashIndex = hostAndRest.indexOf('/');
        if (slashIndex != -1) {
            String hostPart = hostAndRest.substring(0, slashIndex);
            String pathPart = hostAndRest.substring(slashIndex + 1);
            
            int colonIndex = hostPart.indexOf(':');
            if (colonIndex != -1) {
                host = hostPart.substring(0, colonIndex);
                try {
                    port = Integer.parseInt(hostPart.substring(colonIndex + 1));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid port number in DATABASE_URL");
                }
            } else {
                host = hostPart;
            }
            
            int queryIndex = pathPart.indexOf('?');
            if (queryIndex != -1) {
                database = pathPart.substring(0, queryIndex);
                query = pathPart.substring(queryIndex + 1);
            } else {
                database = pathPart;
            }
        } else {
            int colonIndex = hostAndRest.indexOf(':');
            int queryIndex = hostAndRest.indexOf('?');
            
            if (colonIndex != -1 && (queryIndex == -1 || colonIndex < queryIndex)) {
                host = hostAndRest.substring(0, colonIndex);
                String portPart = queryIndex != -1 ? hostAndRest.substring(colonIndex + 1, queryIndex) : hostAndRest.substring(colonIndex + 1);
                try {
                    port = Integer.parseInt(portPart);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid port number in DATABASE_URL");
                }
                if (queryIndex != -1) {
                    query = hostAndRest.substring(queryIndex + 1);
                }
            } else if (queryIndex != -1) {
                host = hostAndRest.substring(0, queryIndex);
                query = hostAndRest.substring(queryIndex + 1);
            } else {
                host = hostAndRest;
            }
        }
        
        if (host == null || host.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid DATABASE_URL: missing host");
        }
        
        validateHostnameWithDnsResolution(host);
        
        StringBuilder jdbcUrl = new StringBuilder();
        jdbcUrl.append("jdbc:postgresql://")
               .append(host)
               .append(":")
               .append(port)
               .append("/")
               .append(database);
        
        StringBuilder params = new StringBuilder();
        params.append("user=").append(user);
        params.append("&password=").append(password);
        
        if (query != null && !query.isEmpty()) {
            params.append("&").append(query);
        }
        
        jdbcUrl.append("?").append(params);
        
        return jdbcUrl.toString();
    }
}
