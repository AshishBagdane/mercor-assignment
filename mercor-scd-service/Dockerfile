# Build stage
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build with dependency verification
RUN mvn dependency:go-offline && \
    mvn clean package -DskipTests && \
    mv target/*.jar app.jar && \
    # Verify PostgreSQL driver is in the JAR
    jar tf app.jar | grep -i postgresql

# Run stage
FROM openjdk:21-slim
WORKDIR /app
COPY --from=build /app/app.jar ./app.jar

# Add debugging information
RUN apt-get update && \
    apt-get install -y curl && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Create startup script for debugging (Remove it in your relevant environments)
RUN echo '#!/bin/sh' > /app/startup.sh && \
    echo 'echo "=== Java Version ===="' >> /app/startup.sh && \
    echo 'java -version' >> /app/startup.sh && \
    echo 'echo "\n=== Classpath ===="' >> /app/startup.sh && \
    echo 'echo $CLASSPATH' >> /app/startup.sh && \
    echo 'echo "\n=== Environment Variables ===="' >> /app/startup.sh && \
    echo 'env | grep -E "DB_|SPRING_|POSTGRES"' >> /app/startup.sh && \
    echo 'echo "\n=== JAR Contents ===="' >> /app/startup.sh && \
    echo 'jar tf app.jar | grep -i "postgresql\|application.yml"' >> /app/startup.sh && \
    echo 'echo "\n=== Starting Application ===="' >> /app/startup.sh && \
    echo 'exec java -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar' >> /app/startup.sh && \
    chmod +x /app/startup.sh

# Expose both HTTP and gRPC ports
EXPOSE 8080 50051
ENTRYPOINT ["/app/startup.sh"]