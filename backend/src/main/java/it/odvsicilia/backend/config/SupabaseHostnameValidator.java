package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

public class SupabaseHostnameValidator {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseHostnameValidator.class);
    
    private static final Pattern VALID_DIRECT_PATTERN = Pattern.compile("^db\\.[a-zA-Z0-9-]+\\.supabase\\.co$");
    private static final Pattern VALID_POOLER_PATTERN = Pattern.compile("^aws-0-[a-zA-Z0-9-]+\\.pooler\\.supabase\\.com$");
    private static final Pattern MALFORMED_PATTERN = Pattern.compile("^db\\.aws-.*");

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        private ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, "Hostname is valid");
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message);
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }

    public static ValidationResult validate(String databaseUrl) {
        if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
            return ValidationResult.failure("DATABASE_URL is null or empty");
        }

        String hostname = extractHostname(databaseUrl);
        if (hostname == null) {
            return ValidationResult.failure("Could not extract hostname from DATABASE_URL");
        }

        if (!hostname.contains("supabase")) {
            return ValidationResult.success();
        }

        if (MALFORMED_PATTERN.matcher(hostname).find()) {
            String errorMessage = String.format(
                "Malformed Supabase hostname detected: '%s'. " +
                "This hostname incorrectly mixes direct and pooler formats. " +
                "Valid formats are:\n" +
                "  - Direct: db.{PROJECT_REF}.supabase.co (e.g., db.abcdefghijklmnop.supabase.co)\n" +
                "  - Pooler: aws-0-{REGION}.pooler.supabase.com (e.g., aws-0-eu-north-1.pooler.supabase.com)",
                hostname
            );
            
            logger.error(errorMessage);
            return ValidationResult.failure(errorMessage);
        }

        if (VALID_DIRECT_PATTERN.matcher(hostname).matches()) {
            logger.debug("Valid Supabase direct connection hostname: {}", hostname);
            return ValidationResult.success();
        }

        if (VALID_POOLER_PATTERN.matcher(hostname).matches()) {
            logger.debug("Valid Supabase pooler connection hostname: {}", hostname);
            return ValidationResult.success();
        }

        String errorMessage = String.format(
            "Invalid Supabase hostname format: '%s'. " +
            "Valid formats are:\n" +
            "  - Direct: db.{PROJECT_REF}.supabase.co (e.g., db.abcdefghijklmnop.supabase.co)\n" +
            "  - Pooler: aws-0-{REGION}.pooler.supabase.com (e.g., aws-0-eu-north-1.pooler.supabase.com)",
            hostname
        );
        
        logger.error(errorMessage);
        return ValidationResult.failure(errorMessage);
    }

    private static String extractHostname(String databaseUrl) {
        try {
            int schemeEnd = databaseUrl.indexOf("://");
            if (schemeEnd == -1) {
                return null;
            }

            String afterScheme = databaseUrl.substring(schemeEnd + 3);

            int atIndex = afterScheme.lastIndexOf('@');
            String hostAndRest = (atIndex != -1) ? afterScheme.substring(atIndex + 1) : afterScheme;

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
            String hostname = (colonIndex != -1) ? hostPart.substring(0, colonIndex) : hostPart;

            return hostname.trim();
        } catch (Exception e) {
            logger.error("Failed to extract hostname from DATABASE_URL: {}", e.getMessage());
            return null;
        }
    }
}
