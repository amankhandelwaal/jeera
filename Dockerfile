# Build Stage: Package the app
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY . .
# Make the Maven wrapper executable
RUN chmod +x mvnw
# Build the JAR file
RUN ./mvnw clean package -DskipTests

# Run Stage: Run the app
FROM eclipse-temurin:17-jre
WORKDIR /app
# Copy the built JAR from the previous stage
COPY --from=build /app/target/jeera-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
# Boot up Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]