FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the JAR file into the container
COPY target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]

ENTRYPOINT ["java", "-jar", "app.jar"]
