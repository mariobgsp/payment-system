# Use an official Maven image to run Maven commands
FROM maven:3.8.5-openjdk-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml and source code into the container
COPY pom.xml /app/
COPY src /app/src

# Run the Maven build (clean install)
RUN mvn clean install

# Start with a clean base image for the runtime
FROM openjdk:17-jdk-slim

# Install necessary libraries
RUN apt-get update && \
    apt-get install -y \
    libfreetype6 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Set the working directory in the container
WORKDIR /app

# create invoice directory
RUN mkdir -p /app/invoice

# copy file for .jrxml file
COPY src/main/resources/templates/payment_notes.jrxml /app/resources/templates/payment_notes.jrxml


# Copy the built application from the previous stage
COPY --from=build /app/target/ms-invoice-0.0.1.jar /app/ms-invoice-0.0.1.jar

# Document the port (optional, for documentation purposes)
EXPOSE 8082

# Define the entry point for the container
ENTRYPOINT ["java","-jar","ms-invoice-0.0.1.jar"]