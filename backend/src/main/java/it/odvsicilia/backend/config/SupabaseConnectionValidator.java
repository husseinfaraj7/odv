package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.io.IOException;

@Component
public class SupabaseConnectionValidator {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseConnectionValidator.class);
    private static final int POOLER_PORT = 6543;
    private static final int DIRECT_PORT = 5432;
    private static final int DEFAULT_TIMEOUT_MS = 5000;

    private final Environment environment;

    public SupabaseConnectionValidator(Environment environment) {
        this.environment = environment;
    }

    public static class SupabaseValidationResult {
        private final boolean poolerAvailable;
        private final boolean directAvailable;
        private final String hostname;
        private final String poolerError;
        private final String directError;

        private SupabaseValidationResult(boolean poolerAvailable, boolean directAvailable, 
                                         String hostname, String poolerError, String directError) {
            this.poolerAvailable = poolerAvailable;
            this.directAvailable = directAvailable;
            this.hostname = hostname;
            this.poolerError = poolerError;
            this.directError = directError;
        }

        public static SupabaseValidationResult of(boolean poolerAvailable, boolean directAvailable,
                                                   String hostname, String poolerError, String directError) {
            return new SupabaseValidationResult(poolerAvailable, directAvailable, hostname, poolerError, directError);
        }

        public boolean isPoolerAvailable() {
            return poolerAvailable;
        }

        public boolean isDirectAvailable() {
            return directAvailable;
        }

        public boolean isAnyAvailable() {
            return poolerAvailable || directAvailable;
        }

        public boolean isBothAvailable() {
            return poolerAvailable && directAvailable;
        }

        public String getHostname() {
            return hostname;
        }

        public String getPoolerError() {
            return poolerError;
        }

        public String getDirectError() {
            return directError;
        }

        public ConnectionMode getAvailableConnectionMode() {
            if (poolerAvailable && directAvailable) {
                return ConnectionMode.BOTH;
            } else if (poolerAvailable) {
                return ConnectionMode.POOLER;
            } else if (directAvailable) {
                return ConnectionMode.DIRECT;
            } else {
                return ConnectionMode.NEITHER;
            }
        }
    }

    public enum ConnectionMode {
        POOLER("Transaction Pooler (port 6543)"),
        DIRECT("Direct Connection (port 5432)"),
        BOTH("Both Pooler and Direct"),
        NEITHER("Neither Pooler nor Direct");

        private final String description;

        ConnectionMode(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public SupabaseValidationResult validate() {
        String databaseUrl = extractDatabaseUrl();
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            logger.warn("DATABASE_URL not configured, skipping Supabase connection validation");
            return SupabaseValidationResult.of(false, false, null, 
                "DATABASE_URL not configured", "DATABASE_URL not configured");
        }

        String hostname = extractHostname(databaseUrl);
        if (hostname == null) {
            logger.error("Failed to extract hostname from DATABASE_URL: {}", maskPassword(databaseUrl));
            return SupabaseValidationResult.of(false, false, null,
                "Failed to extract hostname", "Failed to extract hostname");
        }

        logger.info("=== Starting Supabase Connection Validation ===");
        logger.info("Target hostname: {}", hostname);

        if (!performDnsResolution(hostname)) {
            String errorMsg = "DNS resolution failed for hostname: " + hostname;
            logger.error(errorMsg);
            return SupabaseValidationResult.of(false, false, hostname, errorMsg, errorMsg);
        }

        boolean poolerAvailable = testTcpConnectivity(hostname, POOLER_PORT);
        boolean directAvailable = testTcpConnectivity(hostname, DIRECT_PORT);

        String poolerError = poolerAvailable ? null : "Connection failed to port " + POOLER_PORT;
        String directError = directAvailable ? null : "Connection failed to port " + DIRECT_PORT;

        SupabaseValidationResult result = SupabaseValidationResult.of(
            poolerAvailable, directAvailable, hostname, poolerError, directError);

        logValidationResults(result);

        return result;
    }

    private String extractDatabaseUrl() {
        String url = System.getenv("DATABASE_URL");
        if (url == null || url.trim().isEmpty()) {
            url = environment.getProperty("DATABASE_URL");
        }
        if (url == null || url.trim().isEmpty()) {
            url = environment.getProperty("spring.datasource.url");
        }
        return url;
    }

    private String extractHostname(String databaseUrl) {
        try {
            if (databaseUrl.startsWith("jdbc:postgresql://")) {
                String urlPart = databaseUrl.substring("jdbc:postgresql://".length());
                int endIndex = urlPart.indexOf(':');
                if (endIndex == -1) {
                    endIndex = urlPart.indexOf('/');
                }
                if (endIndex == -1) {
                    endIndex = urlPart.indexOf('?');
                }
                if (endIndex > 0) {
                    return urlPart.substring(0, endIndex);
                }
            } else if (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://")) {
                URI uri = new URI(databaseUrl.replaceFirst("^postgres://", "http://")
                                             .replaceFirst("^postgresql://", "http://"));
                return uri.getHost();
            }
        } catch (URISyntaxException e) {
            logger.error("Failed to parse DATABASE_URL: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error extracting hostname from DATABASE_URL: {}", e.getMessage());
        }
        return null;
    }

    private boolean performDnsResolution(String hostname) {
        try {
            logger.info("Performing DNS resolution for hostname: {}", hostname);
            InetAddress address = InetAddress.getByName(hostname);
            logger.info("✓ DNS resolution successful: {} → {}", hostname, address.getHostAddress());
            return true;
        } catch (UnknownHostException e) {
            logger.error("✗ DNS resolution failed for hostname '{}': {}", hostname, e.getMessage());
            logger.error("ACTION REQUIRED: Verify that the hostname is correct and DNS servers are accessible");
            return false;
        } catch (Exception e) {
            logger.error("✗ Unexpected error during DNS resolution for hostname '{}': {}", hostname, e.getMessage());
            return false;
        }
    }

    private boolean testTcpConnectivity(String hostname, int port) {
        Socket socket = null;
        try {
            logger.info("Testing TCP connectivity to {}:{} (timeout: {}ms)", hostname, port, DEFAULT_TIMEOUT_MS);
            socket = new Socket();
            InetAddress address = InetAddress.getByName(hostname);
            socket.connect(new java.net.InetSocketAddress(address, port), DEFAULT_TIMEOUT_MS);
            logger.info("✓ TCP connection successful to {}:{}", hostname, port);
            return true;
        } catch (SocketTimeoutException e) {
            logger.warn("✗ Connection timeout to {}:{} after {}ms", hostname, port, DEFAULT_TIMEOUT_MS);
            return false;
        } catch (UnknownHostException e) {
            logger.warn("✗ DNS resolution failed for {}:{} during connection attempt", hostname, port);
            return false;
        } catch (java.net.ConnectException e) {
            logger.warn("✗ Connection refused to {}:{} - no service listening on this port", hostname, port);
            return false;
        } catch (java.net.NoRouteToHostException e) {
            logger.warn("✗ No route to host {}:{} - host unreachable", hostname, port);
            return false;
        } catch (IOException e) {
            logger.warn("✗ I/O error connecting to {}:{}: {}", hostname, port, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.warn("✗ Unexpected error connecting to {}:{}: {}", hostname, port, e.getMessage());
            return false;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    logger.debug("Failed to close socket to {}:{}: {}", hostname, port, e.getMessage());
                }
            }
        }
    }

    private void logValidationResults(SupabaseValidationResult result) {
        logger.info("=== Supabase Connection Validation Results ===");
        logger.info("Hostname: {}", result.getHostname());
        logger.info("Transaction Pooler (port {}): {}", POOLER_PORT, 
            result.isPoolerAvailable() ? "✓ AVAILABLE" : "✗ UNAVAILABLE");
        logger.info("Direct Connection (port {}): {}", DIRECT_PORT,
            result.isDirectAvailable() ? "✓ AVAILABLE" : "✗ UNAVAILABLE");
        logger.info("Connection Mode: {}", result.getAvailableConnectionMode().getDescription());

        if (!result.isPoolerAvailable() && result.isDirectAvailable()) {
            logger.warn("");
            logger.warn("⚠ POOLER UNAVAILABLE - ACTION REQUIRED ⚠");
            logger.warn("The transaction pooler (port 6543) is not accessible, but direct connection (port 5432) works.");
            logger.warn("");
            logger.warn("To enable connection pooling in Supabase:");
            logger.warn("  1. Open your Supabase dashboard at https://app.supabase.com/");
            logger.warn("  2. Select your project");
            logger.warn("  3. Navigate to: Settings → Database");
            logger.warn("  4. Scroll down to the 'Connection Pooling' section");
            logger.warn("  5. Enable 'Transaction Mode' pooling");
            logger.warn("  6. Note the pooler connection string (uses port 6543)");
            logger.warn("  7. Update your DATABASE_URL to use the pooler endpoint");
            logger.warn("");
            logger.warn("Benefits of using the pooler:");
            logger.warn("  • Better connection management for serverless environments");
            logger.warn("  • Reduced connection overhead");
            logger.warn("  • Improved scalability");
            logger.warn("");
        }

        if (!result.isDirectAvailable() && result.isPoolerAvailable()) {
            logger.warn("");
            logger.warn("⚠ DIRECT CONNECTION UNAVAILABLE ⚠");
            logger.warn("Direct database connection (port 5432) is not accessible, but pooler (port 6543) works.");
            logger.warn("This is acceptable for most deployments. The pooler provides better connection management.");
            logger.warn("");
        }

        if (!result.isAnyAvailable()) {
            logger.error("");
            logger.error("✗✗✗ NO CONNECTION AVAILABLE ✗✗✗");
            logger.error("Neither the transaction pooler (port 6543) nor direct connection (port 5432) is accessible.");
            logger.error("");
            logger.error("ACTION REQUIRED:");
            logger.error("  1. Verify the hostname '{}' is correct", result.getHostname());
            logger.error("  2. Check network connectivity and firewall rules");
            logger.error("  3. Verify the database server is running");
            logger.error("  4. Ensure ports 5432 and/or 6543 are not blocked");
            logger.error("  5. Check Supabase project settings and connection strings");
            logger.error("");
            logger.error("Pooler error: {}", result.getPoolerError());
            logger.error("Direct error: {}", result.getDirectError());
            logger.error("");
        }

        if (result.isBothAvailable()) {
            logger.info("✓ Both connection modes are available - optimal configuration");
        }

        logger.info("=== End of Supabase Connection Validation ===");
    }

    private String maskPassword(String url) {
        if (url == null) {
            return null;
        }
        String masked = url.replaceAll("://([^:]+):([^@]+)@", "://$1:****@");
        masked = masked.replaceAll("[&?]password=([^&]+)", "&password=****");
        return masked;
    }
}
