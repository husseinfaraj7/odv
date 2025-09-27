# Faraj Project Backend (ODV Sicilia)

A Spring Boot 3.2.0 backend service for the ODV Sicilia website.

## Java Version Requirements

**Java 17+ Required**: This project uses Spring Boot 3.2.0, which has the following Java compatibility:

- **Minimum Version**: Java 17 (LTS)
- **Recommended Version**: Java 17 or Java 21 (LTS)
- **Tested Version**: Java 17

Spring Boot 3.2.0 is fully compatible with Java 17 and requires it as the baseline. Earlier Java versions (8, 11) are not supported.

## Development Setup

### Prerequisites
- Java 17 or later
- Maven 3.6+
- Docker (for containerized deployment)

### Local Development
```bash
# Navigate to backend directory
cd odv/backend

# Install dependencies and run tests
mvn clean install

# Run the application
mvn spring-boot:run
```

## Docker Deployment

The project includes a production-ready Dockerfile optimized for cloud deployment:

```bash
# Build the Docker image
docker build -t odv-backend .

# Run the container
docker run -p 8080:8080 odv-backend
```

### Docker Configuration
- **Build Stage**: Uses `openjdk:17-jdk-slim` for compilation
- **Runtime Stage**: Uses `eclipse-temurin:17-jre-alpine` for production
- **Health Check**: Configured for Spring Boot Actuator endpoint
- **JVM Optimization**: Container-aware settings with G1GC

## Deployment

The application is configured for deployment on Render.com and other container platforms with:
- Dynamic port binding via `PORT` environment variable
- Production JVM settings optimized for cloud environments
- Health check endpoint at `/actuator/health`

## Technology Stack

- **Spring Boot**: 3.2.0
- **Java**: 17 (LTS)
- **Database**: PostgreSQL (production), H2 (development)
- **Build Tool**: Maven
- **Container Runtime**: Docker with Alpine Linux