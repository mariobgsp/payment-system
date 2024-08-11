# payment-system

## overview
Repository that containing multiple microservices for payment system. This repository containing:
* ms-logger microservices that handles log of any service action such as order, inquiry, and payment
* ms-order microservices that handles product inquiry and order
* ms-payment microservices that handles product payment
* ms-paymentagr dummy microservice using golang and redis for payment (payment partner example)

## tech stack
Tech stack being use in this repository
* Java Spring 17 - Main framework
* Postgre SQL - JDBC Connectivity - Main Database
* Mongo DB - NOSQL - Logger Database
* Kafka - Event Publisher for ms-logger and any other services
* Docker - Containerization and deploying service at once (excluding ms-paymentagr) using compose
* Go - As another language for microservices, used at ms-paymentagr
* Redis - Temp database for Go Microservices

## design
* [Sequence Diagram Design](https://github.com/mariobgsp/ms-java/blob/master/payment-system-sequence-diagram.png "Sequence Diagram Design")
* [Payment System Diagram](payment-system-sequence-diagram "Payment System Diagram")
