# Faraj Project - Deployment Guide

This document provides comprehensive instructions for deploying the Spring Boot backend application to Render.com, including database setup, environment configuration, and troubleshooting.

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

### PostgreSQL Database Creation

1. **Navigate to Render Dashboard**
   - Go to [Render.com Dashboard](https://dashboard.render.com)
   - Click "New +" and select "PostgreSQL"

2. **Configure Database Settings**
   - **Name**: `faraj-project-db`
   - **Database Name**: `farajproject`
   - **User**: `farajproject`
   - **Region**: Choose closest to your users (e.g., Frankfurt for EU)
   - **PostgreSQL Version**: Use latest stable (15+)
   - **Plan**: Select appropriate plan (Free tier available)

3. **Database Security**
   - Database will be automatically secured with SSL
   - Connection string will be auto-generated
   - Access restricted to Render services by default

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
- **Purpose**: PostgreSQL connection string for production database
- **Configuration**: Auto-populated from database service
- **Security Setting**: `sync: true` (default)
- **Format**: `postgresql://username:password@host:port/database`

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
      # Database connection auto-configured
      - key: DATABASE_URL
        fromDatabase:
          name: faraj-project-db
          property: connectionString
      # Sensitive variables requiring manual configuration
      - key: SUPABASE_ANON_KEY
        sync: false
      - key: SUPABASE_ROLE_KEY
        sync: false
      - key: ADMIN_EMAIL
        sync: false
      # Other configurations...

databases:
  - name: faraj-project-db
    databaseName: farajproject
    user: farajproject
```

## Troubleshooting

### Database Connection Failures

#### Symptom
```
Application failed to start: Could not connect to database
Connection refused / Connection timeout
```

#### Solutions
1. **Verify Database Status**
   - Check database service status in Render dashboard
   - Ensure database is in "Available" state
   - Verify same region as web service

2. **Check Connection String**
   - Verify `DATABASE_URL` environment variable is set
   - Ensure it follows format: `postgresql://user:pass@host:port/db`
   - Check for special characters in password that need URL encoding

3. **Network Issues**
   - Both services must be in same Render region
   - Check Render status page for ongoing issues
   - Verify no firewall restrictions

#### Database Connection Test
```bash
# Test connection from local environment
psql "$DATABASE_URL"
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