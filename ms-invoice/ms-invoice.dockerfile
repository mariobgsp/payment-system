# Use an official Maven image to run Maven commands
FROM maven:3.9-eclipse-temurin-17 AS build

# Set the working directory in the container
WORKDIR /app

# Copy the pom.xml and source code into the container
COPY pom.xml /app/
COPY src /app/src

# Run the Maven build (clean install, tests included)
RUN mvn clean install

# Start with a clean base image for the runtime
FROM eclipse-temurin:17-jre-jammy

# Install necessary libraries
RUN apt-get update && \
    apt-get install -y \
    libfreetype6 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

# Run as non-root user
RUN groupadd -r app && useradd -r -g app app

# Set the working directory in the container
WORKDIR /app

# create invoice directory
RUN mkdir -p /app/invoice

# copy file for .jrxml file
COPY src/main/resources/templates/payment_notes.jrxml /app/resources/templates/payment_notes.jrxml

# Copy the built application from the previous stage
COPY --from=build /app/target/ms-invoice-0.0.1.jar /app/ms-invoice-0.0.1.jar

RUN chown -R app:app /app

USER app

# Document the port (optional, for documentation purposes)
EXPOSE 8082

# Define the entry point for the container
ENTRYPOINT ["java","-jar","ms-invoice-0.0.1.jar"]
