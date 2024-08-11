FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/ms-payment-0.0.1.jar .
EXPOSE 9090
ENTRYPOINT ["java","-jar","ms-payment-0.0.1.jar"]