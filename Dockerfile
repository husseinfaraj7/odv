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
FROM openjdk:17-jre-slim

WORKDIR /app

# Copy the built JAR from build stage
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]