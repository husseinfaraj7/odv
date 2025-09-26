# Production-ready Dockerfile for Faraj Project (ODV Sicilia)
# Spring Boot 3.2.0 requires Java 17+ - fully compatible with Java 17 LTS
# Optimized for deployment on Render.com and other container platforms

# Build stage
FROM openjdk:17-jdk-slim as builder

# Install Maven and clean up to reduce layer size
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy pom.xml and download dependencies
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY backend/src ./src

# Build the application
RUN mvn clean package -Dmaven.test.skip=true

# Runtime stage - Using JRE for smaller footprint
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=builder /app/target/*.jar app.jar

# Install curl for health check
RUN apk add --no-cache curl

# Expose port using environment variable (supports both PORT and default)
EXPOSE ${PORT:-8080}

# Add health check using PORT environment variable
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# Run the application with production JVM settings optimized for cloud deployment
CMD ["sh", "-c", "java \
     -XX:+UseContainerSupport \
     -XX:MaxRAMPercentage=75.0 \
     -XX:+UseG1GC \
     -XX:+UseStringDeduplication \
     -XX:+OptimizeStringConcat \
     -Djava.security.egd=file:/dev/./urandom \
     -Dspring.profiles.active=prod \
     -Dserver.port=${PORT:-8080} \
     -jar app.jar"]