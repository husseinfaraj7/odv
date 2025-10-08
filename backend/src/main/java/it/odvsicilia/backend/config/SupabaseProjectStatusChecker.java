package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SupabaseProjectStatusChecker {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseProjectStatusChecker.class);

    private static final Pattern DIRECT_CONNECTION_PATTERN = Pattern.compile("^db\\.([a-z0-9]+)\\.supabase\\.co$");
    private static final Pattern POOLER_CONNECTION_PATTERN = Pattern.compile("^aws-([0-9]+)-([a-z]+-[a-z]+-[0-9]+)\\.pooler\\.supabase\\.com$");
    private static final Pattern PROJECT_REF_PATTERN = Pattern.compile("^[a-z0-9]{20}$");

    public ValidationResult validate(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            return ValidationResult.failure("DATABASE_URL is null or empty");
        }

        try {
            String normalizedUrl = normalizeUrl(databaseUrl);
            URI uri = new URI(normalizedUrl);

            String hostname = uri.getHost();
            if (hostname == null || hostname.trim().isEmpty()) {
                logger.debug("DATABASE_URL does not have a valid hostname, treating as non-Supabase");
                return ValidationResult.success("Non-Supabase database detected (no hostname)");
            }

            if (isSupabaseHostname(hostname)) {
                return validateSupabaseUrl(uri, hostname, databaseUrl);
            } else {
                logger.debug("DATABASE_URL does not match Supabase hostname patterns. Hostname: {}", hostname);
                return ValidationResult.success("Non-Supabase database detected");
            }

        } catch (URISyntaxException e) {
            String errorMessage = String.format(
                    "Invalid DATABASE_URL format: %s\n" +
                    "Error: %s at position %d\n" +
                    "Expected format: postgresql://<user>:<password>@<hostname>:<port>/<database>",
                    maskPassword(databaseUrl), e.getReason(), e.getIndex()
            );
            logger.error("Failed to parse DATABASE_URL: {}", errorMessage);
            return ValidationResult.failure(errorMessage);
        } catch (Exception e) {
            String errorMessage = String.format("Unexpected error validating DATABASE_URL: %s", e.getMessage());
            logger.error(errorMessage, e);
            return ValidationResult.failure(errorMessage);
        }
    }

    private ValidationResult validateSupabaseUrl(URI uri, String hostname, String originalUrl) {
        logger.info("=== Supabase Database Configuration Detected ===");
        logger.info("Hostname: {}", hostname);

        Matcher directMatcher = DIRECT_CONNECTION_PATTERN.matcher(hostname);
        Matcher poolerMatcher = POOLER_CONNECTION_PATTERN.matcher(hostname);

        String projectRef = null;
        String connectionMode = null;
        String region = null;

        if (directMatcher.matches()) {
            projectRef = directMatcher.group(1);
            connectionMode = "Direct Connection";
            logger.info("Connection Mode: {} (Port: {})", connectionMode, uri.getPort() != -1 ? uri.getPort() : 5432);
            
            if (uri.getPort() == 6543) {
                logger.warn("⚠️  WARNING: Direct connection hostname with pooler port 6543 detected!");
                logger.warn("   This is likely a misconfiguration. Direct connections should use port 5432.");
                logger.warn("   Either:");
                logger.warn("   1. Change port to 5432 for direct connection, OR");
                logger.warn("   2. Use pooler hostname: aws-*-*.pooler.supabase.com with port 6543");
            }

        } else if (poolerMatcher.matches()) {
            String poolerIndex = poolerMatcher.group(1);
            region = poolerMatcher.group(2);
            connectionMode = "Transaction Pooler";
            logger.info("Connection Mode: {} (Port: {})", connectionMode, uri.getPort() != -1 ? uri.getPort() : 6543);
            logger.info("Detected Region: {}", region);
            logger.info("Pooler Index: {}", poolerIndex);

            projectRef = extractProjectRefFromUserInfo(uri);

            if (uri.getPort() == 5432) {
                logger.warn("⚠️  WARNING: Pooler hostname with direct connection port 5432 detected!");
                logger.warn("   This is likely a misconfiguration. Pooler connections should use port 6543.");
                logger.warn("   Either:");
                logger.warn("   1. Change port to 6543 for pooler connection, OR");
                logger.warn("   2. Use direct hostname: db.<project-ref>.supabase.co with port 5432");
            }
        }

        if (projectRef == null || projectRef.trim().isEmpty()) {
            String errorMessage = buildProjectRefExtractionError(hostname, uri.getUserInfo());
            logger.error(errorMessage);
            return ValidationResult.failure(errorMessage);
        }

        if (!PROJECT_REF_PATTERN.matcher(projectRef).matches()) {
            String errorMessage = buildInvalidProjectRefError(projectRef, hostname);
            logger.error(errorMessage);
            return ValidationResult.failure(errorMessage);
        }

        logger.info("Project Reference ID: {}", projectRef);
        logger.info("Database: {}", uri.getPath() != null && uri.getPath().length() > 1 ? uri.getPath().substring(1) : "postgres");

        checkForPlaceholderValues(uri, projectRef);
        validateSupabaseConfiguration(uri, connectionMode);

        String dashboardUrl = String.format("https://supabase.com/dashboard/project/%s", projectRef);
        logger.info("Supabase Dashboard: {}", dashboardUrl);
        logger.info("Database Settings: {}/settings/database", dashboardUrl);
        logger.info("=== Supabase Configuration Validation Complete ===");

        return ValidationResult.success(String.format(
                "Supabase database validated successfully. Project: %s, Mode: %s%s",
                projectRef, connectionMode, region != null ? ", Region: " + region : ""
        ));
    }

    private String extractProjectRefFromUserInfo(URI uri) {
        String userInfo = uri.getUserInfo();
        if (userInfo == null || userInfo.trim().isEmpty()) {
            return null;
        }

        String username = userInfo.contains(":") ? userInfo.split(":")[0] : userInfo;
        
        if (username.contains(".")) {
            String[] parts = username.split("\\.", 2);
            if (parts.length == 2) {
                return parts[1];
            }
        }

        return null;
    }

    private void checkForPlaceholderValues(URI uri, String projectRef) {
        if ("YOUR_PROJECT_REF".equalsIgnoreCase(projectRef)) {
            logger.error("╔════════════════════════════════════════════════════════════════════════════╗");
            logger.error("║ ⚠️  CONFIGURATION ERROR: Placeholder project reference detected!          ║");
            logger.error("╚════════════════════════════════════════════════════════════════════════════╝");
            logger.error("");
            logger.error("The DATABASE_URL contains 'YOUR_PROJECT_REF' which is a placeholder value.");
            logger.error("");
            logger.error("ACTION REQUIRED:");
            logger.error("1. Go to https://supabase.com/dashboard");
            logger.error("2. Select your project");
            logger.error("3. Navigate to Settings → Database");
            logger.error("4. Copy the 'Connection string' (URI format)");
            logger.error("5. Replace the DATABASE_URL with your actual connection string");
            logger.error("");
        }

        String password = extractPassword(uri);
        if (password != null && (password.equals("password") || password.equals("example_password") || 
                password.contains("YOUR_PASSWORD") || password.contains("your_password"))) {
            logger.warn("⚠️  WARNING: DATABASE_URL appears to contain a placeholder password!");
            logger.warn("   Make sure you've replaced the example password with your actual database password.");
            logger.warn("   Find your password in: Supabase Dashboard → Settings → Database → Password");
        }
    }

    private void validateSupabaseConfiguration(URI uri, String connectionMode) {
        String query = uri.getQuery();
        boolean hasSslMode = query != null && query.contains("sslmode");

        if (!hasSslMode) {
            logger.warn("⚠️  SECURITY WARNING: No SSL mode specified in DATABASE_URL");
            logger.warn("   Recommended: Add '?sslmode=require' to your connection string for secure connections");
            logger.warn("   Example: postgresql://user:pass@hostname:port/db?sslmode=require");
        }

        if ("Transaction Pooler".equals(connectionMode)) {
            logger.info("✓ Using connection pooler - optimized for serverless and high-concurrency workloads");
            logger.info("  Pooler benefits:");
            logger.info("  • Faster connection establishment");
            logger.info("  • Better handling of connection spikes");
            logger.info("  • Reduced database overhead");
        } else {
            logger.info("✓ Using direct connection - suitable for persistent connections");
            logger.info("  For serverless deployments, consider using the pooler connection instead:");
            logger.info("  • Navigate to: Supabase Dashboard → Settings → Database");
            logger.info("  • Copy the 'Connection Pooling' string instead of 'Connection string'");
        }

        int port = uri.getPort();
        if (port == -1) {
            logger.warn("⚠️  WARNING: No port specified in DATABASE_URL");
            logger.warn("   Recommended ports: 5432 (direct) or 6543 (pooler)");
        }
    }

    private String buildProjectRefExtractionError(String hostname, String userInfo) {
        return String.format(
                "Failed to extract Supabase project reference from DATABASE_URL\n" +
                "Hostname: %s\n" +
                "UserInfo present: %s\n\n" +
                "Expected format:\n" +
                "• Direct: postgresql://<role>:<password>@db.<project-ref>.supabase.co:5432/<database>\n" +
                "• Pooler: postgresql://postgres.<project-ref>:<password>@aws-*-*.pooler.supabase.com:6543/<database>\n\n" +
                "ACTION REQUIRED:\n" +
                "1. Visit https://supabase.com/dashboard\n" +
                "2. Select your project\n" +
                "3. Go to Settings → Database\n" +
                "4. Copy the complete 'Connection string' (URI format)\n" +
                "5. Update your DATABASE_URL environment variable\n\n" +
                "The connection string should contain a 20-character alphanumeric project reference.",
                hostname, userInfo != null ? "Yes (masked)" : "No"
        );
    }

    private String buildInvalidProjectRefError(String projectRef, String hostname) {
        return String.format(
                "Invalid Supabase project reference format detected\n" +
                "Extracted value: '%s'\n" +
                "Hostname: %s\n\n" +
                "A valid Supabase project reference should be:\n" +
                "• Exactly 20 characters long\n" +
                "• Contains only lowercase letters (a-z) and numbers (0-9)\n\n" +
                "Common issues:\n" +
                "• Placeholder value not replaced (e.g., 'YOUR_PROJECT_REF')\n" +
                "• Incomplete or truncated connection string\n" +
                "• Using wrong format or manually constructed URL\n\n" +
                "ACTION REQUIRED:\n" +
                "1. Visit https://supabase.com/dashboard\n" +
                "2. Select your project\n" +
                "3. Go to Settings → Database\n" +
                "4. Copy the COMPLETE connection string exactly as provided\n" +
                "5. Do not manually edit or construct the URL\n" +
                "6. Update your DATABASE_URL environment variable",
                projectRef, hostname
        );
    }

    private String normalizeUrl(String url) {
        if (url.startsWith("jdbc:postgresql://")) {
            return url.substring("jdbc:".length());
        } else if (url.startsWith("postgres://")) {
            return url.replace("postgres://", "postgresql://");
        }
        return url;
    }

    private boolean isSupabaseHostname(String hostname) {
        return DIRECT_CONNECTION_PATTERN.matcher(hostname).matches() ||
               POOLER_CONNECTION_PATTERN.matcher(hostname).matches();
    }

    private String extractPassword(URI uri) {
        String userInfo = uri.getUserInfo();
        if (userInfo == null || !userInfo.contains(":")) {
            return null;
        }
        String[] parts = userInfo.split(":", 2);
        return parts.length == 2 ? parts[1] : null;
    }

    private String maskPassword(String url) {
        if (url == null) {
            return "null";
        }
        return url.replaceAll("://([^:@]+):([^@]+)@", "://$1:***@")
                  .replaceAll("password=([^&\\s]+)", "password=***");
    }

    public static class ValidationResult {
        private final boolean success;
        private final String message;

        private ValidationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static ValidationResult success(String message) {
            return new ValidationResult(true, message);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public boolean isFailure() {
            return !success;
        }
    }
}
