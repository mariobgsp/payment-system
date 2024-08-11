FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/ms-invoice-0.0.1.jar .
EXPOSE 8082
ENTRYPOINT ["java","-jar","ms-invoice-0.0.1.jar"]