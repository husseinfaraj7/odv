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

Supabase provides two types of connection URLs that you can find in your project dashboard:

### Accessing Connection Strings

1. Log in to [Supabase Dashboard](https://app.supabase.com/)
2. Select your project
3. Navigate to **Settings** → **Database**
4. Scroll to the **Connection String** or **Connection Pooling** section

### Direct Connection

**Format**: `postgres://postgres:[PASSWORD]@db.[PROJECT_REF].supabase.co:5432/postgres`

**Where to find**: Settings → Database → Connection String → URI tab

**Use for**: Development, migrations, administrative tasks

**Example**:
```
postgres://postgres:myPassword@db.abcdefghijklmnop.supabase.co:5432/postgres
```

### Transaction Pooler (Recommended for Production)

**Format**: `postgres://postgres.[PROJECT_REF]:[PASSWORD]@aws-0-[REGION].pooler.supabase.com:6543/postgres`

**Where to find**: Settings → Database → Connection Pooling → Transaction mode

**Use for**: Production deployments, high-concurrency applications

**Example**:
```
postgres://postgres.abcdefghijklmnop:myPassword@aws-0-eu-central-1.pooler.supabase.com:6543/postgres
```

**Key differences**:
- Hostname: `aws-0-[REGION].pooler.supabase.com` (instead of `db.[PROJECT_REF].supabase.co`)
- Port: `6543` (instead of `5432`)
- Username: `postgres.[PROJECT_REF]` (instead of just `postgres`)

### Adding SSL Mode

Always append `?sslmode=require` for secure connections:

```
postgres://postgres.abcdefghijklmnop:myPassword@aws-0-eu-central-1.pooler.supabase.com:6543/postgres?sslmode=require
```

### Password Encoding

If your password contains special characters, URL-encode them:

- `@` → `%40`
- `#` → `%23`
- `$` → `%24`
- `%` → `%25`
- `&` → `%26`
- `+` → `%2B`
- `=` → `%3D`

## Spring Boot Database Configuration

### HikariCP Connection Pool

Configure connection pool settings in `application-prod.properties`:

```properties
# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# Connection Validation
spring.datasource.hikari.connection-test-query=SELECT 1
spring.datasource.hikari.validation-timeout=5000
spring.datasource.hikari.leak-detection-threshold=60000
```

### Configuration Properties

| Property | Description | Recommended Value |
|----------|-------------|-------------------|
| `maximum-pool-size` | Maximum connections in pool | 5-10 for production |
| `minimum-idle` | Minimum idle connections | 2-5 |
| `connection-timeout` | Max wait time for connection (ms) | 30000 (30 seconds) |
| `idle-timeout` | Max idle time before removal (ms) | 600000 (10 minutes) |
| `max-lifetime` | Max connection lifetime (ms) | 1800000 (30 minutes) |
| `connection-test-query` | Query to test connections | SELECT 1 |
| `validation-timeout` | Max time for validation (ms) | 5000 (5 seconds) |
| `leak-detection-threshold` | Leak detection time (ms) | 60000 (1 minute) |

### Pool Sizing Guidelines

**For Transaction Pooler** (recommended):
```properties
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
```

**For Direct Connection**:
```properties
spring.datasource.hikari.maximum-pool-size=3
spring.datasource.hikari.minimum-idle=1
```

**For Development (H2)**:
```properties
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.minimum-idle=5
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
- **Credentials**: Verify username, password, hostname, and port
- **Password Encoding**: Ensure special characters are URL-encoded
- **SSL Mode**: Include `?sslmode=require` parameter
- **Port Number**: Use 6543 for transaction pooler, 5432 for direct connection

#### 3. Connection Pool Exhaustion

**Symptom**: `PSQLException: FATAL: remaining connection slots are reserved`

**Solutions**:
- Reduce `maximum-pool-size` in HikariCP configuration
- Use transaction pooler instead of direct connection
- Check for slow queries holding connections open

#### 4. Connection Timeouts

**Symptom**: `Connection is not available, request timed out after 30000ms`

**Solutions**:
- Verify database server is accessible
- Check firewall rules and IP restrictions
- Add `connection-test-query=SELECT 1` to validate connections
- Reduce `max-lifetime` to recycle stale connections

## Best Practices

### Security
- Always use SSL connections (`sslmode=require`)
- Never commit secrets to repository
- Use environment variables for all sensitive data
- Rotate database passwords regularly

### Performance
- Use transaction pooler for production APIs
- Configure appropriate pool sizes based on load
- Monitor connection pool metrics
- Set reasonable timeouts

### Reliability
- Configure health checks at `/actuator/health`
- Enable connection leak detection
- Monitor application logs
- Test with expected load

## Support

For additional help:
- **Render**: [Render Documentation](https://render.com/docs)
- **Supabase**: [Supabase Documentation](https://supabase.com/docs)
- **Spring Boot**: [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- **HikariCP**: [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
