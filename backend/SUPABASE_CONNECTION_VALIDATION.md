# Supabase Connection Validation

## Overview

The `SupabaseConnectionValidator` is a pre-startup utility that validates database connectivity before the application initializes its connection pool. It performs comprehensive DNS resolution and TCP connectivity tests for both Supabase connection modes.

## Connection Modes

Supabase databases support two connection modes:

### 1. Transaction Pooler (Port 6543)
- **Recommended for production/serverless deployments**
- Provides connection pooling at the database level
- Better suited for applications with many short-lived connections
- Reduces connection overhead
- Must be explicitly enabled in Supabase dashboard

### 2. Direct Connection (Port 5432)
- **Standard PostgreSQL connection**
- Direct connection to the database server
- Always available by default
- May hit connection limits faster in high-traffic scenarios

## Features

### Pre-Startup Validation
- Runs **before** HikariCP datasource initialization
- Prevents application startup with unclear error messages
- Provides actionable diagnostic information

### DNS Resolution Check
- Verifies hostname can be resolved to an IP address
- Detects DNS configuration issues early
- Logs resolved IP addresses for debugging

### TCP Connectivity Tests
- Tests both port 6543 (pooler) and port 5432 (direct)
- Uses configurable timeout (default: 5 seconds)
- Provides detailed error messages for each connection attempt

### Intelligent Error Reporting
- Differentiates between connection timeout, refused, and DNS failures
- Provides specific guidance based on failure mode
- Includes actionable troubleshooting steps

## Validation Results

The validator returns a `SupabaseValidationResult` object indicating which connection modes are available: **POOLER**, **DIRECT**, **BOTH**, or **NEITHER**.

## Connection Mode Guidance

### Both Available (Optimal)
No action required - optimal configuration.

### Only Direct Available (Warning)
**Action Required:** Enable connection pooling in Supabase:
1. Open Supabase dashboard: https://app.supabase.com/
2. Select your project
3. Navigate to: **Settings → Database**
4. Scroll to **Connection Pooling** section
5. Enable **Transaction Mode** pooling
6. Note the pooler connection string (uses port 6543)
7. Update DATABASE_URL to use the pooler endpoint

### Neither Available (Fatal Error)
Application will not start. Check network connectivity, firewall rules, and Supabase project status.

## Configuration

Validation is **enabled by default** for non-dev/test profiles.

To disable:
```properties
supabase.connection.validation.enabled=false
```

## Testing

Run tests:
```bash
./mvn.ps1 test -Dtest=SupabaseConnectionValidatorTest
```
