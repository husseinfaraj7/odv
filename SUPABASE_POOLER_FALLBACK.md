# Supabase Pooler Fallback Mechanism

## Overview
The `DatabaseConnectionRetryConfig` has been enhanced with automatic fallback detection and handling for Supabase pooler connection failures. When a connection to the Supabase pooler endpoint fails, the system automatically attempts to connect to the direct database endpoint.

## Features

### 1. **Intelligent Pooler Error Detection**
The system detects Supabase pooler-specific connection failures by examining:
- **Exception Messages**: Looks for "Connection refused", "Connection timed out", "ConnectException"
- **URL Pattern**: Validates if the URL is a Supabase pooler endpoint (`.pooler.supabase.com` or port `6543`)
- **Port Numbers**: Identifies pooler port (6543) vs direct port (5432)

### 2. **Automatic Fallback Mechanism**
When a pooler connection error is detected:
1. Logs clear "SUPABASE POOLER CONNECTION FAILED" error message
2. Automatically converts pooler URL to direct database URL
3. Updates the datasource URL in Spring's Environment
4. Attempts connection to direct endpoint
5. Logs success or continued failure with specific categorization

### 3. **Clear Error Logging**
The system provides distinct logging for different failure scenarios:

#### Pooler-Specific Failure
```
ERROR - SUPABASE POOLER CONNECTION FAILED on attempt 1: Connection refused to pooler endpoint
ERROR - Pooler error details: [exception details]
WARN  - Attempting automatic fallback from pooler to direct database connection...
INFO  - Switched datasource URL from pooler endpoint to direct connection: jdbc:postgresql://db.aws-XXX.supabase.co:5432/...
INFO  - Direct database connection established successfully after pooler failure
```

#### Direct Connection Failure
```
ERROR - DIRECT DATABASE CONNECTION FAILED on attempt 2: [exception details]
```

#### General Connectivity Issue
```
ERROR - DATABASE CONNECTION FAILED on attempt 1 (general connectivity issue): [exception details]
```

### 4. **Fail-Fast Mode**
When both pooler and direct connection attempts fail, the system enters fail-fast mode and provides comprehensive diagnostics:

```
================================================================================
FAIL-FAST: DATABASE CONNECTION COMPLETELY FAILED
================================================================================
Both Supabase pooler and direct database connection attempts have failed.

Connection attempts made:
  1. Supabase Pooler endpoint (port 6543) - FAILED: Connection refused
  2. Direct database endpoint (port 5432) - FAILED: Connection refused

Possible causes:
  • Supabase project is paused or suspended
  • Database instance is not running
  • Network connectivity issues to Supabase infrastructure
  • Firewall blocking outbound connections on ports 6543 and 5432
  • Invalid database credentials or URL

Recommended actions:
  1. Check Supabase project status: https://app.supabase.com/project/[project-id]/settings/general
  2. Verify project is not paused (free tier projects pause after inactivity)
  3. Check pooler configuration: https://app.supabase.com/project/[project-id]/settings/database
  4. Verify DATABASE_URL environment variable is correct
  5. Test connectivity: telnet [hostname] 6543
  6. Review Supabase status page: https://status.supabase.com

Current URL: jdbc:postgresql://db.aws-XXX.supabase.co:5432/...
================================================================================
```

## Implementation Details

### URL Conversion Logic
The pooler-to-direct URL conversion handles multiple Supabase URL formats:

1. **Standard Pooler Format**:
   - Input: `jdbc:postgresql://aws-1-eu-north-1.pooler.supabase.com:6543/postgres`
   - Output: `jdbc:postgresql://db.aws-1-eu-north-1.supabase.co:5432/postgres`

2. **Generic Port-Based Conversion**:
   - Any URL containing `:6543` is converted to `:5432`

### State Management
The system tracks two boolean flags:
- `poolerFallbackAttempted`: Prevents multiple fallback attempts
- `directConnectionFailed`: Triggers fail-fast mode when both endpoints fail

### Environment Property Update
The fallback mechanism updates Spring's `spring.datasource.url` property using a high-priority `MapPropertySource`, ensuring the new URL is used throughout the application.

## Error Detection Patterns

### Supabase Pooler URL Detection
```java
url.contains(".pooler.supabase.com") ||
(url.contains(".supabase.com") && url.contains(":6543")) ||
(url.contains(".supabase.co") && url.contains(":6543"))
```

### Connection Refused Detection
```java
fullMessage.contains("Connection refused") ||
fullMessage.contains("Connection timed out") ||
fullMessage.contains("ConnectException")
```

## Benefits

1. **Automatic Recovery**: No manual intervention required for pooler failures
2. **Clear Diagnostics**: Distinct error messages for different failure types
3. **Actionable Guidance**: Fail-fast mode provides specific troubleshooting steps
4. **State Awareness**: Prevents endless retry loops with intelligent state tracking
5. **Production Ready**: Handles real-world Supabase deployment scenarios

## Testing

Basic unit tests are provided in `DatabaseConnectionRetryConfigTest.java`:
- Verifies RetryTemplate creation
- Tests BeanPostProcessor initialization
- Confirms non-DataSource beans pass through unchanged

For integration testing with actual database connections, use the existing integration test suite with H2 database.

## Configuration

No additional configuration is required. The fallback mechanism is automatically enabled when using `DatabaseConnectionRetryConfig` in your Spring Boot application.

### Environment Variables
```bash
# Pooler endpoint (will automatically fallback if connection fails)
DATABASE_URL=postgresql://postgres.aws-XXX.pooler.supabase.com:6543/postgres?user=postgres&password=xxx

# Direct endpoint (used as fallback)
# Automatically converted to: jdbc:postgresql://db.aws-XXX.supabase.co:5432/postgres?user=postgres&password=xxx
```

## Compatibility

- **Spring Boot**: 3.2.0+
- **Java**: 17+
- **Supabase**: All regions and project types
- **HikariCP**: Compatible with all versions used by Spring Boot

## Future Enhancements

Potential improvements for future versions:
1. Configurable retry attempts per endpoint type
2. Custom fallback URL configuration
3. Metrics collection for pooler vs direct connection success rates
4. Health check endpoint to report connection type in use
