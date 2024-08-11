# Use an official OpenJDK image as the base image
FROM openjdk:17-jdk-slim

# Install necessary libraries
RUN apt-get update && \
    apt-get install -y \
    libfreetype6 \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*


WORKDIR /app

COPY target/ms-invoice-0.0.1.jar .

RUN mkdir -p /app/invoice

COPY src/main/resources/templates/payment_notes.jrxml /app/resources/templates/payment_notes.jrxml

EXPOSE 8082
ENTRYPOINT ["java","-jar","ms-invoice-0.0.1.jar"]