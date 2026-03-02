# ── Backend Dockerfile — Multi-Stage Build ──
# Stage 1 (builder): Compile the Java app with Maven → produces a .jar file.
# Stage 2 (runtime): Run the .jar with a minimal JRE image.

# --- Build Stage ---
# JDK needed for compilation.
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /src

# Copy Maven config and download dependencies first (Docker layer caching).
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
RUN chmod +x mvnw && ./mvnw dependency:resolve -B

# Copy source and build (-DskipTests: tests run in CI separately).
COPY src ./src
RUN ./mvnw package -DskipTests -B

# --- Runtime Stage ---
# JRE only (no compiler) — smaller image.
FROM eclipse-temurin:21-jre-alpine

# Copy the compiled .jar from the build stage.
COPY --from=builder /src/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
