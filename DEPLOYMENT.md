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
| `DATABASE_URL` | PostgreSQL connection string | `postgres://user:pass@host:5432/db` |
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
3. Copy the connection string (see [Obtaining Connection URLs](#obtaining-supabase-connection-urls) below)
4. Update the `DATABASE_URL` in Render with your Supabase credentials
5. Ensure your password is properly URL-encoded if it contains special characters

## Obtaining Supabase Connection URLs

Supabase provides two types of connection URLs: **Direct** and **Pooler**. Choose based on your deployment needs.

### Accessing Connection URLs in Supabase Dashboard

1. Log in to [Supabase Dashboard](https://app.supabase.com/)
2. Select your project
3. Navigate to **Settings** → **Database**
4. Scroll to the **Connection String** section

### Direct Connection URL

**Use for**: Development, administrative tasks, long-running processes

**Where to find**:
- In the Supabase dashboard under **Connection String** → **URI** tab
- Look for the connection string starting with `postgresql://`

**Format**:
```
postgres://postgres:[PASSWORD]@db.[PROJECT_REF].supabase.co:5432/postgres
```

**Example**:
```
postgres://postgres:mySecurePassword@db.abcdefghijklmnop.supabase.co:5432/postgres?sslmode=require
```

**Characteristics**:
- Direct connection to PostgreSQL database
- Port: **5432**
- No connection pooling
- Hostname pattern: `db.[PROJECT_REF].supabase.co`
- Best for low-concurrency scenarios

### Pooler Connection URL (Recommended for Production)

**Use for**: Production deployments, API servers, high-concurrency applications

**Where to find**:
- In the Supabase dashboard under **Connection Pooling** section
- Look for **Transaction Mode** or **Session Mode** connection strings

**Transaction Mode (Recommended)**:
```
postgres://postgres.[PROJECT_REF]:[PASSWORD]@aws-0-[REGION].pooler.supabase.com:6543/postgres
```

**Example**:
```
postgres://postgres.abcdefghijklmnop:mySecurePassword@aws-0-eu-central-1.pooler.supabase.com:6543/postgres?sslmode=require
```

**Session Mode**:
```
postgres://postgres.[PROJECT_REF]:[PASSWORD]@aws-0-[REGION].pooler.supabase.com:5432/postgres
```

**Example**:
```
postgres://postgres.abcdefghijklmnop:mySecurePassword@aws-0-eu-central-1.pooler.supabase.com:5432/postgres?sslmode=require
```

**Characteristics**:
- Uses PgBouncer connection pooling
- Transaction mode port: **6543**
- Session mode port: **5432**
- Hostname pattern: `aws-0-[REGION].pooler.supabase.com`
- Username includes project reference: `postgres.[PROJECT_REF]`
- Best for high-concurrency scenarios

### Choosing Between Connection Modes

| Connection Type | Port | Best For | Avoid For |
|----------------|------|----------|-----------|
| **Direct** | 5432 | Development, admin tasks, migrations | Production APIs, serverless |
| **Transaction Pooler** | 6543 | Production APIs, stateless apps, high concurrency | Apps using prepared statements across transactions |
| **Session Pooler** | 5432 | Apps requiring session state, ORM frameworks | Maximum connection efficiency needs |

### Adding SSL Mode Parameter

Always include `sslmode=require` for secure connections:

```
postgres://user:password@host:port/database?sslmode=require
```

Common SSL modes:
- `require` - Requires SSL (recommended for production)
- `disable` - No SSL (only for local development)
- `prefer` - Use SSL if available

## Spring Boot HikariCP Configuration

Configure connection pool settings in `application-prod.properties`:

```properties
# HikariCP Connection Pool Configuration
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000
spring.datasource.hikari.leak-detection-threshold=60000

# Connection Validation
spring.datasource.hikari.connection-test-query=SELECT 1
spring.datasource.hikari.validation-timeout=5000

# Pool Behavior
spring.datasource.hikari.auto-commit=true
spring.datasource.hikari.pool-name=HikariPool-ODV
```

### Configuration Properties Explained

| Property | Description | Recommended Value |
|----------|-------------|-------------------|
| `maximum-pool-size` | Maximum number of connections in pool | 5-10 for production |
| `minimum-idle` | Minimum idle connections maintained | 2-5 |
| `connection-timeout` | Max time to wait for connection (ms) | 30000 (30 seconds) |
| `idle-timeout` | Max time connection can sit idle (ms) | 600000 (10 minutes) |
| `max-lifetime` | Max lifetime of connection in pool (ms) | 1800000 (30 minutes) |
| `leak-detection-threshold` | Connection leak detection time (ms) | 60000 (1 minute) |
| `connection-test-query` | Query to validate connections | SELECT 1 |
| `validation-timeout` | Max time for validation query (ms) | 5000 (5 seconds) |

### Environment-Specific Pool Sizing

**Development (H2 in-memory)**:
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
```

**Production (Supabase pooler)**:
```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
```

**Production (Supabase direct)**:
```properties
spring.datasource.hikari.maximum-pool-size=3
spring.datasource.hikari.minimum-idle=1
```

### Database URL Encoding

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
Original: postgres://user:P@ssw0rd#123@host:5432/db
Encoded:  postgres://user:P%40ssw0rd%23123@host:5432/db
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

### Common Issues

#### 1. Build Failures
- **Maven Dependencies**: Ensure all dependencies in `pom.xml` are available
- **Java Version**: Verify Java 17 compatibility
- **Memory Issues**: Check if build requires more memory

#### 2. Database Connection Issues
- **Hostname Format**: Verify using correct hostname pattern (see [Connection URLs](#obtaining-supabase-connection-urls))
- **Network Access**: Verify database allows connections from Render IPs
- **Credentials**: Double-check username format (e.g., `postgres.[PROJECT_REF]` for pooler)
- **Password Encoding**: Ensure special characters are URL-encoded
- **Port Number**: Use 6543 for transaction pooler, 5432 for direct/session
- **SSL Mode**: Include `?sslmode=require` parameter

#### 3. Connection Pool Exhaustion

**Symptom**:
```
PSQLException: FATAL: remaining connection slots are reserved
```

**Solutions**:
- Reduce `maximum-pool-size` in HikariCP configuration
- Use transaction pooler instead of direct connection
- Optimize slow queries to reduce connection hold time
- Upgrade Supabase plan for more connections

#### 4. Connection Validation Failures

**Symptom**:
```
Connection is not available, request timed out after 30000ms
```

**Solutions**:
- Add `connection-test-query=SELECT 1` to validate connections
- Reduce `max-lifetime` to recycle connections more frequently
- Check database server status and connectivity
- Verify firewall/IP restrictions are not blocking connections

## Best Practices

### Security
- Always use SSL connections (`sslmode=require`)
- Never commit secrets to repository
- Use environment variables for all sensitive data
- Rotate database passwords regularly
- Enable Supabase RLS (Row Level Security) policies

### Performance
- Use transaction pooler for production APIs
- Configure appropriate pool sizes based on load
- Enable connection validation with test queries
- Monitor connection pool metrics
- Set reasonable timeouts to prevent resource exhaustion

### Reliability
- Configure health checks at `/actuator/health`
- Set appropriate connection timeouts
- Enable leak detection for troubleshooting
- Monitor application logs for connection warnings
- Test connection recovery after network interruptions

## Support

For additional help:
- **Render**: [Render Documentation](https://render.com/docs)
- **Supabase**: [Supabase Documentation](https://supabase.com/docs)
- **Spring Boot**: [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- **HikariCP**: [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
