FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/ms-order-0.0.1.jar .
EXPOSE 8080
ENTRYPOINT ["java","-jar","ms-order-0.0.1.jar"]