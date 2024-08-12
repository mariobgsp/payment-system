# Use an official Maven image to run Maven commands
FROM maven:3.8.5-openjdk-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml and source code into the container
COPY pom.xml /app/
COPY src /app/src

# Run the Maven build (clean install)
RUN mvn clean install

FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the built application from the previous stage
COPY --from=build /app/target/ms-payment-0.0.1.jar /app/ms-payment-0.0.1.jar

# Document the port (optional, for documentation purposes)
EXPOSE 9090

# Define the entry point for the container
ENTRYPOINT ["java","-jar","ms-payment-0.0.1.jar"]