# Build stage
FROM openjdk:17-jdk-slim as builder

# Install Maven
RUN apt-get update && apt-get install -y maven

WORKDIR /app

# Copy pom.xml and download dependencies
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY backend/src ./src

# Build the application
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port using environment variable
EXPOSE ${PORT:-8080}

# Add health check using PORT environment variable
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# Install curl for health check
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Run the application with production JVM settings
CMD ["sh", "-c", "java \
     -XX:+UseContainerSupport \
     -XX:MaxRAMPercentage=75.0 \
     -XX:+UseG1GC \
     -XX:+UseStringDeduplication \
     -Djava.security.egd=file:/dev/./urandom \
     -Dspring.profiles.active=production \
     -Dserver.port=${PORT:-8080} \
     -jar app.jar"]