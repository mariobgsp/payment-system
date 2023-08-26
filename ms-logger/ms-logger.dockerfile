FROM openjdk:17-jdk-slim
WORKDIR /app
COPY target/ms-logger-0.0.1.jar .
EXPOSE 1337
ENTRYPOINT ["java","-jar","ms-logger-0.0.1.jar"]