# ── Backend Dockerfile — Multi-Stage Build ──
# Stage 1 (builder): Compile the Java app with Maven → produces a .jar file.
# Stage 2 (runtime): Run the .jar with a minimal JRE image.
# ≈ In a Node.js project: stage 1 = npm ci && npm run build, stage 2 = copy dist/ to a slim image.

# --- Build Stage ---
# eclipse-temurin is the official OpenJDK distribution (like using node:24-alpine).
# JDK = full development kit (compiler + tools), needed to build.
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /src

# Copy Maven config and download dependencies first (Docker layer caching).
# If pom.xml hasn't changed, Docker reuses this layer — much faster rebuilds.
# ≈ COPY package.json pnpm-lock.yaml && pnpm install (before copying source).
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw ./
RUN chmod +x mvnw && ./mvnw dependency:resolve -B

# Now copy source code and build the application.
# "mvn package" compiles, runs tests, and creates a .jar in target/.
# -DskipTests skips tests in the Docker build (they run in CI separately).
# -B = batch mode (non-interactive, cleaner output).
COPY src ./src
RUN ./mvnw package -DskipTests -B

# --- Runtime Stage ---
# JRE = Java Runtime Environment only (no compiler). Much smaller image.
# ≈ Using node:24-alpine as a slim runtime image.
FROM eclipse-temurin:21-jre-alpine

# Copy the compiled .jar from the build stage.
# The .jar includes embedded Tomcat — no external app server needed.
# ≈ Copying the dist/ folder from a Vite build.
COPY --from=builder /src/target/*.jar /app/app.jar

EXPOSE 8080
# ENTRYPOINT runs this command when the container starts.
# "java -jar" starts the Spring Boot app with embedded Tomcat.
# ≈ CMD ["node", "dist/index.js"] in a Node.js Dockerfile.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
