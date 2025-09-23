# Faraj Project - Deployment Guide

This document provides comprehensive instructions for deploying the Spring Boot backend application to Render.com, including Supabase database setup, environment configuration, and troubleshooting.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Docker Configuration](#docker-configuration)
- [Database Setup](#database-setup)
- [Service Creation on Render](#service-creation-on-render)
- [Environment Variables Configuration](#environment-variables-configuration)
- [Deployment Process](#deployment-process)
- [Troubleshooting](#troubleshooting)
- [Monitoring and Health Checks](#monitoring-and-health-checks)

## Prerequisites

Before deploying, ensure you have:
- Render.com account
- Supabase account and project
- GitHub repository with the application code
- Access to required API keys and credentials
- Basic understanding of Docker and PostgreSQL

## Docker Configuration

The application uses a multi-stage Docker build process defined in the `Dockerfile`:

### Build Stage
- Uses `openjdk:17-jdk-slim` with Maven
- Downloads dependencies using `mvn dependency:go-offline`
- Builds the application with `mvn clean package -DskipTests`

### Runtime Stage
- Uses `eclipse-temurin:17-jre` for production
- Exposes port 8080
- Runs the Spring Boot JAR file

### Dockerfile Optimization
The current Dockerfile is optimized for:
- Minimal runtime image size
- Dependency caching
- Production-ready Java runtime

## Database Setup

### Supabase Database Setup

1. **Create Supabase Project**
   - Go to [Supabase Dashboard](https://supabase.com/dashboard)
   - Click "New project"
   - Choose organization and provide project details

2. **Configure Database Settings**
   - **Project Name**: `faraj-project`
   - **Database Password**: Choose a secure password
   - **Region**: Choose closest to your deployment region
   - **Pricing Plan**: Select appropriate plan (Free tier available)

3. **Database Security and Access**
   - Database is automatically secured with SSL/TLS
   - Connection pooling handled by Supabase
   - Access controlled via Row Level Security (RLS) policies

## Service Creation on Render

### Step-by-Step Service Setup

1. **Create Web Service**
   - Click "New +" → "Web Service"
   - Connect your GitHub repository
   - Select the repository containing your application

2. **Basic Configuration**
   - **Name**: `faraj-project-backend`
   - **Environment**: `Docker`
   - **Region**: Same as database region
   - **Branch**: `main` (or your production branch)

3. **Build Configuration**
   - **Dockerfile Path**: `./Dockerfile`
   - **Build Command**: Leave empty (handled by Dockerfile)
   - **Start Command**: Leave empty (handled by Dockerfile ENTRYPOINT)

4. **Advanced Settings**
   - **Auto Deploy**: Enable for automatic deployments on git push
   - **Health Check Path**: `/actuator/health` (if Spring Actuator is enabled)
   - **HTTP Port**: `8080`

## Environment Variables Configuration

Configure the following environment variables in your Render service settings:

### Database Configuration

#### DATABASE_URL
- **Purpose**: PostgreSQL connection string for Supabase database
- **Configuration**: Manual configuration from Supabase project settings
- **Security Setting**: `sync: false` (sensitive)
- **Format**: `postgresql://postgres:[PASSWORD]@db.[PROJECT_REF].supabase.co:5432/postgres`
- **Source**: Supabase project settings → Database → Connection string → URI

### Supabase Configuration

#### SUPABASE_ANON_KEY
- **Purpose**: Supabase anonymous/public key for client-side operations
- **Security Setting**: `sync: false` (sensitive)
- **Source**: Supabase project settings → API → Project API keys
- **Usage**: Used for public API access from frontend

#### SUPABASE_ROLE_KEY
- **Purpose**: Supabase service role key for server-side operations with elevated privileges
- **Security Setting**: `sync: false` (highly sensitive)
- **Source**: Supabase project settings → API → Project API keys
- **Usage**: Server-side operations, user management, bypassing RLS

### Email Configuration

#### ADMIN_EMAIL
- **Purpose**: Administrator email address for notifications and system messages
- **Security Setting**: `sync: false` (contains contact information)
- **Example**: `info@odvsicilia.it`
- **Usage**: Recipient for order notifications, contact form submissions

#### BREVO_API_KEY
- **Purpose**: Brevo (formerly Sendinblue) API key for sending emails
- **Security Setting**: `sync: false` (sensitive API credential)
- **Source**: Brevo Dashboard → SMTP & API → API Keys
- **Usage**: Sending transactional emails, order confirmations

### Additional Variables (Auto-configured)

The following variables are automatically set via `render.yaml`:
- `SUPABASE_URL`: Supabase project URL
- `BREVO_SMTP_SERVER`: SMTP server hostname
- `BREVO_SMTP_PORT`: SMTP port (587)
- `BREVO_SMTP_USERNAME`: SMTP username
- `BREVO_SENDER_EMAIL`: From email address
- `BREVO_SENDER_NAME`: From name
- `FRONTEND_URL`: Frontend application URL

### Security Best Practices

- **sync: false Variables**: These are not synchronized between environments and require manual configuration
- **API Keys**: Rotate periodically and never commit to version control
- **Database Credentials**: Use Render's automatic connection string generation
- **Email Credentials**: Use environment-specific API keys

## Deployment Process

### Automatic Deployment (Recommended)

1. **Enable Auto-Deploy**
   - In Render service settings, enable "Auto Deploy"
   - Select your main/production branch
   - Deployments will trigger on every push to the selected branch

2. **Deployment Workflow**
   ```
   git push origin main
   ↓
   Render detects push
   ↓
   Docker build starts
   ↓
   Dependencies downloaded
   ↓
   Application built
   ↓
   Container deployed
   ↓
   Health checks pass
   ↓
   Traffic routed to new deployment
   ```

### Manual Deployment

1. **Trigger Manual Deploy**
   - Go to Render service dashboard
   - Click "Manual Deploy" → "Deploy latest commit"
   - Monitor deployment logs in real-time

### Deployment Configuration File

The `render.yaml` file in the repository root automates the deployment configuration:

```yaml
services:
  - type: web
    name: faraj-project-backend
    env: docker
    dockerfilePath: ./Dockerfile
    envVars:
      # Database connection must be configured manually from Supabase
      - key: DATABASE_URL
        sync: false
      # Sensitive variables requiring manual configuration
      - key: SUPABASE_ANON_KEY
        sync: false
      - key: SUPABASE_ROLE_KEY
        sync: false
      - key: ADMIN_EMAIL
        sync: false
      # Other configurations...

# Note: No database service defined as using external Supabase database
```

## Troubleshooting

### Database Connection Failures

#### Symptom
```
Application failed to start: Could not connect to database
Connection refused / Connection timeout
```

#### Solutions
1. **Verify Supabase Database Status**
   - Check Supabase project status in dashboard
   - Ensure database is healthy and accessible
   - Verify project is not paused or suspended

2. **Check Connection String**
   - Verify `DATABASE_URL` environment variable is set correctly
   - Ensure it follows Supabase format: `postgresql://postgres:[PASSWORD]@db.[PROJECT_REF].supabase.co:5432/postgres`
   - Check for special characters in password that need URL encoding
   - Verify the PROJECT_REF matches your Supabase project

3. **Network and Authentication Issues**
   - Verify Supabase project allows external connections
   - Check database password is correct
   - Ensure connection pooling is properly configured
   - Verify SSL/TLS settings are compatible

#### Database Connection Test
```bash
# Test connection from local environment using Supabase connection string
psql "postgresql://postgres:[YOUR_PASSWORD]@db.[PROJECT_REF].supabase.co:5432/postgres"
```

### Health Check Timeouts

#### Symptom
```
Service unhealthy: Health check timeout
Deployment failed: Service did not respond to health checks
```

#### Solutions
1. **Verify Application Startup**
   - Check deployment logs for startup errors
   - Ensure port 8080 is exposed and application binds to it
   - Verify no blocking operations during startup

2. **Add Health Check Endpoint**
   Add Spring Boot Actuator dependency to `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-actuator</artifactId>
   </dependency>
   ```
   
   Configure in `application.properties`:
   ```properties
   management.endpoints.web.exposure.include=health
   management.endpoint.health.show-details=always
   ```

3. **Adjust Health Check Settings**
   - Increase health check timeout in Render settings
   - Set health check path to `/actuator/health`
   - Consider adding custom readiness probes

### Environment Variable Configuration Problems

#### Symptom
```
Configuration property 'xyz' not found
NullPointerException accessing environment variable
```

#### Solutions
1. **Missing Variables**
   - Verify all required environment variables are set
   - Check for typos in variable names
   - Ensure sensitive variables have `sync: false` configured

2. **Variable Precedence Issues**
   - Environment variables override application.properties
   - Check for conflicting values between environments
   - Use `@Value` annotations with default values

3. **Sensitive Variable Access**
   - Variables with `sync: false` must be configured manually
   - Cannot be accessed via Render API or CLI
   - Must be set through Render dashboard

#### Environment Variable Debugging
Add to `application.properties`:
```properties
# Enable debug logging for configuration
logging.level.org.springframework.boot.context.config=DEBUG
```

### Build Failures

#### Symptom
```
Docker build failed
Maven compilation errors
Dependency resolution failed
```

#### Solutions
1. **Maven Dependency Issues**
   ```bash
   # Clear local Maven cache
   ./mvnw dependency:purge-local-repository
   ./mvnw clean compile
   ```

2. **Docker Build Cache**
   - Force rebuild by changing Dockerfile
   - Clear Docker build cache in Render (if available)
   - Verify base image availability

3. **Java Version Mismatch**
   - Ensure Docker uses Java 17
   - Verify Maven compiler target matches
   - Check Spring Boot version compatibility

### Memory and Performance Issues

#### Symptom
```
OutOfMemoryError
Application slow response times
Service crashes under load
```

#### Solutions
1. **JVM Memory Configuration**
   Add to Dockerfile ENTRYPOINT:
   ```dockerfile
   ENTRYPOINT ["java", "-Xmx512m", "-Xms256m", "-jar", "app.jar"]
   ```

2. **Database Connection Pool**
   Configure in `application.properties`:
   ```properties
   spring.datasource.hikari.maximum-pool-size=5
   spring.datasource.hikari.connection-timeout=30000
   ```

3. **Resource Limits**
   - Upgrade Render plan for more memory/CPU
   - Optimize database queries
   - Implement caching where appropriate

## Monitoring and Health Checks

### Application Monitoring

1. **Render Metrics**
   - CPU and memory usage graphs
   - Request/response metrics
   - Error rate monitoring

2. **Application Logs**
   - Access via Render dashboard
   - Configure log levels in `application.properties`
   - Use structured logging for better analysis

3. **Database Monitoring**
   - Connection pool metrics
   - Query performance
   - Database size and growth

### Setting Up Alerts

1. **Service Alerts**
   - Configure in Render dashboard
   - Set up email/Slack notifications
   - Monitor deployment failures

2. **Custom Health Checks**
   ```java
   @Component
   public class CustomHealthIndicator implements HealthIndicator {
       @Override
       public Health health() {
           // Custom health logic
           return Health.up().build();
       }
   }
   ```

### Production Monitoring Checklist

- [ ] Service status monitoring enabled
- [ ] Database connection monitoring
- [ ] Email service health checks
- [ ] Error rate alerts configured
- [ ] Performance metrics baseline established
- [ ] Log aggregation and analysis
- [ ] Backup and disaster recovery plan

## Security Considerations

### Environment Security
- Use `sync: false` for all sensitive variables
- Rotate API keys regularly
- Limit database access to necessary services only
- Enable SSL/TLS for all connections

### Application Security
- Implement CORS policies
- Validate all input data
- Use parameterized database queries
- Keep dependencies updated

### Monitoring Security
- Monitor for unusual access patterns
- Set up alerts for failed authentication
- Regularly audit environment variable access
- Implement rate limiting for APIs

---

For additional support, refer to:
- [Render Documentation](https://render.com/docs)
- [Spring Boot Production Guide](https://docs.spring.io/spring-boot/docs/current/reference/html/deployment.html)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)