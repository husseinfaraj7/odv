# DNS Resolution Error Handling - UnknownHostException Detection

## Overview
Modified `DatabaseConnectionRetryConfig` to specifically intercept and handle `UnknownHostException` errors, providing detailed diagnostic information to help users quickly identify and resolve DNS resolution failures due to invalid or malformed hostnames.

## Changes Made

### 1. Enhanced Exception Handling in BeanPostProcessor
- Added `containsUnknownHostException()` method to detect `UnknownHostException` in the exception chain
- Added early detection before pooler fallback logic to catch DNS failures immediately
- Added `extractHostnameFromUrl()` method to extract hostname from JDBC URL for diagnostic reporting
- Added `logDnsResolutionFailure()` method to log comprehensive error details

### 2. Detailed Error Logging for DNS Failures
When an `UnknownHostException` is detected, the system now logs:

- **Error Type Classification**: Clearly identifies this as a DNS resolution failure, not a network connectivity issue
- **Detected Hostname**: Extracted from the JDBC URL
- **Full DATABASE_URL**: Masked password for security
- **Contrast with Other Network Errors**:
  - `UnknownHostException`: Invalid/malformed hostname - DNS cannot resolve it
  - `ConnectException`: Valid hostname but cannot connect (port closed, firewall, service down)
  - `SocketTimeoutException`: Valid hostname but server not responding in time

- **Correct Supabase Hostname Formats**:
  - Direct Connection: `db.<project-ref>.supabase.co:5432`
  - Pooler Connection: `<project-ref>.pooler.supabase.com:6543`
  - Examples provided for both connection modes

- **Remediation Steps**:
  1. Verify DATABASE_URL contains valid hostname
  2. Check for common typos (TLD, missing prefixes, incorrect subdomains)
  3. Get correct connection string from Supabase dashboard
  4. Ensure pattern matches intended connection mode
  5. Update .env file and restart application

### 3. Enhanced RetryListener Logging
Modified `DatabaseConnectionRetryListener.onError()` to:
- Get root cause of exception using `getRootCause()` method
- Provide exception-specific logging:
  - `UnknownHostException`: Log as DNS resolution failure
  - `ConnectException`: Log as connection refused with valid hostname
  - `SocketTimeoutException`: Log as connection timeout with valid hostname
  - Other exceptions: Use existing diagnostic details

### 4. New Test Coverage
Created `UnknownHostExceptionDetectionTest.java` to verify:
- Detection of `UnknownHostException` in exception chains
- Differentiation from `ConnectException` and `SocketTimeoutException`
- Hostname extraction from various JDBC URL formats
- Root cause extraction from nested exception chains

## Benefits

1. **Clear Error Identification**: Users immediately know when they have a DNS/hostname issue vs. network connectivity issue
2. **Actionable Guidance**: Provides specific examples of correct hostname formats for Supabase connections
3. **Common Mistake Detection**: Highlights typical typos (TLD confusion, missing prefixes, etc.)
4. **Faster Resolution**: Reduces debugging time by pointing directly to the DATABASE_URL configuration
5. **Security**: Masks passwords in logged URLs while showing hostname details

## Example Error Output

```
================================================================================
DNS RESOLUTION FAILURE - INVALID OR MALFORMED HOSTNAME
================================================================================
Attempt: 1/3

FAILURE TYPE: UnknownHostException
This indicates the hostname in your DATABASE_URL cannot be resolved to an IP address.
This is a DNS resolution failure, NOT a network connectivity issue.

CONTRAST WITH OTHER NETWORK ERRORS:
  • UnknownHostException (THIS ERROR): Invalid/malformed hostname - DNS cannot resolve it
  • ConnectException: Valid hostname but cannot connect (port closed, firewall, service down)
  • SocketTimeoutException: Valid hostname but server not responding in time

DETECTED HOSTNAME: aws-0-us-east-1.pooler.supabase.co
FULL DATABASE_URL: jdbc:postgresql://aws-0-us-east-1.pooler.supabase.co:6543/postgres?user=****&password=****

CORRECT SUPABASE HOSTNAME FORMATS:
  • Direct Connection:
      Format: db.<project-ref>.supabase.co
      Port: 5432
      Example: jdbc:postgresql://db.aws-0-us-east-1.supabase.co:5432/postgres

  • Pooler Connection (Transaction Mode):
      Format: <project-ref>.pooler.supabase.com
      Port: 6543
      Example: jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:6543/postgres

REMEDIATION STEPS:
  1. Verify DATABASE_URL environment variable contains a valid hostname
  2. Check for typos in the hostname (common mistakes):
      • Incorrect TLD: .supabase.com vs .supabase.co
      • Missing 'db.' prefix for direct connections
      • Extra/missing 'pooler' subdomain
      • Incorrect project reference ID
  3. Get correct connection string from Supabase dashboard:
      https://app.supabase.com/project/[your-project]/settings/database
  4. Ensure DATABASE_URL pattern matches your connection mode:
      Direct: jdbc:postgresql://db.<ref>.supabase.co:5432/<db>?user=<user>&password=<pass>
      Pooler: jdbc:postgresql://<ref>.pooler.supabase.com:6543/<db>?user=<user>&password=<pass>
  5. Update .env file or environment variable and restart the application

NOTE: If hostname looks correct, verify:
  • You have internet connectivity
  • Your DNS resolver is working (test: nslookup aws-0-us-east-1.pooler.supabase.co)
  • No DNS blocking/filtering at network level
================================================================================
```

## Files Modified

1. **backend/src/main/java/it/odvsicilia/backend/config/DatabaseConnectionRetryConfig.java**
   - Added imports: `java.net.ConnectException`, `java.net.SocketTimeoutException`, `java.net.UnknownHostException`
   - Added `containsUnknownHostException()` method
   - Added `extractHostnameFromUrl()` method
   - Added `logDnsResolutionFailure()` method
   - Enhanced `onError()` method in `DatabaseConnectionRetryListener`
   - Added `getRootCause()` method

2. **backend/src/test/java/it/odvsicilia/backend/config/DatabaseConnectionRetryConfigTest.java**
   - Updated imports to include network exception classes

3. **backend/src/test/java/it/odvsicilia/backend/config/UnknownHostExceptionDetectionTest.java** (NEW)
   - Tests for exception detection logic
   - Tests for hostname extraction
   - Tests for root cause extraction

## Validation

To test this functionality:

1. Set an invalid DATABASE_URL with a malformed hostname:
   ```
   DATABASE_URL=jdbc:postgresql://invalid-host.supabase.co:5432/postgres?user=test&password=test
   ```

2. Start the application and observe the detailed DNS resolution failure message

3. Verify the error message includes:
   - Clear identification as DNS failure
   - Extracted hostname
   - Correct format examples
   - Remediation steps
