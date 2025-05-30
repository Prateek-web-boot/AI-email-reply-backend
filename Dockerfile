# Stage 1: Build the application
# Use a JDK image suitable for your Java version (e.g., 21, 17, 11)
FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /app

# Copy Maven wrapper and pom.xml first to leverage Docker layer caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# --- ADD THIS LINE ---
RUN chmod +x mvnw # Grant execute permissions to the Maven wrapper script
# --- END ADDITION ---

# Copy src and build the application
COPY src ./src

# Build the application. Use --mount=type=cache for faster builds on subsequent pushes.
# If using Gradle, replace the RUN command with: RUN --mount=type=cache,target=/root/.gradle ./gradlew clean build -x test
RUN --mount=type=cache,target=/root/.m2 ./mvnw clean package -DskipTests

# Stage 2: Create the final lean image
# Use a JRE image (smaller than JDK) for running the app
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy only the built JAR file from the builder stage
# The wildcard *.jar ensures it works regardless of the exact version in the filename
COPY --from=builder /app/target/*.jar app.jar

# Expose the port your Spring Boot app listens on (default is 8080)
EXPOSE 8080

# Command to run your application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]