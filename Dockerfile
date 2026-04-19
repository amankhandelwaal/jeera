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
# Copy the built JAR from the previous stage
COPY --from=build /app/target/jeera-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# Boot up Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]