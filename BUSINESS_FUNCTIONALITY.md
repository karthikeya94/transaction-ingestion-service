# Transaction Ingestion Service - Business Functionality

## Overview
The Transaction Ingestion Service is responsible for receiving, validating, and routing financial transactions through the initial processing pipeline of the Real-Time Financial Risk Assessment & Compliance Platform. It serves as the entry point for all transaction processing workflows.

## Core Functions

### 1. Transaction Ingestion
- **API Endpoint**: `POST /api/v1/transactions/ingest`
- **Purpose**: Accepts transaction requests from clients and initiates the validation and processing workflow
- **Input**: Transaction details including customer ID, amount, currency, merchant information, timestamp, channel, device, and location
- **Output**: Immediate acknowledgment of receipt with transaction ID and status

### 2. Transaction Validation
The service performs multiple levels of validation to ensure transaction integrity:

#### 2.1 Structural Validation (Synchronous)
Performed immediately upon API call:
- Customer ID format validation (must match "C[0-9]{6,}")
- Amount validation (> 0, ≤ 999,999,999.99)
- Currency validation (ISO 4217 codes)
- Merchant validation (not null, 1-100 characters)
- Timestamp validation (valid ISO 8601, not future-dated)
- Channel validation (online, atm, branch, mobile)
- Device validation (desktop, mobile, tablet, kiosk)

#### 2.2 Business Validation (Synchronous)
- Customer existence and active status verification
- Customer blacklist check
- Transaction amount limit verification based on customer tier
- Merchant registration verification
- Duplicate transaction detection (same customer, amount, merchant within 60 seconds)

#### 2.3 Schema Compliance (Synchronous)
- Required field presence verification
- Data type validation
- No unexpected fields in request

### 3. Event Publishing
Upon successful initial validation, the service publishes events to Kafka topics:
- `transaction-received`: For transactions that pass initial validation
- `transaction-rejected`: For transactions that fail validation

### 4. Transaction Retrieval
- **API Endpoint**: `GET /api/v1/transactions/{transactionId}`
- **Purpose**: Retrieve detailed information about a specific transaction
- **Output**: Complete transaction details including validation status

## Business Rules

### Acceptance Criteria
A transaction is accepted if:
- All structural validations pass
- Customer exists and is ACTIVE
- Transaction amount is within customer limits
- No real-time blacklist match
- Not a duplicate transaction
- Timestamp is within acceptable window (current time ± 5 minutes)

### Rejection Criteria
A transaction is rejected if:
- Any structural validation fails
- Customer does not exist or is not ACTIVE
- Customer is blacklisted
- Transaction amount exceeds customer limit
- Duplicate transaction detected
- Merchant not registered in system

## Integration Points

### Internal Dependencies
- **MongoDB**: Customer data, transaction storage, KYC information
- **Redis**: Customer limits cache, velocity tracking (planned)
- **Kafka**: Event publishing for downstream services

### External Dependencies
- **AML/Sanctions Services**: For compliance checking (via HTTP/gRPC with circuit breaker)
- **Customer Database**: For customer verification and limit checks

## Data Management

### Transaction Lifecycle
1. **Received**: Initial state after passing structural validation
2. **Validated**: After passing all validation checks
3. **Rejected**: If any validation fails

### Data Retention
- Transaction data retained for 7 days in Kafka topics
- MongoDB collections follow organization's data retention policies

## Performance Requirements
- Response time for ingestion API: < 200ms
- Event publishing latency: < 50ms
- Throughput target: 1000 transactions/second

## Error Handling
- Validation errors return appropriate HTTP status codes (400, 409)
- System errors logged and monitored
- Failed transactions stored for audit purposes