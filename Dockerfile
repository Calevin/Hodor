# Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml to cache dependencies properly
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Make sure the wrapper is executable
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy the actual source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre-alpine AS runtime

# Create non-root user and group
RUN addgroup -S hodor && adduser -S hodor -G hodor

WORKDIR /app

# Copy the built jar from the builder stage
COPY --from=builder --chown=hodor:hodor /app/target/*.jar app.jar

# Switch to the non-root user
USER hodor

# Expose the application port
EXPOSE 8080

# Healthcheck configuration using an internal endpoint (Actuator)
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
