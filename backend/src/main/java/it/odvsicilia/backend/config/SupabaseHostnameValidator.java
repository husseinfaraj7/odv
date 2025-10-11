package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SupabaseHostnameValidator {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseHostnameValidator.class);

    private static final Pattern DIRECT_HOSTNAME_PATTERN = Pattern.compile("^db\\.([a-z0-9]{20})\\.supabase\\.co$");
    private static final Pattern POOLER_HOSTNAME_PATTERN = Pattern.compile("^aws-([01])-([a-z]+-[a-z]+-[0-9]+)\\.pooler\\.supabase\\.com$");
    private static final Pattern PROJECT_REF_PATTERN = Pattern.compile("^[a-z0-9]{20}$");

    public static HostnameValidationResult validate(String hostname) {
        if (hostname == null || hostname.trim().isEmpty()) {
            return HostnameValidationResult.invalid("Hostname is null or empty", null);
        }

        hostname = hostname.trim().toLowerCase();

        if (isValidDirectHostname(hostname)) {
            Matcher matcher = DIRECT_HOSTNAME_PATTERN.matcher(hostname);
            if (matcher.matches()) {
                String projectRef = matcher.group(1);
                return HostnameValidationResult.valid(hostname, HostnameType.DIRECT, projectRef, null, null);
            }
        }

        if (isValidPoolerHostname(hostname)) {
            Matcher matcher = POOLER_HOSTNAME_PATTERN.matcher(hostname);
            if (matcher.matches()) {
                String poolerIndex = matcher.group(1);
                String region = matcher.group(2);
                return HostnameValidationResult.valid(hostname, HostnameType.POOLER, null, region, poolerIndex);
            }
        }

        if (hostname.contains("supabase.co") || hostname.contains("supabase.com")) {
            return buildMalformedSupabaseHostnameError(hostname);
        }

        return HostnameValidationResult.invalid("Hostname is not a valid Supabase hostname", hostname);
    }

    private static boolean isValidDirectHostname(String hostname) {
        return DIRECT_HOSTNAME_PATTERN.matcher(hostname).matches();
    }

    private static boolean isValidPoolerHostname(String hostname) {
        return POOLER_HOSTNAME_PATTERN.matcher(hostname).matches();
    }

    private static HostnameValidationResult buildMalformedSupabaseHostnameError(String hostname) {
        StringBuilder errorMessage = new StringBuilder("Malformed Supabase hostname: ").append(hostname).append("\n\n");

        if (hostname.startsWith("db.") && hostname.contains("pooler.supabase.com")) {
            errorMessage.append("This appears to be a malformed hostname mixing DIRECT and POOLER formats.\n");
            errorMessage.append("ERROR: Mixed format detected - cannot combine 'db.' prefix with pooler hostname\n");
            errorMessage.append("Choose one format:\n");
            errorMessage.append("- Direct: db.PROJECT_REF.supabase.co\n");
            errorMessage.append("- Pooler: aws-0-REGION.pooler.supabase.com\n\n");
            errorMessage.append("Valid examples:\n");
            errorMessage.append("- db.abcdefghijklmnopqrst.supabase.co\n");
            errorMessage.append("- aws-0-eu-north-1.pooler.supabase.com\n");
            return HostnameValidationResult.invalid(errorMessage.toString(), hostname);
        }

        if (hostname.startsWith("db.") && hostname.contains(".supabase.")) {
            errorMessage.append("This appears to be a malformed DIRECT connection hostname.\n");
            errorMessage.append("Expected format: db.PROJECT_REF.supabase.co\n");
            errorMessage.append("- PROJECT_REF must be exactly 20 lowercase alphanumeric characters\n");
            errorMessage.append("- Hostname must end with '.supabase.co' (not '.supabase.com')\n");
            errorMessage.append("- Must NOT include 'pooler' in the hostname\n\n");

            if (hostname.contains("pooler")) {
                errorMessage.append("ERROR: Direct connection hostname should not contain 'pooler'\n");
                errorMessage.append("- If you want direct connection, use: db.PROJECT_REF.supabase.co\n");
                errorMessage.append("- If you want pooler connection, use: aws-0-REGION.pooler.supabase.com\n\n");
            }

            if (hostname.endsWith(".supabase.com")) {
                errorMessage.append("ERROR: Direct connection hostname should end with '.supabase.co' not '.supabase.com'\n\n");
            }

            if (hostname.matches("^db\\.[a-z0-9-]+\\.supabase\\.co$")) {
                String extractedRef = hostname.substring(3, hostname.indexOf(".supabase.co"));
                if (extractedRef.length() != 20) {
                    errorMessage.append(String.format("ERROR: Project reference has invalid length: %d (expected 20)\n", extractedRef.length()));
                    errorMessage.append("Extracted: '").append(extractedRef).append("'\n\n");
                }
            }

        } else if (hostname.contains("pooler.supabase.com")) {
            errorMessage.append("This appears to be a malformed POOLER connection hostname.\n");
            errorMessage.append("Expected format: aws-0-REGION.pooler.supabase.com or aws-1-REGION.pooler.supabase.com\n");
            errorMessage.append("- Must start with 'aws-0-' or 'aws-1-' (pooler index)\n");
            errorMessage.append("- Must include a valid AWS region (e.g., eu-north-1, us-east-1, ap-southeast-1)\n");
            errorMessage.append("- Must end with '.pooler.supabase.com'\n");
            errorMessage.append("- Must NOT start with 'db.'\n\n");

            if (hostname.startsWith("db.")) {
                errorMessage.append("ERROR: Pooler hostname should not start with 'db.'\n");
                errorMessage.append("- Direct connection format: db.PROJECT_REF.supabase.co\n");
                errorMessage.append("- Pooler connection format: aws-0-REGION.pooler.supabase.com\n\n");
            }

            if (!hostname.startsWith("aws-")) {
                errorMessage.append("ERROR: Pooler hostname must start with 'aws-0-' or 'aws-1-'\n");
                errorMessage.append("Examples:\n");
                errorMessage.append("- aws-0-eu-north-1.pooler.supabase.com\n");
                errorMessage.append("- aws-1-us-east-1.pooler.supabase.com\n\n");
            }

            if (hostname.matches("^db\\.aws-.*\\.pooler\\.supabase\\.com$")) {
                errorMessage.append("ERROR: Mixed format detected - cannot combine 'db.' prefix with pooler hostname\n");
                errorMessage.append("Choose one format:\n");
                errorMessage.append("- Direct: db.PROJECT_REF.supabase.co\n");
                errorMessage.append("- Pooler: aws-0-REGION.pooler.supabase.com\n\n");
            }

            if (hostname.matches("^aws-[0-9]+-.*\\.supabase\\.co$")) {
                errorMessage.append("ERROR: Pooler hostname should end with '.pooler.supabase.com' not '.supabase.co'\n\n");
            }

        } else if (hostname.endsWith(".supabase.co") && !hostname.startsWith("db.")) {
            errorMessage.append("This appears to be a malformed DIRECT connection hostname.\n");
            errorMessage.append("ERROR: Direct connection hostname must start with 'db.'\n");
            errorMessage.append("Expected format: db.PROJECT_REF.supabase.co\n\n");

        } else if (hostname.endsWith(".supabase.com") && !hostname.contains("pooler")) {
            errorMessage.append("This appears to be a malformed Supabase hostname.\n");
            errorMessage.append("ERROR: '.supabase.com' domain is used for pooler connections only\n");
            errorMessage.append("- For direct connections: db.PROJECT_REF.supabase.co\n");
            errorMessage.append("- For pooler connections: aws-0-REGION.pooler.supabase.com\n\n");

        } else {
            errorMessage.append("This appears to be a malformed or unrecognized Supabase hostname format.\n");
            errorMessage.append("Valid Supabase hostname formats:\n");
            errorMessage.append("- Direct connection: db.PROJECT_REF.supabase.co\n");
            errorMessage.append("- Pooler connection: aws-0-REGION.pooler.supabase.com (or aws-1-REGION)\n\n");
        }

        errorMessage.append("Valid regions include: eu-north-1, us-east-1, us-west-1, ap-southeast-1, etc.\n");
        errorMessage.append("Valid pooler indexes: 0, 1\n");
        errorMessage.append("\nExamples of valid hostnames:\n");
        errorMessage.append("- db.abcdefghijklmnopqrst.supabase.co\n");
        errorMessage.append("- aws-0-eu-north-1.pooler.supabase.com\n");
        errorMessage.append("- aws-1-us-east-1.pooler.supabase.com\n");

        return HostnameValidationResult.invalid(errorMessage.toString(), hostname);
    }

    public enum HostnameType {
        DIRECT("Direct Connection (db.PROJECT_REF.supabase.co)"),
        POOLER("Pooler Connection (aws-INDEX-REGION.pooler.supabase.com)");

        private final String description;

        HostnameType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public static class HostnameValidationResult {
        private final boolean valid;
        private final String hostname;
        private final HostnameType type;
        private final String projectRef;
        private final String region;
        private final String poolerIndex;
        private final String errorMessage;

        private HostnameValidationResult(boolean valid, String hostname, HostnameType type,
                                         String projectRef, String region, String poolerIndex,
                                         String errorMessage) {
            this.valid = valid;
            this.hostname = hostname;
            this.type = type;
            this.projectRef = projectRef;
            this.region = region;
            this.poolerIndex = poolerIndex;
            this.errorMessage = errorMessage;
        }

        public static HostnameValidationResult valid(String hostname, HostnameType type,
                                                      String projectRef, String region, String poolerIndex) {
            return new HostnameValidationResult(true, hostname, type, projectRef, region, poolerIndex, null);
        }

        public static HostnameValidationResult invalid(String errorMessage, String hostname) {
            return new HostnameValidationResult(false, hostname, null, null, null, null, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public boolean isInvalid() {
            return !valid;
        }

        public String getHostname() {
            return hostname;
        }

        public HostnameType getType() {
            return type;
        }

        public String getProjectRef() {
            return projectRef;
        }

        public String getRegion() {
            return region;
        }

        public String getPoolerIndex() {
            return poolerIndex;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public boolean isDirectConnection() {
            return valid && type == HostnameType.DIRECT;
        }

        public boolean isPoolerConnection() {
            return valid && type == HostnameType.POOLER;
        }
    }
}
