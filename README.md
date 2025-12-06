# Transaction Ingestion Service

## Overview

The **Transaction Ingestion Service** serves as the critical entry point for the **Real-Time Financial Risk Assessment & Compliance Platform**. It is responsible for securely receiving, validating, and routing financial transactions through the initial processing pipeline.

This microservice ensures that all incoming transactions meets strict structural and business integrity standards before they are propagated to downstream services via event streaming.

## Key Features

*   **Real-time Ingestion**: High-performance REST APIs to accept individual financial transactions.
*   **Comprehensive Validation**:
    *   **Structural**: Checks for format, data types, and required fields.
    *   **Business Logic**: Verifies customer status, limits, and merchant validity.
    *   **Schema Compliance**: Ensures adherence to the defined data model.
*   **Event-Driven Architecture**: Asynchronously publishes transaction events (`transaction-received`, `transaction-rejected`) to **Apache Kafka** for decoupled processing.
*   **Bulk Processing**: Supports batch transaction uploads for high-volume data ingestion.
*   **Service Discovery**: Fully integrated with **Netflix Eureka** for dynamic service registration and discovery.
*   **API Documentation**: Built-in **Swagger UI** for easy API exploration and testing.

## Technology Stack

*   **Language**: Java 17
*   **Framework**: Spring Boot 3.5.7
*   **Database**: MongoDB (Primary data store for transactions and logs)
*   **Messaging**: Apache Kafka (Event streaming)
*   **Service Discovery**: Netflix Eureka Client
*   **Build Tool**: Maven

## Getting Started

### Prerequisites

Ensure you have the following installed and running:
*   Java 17 SDK
*   Maven 3.8+
*   MongoDB (running locally or accessible remotely)
*   Apache Kafka (Zookeeper & Broker running)
*   Eureka Discovery Server (running on default port 8761)

### Configuration

The service is configured via `src/main/resources/application.yaml`. Key configurations include:

*   **Server Port**: Configured to `0` (random port) for Eureka compatibility.
*   **MongoDB**: `spring.data.mongodb.uri`
*   **Kafka**: `spring.kafka.bootstrap-servers`
*   **Eureka**: `eureka.client.service-url.defaultZone`

### Installation & Running

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    cd transaction-ingestion-service
    ```

2.  **Build the project:**
    ```bash
    mvn clean install
    ```

3.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```
    *Or using the built jar:*
    ```bash
    java -jar target/transaction-ingestion-service-0.0.1-SNAPSHOT.jar
    ```

## API Documentation

Once the application is running, you can access the interactive API documentation via Swagger UI:

```
http://localhost:<port>/swagger-ui.html
```
*(Note: Check the console logs for the assigned random port)*

### Core Endpoints

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/v1/transactions/ingest` | Ingest a single transaction |
| `POST` | `/api/v1/transactions/bulk-upload` | Upload a batch of transactions |

## Architecture

The service follows a clean, layered architecture:
1.  **Controller Layer**: Handles HTTP requests and response formatting.
2.  **Service Layer**: Contains business logic, validation rules, and orchestration.
3.  **Repository Layer**: Manages data persistence with MongoDB.
4.  **Event Layer**: Handles Kafka message production for system-wide events.

For more detailed information, please refer to:
*   [Business Functionality](BUSINESS_FUNCTIONALITY.md)
*   [Technical Documentation](TECHNICAL_DOCUMENTATION.md)
