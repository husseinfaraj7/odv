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

### Common Issues

#### 1. Build Failures
- **Maven Dependencies**: Ensure all dependencies in `pom.xml` are available
- **Java Version**: Verify Java 17 compatibility
- **Memory Issues**: Check if build requires more memory

#### 2. Database Connection Issues
- **URL Encoding**: Ensure special characters in passwords are properly encoded
- **Network Access**: Verify database allows connections from Render IPs
- **Credentials**: Double-check username, password, and database name

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
3. **Test Database Connection**: Verify DATABASE_URL is accessible
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
