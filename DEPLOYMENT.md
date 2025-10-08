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

### Common Issues

#### 1. Build Failures
- **Maven Dependencies**: Ensure all dependencies in `pom.xml` are available
- **Java Version**: Verify Java 17 compatibility
- **Memory Issues**: Check if build requires more memory

#### 2. Database Connection Issues
- **URL Encoding**: Ensure special characters in passwords are properly encoded (see formatting guide above)
- **Network Access**: Verify database allows connections from Render IPs
- **Credentials**: Double-check username, password, and database name
- **URL Format**: Ensure using correct format (see DATABASE_URL Formatting Guide above)

#### 3. Email Service Issues
- **API Key**: Verify Brevo API key is correct and active
- **Rate Limits**: Check if you've exceeded email service limits
- **SMTP Configuration**: Ensure SMTP settings match Brevo requirements

#### 4. Application Startup Issues
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
nslookup db.pejuystijjkjxjctieyb.supabase.co

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
telnet db.pejuystijjkjxjctieyb.supabase.co 5432

# Another example with different project
telnet db.abcdefghijklmnop.supabase.co 5432
```

**Using nc/netcat (Port 5432 - Direct Connection)**:
```bash
# Test direct connection
nc -zv -w5 db.pejuystijjkjxjctieyb.supabase.co 5432
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
psql "postgres://postgres.pejuystijjkjxjctieyb:YOUR_PASSWORD@aws-0-us-west-1.pooler.supabase.com:6543/postgres"

# Test connection with psql (direct)
psql "postgres://postgres.pejuystijjkjxjctieyb:YOUR_PASSWORD@db.pejuystijjkjxjctieyb.supabase.co:5432/postgres"

# Test with explicit SSL mode
psql "postgres://postgres.pejuystijjkjxjctieyb:YOUR_PASSWORD@aws-0-us-west-1.pooler.supabase.com:6543/postgres?sslmode=require"
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
