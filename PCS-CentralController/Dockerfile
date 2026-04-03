FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY . .
RUN ./gradlew :PCS-API:build :PCS-CentralController:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy application
COPY --from=builder /app/PCS-CentralController/build/libs/*.jar app.jar

# Create logs directory
RUN mkdir -p logs

# Expose ports
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
