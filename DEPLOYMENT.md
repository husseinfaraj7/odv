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
