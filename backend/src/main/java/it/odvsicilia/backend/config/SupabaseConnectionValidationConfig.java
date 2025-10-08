package it.odvsicilia.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
public class SupabaseConnectionValidationConfig {

    private static final Logger logger = LoggerFactory.getLogger(SupabaseConnectionValidationConfig.class);

    @Bean
    @ConditionalOnProperty(name = "supabase.connection.validation.enabled", havingValue = "true", matchIfMissing = true)
    public SupabaseConnectionValidationRunner supabaseConnectionValidationRunner(
            SupabaseConnectionValidator validator, Environment environment) {
        return new SupabaseConnectionValidationRunner(validator, environment);
    }

    public static class SupabaseConnectionValidationRunner {
        private final SupabaseConnectionValidator validator;
        private final Environment environment;

        public SupabaseConnectionValidationRunner(SupabaseConnectionValidator validator, Environment environment) {
            this.validator = validator;
            this.environment = environment;
            
            runValidation();
        }

        private void runValidation() {
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isDevOrTest = Arrays.stream(activeProfiles)
                    .anyMatch(profile -> "dev".equals(profile) || "test".equals(profile));

            if (isDevOrTest) {
                logger.info("Supabase connection validation skipped for dev/test profile");
                logger.debug("Active profiles: {}", Arrays.toString(activeProfiles));
                return;
            }

            String databaseUrl = System.getenv("DATABASE_URL");
            if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
                databaseUrl = environment.getProperty("DATABASE_URL");
            }
            if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
                databaseUrl = environment.getProperty("spring.datasource.url");
            }

            if (databaseUrl == null || databaseUrl.trim().isEmpty()) {
                logger.info("DATABASE_URL not configured, skipping Supabase connection validation");
                return;
            }

            if (!isSupabaseUrl(databaseUrl)) {
                logger.info("DATABASE_URL does not appear to be a Supabase endpoint, skipping validation");
                logger.debug("Non-Supabase URL detected: {}", maskPassword(databaseUrl));
                return;
            }

            logger.info("Detected Supabase database URL, performing pre-startup validation");

            SupabaseConnectionValidator.SupabaseValidationResult result = validator.validate();

            if (!result.isAnyAvailable()) {
                String errorMessage = buildConnectionFailureMessage(result);
                logger.error(errorMessage);
                throw new IllegalStateException(errorMessage);
            }

            if (!result.isBothAvailable()) {
                logger.warn("Connection validation completed with warnings - see logs above for details");
            } else {
                logger.info("Supabase connection validation completed successfully");
            }
        }

        private boolean isSupabaseUrl(String url) {
            if (url == null) {
                return false;
            }
            return url.contains("supabase.co") || url.contains("supabase.com");
        }

        private String buildConnectionFailureMessage(SupabaseConnectionValidator.SupabaseValidationResult result) {
            StringBuilder message = new StringBuilder();
            message.append("\n");
            message.append("=".repeat(80)).append("\n");
            message.append("DATABASE CONNECTION VALIDATION FAILED\n");
            message.append("=".repeat(80)).append("\n");
            message.append("\n");
            message.append("Unable to establish connection to Supabase database.\n");
            message.append("\n");
            message.append("Hostname: ").append(result.getHostname()).append("\n");
            message.append("Transaction Pooler (port 6543): ").append(result.isPoolerAvailable() ? "✓ Available" : "✗ Unavailable").append("\n");
            message.append("Direct Connection (port 5432): ").append(result.isDirectAvailable() ? "✓ Available" : "✗ Unavailable").append("\n");
            message.append("\n");
            message.append("ERRORS:\n");
            if (result.getPoolerError() != null) {
                message.append("  Pooler: ").append(result.getPoolerError()).append("\n");
            }
            if (result.getDirectError() != null) {
                message.append("  Direct: ").append(result.getDirectError()).append("\n");
            }
            message.append("\n");
            message.append("TROUBLESHOOTING STEPS:\n");
            message.append("  1. Verify hostname '").append(result.getHostname()).append("' is correct\n");
            message.append("  2. Check network connectivity from this host to Supabase\n");
            message.append("  3. Verify the Supabase project is active and running\n");
            message.append("  4. Ensure firewall/security groups allow outbound connections to ports 5432 and 6543\n");
            message.append("  5. Verify DATABASE_URL environment variable is set correctly\n");
            message.append("  6. Check Supabase dashboard for project status and connection strings:\n");
            message.append("     https://app.supabase.com/ → Project Settings → Database\n");
            message.append("\n");
            message.append("The application cannot start without database connectivity.\n");
            message.append("\n");
            message.append("=".repeat(80)).append("\n");
            return message.toString();
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
}
