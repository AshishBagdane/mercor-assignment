# Mercor SCD Service

A centralized Spring Boot service for handling Slowly Changing Dimensions (SCD) operations across multiple applications.

## Overview

Mercor SCD Service provides a language-agnostic solution for working with SCD data by:

- Centralizing complex SCD query logic
- Exposing a gRPC API for entity operations
- Optimizing database queries for performance
- Supporting cross-language integration
- Providing consistent version management

## Project Structure

```
mercor-scd-service/
.
├── HELP.md
├── README.md
├── compose.yaml
├── mvnw
├── mvnw.cmd
├── pom.xml
└── src
    ├── main
    │   ├── java
    │   │   └── com
    │   │       └── mercor
    │   │           └── assignment
    │   │               └── scd
    │   │                   ├── MercorScdApplication.java
    │   │                   ├── common
    │   │                   │   ├── config
    │   │                   │   │   ├── CacheConfiguration.java
    │   │                   │   │   ├── GrpcConfig.java
    │   │                   │   │   └── GrpcServerConfig.java
    │   │                   │   ├── errorhandling
    │   │                   │   │   ├── exceptions
    │   │                   │   │   ├── interceptor
    │   │                   │   │   └── metrics
    │   │                   │   └── validation
    │   │                   │       └── Validators.java
    │   │                   └── domain
    │   │                       ├── TestServiceImpl.java
    │   │                       ├── core
    │   │                       │   ├── constants
    │   │                       │   ├── enums
    │   │                       │   ├── mapper
    │   │                       │   ├── model
    │   │                       │   ├── repository
    │   │                       │   ├── service
    │   │                       │   ├── util
    │   │                       │   └── validation
    │   │                       ├── job
    │   │                       │   ├── enums
    │   │                       │   ├── mapper
    │   │                       │   ├── model
    │   │                       │   ├── repository
    │   │                       │   └── service
    │   │                       ├── paymentlineitem
    │   │                       │   ├── enums
    │   │                       │   ├── mapper
    │   │                       │   ├── model
    │   │                       │   ├── repository
    │   │                       │   └── service
    │   │                       └── timelog
    │   │                           ├── enums
    │   │                           ├── mapper
    │   │                           ├── model
    │   │                           ├── repository
    │   │                           └── service
    │   ├── proto
    │   │   ├── com
    │   │   │   └── mercor
    │   │   │       └── assignment
    │   │   │           └── scd
    │   │   │               └── domain
    │   │   │                   ├── common
    │   │   │                   │   └── types.proto
    │   │   │                   ├── core
    │   │   │                   │   ├── request.proto
    │   │   │                   │   ├── response.proto
    │   │   │                   │   └── service.proto
    │   │   │                   ├── job
    │   │   │                   │   ├── request.proto
    │   │   │                   │   ├── response.proto
    │   │   │                   │   └── service.proto
    │   │   │                   ├── paymentlineitems
    │   │   │                   │   ├── request.proto
    │   │   │                   │   ├── response.proto
    │   │   │                   │   └── service.proto
    │   │   │                   └── timelog
    │   │   │                       ├── request.proto
    │   │   │                       ├── response.proto
    │   │   │                       └── service.proto
    │   │   └── test.proto
    │   └── resources
    │       ├── application-dev.yaml
    │       ├── application.yaml
    │       └── db
    │           └── changelog
    │               ├── 2025.1.0
    │               │   ├── dev
    │               │   │   ├── job
    │               │   │   │   └── 01_insert_into_table.xml
    │               │   │   ├── payment_line_items
    │               │   │   │   └── 01_insert_into_table.xml
    │               │   │   ├── release-changes.xml
    │               │   │   └── timelog
    │               │   │       └── 01_insert_into_table.xml
    │               │   ├── job
    │               │   │   ├── 01_create_table.xml
    │               │   │   └── 02_alter_table_add_index.xml
    │               │   ├── payment_line_items
    │               │   │   ├── 01_create_table.xml
    │               │   │   └── 02_alter_table_add_index.xml
    │               │   ├── release-changes.xml
    │               │   └── timelog
    │               │       ├── 01_create_table.xml
    │               │       └── 02_alter_table_add_index.xml
    │               ├── db.changelog-master-dev.xml
    │               └── db.changelog-master.xml
    └── test
        ├── java
        │   └── com
        │       └── mercor
        │           └── assignment
        │               └── scd
        │                   ├── MercorScdApplicationTests.java
        │                   ├── common
        │                   │   └── validation
        │                   │       └── ValidatorsTest.java
        │                   └── domain
        │                       └── core
        │                           └── enums
        └── resources
            └── application.yml
```

## Technology Stack

- Java 17
- Spring Boot 3.x
- Spring Data JPA
- gRPC
- PostgreSQL
- Liquibase migrations
- Maven

## Installation

### Prerequisites

- Java 17 or higher
- PostgreSQL 12 or higher
- Maven 3.6 or higher
- Docker (optional)

### Building from Source

```bash
# Clone the repository
git clone https://github.com/mercor-ai/mercor-scd-service.git
cd mercor-scd-service

# Build the service
./mvnw clean package

# Run the service
./mvnw spring-boot:run
```

### Using Docker

```bash
# Build the Docker image
docker build -t mercor/scd-service .

# Run the container
docker run -p 50051:50051 -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/employment \
  -e SPRING_DATASOURCE_USERNAME=user \
  -e SPRING_DATASOURCE_PASSWORD=password \
  mercor/scd-service
```

## Configuration

The service can be configured through the `application.yml` file or environment variables:

```yaml
spring:  
  application:  
    name: mercor-scd-service  
  main:  
    allow-bean-definition-overriding: true  
  
  datasource:  
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:employment}  
    username: ${DB_USERNAME:user}  
    password: ${DB_PASSWORD:password}  
    driver-class-name: org.postgresql.Driver  
    hikari:  
      minimum-idle: ${DB_MIN_IDLE:5}  
      maximum-pool-size: ${DB_MAX_POOL_SIZE:20}  
      idle-timeout: ${DB_IDLE_TIMEOUT:300000}  
      pool-name: TechRadarHikariCP  
      connection-timeout: ${DB_CONNECTION_TIMEOUT:20000}  
      max-lifetime: ${DB_MAX_LIFETIME:1200000}  
  
  jpa:  
    hibernate:  
      ddl-auto: ${JPA_DDL_AUTO:none}  
    properties:  
      hibernate:  
        dialect: org.hibernate.dialect.PostgreSQLDialect  
        jdbc:  
          batch_size: ${JPA_BATCH_SIZE:50}  
          fetch_size: ${JPA_FETCH_SIZE:50}  
          time_zone: UTC  
        order_inserts: true  
        order_updates: true  
        format_sql: true  
    show-sql: true  
  
  
  # Add these Redis configuration properties  
  redis:  
    host: ${REDIS_HOST:localhost}  
    port: ${REDIS_PORT:6379}  
    password: ${REDIS_PASSWORD:}  # Leave empty if no password  
    timeout: 2000  
    database: 0  
  
  # Enable caching explicitly  
  cache:  
    type: redis  
    redis:  
      time-to-live: 86400000  # 1 day in milliseconds (matches your config)  
      cache-null-values: false  
  
  liquibase:  
    change-log: classpath:db/changelog/db.changelog-master.xml  
    enabled: ${LIQUIBASE_ENABLED:true}  
    default-schema: ${DB_SCHEMA:public}  
  
server:  
  port: ${SERVER_PORT:8080}  
  compression:  
    enabled: ${COMPRESSION_ENABLED:true}  
    mime-types: ${COMPRESSION_MIME_TYPES:application/json,application/xml,text/html,text/plain}  
    min-response-size: ${COMPRESSION_MIN_SIZE:1024}  
  
# Application info  
application:  
  api:  
    version: 1.0.0  
  
# gRPC configuration  
grpc:  
  server:  
    port: ${GRPC_SERVER_PORT:50051}  
    reflection-service-enabled: true  
  
logging:  
  level:  
    org.springframework.cache: TRACE  
    org.springframework.data.redis: DEBUG
```

## API Design

The service uses Protocol Buffers (protobuf) and gRPC to provide a highly efficient binary communication protocol. The API is organized into four main services:

1. **Core SCDService** - Generic operations for all SCD entities
2. **JobService** - Job-specific operations
3. **TimelogService** - Timelog-specific operations
4. **PaymentLineItemService** - Payment line item-specific operations

### Core Service

```protobuf
// Generic SCD service for common operations across entity types
service SCDService {
  // Common SCD operations
  rpc GetLatestVersion (GetLatestVersionRequest) returns (EntityResponse);
  rpc GetVersionHistory (GetVersionHistoryRequest) returns (EntityListResponse);
  rpc Query (QueryRequest) returns (EntityListResponse);
  rpc Update (UpdateRequest) returns (EntityResponse);
  rpc BatchGet (BatchGetRequest) returns (BatchResponse);
  rpc BatchUpdate (BatchUpdateRequest) returns (BatchResponse);
}
```

### Job Service

```protobuf
// Job-specific service
service JobService {
  rpc GetActiveJobsForCompany (GetActiveJobsForCompanyRequest) returns (JobListResponse);
  rpc GetActiveJobsForContractor (GetActiveJobsForContractorRequest) returns (JobListResponse);
  rpc GetJobsWithRateAbove (GetJobsWithRateAboveRequest) returns (JobListResponse);
  rpc UpdateStatus (UpdateJobStatusRequest) returns (JobResponse);
  rpc UpdateRate (UpdateJobRateRequest) returns (JobResponse);
}
```

### Timelog Service

```protobuf
// Timelog-specific service
service TimelogService {
  rpc GetTimelogsForJob (GetTimelogsForJobRequest) returns (TimelogListResponse);
  rpc GetTimelogsForContractor (GetTimelogsForContractorRequest) returns (TimelogListResponse);
  rpc GetTimelogsWithDurationAbove (GetTimelogsWithDurationAboveRequest) returns (TimelogListResponse);
  rpc AdjustTimelog (AdjustTimelogRequest) returns (TimelogResponse);
}
```

### Payment Line Item Service

````protobuf
// PaymentLineItem-specific service
service PaymentLineItemService {
  rpc GetPaymentLineItemsForJob (GetPaymentLineItemsForJobRequest) returns (PaymentLineItemListResponse);
  rpc GetPaymentLineItemsForTimelog (GetPaymentLineItemsForTimelogRequest) returns (PaymentLineItemListResponse);
  rpc GetPaymentLineItemsForContractor (GetPaymentLineItemsForContractorRequest) returns (PaymentLineItemListResponse);
  rpc MarkAsPaid (MarkAsPaidRequest) returns (PaymentLineItemResponse);
  rpc GetTotalAmountForContractor (GetTotalAmountForContractorRequest) returns (TotalAmountResponse);
}

### API Method Details

Below are details about the methods provided by each service:

#### SCDService Methods

- `GetLatestVersion` - Retrieves the latest version of any entity by type and ID
- `GetVersionHistory` - Gets all versions of an entity by type and ID
- `Query` - Performs flexible queries with conditions, supporting latest-version-only filtering
- `Update` - Updates an entity (automatically creates a new version)
- `BatchGet` - Efficiently retrieves multiple entities in a single call
- `BatchUpdate` - Updates multiple entities in a single transaction

#### JobService Methods

- `GetActiveJobsForCompany` - Gets all active jobs for a specific company
- `GetActiveJobsForContractor` - Gets all active jobs for a specific contractor
- `GetJobsWithRateAbove` - Gets jobs with a rate above the specified threshold
- `UpdateStatus` - Updates a job's status (automatically creates new version)
- `UpdateRate` - Updates a job's rate (automatically creates new version)

#### TimelogService Methods

- `GetTimelogsForJob` - Gets all timelogs associated with a job
- `GetTimelogsForContractor` - Gets all timelogs for a contractor in a time range
- `GetTimelogsWithDurationAbove` - Gets timelogs with duration above the specified threshold
- `AdjustTimelog` - Adjusts a timelog's duration (automatically creates new version)

#### PaymentLineItemService Methods

- `GetPaymentLineItemsForJob` - Gets all payment line items for a job
- `GetPaymentLineItemsForTimelog` - Gets all payment line items for a timelog
- `GetPaymentLineItemsForContractor` - Gets all payment line items for a contractor in a time range
- `MarkAsPaid` - Marks a payment line item as paid (automatically creates new version)
- `GetTotalAmountForContractor` - Calculates the total amount for a contractor in a time range

## Database Schema

The service works with the following SCD table structure:

```sql
CREATE TABLE jobs (
    id VARCHAR(255) NOT NULL,
    version INT NOT NULL,
    uid VARCHAR(255) NOT NULL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    status VARCHAR(255) NOT NULL,
    rate DECIMAL(10,2) NOT NULL,
    title VARCHAR(255) NOT NULL,
    company_id VARCHAR(255) NOT NULL,
    contractor_id VARCHAR(255) NOT NULL
);

CREATE TABLE timelogs (
    id VARCHAR(255) NOT NULL,
    version INT NOT NULL,
    uid VARCHAR(255) NOT NULL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    duration BIGINT NOT NULL,
    time_start BIGINT NOT NULL,
    time_end BIGINT NOT NULL,
    type VARCHAR(255) NOT NULL,
    job_uid VARCHAR(255) NOT NULL
);

CREATE TABLE payment_line_items (
    id VARCHAR(255) NOT NULL,
    version INT NOT NULL,
    uid VARCHAR(255) NOT NULL PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    job_uid VARCHAR(255) NOT NULL,
    timelog_uid VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(255) NOT NULL
);
````

## Query Optimization (not all covered yet)

The service employs several techniques to optimize SCD queries:

1. **Materialized Views**: For frequent queries on latest versions
2. **Indexing Strategy**: Optimized for (id, version) lookups
3. **Query Caching**: In-memory caching of frequently accessed entities
4. **Batch Processing**: Efficient handling of bulk operations
5. **Query Rewriting**: Transformation of simple queries into SCD-aware versions

## Client Libraries

The service is designed to work with client libraries in various languages:

- [Go Client Library](https://github.com/mercor-ai/scd-go-client) - GORM integration for Go applications (COVERED)
- [Python Client Library](https://github.com/mercor-ai/scd-django-client) - Django ORM integration for Python applications

### Using the gRPC API

```bash
# Using grpcurl to test the API
# Get latest version of a job (using SCDService)
grpcurl -plaintext -d '{"entity_type": "job", "id": "job_ckbk6oo4hn7pacdgcz9f"}' \
  localhost:50051 com.mercor.assignment.scd.domain.core.SCDService/GetLatestVersion

# Get active jobs for a company
grpcurl -plaintext -d '{"company_id": "comp_cab5i8o0rvh5arskod"}' \
  localhost:50051 com.mercor.assignment.scd.domain.job.JobService/GetActiveJobsForCompany

# Update job status (automatically creates new version)
grpcurl -plaintext -d '{"id": "job_ckbk6oo4hn7pacdgcz9f", "status": "extended"}' \
  localhost:50051 com.mercor.assignment.scd.domain.job.JobService/UpdateStatus

# Get timelogs for a job
grpcurl -plaintext -d '{"job_uid": "job_uid_ywij5sh1tvfp5nkq7azav"}' \
  localhost:50051 com.mercor.assignment.scd.domain.timelog.TimelogService/GetTimelogsForJob

# Mark a payment as paid
grpcurl -plaintext -d '{"id": "li_AAABk__7JGVd0tliAnIQOA61"}' \
  localhost:50051 com.mercor.assignment.scd.domain.paymentlineitems.PaymentLineItemService/MarkAsPaid
```

## Performance Monitoring

The service includes metrics endpoints for monitoring on the management port:

- `/actuator/health` - Service health information
- `/actuator/metrics` - Performance metrics
- `/actuator/prometheus` - Prometheus-compatible metrics

These monitoring endpoints are accessible via HTTP on the management port (8080 by default).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT
