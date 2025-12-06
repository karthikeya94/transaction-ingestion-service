# Transaction Ingestion Service - Technical Documentation

## System Architecture

### Overview
The Transaction Ingestion Service is a Spring Boot microservice that implements the transaction ingestion functionality for the Real-Time Financial Risk Assessment & Compliance Platform. It follows a layered architecture with clear separation of concerns between controllers, services, and data access layers.

### Technology Stack
- **Framework**: Spring Boot 3.5.7
- **Language**: Java 17
- **Database**: MongoDB (Primary data store)
- **Messaging**: Apache Kafka (Event streaming)
- **API Documentation**: OpenAPI/Swagger
- **Build Tool**: Maven
- **Service Discovery**: Netflix Eureka

## Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── com/transaction/ingestion/service/
│   │       ├── TransactionIngestionServiceApplication.java
│   │       ├── config/
│   │       ├── controller/
│   │       ├── dto/
│   │       ├── exception/
│   │       ├── model/
│   │       ├── repository/
│   │       └── service/
│   └── resources/
│       ├── application.yaml
│       └── static/
└── test/
    └── java/
```

## Components

### 1. Controllers
Located in `com.transaction.ingestion.service.controller`

#### TransactionsController
- **Path**: `/api/v1/transactions`
- **Endpoints**:
  - `POST /ingest` - Process individual transaction
  - `GET /{transactionId}` - Retrieve transaction details

### 2. Services
Located in `com.transaction.ingestion.service.service`

#### TransactionService
Core service responsible for:
- Transaction validation (structural, business, schema)
- Transaction persistence to MongoDB
- Kafka event publishing
- Response construction

#### AdvancedValidationService
Handles asynchronous validation including:
- KYC status verification
- Velocity checking
- Transaction pattern analysis
- AML/sanctions screening

#### AsyncValidationProcessor
Kafka consumer that processes `transaction-received` events:
- Performs advanced validation asynchronously
- Updates transaction data with validation results
- Publishes validation outcomes to Kafka

#### AMLSanctionsService
Responsible for:
- Merchant sanctions checking
- Country sanctions verification
- External AML service integration (with circuit breaker)

#### CustomerService
Provides customer-related business logic:
- Customer status verification
- Blacklist checking
- Customer limit retrieval

#### KafkaProducerService
Manages Kafka message production:
- Generic message sending capability
- Error handling and logging

### 3. Data Transfer Objects (DTOs)
Located in `com.transaction.ingestion.service.dto`

- **IngestRequest**: Transaction ingestion request payload
- **IngestResponse**: Successful transaction ingestion response
- **ErrorResponse**: Standardized error response format
- **RejectedResponse**: Transaction rejection response
- **BatchStatus**: Batch processing status information
- **BulkUploadRequest**: Bulk transaction upload request

### 4. Models
Located in `com.transaction.ingestion.service.model`

- **Transaction**: Core transaction entity stored in MongoDB
- **TransactionEvent**: Kafka event payload for transaction lifecycle events
- **Customer**: Customer information entity
- **KYCStatus**: Customer KYC verification status
- **ValidationDetails**: Detailed validation results
- **Location**: Geographic location information
- **RejectedTransaction**: Rejected transaction audit record

### 5. Repositories
Located in `com.transaction.ingestion.service.repository`

- **TransactionRepository**: MongoDB repository for Transaction entities
- **KYCStatusRepository**: MongoDB repository for KYCStatus entities
- **RejectedTransactionRepository**: MongoDB repository for rejected transactions

### 6. Configuration
Located in `com.transaction.ingestion.service.config`

#### KafkaConfig
- Kafka producer and consumer configuration
- Topic creation and management
- Serialization/deserialization settings

#### ValidationProperties
- Configuration properties for validation rules
- Externalized configuration via application.yaml

## Kafka Integration

### Topics
- `transaction-received`: Published when transactions pass initial validation
- `transaction-validated`: Published after successful advanced validation
- `transaction-rejected`: Published when transactions fail validation
- `transaction-validation-failed`: Published when validation issues are detected but not blocking

### Message Format
All Kafka messages use JSON serialization with the customer ID as the partition key to ensure ordering per customer.

## MongoDB Schema

### Transaction Collection
```json
{
  "transactionId": "string",
  "customerId": "string",
  "amount": "double",
  "currency": "string",
  "merchant": "string",
  "merchantCategory": "string",
  "timestamp": "ISODate",
  "channel": "string",
  "device": "string",
  "location": {
    "country": "string",
    "city": "string",
    "ip": "string"
  },
  "status": "string",
  "validationDetails": {
    "structuralCheck": "string",
    "businessCheck": "string",
    "kycStatus": "string",
    "velocityFlag": "boolean",
    "sanctionsCheck": "string",
    "validatedAt": "ISODate"
  },
  "riskFlags": ["string"],
  "createdAt": "ISODate",
  "updatedAt": "ISODate",
  "version": "long"
}
```

### Customer KYC Collection
```json
{
  "customerId": "string",
  "verificationStatus": "string",
  "verificationDate": "ISODate",
  "expiryDate": "ISODate",
  "documentType": "string",
  "documentNumber": "string",
  "verifiedBy": "string",
  "createdAt": "ISODate",
  "updatedAt": "ISODate"
}
```

### Rejected Transaction Collection
```json
{
  "transactionId": "string",
  "customerId": "string",
  "amount": "double",
  "currency": "string",
  "merchant": "string",
  "rejectionReason": "string",
  "rejectionDetails": {
    "requestedAmount": "double",
    "customerLimit": "double",
    "validationErrors": "string"
  },
  "eventTimestamp": "ISODate",
  "correlationId": "string",
  "createdAt": "ISODate"
}
```

## Configuration

### Application Properties
Key configuration properties in `application.yaml`:

```yaml
spring:
  data:
    mongodb:
      uri: mongodb connection string
  kafka:
    bootstrap-servers: Kafka cluster addresses

validation:
  devices: Supported device types
  channels: Supported transaction channels
  currency: Currency validation rules
  amount: Amount validation rules
  timestamp: Timestamp validation rules
  customer: Customer validation rules
  rules: Business validation rules

kafka:
  topics: Topic name mappings
  partition-count: Default partition count
  replication-factor: Default replication factor
```

## Error Handling

### Global Exception Handler
Located in `com.transaction.ingestion.service.exception.GlobalExceptionHandler`
- Handles application-specific exceptions
- Provides consistent error response format
- Logs errors for monitoring and debugging

### Response Codes
- **200**: Successful operation
- **400**: Validation errors
- **404**: Resource not found
- **409**: Business rule violations
- **500**: Internal server errors

## Security

### Authentication
- JWT token-based authentication
- Integration with API Gateway for centralized authentication

### Authorization
- Role-based access control (RBAC)
- API-level permission checks

### Data Protection
- PII encryption at rest (AES-256)
- TLS encryption in transit
- Secure logging (masking sensitive data)

## Monitoring and Observability

### Logging
- Structured logging with log levels
- Request/response logging
- Error and exception logging

### Metrics
- Spring Boot Actuator endpoints
- Custom business metrics
- Kafka consumer/producer metrics

### Health Checks
- Database connectivity checks
- Kafka connectivity checks
- External service dependency checks

## Deployment

### Environment Variables
- `PORT`: Service port (default: 0 for random port)
- `SPRING_PROFILES_ACTIVE`: Active Spring profiles
- Database and Kafka connection properties

### Docker Support
- Dockerfile for containerization
- Multi-stage build process
- Health check endpoint configuration

### Scaling
- Horizontal scaling supported
- Kafka partitioning for load distribution
- MongoDB replica sets for high availability

## Testing

### Unit Tests
- JUnit 5 test framework
- Mockito for mocking dependencies
- Coverage for business logic validation

### Integration Tests
- Spring Boot test framework
- Embedded MongoDB for repository tests
- Testcontainers for Kafka integration tests

### API Testing
- Swagger UI for manual testing
- Postman collections for automated API testing