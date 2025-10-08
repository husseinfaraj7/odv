# ODV Sicilia - Deployment Guide for Render.com

This guide provides step-by-step instructions for deploying the ODV Sicilia backend application to Render.com.

## Prerequisites

- A Render.com account
- A Git repository (GitHub, GitLab, or Bitbucket) with your code
- A PostgreSQL database (Supabase recommended)
- Brevo email service account
- Supabase account (for additional services)

## Project Structure

The project is now properly configured for deployment with:

- ✅ **Dockerfile** - Multi-stage build optimized for production
- ✅ **render.yaml** - Render.com deployment configuration
- ✅ **Maven Wrapper** - Consistent build environment
- ✅ **Application Properties** - Environment-specific configuration
- ✅ **Health Checks** - Application monitoring endpoints

## Deployment Steps

### 1. Prepare Your Repository

Ensure your code is pushed to a Git repository with the following structure:

```
odv/
├── Dockerfile
├── render.yaml
├── mvnw
├── mvnw.cmd
├── .mvn/
│   └── wrapper/
│       └── maven-wrapper.properties
└── backend/
    ├── pom.xml
    └── src/
        └── main/
            ├── java/
            └── resources/
                ├── application.properties
                └── application-prod.properties
```

### 2. Create Render.com Service

1. Log in to [Render.com](https://render.com)
2. Click "New +" → "Web Service"
3. Connect your Git repository
4. Render will automatically detect the `render.yaml` configuration

### 3. Configure Environment Variables

In your Render service dashboard, add the following environment variables:

#### Required Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection string | `postgresql://user:pass@host:5432/db` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `your_secure_password` |
| `BREVO_API_KEY` | Brevo email API key | `xkeys-xxxxxxxxxxxx` |
| `ADMIN_EMAIL` | Admin email address | `admin@odvsicilia.it` |
| `SUPABASE_ANON_KEY` | Supabase anonymous key | `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` |
| `SUPABASE_ROLE_KEY` | Supabase service role key | `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` |

#### Optional Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SPRING_PROFILES_ACTIVE` | Spring profile | `prod` |
| `PORT` | Application port | `8080` |
| `DDL_AUTO` | Database schema mode | `validate` |
| `H2_CONSOLE_ENABLED` | H2 console (dev only) | `false` |

### 4. Database Setup

#### Using Supabase (Recommended)

1. Create a new project at [Supabase](https://supabase.com)
2. Go to Settings → Database
3. Copy the connection string
4. Update the `DATABASE_URL` in Render with your Supabase credentials
5. Ensure your password is properly URL-encoded if it contains special characters

#### Database URL Encoding

If your database password contains special characters, encode them:

- `@` → `%40`
- `#` → `%23`
- `$` → `%24`
- `%` → `%25`
- `&` → `%26`
- `+` → `%2B`
- `=` → `%3D`
- `:` → `%3A`
- `/` → `%2F`
- `?` → `%3F`
- ` ` (space) → `%20`

Example:
```
Original: postgresql://user:P@ssw0rd#123@host:5432/db
Encoded:  postgresql://user:P%40ssw0rd%23123@host:5432/db
```

### 5. Email Service Setup (Brevo)

1. Sign up at [Brevo](https://www.brevo.com)
2. Go to SMTP & API → API Keys
3. Create a new API key
4. Add the key to your Render environment variables

### 6. Deploy

1. Click "Deploy" in your Render service
2. Monitor the build logs for any issues
3. Once deployed, your service will be available at `https://your-service-name.onrender.com`

## Health Checks

The application includes health check endpoints:

- **Health Check**: `GET /actuator/health`
- **Application Status**: Returns service status and database connectivity

## Monitoring

### Build Logs
Monitor the build process in Render dashboard for any compilation or dependency issues.

### Runtime Logs
Check application logs for runtime errors, database connection issues, or email service problems.

### Health Monitoring
Render automatically monitors the `/actuator/health` endpoint for service availability.

## Troubleshooting

### DATABASE_URL Formatting Guide

The application supports both standard PostgreSQL and JDBC URL formats. The `DatabaseUrlEnvironmentPostProcessor` automatically converts standard PostgreSQL URLs to JDBC format during startup, so either format can be used in environment variables.

#### Supported URL Formats

**Standard PostgreSQL Format (Recommended)**
```
postgres://username:password@hostname:port/database
postgresql://username:password@hostname:port/database
```

Examples:
```
postgres://myuser:mypass@localhost:5432/odvsicilia
postgresql://admin:SecurePass123@db.example.com:5432/production_db
```

**JDBC Format (Alternative)**
```
jdbc:postgresql://hostname:port/database?user=username&password=password
```

Examples:
```
jdbc:postgresql://localhost:5432/odvsicilia?user=myuser&password=mypass
jdbc:postgresql://db.example.com:5432/production_db?user=admin&password=SecurePass123
```

#### URL Encoding Special Characters

If your password contains special characters, they **must** be URL-encoded:

| Character | Encoded | Character | Encoded |
|-----------|---------|-----------|---------|
| `@` | `%40` | `&` | `%26` |
| `#` | `%23` | `+` | `%2B` |
| `$` | `%24` | `=` | `%3D` |
| `%` | `%25` | `:` | `%3A` |
| `/` | `%2F` | `?` | `%3F` |
| ` ` (space) | `%20` | `!` | `%21` |

**Example with Special Characters:**
```
Original password: P@ss#word$123
Standard format:   postgres://user:P%40ss%23word%24123@host:5432/db
JDBC format:       jdbc:postgresql://host:5432/db?user=user&password=P%40ss%23word%24123
```

#### Common Connection Error Symptoms

| Error Message | Likely Cause | Solution |
|---------------|--------------|----------|
| `Connection refused` | Database server not accessible | Check hostname, port, and firewall rules |
| `Authentication failed` | Wrong username or password | Verify credentials and URL encoding |
| `Database "xyz" does not exist` | Database name incorrect | Check database name in URL |
| `No suitable driver found` | JDBC URL malformed | Verify URL starts with `jdbc:postgresql://` |
| `Unterminated quoted string` | Special char not URL-encoded | Encode special characters in password |
| `Could not create connection to database` | Network timeout or DNS issue | Test DNS resolution and network connectivity |
| `SSL error` | SSL mode mismatch | Add `?sslmode=require` or `?sslmode=disable` |

#### Debugging Steps

**1. Verify DATABASE_URL Conversion**

Check application logs during startup for the conversion process:

```
INFO  - DatabaseUrlEnvironmentPostProcessor: Original DATABASE_URL: postgres://user:pass@host:5432/db
INFO  - DatabaseUrlEnvironmentPostProcessor: Converted to JDBC URL: jdbc:postgresql://host:5432/db?user=user&password=pass
```

If the conversion log doesn't appear, the URL is being used as-is (already in JDBC format or missing).

**2. Test Connection String Independently**

Use `psql` (PostgreSQL command-line tool) to test the connection:

```bash
# For standard format (parse it manually):
psql -h hostname -p port -U username -d database

# Example:
psql -h localhost -p 5432 -U myuser -d odvsicilia
```

Or use a Java/JDBC connection test tool:

```bash
java -cp postgresql-driver.jar TestConnection "jdbc:postgresql://host:5432/db?user=username&password=password"
```

**3. Enable Detailed Database Logging**

Add these environment variables temporarily to see detailed connection logs:

```
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_JDBC=DEBUG
LOGGING_LEVEL_COM_ZAXXER_HIKARI=DEBUG
```

Check logs for:
- Connection pool initialization messages
- Actual JDBC URL being used (password will be masked)
- Connection attempt details and error messages

**4. Validate URL Format**

Common mistakes to check:
- Missing `://` separator: `postgres:user:pass@host/db` ❌
- Wrong protocol: `http://` or `https://` ❌
- Unencoded special characters in password
- Missing port number (should be `5432` for PostgreSQL)
- Wrong database name or typo
- Extra spaces or line breaks in environment variable

**5. Check Environment Variable Loading**

Verify the variable is properly set in your deployment environment:

```bash
# On Render or similar platforms, check the dashboard
# Locally, you can test with:
echo $DATABASE_URL
# or in PowerShell:
echo $env:DATABASE_URL
```

**6. Test with Minimal Configuration**

Try connecting with a simple configuration first:

```
# Remove SSL and other parameters
postgres://user:password@hostname:5432/database

# If that works, gradually add parameters:
postgres://user:password@hostname:5432/database?sslmode=require
```

### Password Encoding in DATABASE_URL

**CRITICAL**: Always use **raw passwords** (not URL-encoded) in your DATABASE_URL. The application's `DatabaseUrlEnvironmentPostProcessor` automatically handles password encoding during URL conversion. Manually encoding passwords causes **double-encoding** which results in authentication failures.

#### ✅ Correct: Use Raw Password

```bash
# Raw password: MyP@ss#2024
# Use it directly in DATABASE_URL:
DATABASE_URL=postgres://postgres.YOUR_PROJECT_REF:MyP@ss#2024@aws-0-REGION.pooler.supabase.com:6543/postgres
```

**What happens internally:**
1. `DatabaseUrlEnvironmentPostProcessor` extracts password: `MyP@ss#2024`
2. Automatically URL-encodes it: `MyP%40ss%232024`
3. Constructs JDBC URL: `jdbc:postgresql://aws-0-REGION.pooler.supabase.com:6543/postgres?user=postgres.YOUR_PROJECT_REF&password=MyP%40ss%232024`
4. ✅ Authentication succeeds

#### ❌ Wrong: Manually URL-Encoded Password

```bash
# If you manually encode the password:
DATABASE_URL=postgres://postgres.YOUR_PROJECT_REF:MyP%40ss%232024@aws-0-REGION.pooler.supabase.com:6543/postgres
```

**What happens internally:**
1. `DatabaseUrlEnvironmentPostProcessor` extracts password: `MyP%40ss%232024` (already encoded)
2. Encodes it again: `MyP%2540ss%25232024` (double-encoded!)
3. Constructs JDBC URL with double-encoded password
4. ❌ Authentication fails with error: `password authentication failed for user "postgres.YOUR_PROJECT_REF"`

#### Example Scenarios

**Scenario 1: Password with Special Characters**
```bash
# Password: Test@123#DB$Pass
# ✅ Correct usage:
DATABASE_URL=postgres://postgres.YOUR_PROJECT_REF:Test@123#DB$Pass@aws-0-REGION.pooler.supabase.com:6543/postgres

# ❌ Wrong (manual encoding causes double-encoding):
DATABASE_URL=postgres://postgres.YOUR_PROJECT_REF:Test%40123%23DB%24Pass@aws-0-REGION.pooler.supabase.com:6543/postgres
```

**Scenario 2: Connection Error Messages**

```
# Double-encoded password error (from manual encoding):
ERROR: password authentication failed for user "postgres.YOUR_PROJECT_REF"
FATAL: password authentication failed for user "postgres.YOUR_PROJECT_REF"
Caused by: org.postgresql.util.PSQLException: FATAL: password authentication failed

# Log shows double-encoded password in JDBC URL:
jdbc:postgresql://aws-0-REGION.pooler.supabase.com:6543/postgres?user=postgres.YOUR_PROJECT_REF&password=MyP%2540ss%25232024
                                                                                                                      ^^^^^ double-encoded
```

```
# Success with raw password:
INFO  - HikariPool-1 - Start completed.
INFO  - Started BackendApplication in 4.235 seconds
# Connection established successfully
```

#### Verification Steps for Supabase Connectivity

Before deploying, verify connectivity to your Supabase database:

**Step 1: Extract Connection Details from Supabase Dashboard**

1. Go to Supabase Dashboard → Settings → Database
2. Find **Connection Pooling** section
3. Note the connection details:
   - Host: `aws-0-REGION.pooler.supabase.com` (eu-north-1 region)
   - Port: `6543` (transaction pooler)
   - Username: `postgres.YOUR_PROJECT_REF` (includes project reference)
   - Password: Your raw database password

**Step 2: Test Connectivity with psql**

```bash
# Use raw password directly (psql handles encoding)
psql "postgres://postgres.YOUR_PROJECT_REF:YOUR_RAW_PASSWORD@aws-0-REGION.pooler.supabase.com:6543/postgres"

# Alternative format with connection parameters
psql -h aws-0-REGION.pooler.supabase.com -p 6543 -U postgres.YOUR_PROJECT_REF -d postgres

# Test with SSL requirement
psql "postgres://postgres.YOUR_PROJECT_REF:YOUR_RAW_PASSWORD@aws-0-REGION.pooler.supabase.com:6543/postgres?sslmode=require"
```

**Expected output (success):**
```
psql (14.x, server 15.x)
SSL connection (protocol: TLSv1.3, cipher: TLS_AES_256_GCM_SHA384, bits: 256)
Type "help" for help.

postgres=>
```

**Step 3: Test Network Connectivity with telnet**

```bash
# Test if pooler port is reachable
telnet aws-0-REGION.pooler.supabase.com 6543

# Expected output (success):
# Trying 13.48.XXX.XXX...
# Connected to aws-0-REGION.pooler.supabase.com.
# Escape character is '^]'.
```

**Step 4: Test with nc (netcat)**

```bash
# Test TCP connection with timeout
nc -zv -w5 aws-0-REGION.pooler.supabase.com 6543

# Expected output (success):
# Connection to aws-0-REGION.pooler.supabase.com 6543 port [tcp/*] succeeded!
```

#### Troubleshooting Authentication Errors

| Error Symptom | Likely Cause | Solution |
|---------------|--------------|----------|
| `password authentication failed` after encoding password | Double-encoding | Use raw password in DATABASE_URL |
| `password authentication failed` with special chars | Missing encoding (legacy systems) | Verify DatabaseUrlEnvironmentPostProcessor is active; check logs for conversion |
| Authentication works locally but fails on Render | Environment variable whitespace/newlines | Check for extra spaces, use Render dashboard to re-enter DATABASE_URL |
| Connection succeeds with psql but fails in app | JDBC URL construction issue | Check application logs for `DatabaseUrlEnvironmentPostProcessor` output |

#### How to Verify Automatic Encoding is Working

Check your application startup logs for these messages:

```
INFO  - DatabaseUrlEnvironmentPostProcessor: Original DATABASE_URL detected: postgres://...
INFO  - DatabaseUrlEnvironmentPostProcessor: Converted to JDBC URL with encoded credentials
```

If you don't see these messages, verify:
1. `DatabaseUrlEnvironmentPostProcessor` class exists in your project
2. It's registered in `META-INF/spring.factories` or as a Spring component
3. Environment variable is named exactly `DATABASE_URL` (case-sensitive)

### Common Issues

#### 1. Build Failures
- **Maven Dependencies**: Ensure all dependencies in `pom.xml` are available
- **Java Version**: Verify Java 17 compatibility
- **Memory Issues**: Check if build requires more memory

#### 2. Database Connection Issues
- **Password Encoding**: Use raw passwords only - manual encoding causes double-encoding failures (see Password Encoding section)
- **Network Access**: Verify database allows connections from Render IPs
- **Credentials**: Double-check username format (e.g., `postgres.YOUR_PROJECT_REF` for Supabase)
- **URL Format**: Ensure using correct format (see DATABASE_URL Formatting Guide above)
- **Port Number**: Use 6543 for Supabase pooler or 5432 for direct connection

#### 3. HikariCP Property Override Issue: "Driver claims to not accept jdbcUrl, postgresql://"

**Problem**: When using `spring.datasource.hikari.jdbc-url` in your application properties, HikariCP bypasses Spring Boot's normal property binding mechanism and ignores the converted JDBC URL from `DatabaseUrlEnvironmentPostProcessor`. Instead, it uses the raw `DATABASE_URL` environment variable directly, which results in the error:

```
java.sql.SQLException: Driver claims to not accept jdbcUrl, postgresql://hostname:port/database
```

**Root Cause**:

Spring Boot's property binding follows a hierarchical resolution order. When `spring.datasource.hikari.jdbc-url` is explicitly set, it takes precedence over `spring.datasource.url`, causing HikariCP to use the unconverted DATABASE_URL value (in `postgresql://` format) instead of the properly converted JDBC URL (in `jdbc:postgresql://` format).

Here's what happens:

1. **With hikari.jdbc-url property defined**:
   ```
   DATABASE_URL=postgresql://user:pass@host:5432/db
   spring.datasource.url=${DATABASE_URL}              # Converted by DatabaseUrlEnvironmentPostProcessor
   spring.datasource.hikari.jdbc-url=${DATABASE_URL}  # NOT converted - used directly by HikariCP
   
   Result: HikariCP uses postgresql://... ❌ ERROR
   ```

2. **Without hikari.jdbc-url property** (correct):
   ```
   DATABASE_URL=postgresql://user:pass@host:5432/db
   spring.datasource.url=${DATABASE_URL}              # Converted by DatabaseUrlEnvironmentPostProcessor
   
   Result: HikariCP uses jdbc:postgresql://... ✅ SUCCESS
   ```

**Solution**: Remove or comment out the `spring.datasource.hikari.jdbc-url` property from your `application.properties` or `application-prod.properties` file:

```properties
# ❌ Remove or comment this line:
# spring.datasource.hikari.jdbc-url=${DATABASE_URL}

# ✅ Keep only this (HikariCP will automatically use spring.datasource.url):
spring.datasource.url=${DATABASE_URL}
```

**Why This Works**:

When `spring.datasource.hikari.jdbc-url` is not explicitly set, HikariCP falls back to using `spring.datasource.url`, which has been properly converted by the `DatabaseUrlEnvironmentPostProcessor` from `postgresql://` to `jdbc:postgresql://` format.

**Verification Steps**:

1. **Check your application properties** for any hikari-specific URL configuration:
   ```bash
   # Search for hikari.jdbc-url in your properties files
   grep -r "hikari.jdbc-url" src/main/resources/
   ```

2. **Enable HikariCP debug logging** to see what URL is being used:
   ```properties
   # Add to application.properties temporarily
   logging.level.com.zaxxer.hikari=DEBUG
   logging.level.com.zaxxer.hikari.HikariConfig=DEBUG
   ```

3. **Check startup logs** for HikariCP initialization messages:
   ```
   # ✅ Correct format (should see):
   DEBUG HikariConfig - jdbcUrl.........................jdbc:postgresql://aws-1-eu-north-1.pooler.supabase.com:6543/postgres
   INFO  HikariDataSource - HikariPool-1 - Starting...
   INFO  HikariPool - HikariPool-1 - Start completed.
   
   # ❌ Incorrect format (if you see this, hikari.jdbc-url is still set):
   DEBUG HikariConfig - jdbcUrl.........................postgresql://aws-1-eu-north-1.pooler.supabase.com:6543/postgres
   ERROR HikariPool - Exception during pool initialization
   java.sql.SQLException: Driver claims to not accept jdbcUrl, postgresql://aws-1-eu-north-1.pooler.supabase.com:6543/postgres
   ```

4. **Search for "HikariConfig" in logs** during application startup:
   ```bash
   # If deploying to Render, check logs for:
   grep -i "HikariConfig" render-logs.txt
   grep -i "jdbcUrl" render-logs.txt
   ```

**Example Log Comparison**:

**Incorrect Configuration** (with `hikari.jdbc-url`):
```
2024-01-15 10:23:45.123 INFO  - DatabaseUrlEnvironmentPostProcessor: Original DATABASE_URL: postgresql://user:pass@host:5432/db
2024-01-15 10:23:45.124 INFO  - DatabaseUrlEnvironmentPostProcessor: Converted to JDBC URL: jdbc:postgresql://host:5432/db?user=user&password=pass
2024-01-15 10:23:45.456 DEBUG - HikariConfig - jdbcUrl.........................postgresql://user:pass@host:5432/db
2024-01-15 10:23:45.789 ERROR - HikariPool-1 - Exception during pool initialization
java.sql.SQLException: Driver claims to not accept jdbcUrl, postgresql://user:pass@host:5432/db
```
*Note: The conversion happens, but HikariCP ignores it because `hikari.jdbc-url` bypasses `spring.datasource.url`*

**Correct Configuration** (without `hikari.jdbc-url`):
```
2024-01-15 10:23:45.123 INFO  - DatabaseUrlEnvironmentPostProcessor: Original DATABASE_URL: postgresql://user:pass@host:5432/db
2024-01-15 10:23:45.124 INFO  - DatabaseUrlEnvironmentPostProcessor: Converted to JDBC URL: jdbc:postgresql://host:5432/db?user=user&password=pass
2024-01-15 10:23:45.456 DEBUG - HikariConfig - jdbcUrl.........................jdbc:postgresql://host:5432/db?user=user&password=pass
2024-01-15 10:23:45.789 INFO  - HikariPool-1 - Starting...
2024-01-15 10:23:46.012 INFO  - HikariPool-1 - Start completed.
```
*Note: HikariCP now correctly uses the converted JDBC URL from `spring.datasource.url`*

**Key Points**:
- The `hikari.jdbc-url` property has **higher precedence** than `spring.datasource.url` in Spring Boot's property binding
- `DatabaseUrlEnvironmentPostProcessor` only converts `spring.datasource.url`, not individual HikariCP properties
- Always use `spring.datasource.url` and let HikariCP inherit it automatically
- Never set `spring.datasource.hikari.jdbc-url` when using the `DATABASE_URL` environment variable with non-JDBC format

#### 4. Email Service Issues
- **API Key**: Verify Brevo API key is correct and active
- **Rate Limits**: Check if you've exceeded email service limits
- **SMTP Configuration**: Ensure SMTP settings match Brevo requirements

#### 5. Application Startup Issues
- **Environment Variables**: Verify all required variables are set
- **Database Schema**: Ensure database tables are created (DDL_AUTO=update)
- **Port Configuration**: Check if PORT environment variable is set correctly

### Debug Steps

1. **Check Build Logs**: Look for compilation errors or missing dependencies
2. **Verify Environment Variables**: Ensure all required variables are set
3. **Test Database Connection**: Verify DATABASE_URL is accessible (see DATABASE_URL debugging steps)
4. **Check Application Logs**: Look for runtime errors or configuration issues
5. **Validate Health Endpoint**: Test `/actuator/health` endpoint

## Network Connectivity Troubleshooting

This section provides diagnostic steps for troubleshooting network connectivity issues between Render and Supabase, particularly when encountering "network unreachable" errors or connection timeouts referenced in earlier PostgreSQL connection issues.

### Understanding Supabase Connection Modes

Supabase provides two types of database connections:

#### 1. Direct Connection (Port 5432)
- **Format**: `postgres://postgres.PROJECT_REF:PASSWORD@db.PROJECT_REF.supabase.co:5432/postgres`
- **Use Case**: Direct database access, long-running queries, administrative tasks
- **Pros**: Full PostgreSQL feature support, dedicated connection
- **Cons**: Limited connection pool (based on your plan), may exhaust connections under load
- **Connection Limit**: Typically 60-500 connections depending on plan

#### 2. Connection Pooler (Port 6543)
- **Format**: `postgres://postgres.PROJECT_REF:PASSWORD@aws-0-REGION.pooler.supabase.com:6543/postgres`
- **Use Case**: Production applications, high concurrency, serverless deployments
- **Pros**: Scalable connection pooling, handles thousands of connections
- **Cons**: Transaction mode only (some PostgreSQL features unavailable)
- **Connection Limit**: Virtually unlimited through pooling

**Recommendation**: For Render deployments, use the **Connection Pooler (port 6543)** for better scalability and reliability.

### Verifying Supabase Network Configuration

#### Step 1: Check Connection Pooling Settings

1. Log in to [Supabase Dashboard](https://app.supabase.com)
2. Navigate to **Project Settings** → **Database**
3. Scroll to **Connection Pooling** section
4. Verify settings:
   - ✅ **Connection pooling enabled**: Should be ON
   - ✅ **Pool Mode**: Should be set to "Transaction"
   - ✅ **Connection string**: Copy the pooler URL (port 6543)

#### Step 2: Verify External Connection Settings

Supabase allows external connections by default, but verify:

1. In Supabase Dashboard: **Project Settings** → **Database**
2. Under **Connection Info**, check:
   - ✅ **Host**: Should be `aws-0-REGION.pooler.supabase.com` (pooler) or `db.PROJECT_REF.supabase.co` (direct)
   - ✅ **Port**: `6543` (pooler) or `5432` (direct)
   - ✅ **SSL Mode**: Should be enabled/required

3. Check for any network restrictions:
   - Navigate to **Settings** → **Network Restrictions** (if available)
   - Verify no IP allowlist is configured, OR
   - Add Render's outbound IP ranges to allowlist (see below)

#### Step 3: Configure IP Allowlisting (If Required)

Some Supabase plans support IP allowlisting. If enabled, you must whitelist Render's IPs:

**To find Render's Outbound IPs:**
```bash
# From Render shell (after deploying):
curl -s https://api.ipify.org && echo
# Or check Render documentation for static IP ranges
```

**Note**: Render does not provide static IP addresses on free/starter plans. If Supabase requires IP allowlisting, consider:
- Upgrading to Render plans with static IPs
- Disabling IP allowlisting in Supabase (if supported by your plan)
- Using Supabase with open external access (recommended for most use cases)

### Testing Network Connectivity from Render

Once your service is deployed to Render, you can test network connectivity directly from the Render shell.

#### Step 1: Access Render Shell

1. Go to Render Dashboard → Your Service
2. Click **Shell** tab in the left sidebar
3. Wait for shell session to initialize

#### Step 2: Test DNS Resolution

Verify that Supabase hostnames resolve correctly:

```bash
# Test pooler hostname resolution
nslookup aws-0-us-west-1.pooler.supabase.com

# Test direct connection hostname resolution
nslookup db.YOUR_PROJECT_REF.supabase.co

# Alternative DNS test
host aws-0-us-west-1.pooler.supabase.com
```

**Expected Output**:
```
Server:  10.0.0.2
Address: 10.0.0.2#53

Non-authoritative answer:
Name:    aws-0-us-west-1.pooler.supabase.com
Address: 54.183.XXX.XXX
```

**Troubleshooting DNS Issues**:
- ❌ **"can't find ... NXDOMAIN"**: Hostname doesn't exist, check for typos in DATABASE_URL
- ❌ **Timeout**: DNS server unreachable, contact Render support
- ✅ **Returns IP address**: DNS resolution working correctly

#### Step 3: Test TCP Connectivity to Database Port

Test if you can reach the Supabase database port from Render:

**Using telnet (Port 6543 - Pooler)**:
```bash
# Test connection pooler endpoint
telnet aws-0-us-west-1.pooler.supabase.com 6543

# Alternative example with different region
telnet aws-0-eu-west-1.pooler.supabase.com 6543
```

**Using nc/netcat (Port 6543 - Pooler)**:
```bash
# Test connection pooler with timeout
nc -zv -w5 aws-0-us-west-1.pooler.supabase.com 6543

# Test with explicit timeout and verbose output
timeout 5 nc -vz aws-0-us-west-1.pooler.supabase.com 6543
```

**Using telnet (Port 5432 - Direct Connection)**:
```bash
# Test direct database connection
telnet db.YOUR_PROJECT_REF.supabase.co 5432

# Another example with different project
telnet db.abcdefghijklmnop.supabase.co 5432
```

**Using nc/netcat (Port 5432 - Direct Connection)**:
```bash
# Test direct connection
nc -zv -w5 db.YOUR_PROJECT_REF.supabase.co 5432
```

**Expected Output (Success)**:
```
# telnet success:
Connected to aws-0-us-west-1.pooler.supabase.com.
Escape character is '^]'.

# nc success:
Connection to aws-0-us-west-1.pooler.supabase.com 6543 port [tcp/*] succeeded!
```

**Troubleshooting Connectivity Issues**:

| Error Message | Meaning | Solution |
|---------------|---------|----------|
| `Connection refused` | Port is closed or service not running | Verify port number (6543 vs 5432), check Supabase status page |
| `Connection timed out` / `Network is unreachable` | Firewall blocking or network issue | Check Supabase IP allowlist, verify pooler is enabled |
| `No route to host` | DNS resolved but routing failed | Contact Render support, check Supabase status |
| `Name or service not known` | DNS resolution failed | Check hostname for typos, verify it matches Supabase dashboard |

#### Step 4: Test Full PostgreSQL Connection

If TCP connectivity works, test the actual PostgreSQL connection:

```bash
# Install PostgreSQL client (if not available)
apt-get update && apt-get install -y postgresql-client

# Test connection with psql (pooler)
psql "postgres://postgres.YOUR_PROJECT_REF:YOUR_PASSWORD@aws-0-us-west-1.pooler.supabase.com:6543/postgres"

# Test connection with psql (direct)
psql "postgres://postgres.YOUR_PROJECT_REF:YOUR_PASSWORD@db.YOUR_PROJECT_REF.supabase.co:5432/postgres"

# Test with explicit SSL mode
psql "postgres://postgres.YOUR_PROJECT_REF:YOUR_PASSWORD@aws-0-us-west-1.pooler.supabase.com:6543/postgres?sslmode=require"
```

**Expected Output (Success)**:
```
psql (14.x, server 15.x)
SSL connection (protocol: TLSv1.3, cipher: TLS_AES_256_GCM_SHA384, bits: 256)
Type "help" for help.

postgres=>
```

**Troubleshooting psql Errors**:
- `password authentication failed`: Wrong password or username format
- `database "postgres" does not exist`: Incorrect database name in URL
- `could not connect to server`: Network/firewall issue (revisit TCP connectivity tests)
- `SSL error`: Add `?sslmode=require` or `?sslmode=disable` to connection string

### Diagnostic Workflow for "Network Unreachable" Errors

When encountering persistent network errors, follow this step-by-step diagnostic workflow:

#### Phase 1: Verify Configuration
1. ✅ Check DATABASE_URL format in Render environment variables
2. ✅ Verify special characters are URL-encoded (see DATABASE_URL Formatting Guide)
3. ✅ Confirm you're using the correct hostname from Supabase dashboard
4. ✅ Verify port number: 6543 (pooler) or 5432 (direct)
5. ✅ Ensure environment variables are saved and service redeployed

#### Phase 2: Test from Render Shell
1. ✅ Access Render shell and test DNS resolution (Step 2 above)
2. ✅ Test TCP connectivity with `telnet` or `nc` (Step 3 above)
3. ✅ If available, test with `psql` client (Step 4 above)

#### Phase 3: Verify Supabase Settings
1. ✅ Check connection pooling is enabled (Supabase Dashboard)
2. ✅ Verify no IP restrictions are blocking Render's IPs
3. ✅ Check Supabase status page for outages: https://status.supabase.com
4. ✅ Try both pooler (6543) and direct (5432) connection modes

#### Phase 4: Check Application Logs
1. ✅ Review Render logs for `DatabaseUrlEnvironmentPostProcessor` conversion
2. ✅ Look for HikariCP connection pool errors
3. ✅ Check for timeout vs. authentication vs. DNS errors
4. ✅ Enable debug logging if needed:
   ```
   LOGGING_LEVEL_COM_ZAXXER_HIKARI=DEBUG
   LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_JDBC=DEBUG
   ```

#### Phase 5: Escalation
If all steps above pass but connection still fails:
1. Contact Render support with connectivity test results
2. Contact Supabase support with connection details
3. Check for regional issues or outages
4. Consider trying a different Supabase region

### Example Troubleshooting Session

Here's a complete example of troubleshooting a connection issue:

```bash
# 1. Verify DNS resolution
$ nslookup aws-0-us-west-1.pooler.supabase.com
Server:  10.0.0.2
Address: 10.0.0.2#53

Non-authoritative answer:
Name:    aws-0-us-west-1.pooler.supabase.com
Address: 54.183.XXX.XXX
# ✅ DNS works

# 2. Test TCP connectivity to pooler port
$ nc -zv -w5 aws-0-us-west-1.pooler.supabase.com 6543
Connection to aws-0-us-west-1.pooler.supabase.com 6543 port [tcp/*] succeeded!
# ✅ Network connectivity works

# 3. Test PostgreSQL connection
$ psql "postgres://postgres.myproject:MyPassword123@aws-0-us-west-1.pooler.supabase.com:6543/postgres"
psql (14.10, server 15.1)
SSL connection (protocol: TLSv1.3, cipher: TLS_AES_256_GCM_SHA384)
Type "help" for help.

postgres=> SELECT version();
                                                version
-------------------------------------------------------------------------------------------------------
 PostgreSQL 15.1 on x86_64-pc-linux-gnu, compiled by gcc, 64-bit
(1 row)

postgres=> \q
# ✅ Database connection works

# Conclusion: Network connectivity is healthy
# If application still fails, check:
# - Environment variable format in Render
# - Password URL encoding in DATABASE_URL
# - Application logs for specific error messages
```

### Quick Reference: Common Connection String Formats

```bash
# Connection Pooler (Recommended for Production)
postgres://postgres.PROJECT_REF:PASSWORD@aws-0-REGION.pooler.supabase.com:6543/postgres

# Direct Connection (For Admin/Long Queries)
postgres://postgres.PROJECT_REF:PASSWORD@db.PROJECT_REF.supabase.co:5432/postgres

# With URL-Encoded Password (Special Characters)
postgres://postgres.myproject:MyP%40ss%23word@aws-0-us-west-1.pooler.supabase.com:6543/postgres

# With Explicit SSL Mode
postgres://postgres.myproject:MyPassword@aws-0-us-west-1.pooler.supabase.com:6543/postgres?sslmode=require

# JDBC Format (Auto-Converted by Application)
jdbc:postgresql://aws-0-us-west-1.pooler.supabase.com:6543/postgres?user=postgres.myproject&password=MyPassword
```

### Additional Resources

- **Supabase Network Documentation**: https://supabase.com/docs/guides/platform/network-restrictions
- **Supabase Connection Pooling**: https://supabase.com/docs/guides/database/connecting-to-postgres#connection-pooler
- **Render Network Information**: https://render.com/docs/networking
- **PostgreSQL Connection Strings**: https://www.postgresql.org/docs/current/libpq-connect.html#LIBPQ-CONNSTRING

## Supabase Transaction Pooler Configuration

This section provides detailed guidance on configuring Supabase's connection pooler for optimal performance and reliability, including understanding pooling modes, regional URL formats, and troubleshooting pooler-specific connection issues.

### Understanding Pooling Modes

Supabase offers two distinct connection pooling modes, each optimized for different use cases:

#### Transaction Mode (Port 6543) - Recommended for Production

**URL Format:**
```
postgres://postgres.PROJECT_REF:PASSWORD@aws-1-REGION.pooler.supabase.com:6543/postgres
```

**Characteristics:**
- **Port**: 6543
- **Connection Pool Size**: Small application-side pools recommended (maximum-pool-size=10)
- **How It Works**: Connections are pooled at the transaction level. A server connection is assigned to a client only for the duration of a transaction. After the transaction completes, the connection returns to the pool.
- **Concurrency**: Handles thousands of concurrent connections efficiently
- **Use Cases**: 
  - Production web applications
  - Serverless deployments (e.g., AWS Lambda, Vercel)
  - High-concurrency scenarios
  - Applications with short-lived transactions

**Limitations:**
- Session-level PostgreSQL features unavailable:
  - Prepared statements don't persist across transactions
  - Temporary tables are cleared after each transaction
  - `SET` commands (session variables) don't persist
  - `LISTEN/NOTIFY` not supported

**Connection Pool Configuration:**
```properties
# application-prod.properties (Transaction Mode)
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=300000
```

**When to Use:**
- ✅ Standard REST APIs with stateless operations
- ✅ CRUD operations with automatic transaction management
- ✅ Applications running on platforms with connection limits (Render free tier, Heroku, etc.)
- ✅ Microservices with many instances
- ✅ Serverless functions

#### Session Mode (Port 5432) - Direct Connection

**URL Format:**
```
postgres://postgres.PROJECT_REF:PASSWORD@db.PROJECT_REF.supabase.co:5432/postgres
```

**Characteristics:**
- **Port**: 5432 (standard PostgreSQL port)
- **Connection Pool Size**: Larger application-side pools supported (maximum-pool-size=20-50)
- **How It Works**: Each client connection maps to a dedicated PostgreSQL connection for the entire session lifetime.
- **Concurrency**: Limited by database plan (typically 60-500 connections)
- **Use Cases**:
  - Administrative operations
  - Long-running queries or reports
  - Applications requiring session-level features
  - Database migrations

**Full Feature Support:**
- ✅ Prepared statements persist for the session
- ✅ Temporary tables available
- ✅ Session variables (`SET` commands) work
- ✅ `LISTEN/NOTIFY` supported
- ✅ Advisory locks available

**Connection Pool Configuration:**
```properties
# application-prod.properties (Session Mode)
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=20000
spring.datasource.hikari.idle-timeout=600000
```

**When to Use:**
- ✅ Background jobs requiring long-running transactions
- ✅ Database migration scripts
- ✅ Administrative tasks
- ✅ Applications needing PostgreSQL-specific session features
- ❌ **NOT recommended** for high-concurrency web applications (risk of exhausting connection pool)

### Regional URL Patterns

Supabase pooler URLs follow a predictable pattern based on the region where your project is hosted:

#### Standard Pooler URL Format
```
postgres://postgres.PROJECT_REF:PASSWORD@aws-1-REGION.pooler.supabase.com:6543/postgres
```

#### Regional Examples

**Europe (North) - Stockholm:**
```
postgres://postgres.myproject:password@aws-0-REGION.pooler.supabase.com:6543/postgres
```

**Europe (West) - Ireland:**
```
postgres://postgres.myproject:password@aws-1-eu-west-1.pooler.supabase.com:6543/postgres
```

**Europe (Central) - Frankfurt:**
```
postgres://postgres.myproject:password@aws-1-eu-central-1.pooler.supabase.com:6543/postgres
```

**US East (North Virginia):**
```
postgres://postgres.myproject:password@aws-1-us-east-1.pooler.supabase.com:6543/postgres
```

**US West (Oregon):**
```
postgres://postgres.myproject:password@aws-1-us-west-2.pooler.supabase.com:6543/postgres
```

**Asia Pacific (Singapore):**
```
postgres://postgres.myproject:password@aws-1-ap-southeast-1.pooler.supabase.com:6543/postgres
```

**Asia Pacific (Sydney):**
```
postgres://postgres.myproject:password@aws-1-ap-southeast-2.pooler.supabase.com:6543/postgres
```

#### How to Find Your Region

1. Log in to [Supabase Dashboard](https://app.supabase.com)
2. Navigate to **Project Settings** → **General**
3. Look for **Region** field (e.g., "Europe (North) - eu-north-1")
4. Construct pooler URL: `aws-1-{region-code}.pooler.supabase.com`

**Note**: Some older Supabase projects may use `aws-0-{region}` instead of `aws-1-{region}`. Check your project's database settings for the exact URL.

### Troubleshooting Pooler Connection Failures

Connection issues with Supabase pooler can stem from various causes. Follow these diagnostic steps:

#### Step 1: Verify the Correct Port

**Problem**: Using wrong port for the intended connection mode

**Symptoms:**
```
Connection refused on port 6543
PSQLException: Connection to localhost:6543 refused
org.postgresql.util.PSQLException: The connection attempt failed.
```

**Solution:**
```bash
# Verify which port your DATABASE_URL uses
echo $DATABASE_URL

# Transaction mode MUST use port 6543:
postgres://user:pass@aws-0-REGION.pooler.supabase.com:6543/postgres  # ✅ Correct

# Session mode (direct connection) uses port 5432:
postgres://user:pass@db.projectref.supabase.co:5432/postgres  # ✅ Correct

# Common mistakes:
postgres://user:pass@aws-0-REGION.pooler.supabase.com:5432/postgres  # ❌ Wrong port for pooler
postgres://user:pass@db.projectref.supabase.co:6543/postgres             # ❌ Wrong port for direct
```

**Quick Test:**
```bash
# Test pooler connectivity (port 6543)
nc -zv -w5 aws-0-REGION.pooler.supabase.com 6543

# Test direct connectivity (port 5432)
nc -zv -w5 db.yourproject.supabase.co 5432
```

#### Step 2: Check SSL Requirements

**Problem**: Missing or incorrect SSL configuration

**Symptoms:**
```
FATAL: no pg_hba.conf entry for host
SSL error: certificate verify failed
javax.net.ssl.SSLHandshakeException
```

**Solution:**

Supabase **requires SSL** for all connections. Ensure your DATABASE_URL includes SSL mode:

```bash
# ✅ Correct - SSL enabled
postgres://user:pass@aws-0-REGION.pooler.supabase.com:6543/postgres?sslmode=require

# ❌ Incorrect - SSL disabled (will fail)
postgres://user:pass@aws-0-REGION.pooler.supabase.com:6543/postgres?sslmode=disable

# ⚠️ May work but insecure
postgres://user:pass@aws-0-REGION.pooler.supabase.com:6543/postgres
```

**For Spring Boot applications**, add SSL parameters to `application-prod.properties`:

```properties
# Method 1: Add sslmode to DATABASE_URL
# DATABASE_URL=postgres://user:pass@host:6543/db?sslmode=require

# Method 2: Configure via HikariCP data source properties
spring.datasource.hikari.data-source-properties.ssl=true
spring.datasource.hikari.data-source-properties.sslmode=require

# Method 3: Add to JDBC URL parameters (if using JDBC format)
# jdbc:postgresql://host:6543/db?sslmode=require&ssl=true
```

**Test SSL connection:**
```bash
# Install PostgreSQL client if needed
apt-get update && apt-get install -y postgresql-client

# Test connection with SSL
psql "postgres://user:pass@aws-0-REGION.pooler.supabase.com:6543/postgres?sslmode=require"

# Check SSL status after connecting
postgres=> \conninfo
You are connected to database "postgres" as user "postgres.myproject" on host "aws-0-REGION.pooler.supabase.com" (address "x.x.x.x") at port "6543".
SSL connection (protocol: TLSv1.3, cipher: TLS_AES_256_GCM_SHA384, bits: 256, compression: off)
```

#### Step 3: Identify Pool Exhaustion in Transaction Mode

**Problem**: Application runs out of pooler connections

**Symptoms:**
```
HikariPool-1 - Connection is not available, request timed out after 20000ms
FATAL: remaining connection slots are reserved
PSQLException: FATAL: sorry, too many clients already
Timeout after 20000ms of waiting for a connection
```

**Understanding Pool Exhaustion:**

In **transaction mode**, if your application:
- Opens connections but doesn't close them promptly
- Has long-running transactions that hold connections
- Has a pool size too large relative to available pooler capacity
- Has connection leaks in application code

The pooler's ability to multiplex connections can be overwhelmed.

**Diagnostic Steps:**

**1. Check Current Connection Pool Settings:**

```bash
# In Render logs, look for HikariCP initialization:
HikariPool-1 - configuration:
HikariPool-1 - maximumPoolSize......................20  # ⚠️ Too high for transaction mode
HikariPool-1 - minimumIdle..........................5
```

**2. Monitor Connection Pool Metrics:**

Add these environment variables temporarily:
```
LOGGING_LEVEL_COM_ZAXXER_HIKARI=DEBUG
```

Look for these log patterns:
```
# Connection acquisition taking too long:
HikariPool-1 - Timeout failure stats (total=20, active=20, idle=0, waiting=5)

# Frequent connection recycling (normal in transaction mode):
HikariPool-1 - Pool stats (total=10, active=3, idle=7, waiting=0)
```

**3. Check for Connection Leaks:**

Connection leaks occur when connections are acquired but never returned to the pool. Common causes:
```java
// ❌ BAD: Connection leak if exception occurs
Connection conn = dataSource.getConnection();
// ... use connection ...
conn.close(); // Won't execute if exception thrown above

// ✅ GOOD: Always close connection
try (Connection conn = dataSource.getConnection()) {
    // ... use connection ...
} // Automatically closed even if exception occurs
```

Enable leak detection:
```properties
# application-prod.properties
spring.datasource.hikari.leak-detection-threshold=60000  # 60 seconds
```

**4. Verify Transaction Management:**

```bash
# Look for long-running transactions in logs:
grep -i "transaction" application.log | grep -i "timeout\|long"

# Check for unclosed transactions:
grep -i "rollback\|commit" application.log
```

**Solutions for Pool Exhaustion:**

**Solution 1: Reduce Maximum Pool Size (Recommended for Transaction Mode)**

```properties
# application-prod.properties
# For transaction mode, use SMALLER pools
spring.datasource.hikari.maximum-pool-size=10  # Reduced from 20
spring.datasource.hikari.minimum-idle=2        # Reduced from 5
spring.datasource.hikari.connection-timeout=30000  # Increased to 30s
```

**Rationale**: Transaction mode is designed for high concurrency with small pools. The pooler handles multiplexing, so your application doesn't need many connections.

**Solution 2: Optimize Transaction Boundaries**

```java
// ❌ BAD: Transaction spans unnecessary operations
@Transactional
public void processOrder(Order order) {
    Order saved = orderRepository.save(order);
    sendEmail(saved);  // External I/O inside transaction
    logAudit(saved);   // Holds connection unnecessarily
}

// ✅ GOOD: Minimal transaction scope
public void processOrder(Order order) {
    Order saved = saveOrder(order);      // Transactional
    sendEmail(saved);                     // Non-transactional
    logAudit(saved);                      // Non-transactional
}

@Transactional
private Order saveOrder(Order order) {
    return orderRepository.save(order);
}
```

**Solution 3: Add Connection Timeout and Retry Logic**

```properties
# application-prod.properties
spring.datasource.hikari.connection-timeout=30000  # Wait up to 30s for connection
spring.datasource.hikari.validation-timeout=5000   # 5s to validate connection
spring.datasource.hikari.max-lifetime=600000       # 10 minutes max lifetime
```

**Solution 4: Monitor Supabase Dashboard**

1. Go to **Supabase Dashboard** → **Database** → **Connection Pooling**
2. Check **Active Connections** metric
3. If consistently at maximum, consider:
   - Upgrading Supabase plan for higher limits
   - Reducing application pool size further
   - Optimizing query performance

**5. Quick Health Check Commands:**

```bash
# From Render shell, test connection acquisition speed:
time psql "postgres://user:pass@aws-0-REGION.pooler.supabase.com:6543/postgres?sslmode=require" -c "SELECT 1"

# Should complete in < 1 second. If slower:
# - Pooler may be under heavy load
# - Network latency issues
# - Pool exhaustion occurring
```

**Expected Output (Healthy):**
```bash
 ?column? 
----------
        1
(1 row)

real    0m0.234s  # < 1 second = healthy
```

**Problem Indicator (Unhealthy):**
```bash
psql: error: connection to server at "aws-0-REGION.pooler.supabase.com" (x.x.x.x), port 6543 failed: 
FATAL: remaining connection slots are reserved

real    0m20.045s  # 20+ seconds = pool exhausted
```

### Configuration Recommendations by Deployment Type

#### High-Traffic Production App (Transaction Mode)
```properties
# application-prod.properties
spring.datasource.url=${DATABASE_URL}?sslmode=require
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=300000
spring.datasource.hikari.max-lifetime=600000
spring.datasource.hikari.leak-detection-threshold=60000
```

#### Background Worker / Admin Tasks (Session Mode)
```properties
# application-worker.properties
spring.datasource.url=${DATABASE_URL_DIRECT}?sslmode=require
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=60000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

#### Development / Testing (Direct Connection)
```properties
# application-dev.properties (or use H2 in-memory)
spring.datasource.url=jdbc:postgresql://localhost:5432/odvsicilia_dev
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
```

### Quick Reference: Pooler vs Direct Connection

| Aspect | Transaction Mode (6543) | Session Mode (5432) |
|--------|------------------------|---------------------|
| **URL Pattern** | `aws-1-{region}.pooler.supabase.com:6543` | `db.{project}.supabase.co:5432` |
| **Pool Size** | Small (10 or less) | Medium-Large (20-50) |
| **Concurrency** | Thousands of connections | Limited by plan (60-500) |
| **Transaction Scope** | Per-transaction assignment | Full session duration |
| **Prepared Statements** | ❌ Don't persist | ✅ Persist across queries |
| **Temporary Tables** | ❌ Not supported | ✅ Fully supported |
| **Session Variables** | ❌ Don't persist | ✅ Persist for session |
| **SSL Required** | ✅ Yes (`sslmode=require`) | ✅ Yes (`sslmode=require`) |
| **Best For** | Production web apps | Admin tasks, migrations |
| **Cost Efficiency** | ✅ High (connection reuse) | ⚠️ Limited by plan |

### Supabase Connection Refused Errors

This section helps diagnose and resolve "connection refused" errors when connecting to Supabase, covering how to distinguish between pooler and direct connection failures, verify Supabase configuration, test connectivity, and choose the appropriate connection mode for your deployment platform.

#### Distinguishing Pooler vs Direct Connection Failures in Logs

Connection failures to Supabase manifest differently depending on which connection mode is being used. Examining the **port number** in error messages is the key to identifying the failure type:

**Pooler Connection Failure (Port 6543)**

```
ERROR: Connection to aws-0-eu-north-1.pooler.supabase.com:6543 refused
org.postgresql.util.PSQLException: Connection to aws-0-eu-north-1.pooler.supabase.com:6543 refused. 
  Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.

HikariPool-1 - Exception during pool initialization
java.net.ConnectException: Connection refused (Connection refused)
  at java.base/java.net.PlainSocketImpl.connect0(Native Method)
  ...connection attempt to aws-0-eu-north-1.pooler.supabase.com:6543...
```

**Key Indicators**:
- Port `6543` in hostname
- Hostname pattern: `aws-X-REGION.pooler.supabase.com`
- Error typically means: pooler not enabled, wrong region, or network issue

**Direct Connection Failure (Port 5432)**

```
ERROR: Connection to db.abcdefghijklmnop.supabase.co:5432 refused
org.postgresql.util.PSQLException: Connection to db.abcdefghijklmnop.supabase.co:5432 refused.
  Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.

HikariPool-1 - Exception during pool initialization
java.net.ConnectException: Connection refused (Connection refused)
  at java.base/java.net.PlainSocketImpl.connect0(Native Method)
  ...connection attempt to db.abcdefghijklmnop.supabase.co:5432...
```

**Key Indicators**:
- Port `5432` in hostname
- Hostname pattern: `db.PROJECT_REF.supabase.co`
- Error typically means: project paused, suspended, or direct connections disabled

**Log Analysis Checklist**:

1. **Extract the port number** from the error message:
   ```bash
   # Search application logs for connection errors
   grep -i "connection refused" application.log
   grep -i "port 6543\|port 5432" application.log
   ```

2. **Identify the hostname pattern**:
   - `pooler.supabase.com` → Transaction pooler mode
   - `db.*.supabase.co` → Direct connection mode

3. **Check DatabaseUrlEnvironmentPostProcessor logs** for URL conversion:
   ```
   INFO  - DatabaseUrlEnvironmentPostProcessor: Original DATABASE_URL: postgres://postgres.myproject:pass@aws-0-eu-north-1.pooler.supabase.com:6543/postgres
   INFO  - DatabaseUrlEnvironmentPostProcessor: Converted to JDBC URL: jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:6543/postgres?user=postgres.myproject&password=...
   ```
   This confirms which endpoint your application is attempting to connect to.

4. **Correlate with HikariCP initialization logs**:
   ```bash
   # Look for HikariCP configuration showing which URL is being used
   grep -i "HikariConfig" application.log | grep -i "jdbcUrl"
   ```

#### Verifying Connection Pooling in Supabase Dashboard

Follow these steps to verify that connection pooling is properly enabled and configured in your Supabase project:

**Step 1: Navigate to Database Settings**

1. Log in to [Supabase Dashboard](https://app.supabase.com)
2. Select your project from the project list
3. In the left sidebar, click **Settings** (gear icon)
4. Click **Database** in the settings menu

**Step 2: Locate Connection Pooling Section**

1. Scroll down to the **Connection Pooling** section (typically below "Connection Info")
2. You should see:
   - **Connection pooling enabled** toggle switch
   - **Pool Mode** dropdown
   - **Connection string** for the pooler

**Step 3: Verify Connection Pooling Settings**

Check the following configuration:

| Setting | Expected Value | Description |
|---------|---------------|-------------|
| **Connection pooling enabled** | ✅ ON (green toggle) | If OFF, pooler endpoint won't work |
| **Pool Mode** | **Transaction** | Recommended for web apps; Session mode for long-running operations |
| **Host** | `aws-X-REGION.pooler.supabase.com` | Region-specific pooler endpoint |
| **Port** | `6543` | Transaction pooler port (NOT 5432) |
| **Database** | `postgres` | Default database name |
| **User** | `postgres.YOUR_PROJECT_REF` | Includes project reference suffix |

**Visual Verification Checklist**:

- ✅ **Toggle is GREEN** and shows "Enabled"
- ✅ **Pool Mode** shows "Transaction" (or "Session" if intentionally using that mode)
- ✅ **Connection string** shown contains port `6543`
- ✅ **Username** format is `postgres.PROJECT_REF` (not just `postgres`)

**If Connection Pooling is Disabled**:

1. Click the **toggle switch** to enable it
2. Wait 10-15 seconds for the pooler to initialize
3. Copy the new connection string from the dashboard
4. Update your `DATABASE_URL` environment variable in Render
5. Restart your Render service

**Step 4: Copy the Correct Connection String**

1. In the **Connection Pooling** section, locate the **Connection string** field
2. Click the **Copy** icon next to the connection string
3. The string should look like:
   ```
   postgres://postgres.YOUR_PROJECT_REF:[YOUR-PASSWORD]@aws-0-REGION.pooler.supabase.com:6543/postgres
   ```
4. Replace `[YOUR-PASSWORD]` with your actual database password
5. Use this exact string as your `DATABASE_URL` in Render

**Common Mistakes to Avoid**:

- ❌ Using direct connection string (port 5432) when you need pooler (port 6543)
- ❌ Using username `postgres` instead of `postgres.PROJECT_REF`
- ❌ Copying old connection string after enabling pooler (refresh the page)
- ❌ Forgetting to restart your application after changing DATABASE_URL

#### Testing Connectivity to Supabase Endpoints

Use these commands to test connectivity to both Supabase connection modes. Run these tests from your Render shell or local environment.

**Testing Transaction Pooler (Port 6543)**

**Using telnet:**
```bash
# Test TCP connectivity to pooler endpoint
telnet aws-0-eu-north-1.pooler.supabase.com 6543

# Expected output (success):
# Trying 13.48.XX.XXX...
# Connected to aws-0-eu-north-1.pooler.supabase.com.
# Escape character is '^]'.

# Expected output (failure):
# Trying 13.48.XX.XXX...
# telnet: Unable to connect to remote host: Connection refused
```

**Using netcat (nc):**
```bash
# Test pooler connectivity with timeout
nc -zv -w5 aws-0-eu-north-1.pooler.supabase.com 6543

# Expected output (success):
# Connection to aws-0-eu-north-1.pooler.supabase.com 6543 port [tcp/*] succeeded!

# Expected output (failure):
# nc: connect to aws-0-eu-north-1.pooler.supabase.com port 6543 (tcp) failed: Connection refused
```

**Using psql (full authentication test):**
```bash
# Install PostgreSQL client if needed (in Render shell)
apt-get update && apt-get install -y postgresql-client

# Test pooler connection with authentication
psql "postgres://postgres.YOUR_PROJECT_REF:YOUR_PASSWORD@aws-0-eu-north-1.pooler.supabase.com:6543/postgres?sslmode=require"

# Expected output (success):
# psql (14.x, server 15.x)
# SSL connection (protocol: TLSv1.3, cipher: TLS_AES_256_GCM_SHA384, bits: 256)
# Type "help" for help.
# 
# postgres=>

# Expected output (failure - wrong password):
# psql: error: connection to server at "aws-0-eu-north-1.pooler.supabase.com" (13.48.XX.XXX), port 6543 failed:
# FATAL: password authentication failed for user "postgres.YOUR_PROJECT_REF"

# Expected output (failure - connection refused):
# psql: error: connection to server at "aws-0-eu-north-1.pooler.supabase.com" (13.48.XX.XXX), port 6543 failed:
# Connection refused
```

**Quick verification query:**
```bash
# Once connected, test a simple query
psql "postgres://postgres.YOUR_PROJECT_REF:YOUR_PASSWORD@aws-0-eu-north-1.pooler.supabase.com:6543/postgres?sslmode=require" \
  -c "SELECT version();"

# Expected output:
#                                                version
# -------------------------------------------------------------------------------------------------------
#  PostgreSQL 15.1 on x86_64-pc-linux-gnu, compiled by gcc, 64-bit
# (1 row)
```

**Testing Direct Connection (Port 5432)**

**Using telnet:**
```bash
# Test TCP connectivity to direct database endpoint
telnet db.abcdefghijklmnop.supabase.co 5432

# Expected output (success):
# Trying 13.48.XX.XXX...
# Connected to db.abcdefghijklmnop.supabase.co.
# Escape character is '^]'.

# Expected output (failure - project paused):
# Trying 13.48.XX.XXX...
# telnet: Unable to connect to remote host: Connection refused
```

**Using netcat (nc):**
```bash
# Test direct connection with timeout
nc -zv -w5 db.abcdefghijklmnop.supabase.co 5432

# Expected output (success):
# Connection to db.abcdefghijklmnop.supabase.co 5432 port [tcp/postgresql] succeeded!

# Expected output (failure):
# nc: connect to db.abcdefghijklmnop.supabase.co port 5432 (tcp) failed: Connection refused
```

**Using psql (full authentication test):**
```bash
# Test direct connection with authentication
psql "postgres://postgres.YOUR_PROJECT_REF:YOUR_PASSWORD@db.abcdefghijklmnop.supabase.co:5432/postgres?sslmode=require"

# Expected output (success):
# psql (14.x, server 15.x)
# SSL connection (protocol: TLSv1.3, cipher: TLS_AES_256_GCM_SHA384, bits: 256)
# Type "help" for help.
# 
# postgres=>
```

**Comparative Test (Both Endpoints)**

```bash
# Test both endpoints simultaneously to compare
echo "Testing Pooler (6543)..."
nc -zv -w5 aws-0-eu-north-1.pooler.supabase.com 6543

echo "Testing Direct (5432)..."
nc -zv -w5 db.abcdefghijklmnop.supabase.co 5432

# Interpretation:
# Both succeed → Both connection modes available
# Only pooler succeeds → Project may be paused; use pooler
# Only direct succeeds → Connection pooling disabled; enable it or use direct
# Both fail → Network issue, project suspended, or wrong hostnames
```

**Troubleshooting Test Failures**:

| Test Result | Port 6543 (Pooler) | Port 5432 (Direct) | Diagnosis | Solution |
|-------------|-------------------|-------------------|-----------|----------|
| ✅ Success | ✅ Connected | ✅ Connected | All healthy | Use pooler (6543) for production |
| ⚠️ Partial | ✅ Connected | ❌ Refused | Direct connection disabled or project paused | Use pooler mode; verify project is active |
| ⚠️ Partial | ❌ Refused | ✅ Connected | Connection pooling disabled | Enable pooling in dashboard or use direct |
| ❌ Failure | ❌ Refused | ❌ Refused | Network issue or project suspended | Check DNS, firewall, project status |

#### Supabase Project States and Connection Availability

Supabase projects can exist in different states that affect database connectivity. Understanding these states helps diagnose connection failures.

**Project State: Active**

- **Status**: Project is running normally
- **Pooler (6543)**: ✅ Available (if enabled)
- **Direct (5432)**: ✅ Available
- **Indicators in Dashboard**:
  - Green "Active" badge on project card
  - Database shows "Healthy" status
  - No warning banners
- **Connection Behavior**:
  - Both connection modes work normally
  - Low latency, fast connection establishment
  - Queries execute successfully

**Project State: Paused (Due to Inactivity)**

- **Status**: Project auto-paused after 7 days of inactivity (Free tier only)
- **Pooler (6543)**: ⚠️ **May work** with auto-resume behavior
- **Direct (5432)**: ❌ **Connection Refused** until resumed
- **Indicators in Dashboard**:
  - Yellow/Orange "Paused" badge
  - Banner: "This project has been paused due to inactivity"
  - "Resume Project" button visible
- **Connection Behavior**:
  - **Direct Connection (5432)**:
    ```
    Connection refused
    FATAL: the database system is starting up
    ```
  - **Pooler Connection (6543)**:
    - First connection may timeout (30-60 seconds) while project resumes
    - Subsequent connections work normally
    - Application may experience `HikariPool - Connection is not available, request timed out`
- **Resolution**:
  1. Click **Resume Project** in Supabase dashboard
  2. Wait 30-60 seconds for database to start
  3. Test connectivity again
  4. Consider upgrading to Pro tier to disable auto-pause

**Project State: Suspended (Billing Issues)**

- **Status**: Project suspended due to unpaid invoices or plan limits exceeded
- **Pooler (6543)**: ❌ **Connection Refused**
- **Direct (5432)**: ❌ **Connection Refused**
- **Indicators in Dashboard**:
  - Red "Suspended" badge
  - Banner: "Your project has been suspended"
  - Payment required to reactivate
- **Connection Behavior**:
  - All connection attempts fail immediately:
    ```
    Connection refused
    could not connect to server: Connection refused
    ```
  - No auto-resume behavior
  - Application logs show persistent connection failures
- **Resolution**:
  1. Go to **Organization Settings** → **Billing**
  2. Resolve outstanding payment issues
  3. Contact Supabase support if needed
  4. Project will resume within 5-10 minutes after resolution

**Project State: Upgrading/Maintenance**

- **Status**: Project undergoing planned maintenance or upgrade
- **Pooler (6543)**: ⚠️ **Intermittent availability**
- **Direct (5432)**: ⚠️ **Intermittent availability**
- **Indicators in Dashboard**:
  - Blue "Maintenance" banner
  - Scheduled maintenance notification
- **Connection Behavior**:
  - Brief connection interruptions (1-5 seconds)
  - Automatic reconnection usually works
  - HikariCP handles retries transparently
- **Resolution**:
  - Wait for maintenance window to complete
  - Application should auto-recover
  - Monitor logs for connection pool recovery

**Connection Mode Failure Matrix by Project State**

| Project State | Pooler (6543) | Direct (5432) | Recommended Action |
|--------------|---------------|---------------|--------------------|
| **Active** | ✅ Works | ✅ Works | Use pooler for production |
| **Paused (Free tier)** | ⚠️ Auto-resumes (slow) | ❌ Fails | Resume project; upgrade to Pro to disable auto-pause |
| **Suspended (Billing)** | ❌ Fails | ❌ Fails | Resolve billing issue |
| **Maintenance** | ⚠️ Intermittent | ⚠️ Intermittent | Wait for completion; connection pool auto-recovers |
| **Pooling Disabled** | ❌ Fails | ✅ Works | Enable pooling or use direct connection |

**Checking Project State Programmatically**:

```bash
# From Render shell or local terminal
# Test if project is responsive
curl -s -o /dev/null -w "%{http_code}" https://YOUR_PROJECT_REF.supabase.co/rest/v1/

# Expected responses:
# 200 → Project is active
# 401/403 → Project is active (authentication required)
# 503 → Project may be paused or in maintenance
# Connection timeout → Project suspended or network issue
```

#### Choosing Between Pooler and Direct Connection: Decision Guide

Use this decision tree to determine the optimal connection mode for your deployment:

**Decision Tree**

```
START: Which connection mode should I use?
│
├─ Q1: What platform are you deploying to?
│  │
│  ├─ Serverless (AWS Lambda, Vercel, Netlify Functions)
│  │  └─→ ✅ USE POOLER (6543) - Transaction Mode
│  │      Reason: Serverless functions create many concurrent connections;
│  │              pooler prevents exhausting database connection limits
│  │
│  ├─ Platform with connection limits (Render Free, Heroku Free/Hobby)
│  │  └─→ ✅ USE POOLER (6543) - Transaction Mode
│  │      Reason: Connection pooling reduces total connections needed
│  │
│  ├─ Standard web server (Render Pro, dedicated servers, VPS)
│  │  └─→ Continue to Q2
│  │
│  └─ Background workers, cron jobs, data migrations
│     └─→ Continue to Q3
│
├─ Q2: Does your application need session-level PostgreSQL features?
│  │
│  ├─ YES - Need prepared statements, temp tables, session variables, LISTEN/NOTIFY
│  │  └─→ ✅ USE DIRECT (5432) - Session Mode
│  │      Configuration: maximum-pool-size=20, minimum-idle=5
│  │      Monitor: Keep pool size within plan limits (60-500 connections)
│  │
│  └─ NO - Standard CRUD operations, REST API, stateless transactions
│     └─→ ✅ USE POOLER (6543) - Transaction Mode
│         Configuration: maximum-pool-size=10, minimum-idle=2
│         Benefit: Better scalability, more efficient resource usage
│
└─ Q3: What type of database operations are you running?
   │
   ├─ Long-running queries (>30 seconds), analytics, reports
   │  └─→ ✅ USE DIRECT (5432) - Session Mode
   │      Reason: Transaction pooler may timeout long-running operations
   │
   ├─ Database migrations, schema changes, admin operations
   │  └─→ ✅ USE DIRECT (5432) - Session Mode
   │      Reason: Requires full PostgreSQL feature set and DDL operations
   │
   └─ Short transactions, API endpoints, typical web app queries
      └─→ ✅ USE POOLER (6543) - Transaction Mode
          Reason: Optimal for high-throughput, short-lived transactions
```

**Quick Reference Matrix**

| Use Case | Connection Mode | Port | URL Pattern | Pool Size | Rationale |
|----------|----------------|------|-------------|-----------|-----------|
| **Production web app (Render)** | Pooler | 6543 | `aws-X-REGION.pooler.supabase.com` | 10 max | High concurrency, connection efficiency |
| **Serverless functions** | Pooler | 6543 | `aws-X-REGION.pooler.supabase.com` | 5-10 max | Many concurrent cold starts |
| **Background workers** | Direct | 5432 | `db.PROJECT_REF.supabase.co` | 5-10 max | Long-running operations, full features |
| **Database migrations** | Direct | 5432 | `db.PROJECT_REF.supabase.co` | 1-3 max | DDL operations, schema changes |
| **Analytics/Reporting** | Direct | 5432 | `db.PROJECT_REF.supabase.co` | 5-10 max | Long-running queries |
| **Development (local)** | Direct or H2 | 5432 or N/A | `localhost:5432` or in-memory | 5 max | Debugging, testing |
| **Microservices (many instances)** | Pooler | 6543 | `aws-X-REGION.pooler.supabase.com` | 5-10 max per instance | Distributed connection pooling |
| **Admin dashboard** | Direct | 5432 | `db.PROJECT_REF.supabase.co` | 10 max | Session features, ad-hoc queries |

**Configuration Examples by Platform**

**Render.com (Recommended: Pooler)**

```bash
# Render Environment Variables
DATABASE_URL=postgres://postgres.PROJECT_REF:PASSWORD@aws-0-eu-north-1.pooler.supabase.com:6543/postgres

# application-prod.properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000
```

**AWS Lambda (Required: Pooler)**

```bash
# Lambda Environment Variables
DATABASE_URL=postgres://postgres.PROJECT_REF:PASSWORD@aws-0-us-east-1.pooler.supabase.com:6543/postgres

# Configuration (if using Spring Boot)
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=20000
```

**Dedicated Server / VPS (Optional: Direct)**

```bash
# Environment Variables
DATABASE_URL=postgres://postgres.PROJECT_REF:PASSWORD@db.PROJECT_REF.supabase.co:5432/postgres

# application.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
```

**Background Worker (Recommended: Direct)**

```bash
# Environment Variables
DATABASE_URL=postgres://postgres.PROJECT_REF:PASSWORD@db.PROJECT_REF.supabase.co:5432/postgres

# application-worker.properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=1
spring.datasource.hikari.connection-timeout=60000
spring.datasource.hikari.max-lifetime=1800000  # 30 minutes
```

#### Configuration and Diagnostic Integration

This application provides built-in capabilities to simplify Supabase connection management and diagnostics.

**DatabaseUrlEnvironmentPostProcessor - Automatic URL Conversion**

The application automatically converts standard PostgreSQL URLs to JDBC format, supporting both pooler and direct connection modes:

```
# You provide (any of these formats):
DATABASE_URL=postgres://postgres.myproject:pass@aws-0-eu-north-1.pooler.supabase.com:6543/postgres
DATABASE_URL=postgresql://postgres.myproject:pass@db.myproject.supabase.co:5432/postgres

# Application automatically converts to:
jdbc:postgresql://aws-0-eu-north-1.pooler.supabase.com:6543/postgres?user=postgres.myproject&password=pass
jdbc:postgresql://db.myproject.supabase.co:5432/postgres?user=postgres.myproject&password=pass
```

**Check conversion logs** during application startup:

```
INFO  - DatabaseUrlEnvironmentPostProcessor: Original DATABASE_URL detected: postgres://postgres.myproject:***@aws-0-eu-north-1.pooler.supabase.com:6543/postgres
INFO  - DatabaseUrlEnvironmentPostProcessor: Converted to JDBC URL with encoded credentials
```

**Diagnostic Capabilities**

1. **Verify which endpoint is being used**:
   ```bash
   # Check Render logs during application startup
   grep "DatabaseUrlEnvironmentPostProcessor" render-logs.txt
   
   # Look for HikariCP configuration
   grep "HikariConfig" render-logs.txt | grep "jdbcUrl"
   ```

2. **Identify connection failures by port**:
   ```bash
   # Search for connection errors showing port numbers
   grep -E "port (5432|6543)" application.log
   
   # Find which endpoint failed
   grep -E "(pooler\.supabase\.com|db\..*\.supabase\.co)" application.log
   ```

3. **Monitor connection pool health**:
   ```bash
   # Enable debug logging temporarily
   LOGGING_LEVEL_COM_ZAXXER_HIKARI=DEBUG
   
   # Check pool statistics in logs
   grep "HikariPool.*stats" application.log
   ```

**Fallback Behavior**

If `DATABASE_URL` is not set or connection fails, the application behavior depends on the active profile:

- **dev profile**: Falls back to H2 in-memory database
  ```properties
  # Automatic fallback - no configuration needed
  # Application logs will show:
  INFO  - Using H2 in-memory database (dev profile)
  ```

- **prod profile**: Application fails to start with clear error message
  ```
  ERROR - Failed to configure a DataSource: 'url' attribute is not specified
  ```

**Environment Variable Reference**

| Variable | Purpose | Example | Required |
|----------|---------|---------|----------|
| `DATABASE_URL` | Primary database connection | `postgres://user:pass@host:6543/db` | Yes (prod) |
| `DB_USERNAME` | Override username (optional) | `postgres.myproject` | No |
| `DB_PASSWORD` | Override password (optional) | `my_secure_password` | No |
| `SPRING_PROFILES_ACTIVE` | Activate configuration profile | `prod` | Yes |
| `LOGGING_LEVEL_COM_ZAXXER_HIKARI` | Connection pool debug logs | `DEBUG` | No (debugging) |

**Testing Connection Configuration**

From Render shell, verify your configuration:

```bash
# 1. Check environment variable is set correctly
echo $DATABASE_URL
# Should show: postgres://postgres.PROJECT:***@aws-0-REGION.pooler.supabase.com:6543/postgres

# 2. Extract and verify connection components
echo $DATABASE_URL | grep -oE ":[0-9]+"
# Should show: :6543 (pooler) or :5432 (direct)

# 3. Test connection with psql using exact DATABASE_URL
psql "$DATABASE_URL?sslmode=require" -c "SELECT 1;"
# Should return: 1 row with value 1

# 4. Check application startup logs
grep -E "(DatabaseUrlEnvironmentPostProcessor|HikariConfig)" /var/log/application.log

# 5. Verify SSL connection
psql "$DATABASE_URL?sslmode=require" -c "\conninfo"
# Should show: SSL connection (protocol: TLSv1.3, ...)
```

**Troubleshooting Checklist for Connection Refused Errors**:

- [ ] **1. Verify port number**: Check logs for `6543` (pooler) vs `5432` (direct)
- [ ] **2. Check Supabase dashboard**: Confirm connection pooling is enabled (if using port 6543)
- [ ] **3. Test with telnet/nc**: Verify TCP connectivity to the specific port
- [ ] **4. Test with psql**: Confirm authentication and SSL work correctly
- [ ] **5. Check project state**: Ensure project is not paused or suspended
- [ ] **6. Verify DATABASE_URL format**: Correct hostname pattern for chosen mode
- [ ] **7. Review conversion logs**: Check `DatabaseUrlEnvironmentPostProcessor` output
- [ ] **8. Monitor pool exhaustion**: Look for timeout messages in HikariCP logs
- [ ] **9. Validate SSL configuration**: Ensure `sslmode=require` is present
- [ ] **10. Consider connection mode switch**: Try alternative mode if persistent issues

**Additional Resources**:
- **Earlier section**: [Understanding Supabase Connection Modes](#understanding-supabase-connection-modes) - Detailed explanation of pooler vs direct
- **Earlier section**: [Testing Network Connectivity from Render](#testing-network-connectivity-from-render) - Comprehensive connectivity tests
- **Earlier section**: [Troubleshooting Pooler Connection Failures](#troubleshooting-pooler-connection-failures) - Pool exhaustion diagnosis
- **Supabase Docs**: [Connection Pooling Guide](https://supabase.com/docs/guides/database/connecting-to-postgres#connection-pooler)

## Performance Optimization

### Database Connection Pool
The application is configured with optimized HikariCP settings:
- Maximum pool size: 20 connections
- Minimum idle: 5 connections
- Connection timeout: 20 seconds
- Idle timeout: 10 minutes

### JVM Settings
The Dockerfile includes production-optimized JVM settings:
- Container support enabled
- G1 garbage collector
- Memory optimization for cloud deployment

## Security Considerations

1. **Environment Variables**: Never commit sensitive data to version control
2. **Database Security**: Use strong passwords and enable SSL connections
3. **API Keys**: Rotate API keys regularly
4. **CORS Configuration**: Ensure CORS settings match your frontend domain
5. **Input Validation**: All endpoints include proper validation and error handling

## Scaling

Render automatically handles:
- **Horizontal Scaling**: Based on traffic and resource usage
- **Load Balancing**: Automatic distribution of requests
- **Health Monitoring**: Automatic restart of unhealthy instances

## Troubleshooting Render-Specific Environment Variable Issues

### Verifying Environment Variables in Render Dashboard

When diagnosing configuration issues, always start by verifying your environment variables are properly set:

1. **Navigate to Environment Settings**:
   - Log in to [Render Dashboard](https://dashboard.render.com)
   - Select your web service
   - Click the **Environment** tab in the left sidebar

2. **Verify DATABASE_URL Configuration**:
   - Locate `DATABASE_URL` in the environment variables list
   - Click the eye icon (👁️) to reveal the value
   - Confirm the URL format matches one of these patterns:
     - `postgres://username:password@hostname:port/database`
     - `postgresql://username:password@hostname:port/database`
     - `jdbc:postgresql://hostname:port/database?user=username&password=password`
   - Check for common issues:
     - Special characters in password must be URL-encoded (e.g., `@` → `%40`)
     - No extra spaces or line breaks
     - Correct hostname and port (default PostgreSQL port is `5432`)
     - Database name matches your actual database

3. **Verify Other Required Variables**:
   - Ensure all required variables are present (see "Configure Environment Variables" section)
   - Check variable names for typos (case-sensitive!)
   - Verify values are not empty or placeholder text

### Locating EnvironmentPostProcessor Conversion Logs

The application includes `DatabaseUrlEnvironmentPostProcessor` which converts standard PostgreSQL URLs to JDBC format. These logs help verify the conversion is working correctly:

1. **Access Render Service Logs**:
   - In your Render service dashboard, click the **Logs** tab
   - Filter to "Deploy logs" or "Application logs" depending on when you want to check

2. **Find Startup Logs**:
   - Look for logs during application startup (immediately after "Starting BackendApplication")
   - Search for lines containing `DatabaseUrlEnvironmentPostProcessor`

3. **Interpret Conversion Logging**:

   **✅ Successful Conversion Example:**
   ```
   INFO  - DatabaseUrlEnvironmentPostProcessor: Original DATABASE_URL: postgres://user:pass@host.com:5432/odvsicilia
   INFO  - DatabaseUrlEnvironmentPostProcessor: Converted to JDBC URL: jdbc:postgresql://host.com:5432/odvsicilia?user=user&password=pass
   INFO  - HikariPool-1 - Starting...
   INFO  - HikariPool-1 - Start completed.
   ```
   This indicates the URL was detected, converted, and the connection succeeded.

   **⚠️ No Conversion Log (Already JDBC Format):**
   ```
   INFO  - Starting BackendApplication using Java 17...
   INFO  - HikariPool-1 - Starting...
   ```
   If you don't see `DatabaseUrlEnvironmentPostProcessor` logs, the URL is either:
   - Already in JDBC format (no conversion needed)
   - Not set in environment variables (using default configuration)

   **❌ Connection Failure After Conversion:**
   ```
   INFO  - DatabaseUrlEnvironmentPostProcessor: Original DATABASE_URL: postgres://user:pass@host.com:5432/odvsicilia
   INFO  - DatabaseUrlEnvironmentPostProcessor: Converted to JDBC URL: jdbc:postgresql://host.com:5432/odvsicilia?user=user&password=pass
   ERROR - HikariPool-1 - Exception during pool initialization
   ERROR - Failed to configure a DataSource: 'url' attribute is not specified
   ```
   This indicates conversion occurred but connection failed. Check:
   - Database is accessible from Render's IPs
   - Credentials are correct
   - Database name exists
   - Special characters are properly URL-encoded

4. **Enable Detailed Logging (If Needed)**:
   - Add these environment variables temporarily:
     ```
     LOGGING_LEVEL_IT_ODVSICILIA_BACKEND=DEBUG
     LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_JDBC=DEBUG
     LOGGING_LEVEL_COM_ZAXXER_HIKARI=DEBUG
     ```
   - Trigger a redeploy to see detailed connection logs

### Common Render-Specific Mistakes

Even experienced developers make these mistakes when deploying to Render:

#### 1. **Forgetting to Save Environment Variable Changes**
   - **Symptom**: Changes don't take effect, old values still in use
   - **Cause**: After editing an environment variable in Render, the "Save Changes" button at the bottom must be clicked
   - **Solution**: 
     - Always scroll down and click **"Save Changes"** after editing variables
     - Wait for the "Environment variables updated" confirmation message
     - If unsure, refresh the page and verify your changes are visible

#### 2. **Using Incorrect Variable Names**
   - **Symptom**: Application uses default values or fails to start
   - **Cause**: Variable names are case-sensitive and must match exactly
   - **Common Mistakes**:
     - `database_url` instead of `DATABASE_URL` ❌
     - `DB_URL` instead of `DATABASE_URL` ❌
     - `BREVO_KEY` instead of `BREVO_API_KEY` ❌
     - `SUPABASE_KEY` instead of `SUPABASE_ANON_KEY` ❌
   - **Solution**: 
     - Copy variable names exactly from the "Configure Environment Variables" section above
     - Use the exact naming convention: `DATABASE_URL`, `BREVO_API_KEY`, etc.
     - Check for typos: extra spaces, underscores, or hyphens

#### 3. **Failing to Trigger a Redeploy After Configuration Updates**
   - **Symptom**: Changed environment variables don't take effect
   - **Cause**: Render does **not** automatically restart services when environment variables change
   - **Solution**: 
     - After saving environment variable changes, you must manually trigger a redeploy:
       1. Go to the **Manual Deploy** section in your service dashboard
       2. Click **"Deploy latest commit"** button
       3. Or, select "Settings" → "Deploy Hook" and trigger via webhook
     - Alternatively, push a new commit to trigger automatic deployment
     - Wait for deployment to complete and check logs to verify new values are used

#### 4. **Copy-Pasting Values with Hidden Characters**
   - **Symptom**: Authentication fails despite correct-looking credentials
   - **Cause**: Invisible characters (spaces, newlines, tabs) copied from other sources
   - **Solution**: 
     - Manually type sensitive values when possible
     - If copying, paste into a plain text editor first to check for hidden characters
     - Trim leading/trailing spaces before pasting into Render

#### 5. **Not URL-Encoding Special Characters in DATABASE_URL**
   - **Symptom**: Database connection fails with authentication or parsing errors
   - **Cause**: Special characters in password (like `@`, `#`, `$`, `%`) break URL parsing
   - **Solution**: 
     - See "Database URL Encoding" section above
     - Use an online URL encoder or encode manually
     - Test the encoded URL with `psql` before deploying

#### 6. **Setting Variables in Wrong Environment**
   - **Symptom**: Production works but preview/branch deploys fail (or vice versa)
   - **Cause**: Render allows different environment variables for branch deploys
   - **Solution**: 
     - Check which environment you're configuring (main branch vs. pull request previews)
     - In Environment tab, ensure variables are set for the correct environment
     - For production: set variables in the main service
     - For preview: check "Preview Environments" settings

#### 7. **Deleting Instead of Updating Variables**
   - **Symptom**: Service fails to start with missing configuration errors
   - **Cause**: Accidentally deleting a variable instead of updating it
   - **Solution**: 
     - Always use the "Edit" button (pencil icon) to modify values
     - Double-check before clicking delete (trash icon)
     - Keep a backup of your environment variables in a secure location (password manager, not in code!)

### Render-Specific Debugging Checklist

When experiencing configuration issues on Render, work through this checklist:

- [ ] Environment variables are saved (green confirmation message appeared)
- [ ] Variable names match exactly (case-sensitive)
- [ ] DATABASE_URL is in correct format and properly URL-encoded
- [ ] Manual redeploy triggered after configuration changes
- [ ] Deployment completed successfully (check deploy logs)
- [ ] Application started (look for "Started BackendApplication" in logs)
- [ ] `DatabaseUrlEnvironmentPostProcessor` conversion logs appear (if using postgres:// format)
- [ ] HikariCP connection pool initialized successfully
- [ ] Health endpoint returns 200 OK: `https://your-service.onrender.com/actuator/health`
- [ ] No trailing spaces or hidden characters in environment variable values
- [ ] All required environment variables are present (see "Required Variables" table)
- [ ] Service logs don't show "null" or "undefined" for configuration values

## Support

For deployment issues:
1. Check Render documentation: [Render Docs](https://render.com/docs)
2. Review application logs in Render dashboard
3. Verify all environment variables are correctly set
4. Test database connectivity independently

## Success Indicators

Your deployment is successful when:
- ✅ Build completes without errors
- ✅ Application starts and shows "Started BackendApplication"
- ✅ Health check endpoint returns 200 OK
- ✅ Database connection is established
- ✅ Email service is configured
- ✅ All API endpoints are accessible

---

**Note**: This deployment configuration is optimized for Render.com's platform. For other cloud providers, you may need to adjust the Dockerfile and environment configuration accordingly.
