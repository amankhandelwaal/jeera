# Build stage: package the app without relying on Maven Wrapper files.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Cache dependencies first for faster rebuilds.
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline

# Copy sources and build.
COPY src ./src
RUN mvn -q clean package -DskipTests

# Run Stage: Run the app
FROM eclipse-temurin:17-jre
WORKDIR /app

# curl is needed by the container HEALTHCHECK below.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Copy the built JAR from the previous stage (wildcard survives version bumps).
COPY --from=build /app/target/jeera-*.jar app.jar
EXPOSE 8080

# Container-level health probe against the Spring Boot Actuator health endpoint.
# Shell form so ${PORT} expands at runtime (Render injects PORT; defaults to 8080).
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -fsS http://localhost:${PORT:-8080}/actuator/health || exit 1

# JAVA_OPTS lets the host tune heap/GC without rebuilding the image.
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]