# Supabase Connection Troubleshooting Guide

This guide provides comprehensive troubleshooting steps for diagnosing and resolving Supabase database connection issues in the ODV Sicilia backend application.

## Table of Contents

1. [Connection Modes Overview](#connection-modes-overview)
2. [Enabling and Verifying Connection Pooler](#enabling-and-verifying-connection-pooler)
3. [Transaction Mode vs Session Mode](#transaction-mode-vs-session-mode)
4. [Common Failure Scenarios](#common-failure-scenarios)
5. [Project Status and Billing Issues](#project-status-and-billing-issues)
6. [IP Allowlisting and Security Configuration](#ip-allowlisting-and-security-configuration)
7. [Switching Connection Modes](#switching-connection-modes)
8. [Render-Specific Considerations](#render-specific-considerations)
9. [DATABASE_URL Examples by Region](#database_url-examples-by-region)

---

## Connection Modes Overview

Supabase provides two primary connection methods:

1. **Direct Connection** (Port 5432): Direct connection to PostgreSQL database
2. **Connection Pooler** (Port 6543 for transaction mode, 5432 with pooler subdomain for session mode): Uses PgBouncer for connection pooling

**When to use each:**
- **Direct Connection**: Local development, administrative tasks, long-running queries
- **Connection Pooler**: Production deployments, serverless environments, high-concurrency applications

---

## Enabling and Verifying Connection Pooler

### Enabling Connection Pooler in Supabase Dashboard

1. **Navigate to Database Settings**
   - Log in to [Supabase Dashboard](https://app.supabase.com/)
   - Select your project
   - Go to **Settings** → **Database**

2. **Enable Connection Pooling**
   - Scroll to the **Connection Pooling** section
   - Verify that connection pooling is **enabled** (should be enabled by default on newer projects)
   - Note the pooler connection string provided

3. **Configure Pooler Mode**
   - Choose between **Transaction** or **Session** mode (Transaction is recommended for most applications)
   - Transaction mode: `pooler.supabase.com:6543`
   - Session mode: `pooler.supabase.com:5432`

### Verifying Pooler Status

```bash
# Test connection pooler availability
psql "postgresql://postgres:[PASSWORD]@aws-0-[REGION].pooler.supabase.com:6543/postgres" -c "SELECT version();"
```

**Expected output**: PostgreSQL version information

**If connection fails:**
- Verify project is not paused (see [Project Status](#project-status-and-billing-issues))
- Check firewall/IP allowlist settings (see [IP Allowlisting](#ip-allowlisting-and-security-configuration))
- Confirm password is correct (use URL-encoded password if it contains special characters)

---

## Transaction Mode vs Session Mode

### Transaction Mode (Port 6543)

**Recommended for most applications, including this Spring Boot backend.**

**Characteristics:**
- Connection is held only for the duration of a transaction
- Most efficient for connection pooling
- Lower memory footprint
- Supports multiple clients sharing connections

**Limitations:**
- No support for prepared statements across transactions
- No support for `LISTEN/NOTIFY`
- No support for cursor-based queries (`DECLARE CURSOR`)
- Session-level PostgreSQL features not available

**Connection String Format:**
```
postgresql://postgres.[PROJECT_REF]:[PASSWORD]@aws-0-[REGION].pooler.supabase.com:6543/postgres
```

**Best for:**
- REST APIs (like this Spring Boot application)
- Stateless web applications
- Serverless functions
- High-concurrency scenarios

### Session Mode (Port 5432 via Pooler)

**Use when transaction mode limitations are an issue.**

**Characteristics:**
- Connection held for entire client session
- Supports all PostgreSQL features
- Higher memory usage per connection
- Better for complex database interactions

**Limitations:**
- Fewer concurrent connections available
- Higher resource consumption
- May hit connection limits faster

**Connection String Format:**
```
postgresql://postgres.[PROJECT_REF]:[PASSWORD]@aws-0-[REGION].pooler.supabase.com:5432/postgres
```

**Best for:**
- Applications requiring prepared statements
- Long-running transactions
- Applications using `LISTEN/NOTIFY`
- Complex cursor-based operations

### Direct Connection (Port 5432, No Pooler)

**Use only for development or administrative tasks.**

**Connection String Format:**
```
postgresql://postgres:[PASSWORD]@db.[PROJECT_REF].supabase.co:5432/postgres
```

**Best for:**
- Local development
- Database migrations
- Administrative operations
- Debugging connection issues

---

## Common Failure Scenarios

### Transaction Mode Failures

#### 1. Prepared Statement Errors
**Symptom:**
```
ERROR: prepared statement "pstmt_12345" does not exist
```

**Cause:** Spring Boot/Hibernate attempting to use prepared statements across transactions

**Solution:**
```properties
# In application.properties
spring.jpa.properties.hibernate.jdbc.use_get_generated_keys=false
spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults=false
```

Or switch to session mode.

#### 2. Connection Pool Exhaustion
**Symptom:**
```
PSQLException: FATAL: remaining connection slots are reserved
```

**Cause:** Too many concurrent connections exceeding pooler limits

**Solution:**
- Reduce application's connection pool size
- Upgrade Supabase plan for more connections
- Optimize query performance to reduce connection hold time

**Configuration:**
```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
```

#### 3. Transaction Timeout
**Symptom:**
```
ERROR: canceling statement due to user request
```

**Cause:** Transaction exceeds pooler timeout (default 60 seconds in transaction mode)

**Solution:**
- Optimize slow queries
- Split large transactions into smaller ones
- Switch to session mode for long-running operations

### Session Mode Failures

#### 1. Connection Limit Reached
**Symptom:**
```
FATAL: remaining connection slots are reserved for non-replication superuser connections
```

**Cause:** Exceeded maximum connections (lower limit in session mode)

**Solution:**
- Reduce application connection pool size
- Close idle connections more aggressively
- Switch to transaction mode if features allow

#### 2. Connection Timeout
**Symptom:**
```
PSQLException: Connection attempt timed out
```

**Cause:** Network issues, IP allowlist, or project paused

**Solution:**
- Check network connectivity
- Verify IP allowlist configuration
- Confirm project status

### Direct Connection Failures

#### 1. Port Blocked
**Symptom:**
```
Connection refused on port 5432
```

**Cause:** Firewall blocking port 5432, or IPv6 issues

**Solution:**
- Use connection pooler instead
- Check firewall rules
- Verify direct connections are enabled in Supabase settings

#### 2. SSL/TLS Errors
**Symptom:**
```
SSLException: SSL handshake failed
```

**Cause:** Supabase requires SSL connections

**Solution:**
```
DATABASE_URL=jdbc:postgresql://db.[PROJECT_REF].supabase.co:5432/postgres?sslmode=require
```

---

## Project Status and Billing Issues

### Checking Project Status

1. **Via Supabase Dashboard**
   - Go to [Supabase Dashboard](https://app.supabase.com/)
   - Check project status indicator (top of page)
   - Possible statuses:
     - **Active** (green): Project running normally
     - **Paused** (yellow): Project paused due to inactivity or billing
     - **Restoring** (blue): Project being restored from pause
     - **Unhealthy** (red): Infrastructure issues

2. **Via API Health Check**
   ```bash
   curl https://[PROJECT_REF].supabase.co/rest/v1/
   ```
   **Expected**: HTTP 200 or 401 (auth required)
   **Project paused**: No response or connection timeout

### Paused Project Issues

**Causes:**
- Free tier projects paused after 7 days of inactivity
- Billing issues on paid plans
- Exceeded usage limits

**Solutions:**

1. **Restore Project**
   - Click **Restore Project** button in dashboard
   - Wait 2-5 minutes for restoration
   - Test database connection

2. **Prevent Auto-Pause (Free Tier)**
   - Set up a cron job to ping your API every 6 days
   - Upgrade to Pro plan ($25/month, no auto-pause)

3. **Billing Issues**
   - Go to **Settings** → **Billing**
   - Check payment method status
   - Resolve any outstanding invoices
   - Contact Supabase support if issues persist

### Checking for Outages

- **Supabase Status Page**: https://status.supabase.com/
- **Twitter**: [@supabase](https://twitter.com/supabase)

---

## IP Allowlisting and Security Configuration

### Checking IP Allowlist Settings

1. **Navigate to Network Restrictions**
   - Supabase Dashboard → **Settings** → **Database**
   - Scroll to **Network Restrictions** section

2. **Verify Configuration**
   - **Allow all IP addresses** (default): No restrictions
   - **Allowlist specific IPs**: Only listed IPs can connect

### Common IP Allowlist Issues

#### Problem: Connection Refused Due to IP Restriction
**Symptom:**
```
FATAL: no pg_hba.conf entry for host "123.45.67.89"
```

**Solution:**
1. Determine your IP address:
   ```bash
   curl ifconfig.me
   ```
2. Add IP to allowlist in Supabase Dashboard
3. For Render deployments, add Render egress IPs (see [Render-Specific Considerations](#render-specific-considerations))

#### Problem: Dynamic IP Address
**Solution:**
- Use connection pooler (pooler IPs are more stable)
- Add IP ranges instead of single IPs
- Use Supabase Auth + Row Level Security instead of IP restrictions
- Consider upgrading to paid plan for better IP management

### Best Practices

- **Development**: Allow all IPs or use VPN
- **Production**: Restrict to known server IPs only
- **Render/Heroku**: Add their egress IP ranges
- **Security**: Prefer RLS + Auth over IP allowlisting

---

## Switching Connection Modes

This application supports switching between connection modes via the `SUPABASE_CONNECTION_MODE` environment variable.

### Configuration

#### Method 1: Environment Variable (Recommended)

**In `.env` file:**
```bash
# Use transaction mode pooler (recommended for production)
SUPABASE_CONNECTION_MODE=pooler

# Use session mode pooler
SUPABASE_CONNECTION_MODE=session

# Use direct connection (development only)
SUPABASE_CONNECTION_MODE=direct
```

**Set project reference and password separately:**
```bash
SUPABASE_PROJECT_REF=abc123xyz
SUPABASE_DB_PASSWORD=your-secure-password
SUPABASE_REGION=eu-central-1
```

**The application will construct the appropriate DATABASE_URL automatically.**

#### Method 2: Manual DATABASE_URL (Override)

If `DATABASE_URL` is explicitly set, it takes precedence over `SUPABASE_CONNECTION_MODE`:

```bash
# Manual override - transaction mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:password@aws-0-eu-central-1.pooler.supabase.com:6543/postgres

# Manual override - direct connection
DATABASE_URL=jdbc:postgresql://postgres:password@db.abc123xyz.supabase.co:5432/postgres
```

### Switching Between Modes

**On Render:**
1. Go to your service → **Environment**
2. Update `SUPABASE_CONNECTION_MODE` value
3. Click **Save Changes**
4. Service will automatically redeploy

**Locally:**
1. Update `.env` file
2. Restart application:
   ```powershell
   .\mvn.ps1 spring-boot:run
   ```

### Verification

**Check which mode is active:**
```bash
# In application logs at startup
2024-01-15 10:30:15.123 INFO ... Using Supabase connection mode: pooler (transaction)
2024-01-15 10:30:15.456 INFO ... Database URL: jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:6543/postgres
```

**Test connection:**
```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP"
    }
  }
}
```

---

## Render-Specific Considerations

### Egress IP Configuration

Render services use dynamic egress IPs that change. To enable IP allowlisting:

#### Option 1: Use Render Static IP (Paid Feature)
1. Purchase Render's Static Outbound IP add-on ($20/month)
2. Note the assigned static IP(s)
3. Add to Supabase IP allowlist

#### Option 2: Disable IP Restrictions (Recommended)
1. In Supabase Dashboard, set network restrictions to **Allow all IPs**
2. Rely on strong database password and RLS policies for security

#### Option 3: Use Supabase Connection Pooler
The pooler endpoint has more stable IP ranges, improving reliability with Render.

### Environment Variables on Render

**Recommended Render environment configuration:**

```bash
# Core database config
SUPABASE_PROJECT_REF=abc123xyz
SUPABASE_DB_PASSWORD=your-secure-password
SUPABASE_REGION=eu-central-1
SUPABASE_CONNECTION_MODE=pooler

# Application config
SPRING_PROFILES_ACTIVE=prod
PORT=10000

# Optional: Manual override if needed
# DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:password@aws-0-eu-central-1.pooler.supabase.com:6543/postgres
```

### Health Check Configuration

Render's health check can cause connection pool issues. Configure appropriately:

**In Render Dashboard:**
- **Health Check Path**: `/actuator/health`
- **Health Check Interval**: 60 seconds (not too frequent)

**In application.properties:**
```properties
# Ensure health endpoint doesn't hold connections
management.health.db.enabled=true
management.endpoint.health.show-details=when-authorized
```

### Connection Pool Sizing for Render

Render services have connection limits. Configure conservatively:

```properties
# For basic Render instance
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
```

### Troubleshooting Render Deployments

#### Issue: Connection Works Locally but Fails on Render
**Check:**
1. Environment variables are set correctly
2. No typos in `SUPABASE_PROJECT_REF` or `SUPABASE_DB_PASSWORD`
3. Password doesn't contain special characters requiring URL encoding
4. Supabase project is not paused
5. Connection pooler is enabled

#### Issue: Intermittent Connection Drops
**Solutions:**
- Use connection pooler (more stable)
- Reduce `max-lifetime` in HikariCP config
- Add connection test query:
  ```properties
  spring.datasource.hikari.connection-test-query=SELECT 1
  ```

#### Issue: "Too Many Connections" Error
**Solutions:**
- Reduce `maximum-pool-size`
- Close connections properly in application code
- Check for connection leaks in logs
- Upgrade Supabase plan for more connections

---

## DATABASE_URL Examples by Region

Below are complete, working `DATABASE_URL` examples for all major Supabase regions in both pooler and direct connection formats.

### Format Template

**Transaction Mode Pooler (Recommended):**
```
jdbc:postgresql://postgres.[PROJECT_REF]:[PASSWORD]@aws-0-[REGION].pooler.supabase.com:6543/postgres
```

**Session Mode Pooler:**
```
jdbc:postgresql://postgres.[PROJECT_REF]:[PASSWORD]@aws-0-[REGION].pooler.supabase.com:5432/postgres
```

**Direct Connection:**
```
jdbc:postgresql://postgres:[PASSWORD]@db.[PROJECT_REF].supabase.co:5432/postgres
```

### Regional Examples

Replace `[PROJECT_REF]` with your actual project reference (e.g., `abc123xyz`) and `[PASSWORD]` with your database password.

#### Europe (Frankfurt) - eu-central-1
```bash
# Transaction mode pooler (recommended)
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-eu-central-1.pooler.supabase.com:6543/postgres

# Session mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-eu-central-1.pooler.supabase.com:5432/postgres

# Direct connection
DATABASE_URL=jdbc:postgresql://postgres:yourpassword@db.abc123xyz.supabase.co:5432/postgres
```

#### Europe (London) - eu-west-2
```bash
# Transaction mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-eu-west-2.pooler.supabase.com:6543/postgres

# Session mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-eu-west-2.pooler.supabase.com:5432/postgres

# Direct connection
DATABASE_URL=jdbc:postgresql://postgres:yourpassword@db.abc123xyz.supabase.co:5432/postgres
```

#### Europe (Ireland) - eu-west-1
```bash
# Transaction mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-eu-west-1.pooler.supabase.com:6543/postgres

# Session mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-eu-west-1.pooler.supabase.com:5432/postgres

# Direct connection
DATABASE_URL=jdbc:postgresql://postgres:yourpassword@db.abc123xyz.supabase.co:5432/postgres
```

#### US East (N. Virginia) - us-east-1
```bash
# Transaction mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-us-east-1.pooler.supabase.com:6543/postgres

# Session mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-us-east-1.pooler.supabase.com:5432/postgres

# Direct connection
DATABASE_URL=jdbc:postgresql://postgres:yourpassword@db.abc123xyz.supabase.co:5432/postgres
```

#### US West (Oregon) - us-west-1
```bash
# Transaction mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-us-west-1.pooler.supabase.com:6543/postgres

# Session mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-us-west-1.pooler.supabase.com:5432/postgres

# Direct connection
DATABASE_URL=jdbc:postgresql://postgres:yourpassword@db.abc123xyz.supabase.co:5432/postgres
```

#### Asia Pacific (Singapore) - ap-southeast-1
```bash
# Transaction mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-ap-southeast-1.pooler.supabase.com:6543/postgres

# Session mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-ap-southeast-1.pooler.supabase.com:5432/postgres

# Direct connection
DATABASE_URL=jdbc:postgresql://postgres:yourpassword@db.abc123xyz.supabase.co:5432/postgres
```

#### Asia Pacific (Sydney) - ap-southeast-2
```bash
# Transaction mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-ap-southeast-2.pooler.supabase.com:6543/postgres

# Session mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-ap-southeast-2.pooler.supabase.com:5432/postgres

# Direct connection
DATABASE_URL=jdbc:postgresql://postgres:yourpassword@db.abc123xyz.supabase.co:5432/postgres
```

#### Asia Pacific (Tokyo) - ap-northeast-1
```bash
# Transaction mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-ap-northeast-1.pooler.supabase.com:6543/postgres

# Session mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-ap-northeast-1.pooler.supabase.com:5432/postgres

# Direct connection
DATABASE_URL=jdbc:postgresql://postgres:yourpassword@db.abc123xyz.supabase.co:5432/postgres
```

#### Asia Pacific (Mumbai) - ap-south-1
```bash
# Transaction mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-ap-south-1.pooler.supabase.com:6543/postgres

# Session mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-ap-south-1.pooler.supabase.com:5432/postgres

# Direct connection
DATABASE_URL=jdbc:postgresql://postgres:yourpassword@db.abc123xyz.supabase.co:5432/postgres
```

#### South America (São Paulo) - sa-east-1
```bash
# Transaction mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-sa-east-1.pooler.supabase.com:6543/postgres

# Session mode pooler
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:yourpassword@aws-0-sa-east-1.pooler.supabase.com:5432/postgres

# Direct connection
DATABASE_URL=jdbc:postgresql://postgres:yourpassword@db.abc123xyz.supabase.co:5432/postgres
```

### Finding Your Region

**To determine your Supabase project region:**

1. **Via Dashboard:**
   - Go to **Settings** → **General**
   - Look for **Region** field

2. **Via Connection String:**
   - Go to **Settings** → **Database**
   - Check the pooler connection string
   - Region appears after `aws-0-` in the hostname

3. **Via Database Query:**
   ```sql
   SELECT current_setting('cluster_name');
   ```

### Special Characters in Passwords

If your password contains special characters, URL-encode them:

| Character | Encoded |
|-----------|---------|
| `@`       | `%40`   |
| `:`       | `%3A`   |
| `/`       | `%2F`   |
| `?`       | `%3F`   |
| `#`       | `%23`   |
| `&`       | `%26`   |
| `=`       | `%3D`   |
| `%`       | `%25`   |

**Example with special characters:**
```bash
# Original password: P@ss:word/123
# Encoded password: P%40ss%3Aword%2F123
DATABASE_URL=jdbc:postgresql://postgres.abc123xyz:P%40ss%3Aword%2F123@aws-0-eu-central-1.pooler.supabase.com:6543/postgres
```

---

## Quick Diagnostic Checklist

Use this checklist to quickly diagnose connection issues:

- [ ] **Project Status**: Is project active (not paused)?
- [ ] **Credentials**: Are `SUPABASE_PROJECT_REF` and `SUPABASE_DB_PASSWORD` correct?
- [ ] **Region**: Does region in DATABASE_URL match project region?
- [ ] **Port**: Using correct port for connection mode (6543 for transaction, 5432 for session/direct)?
- [ ] **IP Allowlist**: Is server IP allowed (or restrictions disabled)?
- [ ] **Pooler Enabled**: Is connection pooler enabled in dashboard?
- [ ] **Network**: Can you reach `pooler.supabase.com` from server?
- [ ] **Password Encoding**: Are special characters in password URL-encoded?
- [ ] **Connection Pool**: Is HikariCP pool size appropriate (5-10 for most cases)?
- [ ] **Mode Compatibility**: If using transaction mode, does app avoid prepared statements across transactions?

---

## Additional Resources

- **Supabase Documentation**: https://supabase.com/docs/guides/database/connecting-to-postgres
- **PgBouncer Documentation**: https://www.pgbouncer.org/
- **Spring Boot Datasource Config**: https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html#application-properties.data
- **HikariCP Configuration**: https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby

---

## Getting Help

If you've tried all troubleshooting steps and still have issues:

1. **Check Logs**: Review application logs for specific error messages
2. **Supabase Support**: Contact via dashboard or support@supabase.io
3. **Community**: Post in [Supabase Discord](https://discord.supabase.com/) or [GitHub Discussions](https://github.com/supabase/supabase/discussions)
4. **Render Support**: For deployment issues, contact Render support with connection error details
