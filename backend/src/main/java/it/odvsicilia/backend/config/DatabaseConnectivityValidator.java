package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DatabaseConnectivityValidator {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectivityValidator.class);
    private static final int DEFAULT_TIMEOUT_MS = 5000;
    private static final Pattern JDBC_URL_PATTERN = Pattern.compile("jdbc:postgresql://([^:/]+):(\\d+)/");
    private static final Pattern POSTGRES_URL_PATTERN = Pattern.compile("postgres(?:ql)?://(?:.*@)?([^:/]+):(\\d+)");

    public static class ValidationResult {
        private final boolean success;
        private final String hostname;
        private final int port;
        private final String errorMessage;

        private ValidationResult(boolean success, String hostname, int port, String errorMessage) {
            this.success = success;
            this.hostname = hostname;
            this.port = port;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success(String hostname, int port) {
            return new ValidationResult(true, hostname, port, null);
        }

        public static ValidationResult failure(String hostname, int port, String errorMessage) {
            return new ValidationResult(false, hostname, port, errorMessage);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, null, -1, errorMessage);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getHostname() {
            return hostname;
        }

        public int getPort() {
            return port;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public static ValidationResult validateDatabaseConnectivity(String databaseUrl) {
        return validateDatabaseConnectivity(databaseUrl, DEFAULT_TIMEOUT_MS);
    }

    public static ValidationResult validateDatabaseConnectivity(String databaseUrl, int timeoutMs) {
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            logger.warn("DATABASE_URL validation skipped: URL is null or empty");
            return ValidationResult.failure("DATABASE_URL is null or empty");
        }

        logger.info("Starting database connectivity validation for URL: {}", maskPassword(databaseUrl));

        ValidationResult urlValidation = validateUrlFormat(databaseUrl);
        if (!urlValidation.isSuccess()) {
            logger.error("URL format validation failed: {}", urlValidation.getErrorMessage());
            return urlValidation;
        }

        String hostname = urlValidation.getHostname();
        int port = urlValidation.getPort();
        logger.info("Successfully extracted hostname '{}' and port {} from DATABASE_URL", hostname, port);

        ValidationResult dnsValidation = validateDnsResolution(hostname);
        if (!dnsValidation.isSuccess()) {
            logger.error("DNS resolution failed for hostname '{}': {}", hostname, dnsValidation.getErrorMessage());
            logger.error("ACTION REQUIRED: Verify that the hostname is correct and that DNS servers are accessible. " +
                        "Check network connectivity and firewall rules.");
            return ValidationResult.failure(hostname, port, dnsValidation.getErrorMessage());
        }

        logger.info("DNS resolution successful for hostname '{}'", hostname);

        ValidationResult connectivityValidation = validateTcpConnectivity(hostname, port, timeoutMs);
        if (!connectivityValidation.isSuccess()) {
            logger.error("TCP connectivity test failed for {}:{} - {}", hostname, port, connectivityValidation.getErrorMessage());
            logger.error("ACTION REQUIRED: Verify that the database server is running, accessible from this network, " +
                        "and that port {} is not blocked by firewalls. Check security groups and network ACLs.", port);
            return connectivityValidation;
        }

        logger.info("TCP connectivity test successful for {}:{}", hostname, port);
        logger.info("Database connectivity validation completed successfully");
        return ValidationResult.success(hostname, port);
    }

    private static ValidationResult validateUrlFormat(String databaseUrl) {
        try {
            String hostname = null;
            int port = -1;

            if (databaseUrl.startsWith("jdbc:postgresql://")) {
                Matcher matcher = JDBC_URL_PATTERN.matcher(databaseUrl);
                if (matcher.find()) {
                    hostname = matcher.group(1);
                    port = Integer.parseInt(matcher.group(2));
                } else {
                    return ValidationResult.failure("Malformed JDBC URL: could not extract hostname and port. " +
                                                   "Expected format: jdbc:postgresql://hostname:port/database");
                }
            } else if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
                Matcher matcher = POSTGRES_URL_PATTERN.matcher(databaseUrl);
                if (matcher.find()) {
                    hostname = matcher.group(1);
                    port = Integer.parseInt(matcher.group(2));
                } else {
                    try {
                        URI uri = new URI(databaseUrl.replaceFirst("^postgres://", "http://")
                                                     .replaceFirst("^postgresql://", "http://"));
                        hostname = uri.getHost();
                        port = uri.getPort();
                        if (port == -1) {
                            port = 5432;
                        }
                    } catch (Exception e) {
                        return ValidationResult.failure("Malformed PostgreSQL URL: could not extract hostname and port. " +
                                                       "Expected format: postgres://[user:pass@]hostname:port/database. " +
                                                       "Parse error: " + e.getMessage());
                    }
                }
            } else {
                return ValidationResult.failure("Unsupported DATABASE_URL scheme. " +
                                               "Supported schemes: jdbc:postgresql://, postgres://, postgresql://");
            }

            if (hostname == null || hostname.trim().isEmpty()) {
                return ValidationResult.failure("Invalid DATABASE_URL: hostname is missing or empty");
            }

            if (port <= 0 || port > 65535) {
                return ValidationResult.failure("Invalid DATABASE_URL: port " + port + " is out of valid range (1-65535)");
            }

            return ValidationResult.success(hostname, port);
        } catch (NumberFormatException e) {
            return ValidationResult.failure("Invalid port number in DATABASE_URL: " + e.getMessage());
        } catch (Exception e) {
            return ValidationResult.failure("Failed to parse DATABASE_URL: " + e.getMessage());
        }
    }

    private static ValidationResult validateDnsResolution(String hostname) {
        try {
            InetAddress address = InetAddress.getByName(hostname);
            logger.info("DNS resolution successful: hostname '{}' resolved to IP address {}", 
                       hostname, address.getHostAddress());
            return ValidationResult.success(hostname, -1);
        } catch (UnknownHostException e) {
            String errorMsg = String.format("DNS resolution failed: hostname '%s' could not be resolved. " +
                                           "Error: %s", hostname, e.getMessage());
            return ValidationResult.failure(errorMsg);
        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error during DNS resolution for hostname '%s': %s", 
                                           hostname, e.getMessage());
            return ValidationResult.failure(errorMsg);
        }
    }

    private static ValidationResult validateTcpConnectivity(String hostname, int port, int timeoutMs) {
        Socket socket = null;
        try {
            logger.info("Attempting TCP connection to {}:{} with timeout {}ms", hostname, port, timeoutMs);
            socket = new Socket();
            InetAddress address = InetAddress.getByName(hostname);
            socket.connect(new java.net.InetSocketAddress(address, port), timeoutMs);
            logger.info("TCP connection established successfully to {}:{}", hostname, port);
            return ValidationResult.success(hostname, port);
        } catch (SocketTimeoutException e) {
            String errorMsg = String.format("Connection timeout after %dms: host %s:%d is unreachable or not responding. " +
                                           "This may indicate network connectivity issues, firewall blocking, or the database service not running.",
                                           timeoutMs, hostname, port);
            return ValidationResult.failure(hostname, port, errorMsg);
        } catch (UnknownHostException e) {
            String errorMsg = String.format("DNS resolution failed during connection attempt to %s:%d: %s",
                                           hostname, port, e.getMessage());
            return ValidationResult.failure(hostname, port, errorMsg);
        } catch (java.net.ConnectException e) {
            String errorMsg = String.format("Connection refused to %s:%d. The host is reachable but no service is listening on port %d. " +
                                           "Verify the database server is running and configured to listen on this port. Error: %s",
                                           hostname, port, port, e.getMessage());
            return ValidationResult.failure(hostname, port, errorMsg);
        } catch (java.net.NoRouteToHostException e) {
            String errorMsg = String.format("No route to host %s:%d. The host may be down or unreachable due to network configuration. " +
                                           "Check routing tables and network connectivity. Error: %s",
                                           hostname, port, e.getMessage());
            return ValidationResult.failure(hostname, port, errorMsg);
        } catch (IOException e) {
            String errorMsg = String.format("I/O error connecting to %s:%d: %s. " +
                                           "Check network connectivity and firewall rules.",
                                           hostname, port, e.getMessage());
            return ValidationResult.failure(hostname, port, errorMsg);
        } catch (Exception e) {
            String errorMsg = String.format("Unexpected error connecting to %s:%d: %s",
                                           hostname, port, e.getMessage());
            return ValidationResult.failure(hostname, port, errorMsg);
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.warn("Failed to close socket to {}:{}: {}", hostname, port, e.getMessage());
                }
            }
        }
    }

    private static String maskPassword(String url) {
        if (url == null) {
            return null;
        }
        String masked = url.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
        masked = masked.replaceAll("[&?]password=([^&]+)", "&password=****");
        return masked;
    }
}
