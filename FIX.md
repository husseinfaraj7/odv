# HikariCP JDBC URL Format Issue - Fix Documentation

## Problem Summary

When deploying to Render with a `DATABASE_URL` environment variable in the `postgres://` format, the application failed to start with the error:

```
java.sql.SQLException: Driver claims to not accept jdbcUrl, jdbc:postgresql://...
```

## Root Cause

The issue was caused by a configuration conflict in `application-prod.properties`:

```properties
# PROBLEMATIC CONFIGURATION (Before Fix)
spring.datasource.hikari.jdbc-url=${DATABASE_URL:jdbc:postgresql://...}
```

This configuration directly assigned the `DATABASE_URL` environment variable to HikariCP's `jdbc-url` property, **bypassing** the `DatabaseUrlEnvironmentPostProcessor` conversion logic.

### How It Was Supposed to Work

1. Render provides `DATABASE_URL` in format: `postgres://user:pass@host:port/database`
2. `DatabaseUrlEnvironmentPostProcessor` intercepts this value early in Spring Boot startup
3. Converts it to proper JDBC format: `jdbc:postgresql://host:port/database?user=user&password=pass`
4. Sets the converted value as `spring.datasource.url`
5. HikariCP reads `spring.datasource.url` (the converted value)

### What Actually Happened

With `spring.datasource.hikari.jdbc-url=${DATABASE_URL}`:

1. Render provides `DATABASE_URL`: `postgres://user:pass@host:port/database`
2. `DatabaseUrlEnvironmentPostProcessor` runs and converts it, setting `spring.datasource.url` correctly
3. **BUT** HikariCP ignores `spring.datasource.url` when `hikari.jdbc-url` is explicitly set
4. HikariCP receives the **unconverted** `postgres://` URL directly from `DATABASE_URL`
5. PostgreSQL JDBC driver rejects the `postgres://` prefix (expects `jdbc:postgresql://`)
6. Application fails to start

## The Solution

**Remove the `spring.datasource.hikari.jdbc-url` override** from `application-prod.properties`:

```properties
# CORRECTED CONFIGURATION (After Fix)
# Removed: spring.datasource.hikari.jdbc-url=${DATABASE_URL:...}
# 
# HikariCP will now automatically use spring.datasource.url which contains
# the properly converted JDBC URL from DatabaseUrlEnvironmentPostProcessor
```

By removing this property:
- HikariCP falls back to reading `spring.datasource.url` (the standard property)
- `spring.datasource.url` contains the properly converted JDBC URL from `DatabaseUrlEnvironmentPostProcessor`
- Connection succeeds with correct JDBC format

## Configuration Files

### Before (Problematic)

**backend/src/main/resources/application-prod.properties**
```properties
# Database Configuration
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://...}
spring.datasource.username=${DATABASE_USER:}
spring.datasource.password=${DATABASE_PASSWORD:}

# HikariCP Configuration
spring.datasource.hikari.jdbc-url=${DATABASE_URL:jdbc:postgresql://...}  # ❌ WRONG
# ... other hikari properties
```

### After (Fixed)

**backend/src/main/resources/application-prod.properties**
```properties
# Database Configuration
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://...}
spring.datasource.username=${DATABASE_USER:}
spring.datasource.password=${DATABASE_PASSWORD:}

# HikariCP Configuration
# Note: hikari.jdbc-url property removed to allow HikariCP to use
# the properly converted spring.datasource.url value
spring.datasource.hikari.maximum-pool-size=5
# ... other hikari properties
```

## Verification Steps

### 1. Check Application Startup Logs

When the fix is working correctly, you should see these log entries in order:

```
INFO  - DatabaseUrlEnvironmentPostProcessor: Original DATABASE_URL detected: postgres://****:****@dpg-xxxxxxxxxxxx-a.oregon-postgres.render.com:5432/odvsicilia_db
INFO  - DatabaseUrlEnvironmentPostProcessor: DATABASE_URL conversion occurred: Standard PostgreSQL URL converted to JDBC format
INFO  - DatabaseUrlEnvironmentPostProcessor: Resulting spring.datasource.url: jdbc:postgresql://dpg-xxxxxxxxxxxx-a.oregon-postgres.render.com:5432/odvsicilia_db?user=****&password=****
INFO  - com.zaxxer.hikari.HikariDataSource: HikariPool-1 - Starting...
INFO  - com.zaxxer.hikari.pool.HikariPool: HikariPool-1 - Added connection org.postgresql.jdbc.PgConnection@...
INFO  - com.zaxxer.hikari.HikariDataSource: HikariPool-1 - Start completed.
```

### 2. Verify on Render Deployment

1. Deploy the application to Render
2. Go to your web service dashboard
3. Click on "Logs" tab
4. Search for "DatabaseUrlEnvironmentPostProcessor" to confirm conversion is happening
5. Search for "HikariPool" to verify connection pool initialization succeeded
6. Confirm no errors containing "Driver claims to not accept jdbcUrl"

### 3. Key Log Patterns to Look For

**✅ Success Indicators:**
```
DatabaseUrlEnvironmentPostProcessor: Resulting spring.datasource.url: jdbc:postgresql://
HikariPool-1 - Start completed
```

**❌ Failure Indicators (if fix not applied):**
```
Driver claims to not accept jdbcUrl
org.postgresql.util.PSQLException
HikariPool-1 - Exception during pool initialization
```

## Related Files

- **DatabaseUrlEnvironmentPostProcessor.java**: Converts `postgres://` to `jdbc:postgresql://`
- **application.properties**: Base configuration (no hikari.jdbc-url override)
- **application-prod.properties**: Production configuration (fixed by removing hikari.jdbc-url)
- **DEPLOYMENT.md**: Comprehensive deployment documentation including URL format handling

## Testing Locally

To test this configuration locally with a Render-style DATABASE_URL:

```powershell
# Set environment variable with postgres:// format
$env:DATABASE_URL="postgres://user:password@localhost:5432/testdb"
$env:SPRING_PROFILES_ACTIVE="prod"

# Run the application
.\mvn.ps1 spring-boot:run

# Check logs for DatabaseUrlEnvironmentPostProcessor conversion
```

## Key Takeaways

1. **Never override `spring.datasource.hikari.jdbc-url`** when using `DatabaseUrlEnvironmentPostProcessor`
2. HikariCP's explicit `jdbc-url` property **takes precedence** over `spring.datasource.url`
3. The `DatabaseUrlEnvironmentPostProcessor` only sets `spring.datasource.url`, not `hikari.jdbc-url`
4. Always verify deployment logs show the conversion process completing successfully
5. The standard `spring.datasource.url` property is sufficient for HikariCP to function correctly

## Additional Notes

- This issue only manifests when `DATABASE_URL` is provided in `postgres://` format (common on platforms like Render, Heroku)
- If `DATABASE_URL` is already in `jdbc:postgresql://` format, the processor detects this and skips conversion
- The fix ensures consistent behavior across all deployment environments
- No code changes were required - only configuration cleanup

## References

- [HikariCP Configuration Documentation](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [Spring Boot JDBC Configuration](https://docs.spring.io/spring-boot/reference/data/sql.html#data.sql.datasource.configuration)
- Project file: `DEPLOYMENT.md` (sections on Database URL Configuration)
