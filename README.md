# payment-system

## Overview
Repository that containing multiple microservices for payment system. This repository containing:
* ms-logger microservices that handles log of any service action such as order, inquiry, and payment
* ms-order microservices that handles product inquiry and order
* ms-payment microservices that handles product payment
* ms-paymentagr dummy microservice using golang and redis for payment (payment partner example)
* ms-invoice provisions by generating document report

## Tech Stack
Tech stack being use in this repository
* Java Spring 17 - Main framework
* Postgre SQL - JDBC Connectivity - Main Database
* Mongo DB - NOSQL - Logger Database
* Kafka - Event Publisher for ms-logger and any other services
* Docker - Containerization and deploying service at once (excluding ms-paymentagr) using compose
* Go - As another language for microservices, used at ms-paymentagr
* Redis - Temp database for Go Microservices

## Design
* [Sequence Diagram Design](https://github.com/mariobgsp/ms-java/blob/master/payment-system-sequence-diagram.png "Sequence Diagram Design")
* [Payment System Diagram](payment-system-sequence-diagram "Payment System Diagram")


## Getting Started

### 1. Clone the Repository

Start by cloning the repository to your local machine:

```bash
git clone https://github.com/mariobgsp/payment-system.git
```

Navigate to the project directory:

```bash
cd payment-system
```

### 2. Navigate to the Project Directory

Ensure you are in the correct directory where the \`docker-compose.yml\` file is located:

```bash
cd projects
```

### 3. Build and Start the Containers

Use Docker Compose to build the images and start the containers:

```bash
docker-compose up --build
```

This command will:

- Build the Docker images as defined in the \`docker-compose.yml\` file.
- Start all the services defined in the file.

### 4. Accessing the Services

Once the containers are up and running, you can access the services:

- **ms-logger**: [http://localhost:8080](http://localhost:8080).
- **ms-order**: [http://localhost:1337](http://localhost:1337).
- **ms-payment**: [http://localhost:9090](http://localhost:9090).
- **ms-paymentagr**: [http://localhost:8081](http://localhost:8081).
- **ms-invoice**: [http://localhost:8082](http://localhost:8082).

Api Documentation:
ms-order and ms-payment documentation: [API Documentation: payment-system](https://www.notion.so/mariobgsp/API-Documentation-payment-sistem-d422ba229b184210af6b865ec8991ffd?pvs=4 "API Documentation: payment-system")

Check your `docker-compose.yml` file for the correct ports.

### 5. Stopping the Services

To stop the running services, press \`CTRL+C\` in the terminal where \`docker-compose up\` is running, or use the following command:

```bash
docker-compose down
```

This command stops and removes all containers defined in the \`docker-compose.yml\` file.

### 6. Build and Test Github Action already works (Optional)

### End of The Documentation
