# Supabase Connection Validator - Implementation Summary

## Overview
Created a comprehensive pre-startup database connectivity validation system that checks both Supabase transaction pooler (port 6543) and direct connection (port 5432) endpoints before HikariCP datasource initialization.

## Files Created

### 1. Core Validator (`SupabaseConnectionValidator.java`)
**Location:** `backend/src/main/java/it/odvsicilia/backend/config/SupabaseConnectionValidator.java`

**Key Features:**
- DNS resolution verification
- TCP socket connectivity tests for both ports 6543 and 5432
- Hostname extraction from DATABASE_URL (supports JDBC and postgres:// formats)
- Returns structured `SupabaseValidationResult` with availability status
- Detailed diagnostic logging with actionable guidance

**Key Methods:**
- `validate()` - Main entry point, returns validation result
- `extractHostname()` - Parses DATABASE_URL to extract hostname
- `performDnsResolution()` - Validates DNS can resolve hostname
- `testTcpConnectivity()` - Tests TCP connection on specific port
- `logValidationResults()` - Comprehensive logging with guidance

**Result Object:**
```java
SupabaseValidationResult {
    boolean isPoolerAvailable()     // Port 6543 accessible
    boolean isDirectAvailable()     // Port 5432 accessible
    boolean isAnyAvailable()        // At least one works
    boolean isBothAvailable()       // Both work (optimal)
    ConnectionMode getAvailableConnectionMode() // POOLER, DIRECT, BOTH, NEITHER
    String getHostname()            // Extracted hostname
    String getPoolerError()         // Error if pooler failed
    String getDirectError()         // Error if direct failed
}
```

### 2. Configuration & Startup Wiring (`SupabaseConnectionValidationConfig.java`)
**Location:** `backend/src/main/java/it/odvsicilia/backend/config/SupabaseConnectionValidationConfig.java`

**Key Features:**
- Wires validator into Spring Bean initialization phase
- Runs **before** HikariCP datasource creation
- Profile-aware (skips for dev/test profiles)
- Supabase URL detection (only validates Supabase databases)
- Fail-fast with detailed error messages when neither connection mode works
- Conditional execution via `supabase.connection.validation.enabled` property

**Startup Sequence:**
```
Application Start
    ↓
Environment Post-Processing
    ↓
@Configuration Bean Creation
    ↓
SupabaseConnectionValidationRunner Constructor
    ↓
    ├─ Profile Check (skip if dev/test)
    ├─ URL Check (skip if not Supabase)
    ├─ DNS Resolution
    ├─ TCP Test: Port 6543 (Pooler)
    └─ TCP Test: Port 5432 (Direct)
    ↓
Validation Result → Fail Fast if NEITHER available
    ↓
HikariCP Datasource Initialization
    ↓
Application Ready
```

### 3. Unit Tests (`SupabaseConnectionValidatorTest.java`)
**Location:** `backend/src/test/java/it/odvsicilia/backend/config/SupabaseConnectionValidatorTest.java`

**Test Coverage:**
- Connection mode detection (BOTH, POOLER, DIRECT, NEITHER)
- Error message handling
- Hostname extraction
- Result object state management
- ConnectionMode enum descriptions

**Tests:**
- `testConnectionModeBoth()` - Both available
- `testConnectionModePoolerOnly()` - Only pooler available
- `testConnectionModeDirectOnly()` - Only direct available
- `testConnectionModeNeither()` - Neither available
- `testErrorMessages()` - Error propagation
- `testConnectionModeDescriptions()` - Enum descriptions

### 4. Integration Tests (`SupabaseConnectionValidationIntegrationTest.java`)
**Location:** `backend/src/test/java/it/odvsicilia/backend/config/SupabaseConnectionValidationIntegrationTest.java`

**Test Coverage:**
- Profile-based skipping (test profile)
- Bean availability
- Test environment handling

### 5. Documentation (`SUPABASE_CONNECTION_VALIDATION.md`)
**Location:** `backend/SUPABASE_CONNECTION_VALIDATION.md`

Comprehensive documentation covering:
- Overview and features
- Connection modes explanation
- Configuration options
- Troubleshooting guide
- Integration with existing components

## Connection Mode Scenarios

### Scenario 1: Both Available (Optimal)
```
✓ Transaction Pooler (port 6543): AVAILABLE
✓ Direct Connection (port 5432): AVAILABLE
Status: Application starts normally
Action: None required
```

### Scenario 2: Direct Only (Warning)
```
✗ Transaction Pooler (port 6543): UNAVAILABLE
✓ Direct Connection (port 5432): AVAILABLE
Status: Application starts with warning
Action: Enable pooler in Supabase dashboard
```

**Actionable Guidance Logged:**
```
⚠ POOLER UNAVAILABLE - ACTION REQUIRED ⚠
To enable connection pooling in Supabase:
  1. Open your Supabase dashboard at https://app.supabase.com/
  2. Select your project
  3. Navigate to: Settings → Database
  4. Scroll down to the 'Connection Pooling' section
  5. Enable 'Transaction Mode' pooling
  6. Note the pooler connection string (uses port 6543)
  7. Update your DATABASE_URL to use the pooler endpoint

Benefits of using the pooler:
  • Better connection management for serverless environments
  • Reduced connection overhead
  • Improved scalability
```

### Scenario 3: Pooler Only (Acceptable)
```
✓ Transaction Pooler (port 6543): AVAILABLE
✗ Direct Connection (port 5432): UNAVAILABLE
Status: Application starts normally
Action: None required (pooler is preferred)
```

### Scenario 4: Neither Available (Fatal)
```
✗ Transaction Pooler (port 6543): UNAVAILABLE
✗ Direct Connection (port 5432): UNAVAILABLE
Status: Application fails to start
Action: Check network, firewall, and database status
```

**Error Message:**
```
================================================================================
DATABASE CONNECTION VALIDATION FAILED
================================================================================

Unable to establish connection to Supabase database.

Hostname: db.example.supabase.co
Transaction Pooler (port 6543): ✗ Unavailable
Direct Connection (port 5432): ✗ Unavailable

ERRORS:
  Pooler: Connection failed to port 6543
  Direct: Connection failed to port 5432

TROUBLESHOOTING STEPS:
  1. Verify hostname 'db.example.supabase.co' is correct
  2. Check network connectivity from this host to Supabase
  3. Verify the Supabase project is active and running
  4. Ensure firewall/security groups allow outbound connections to ports 5432 and 6543
  5. Verify DATABASE_URL environment variable is set correctly
  6. Check Supabase dashboard for project status and connection strings:
     https://app.supabase.com/ → Project Settings → Database
================================================================================
```

## Configuration

### Enable/Disable Validation
```properties
# application.properties
# Enabled by default
supabase.connection.validation.enabled=true

# To disable
supabase.connection.validation.enabled=false
```

### Automatic Skipping

Validation is automatically skipped for:
1. **Dev profile** (`spring.profiles.active=dev`) - uses H2
2. **Test profile** (`spring.profiles.active=test`) - uses H2
3. **Non-Supabase URLs** - when DATABASE_URL doesn't contain "supabase.co" or "supabase.com"
4. **Missing DATABASE_URL** - when environment variable is not configured

## Integration with Existing Components

### Works Alongside
- `DatabaseUrlEnvironmentPostProcessor` - Converts DATABASE_URL format
- `DatabaseConnectivityValidator` - Generic connectivity validator
- `DatabaseValidationConfig` - Post-datasource validation
- `DatabaseConnectionRetryConfig` - Retry logic for transient failures

### Execution Order
1. `DatabaseUrlEnvironmentPostProcessor` - URL format conversion
2. **`SupabaseConnectionValidator`** - Pre-startup validation (NEW)
3. HikariCP Datasource Initialization
4. `DatabaseValidationConfig` - Post-datasource validation
5. Application Ready

## Technical Details

### DNS Resolution
- Uses `InetAddress.getByName()` to resolve hostname
- Logs resolved IP address for debugging
- Fails fast if DNS resolution fails

### TCP Connectivity Tests
- Uses `Socket.connect()` with 5-second timeout
- Tests both ports sequentially
- Distinguishes between timeout, refused, and unreachable errors

### Hostname Extraction
Supports multiple URL formats:
- `jdbc:postgresql://host:port/db?params`
- `postgres://user:pass@host:port/db`
- `postgresql://user:pass@host:port/db`

Extraction logic:
1. Checks for JDBC format first
2. Falls back to postgres:// format
3. Handles URLs with/without explicit port
4. Returns null if parsing fails

### Error Handling
- Catches and differentiates:
  - `UnknownHostException` - DNS failure
  - `SocketTimeoutException` - Connection timeout
  - `ConnectException` - Connection refused
  - `NoRouteToHostException` - Network unreachable
  - `IOException` - General I/O errors

## Logging Examples

### Successful Validation
```
INFO  - Detected Supabase database URL, performing pre-startup validation
INFO  - === Starting Supabase Connection Validation ===
INFO  - Target hostname: db.example.supabase.co
INFO  - Performing DNS resolution for hostname: db.example.supabase.co
INFO  - ✓ DNS resolution successful: db.example.supabase.co → 192.0.2.1
INFO  - Testing TCP connectivity to db.example.supabase.co:6543 (timeout: 5000ms)
INFO  - ✓ TCP connection successful to db.example.supabase.co:6543
INFO  - Testing TCP connectivity to db.example.supabase.co:5432 (timeout: 5000ms)
INFO  - ✓ TCP connection successful to db.example.supabase.co:5432
INFO  - === Supabase Connection Validation Results ===
INFO  - Hostname: db.example.supabase.co
INFO  - Transaction Pooler (port 6543): ✓ AVAILABLE
INFO  - Direct Connection (port 5432): ✓ AVAILABLE
INFO  - Connection Mode: Both Pooler and Direct
INFO  - ✓ Both connection modes are available - optimal configuration
INFO  - === End of Supabase Connection Validation ===
```

### Pooler Unavailable Warning
```
INFO  - Testing TCP connectivity to db.example.supabase.co:6543 (timeout: 5000ms)
WARN  - ✗ Connection refused to db.example.supabase.co:6543 - no service listening on this port
INFO  - Testing TCP connectivity to db.example.supabase.co:5432 (timeout: 5000ms)
INFO  - ✓ TCP connection successful to db.example.supabase.co:5432
WARN  - ⚠ POOLER UNAVAILABLE - ACTION REQUIRED ⚠
WARN  - The transaction pooler (port 6543) is not accessible, but direct connection (port 5432) works.
[... detailed guidance logged ...]
```

## Testing

### Run Unit Tests
```bash
./mvn.ps1 test -Dtest=SupabaseConnectionValidatorTest
```

### Run Integration Tests
```bash
./mvn.ps1 test -Dtest=SupabaseConnectionValidationIntegrationTest
```

### Run All Tests
```bash
./mvn.ps1 test
```

## Production Recommendations

1. **Enable Connection Pooling** - Always enable transaction pooler in Supabase for production
2. **Monitor Logs** - Review startup logs to ensure both modes are available
3. **Alert on Warnings** - Set up alerts for pooler unavailability
4. **Network Configuration** - Ensure ports 5432 and 6543 are accessible
5. **Regular Testing** - Periodically verify both connection modes work

## Security Notes

- No credentials are logged (passwords masked in URLs)
- Only performs read-only network checks (DNS and TCP handshake)
- Does not authenticate or query the database during validation
- Fails fast to prevent unclear runtime errors

## Benefits

1. **Clear Error Messages** - No more cryptic connection pool errors
2. **Actionable Guidance** - Specific instructions for common issues
3. **Early Detection** - Catches connectivity issues before datasource init
4. **Production Best Practices** - Encourages use of transaction pooler
5. **Fail Fast** - Application won't start with unclear error state
6. **Network Diagnostics** - DNS and TCP checks provide troubleshooting info
7. **Profile Aware** - Doesn't interfere with local dev/test workflows
