# Stage 1: Build the application using Maven with OpenJDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
# Set the working directory inside the container
WORKDIR /app
# Copy the pom.xml file to download dependencies
COPY pom.xml .
# Download all dependencies
RUN mvn dependency:go-offline
# Copy the rest of your source code
COPY src ./src
# Package the application into a .jar file
RUN mvn package -DskipTests

# Stage 2: Create the final, lightweight image with Eclipse Temurin JRE
# --- FIX APPLIED HERE ---
# Switched to the eclipse-temurin JRE image, which is a robust alternative
# to the standard openjdk image.
FROM eclipse-temurin:21-jre
# Set the working directory
WORKDIR /app
# Copy the executable .jar file from the build stage
COPY --from=build /app/target/*.jar app.jar
# Expose the port your application runs on
EXPOSE 8080
# The command to run your application
ENTRYPOINT ["java", "-jar", "app.jar"]
