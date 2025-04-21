# SCD Go Client Library

A Go client library for working with Slowly Changing Dimensions (SCD) data, providing a clean abstraction over SCD entities and operations.

## Overview

This library simplifies working with SCD data in Go applications by:

- Abstracting SCD handling and versioning logic
- Integrating with GORM for database operations
- Providing a clean repository pattern for entity access
- Communicating with SCD services via gRPC

## Installation

```bash
go get github.com/mercor-ai/scd-go-client
```

## Project Structure

```
scd-go-client/
├── api/                     # Generated proto code
├── pkg/
│   ├── client/              # gRPC client implementation
│   ├── config/              # Configuration
│   ├── models/              # Domain models
│   │   ├── job/             # Job entity
│   │   ├── payment/         # Payment entity
│   │   └── timelog/         # Timelog entity
│   ├── repository/          # Repository implementations
│   └── scd/                 # SCD-specific utilities
└── cmd/examples/            # Example applications
```

## Configuration

The library provides a centralized configuration system for database and gRPC settings:

```go
// Load default configuration
cfg := config.DefaultConfig()

// Or customize as needed
cfg := &config.Config{
    Database: config.DatabaseConfig{
        Host:     "localhost",
        Port:     5432,
        User:     "user",
        Password: "password",
        DBName:   "employment",
        SSLMode:  "disable",
    },
    GRPC: config.GRPCServerConfig{
        Host:    "localhost",
        Port:    50051,
        Timeout: 5 * time.Second,
    },
}
```

## Basic Usage

### Connecting to the Database and SCD Service

```go
// Load configuration
cfg := config.DefaultConfig()

// Connect to database
db, err := gorm.Open(postgres.Open(cfg.Database.GetDSN()), &gorm.Config{})
if err != nil {
    log.Fatalf("Failed to connect to database: %v", err)
}

// Create SCD client
scdClient, err := client.New(client.Config{
    ServerAddress: cfg.GRPC.GetServerAddress(),
    DialOptions:   cfg.GRPC.DialOptions,
    Timeout:       cfg.GRPC.Timeout,
})
if err != nil {
    log.Fatalf("Failed to create SCD client: %v", err)
}
defer scdClient.Close()
```

### Working with Job Entities

```go
// Create job repository
jobRepo := repository.NewJobRepository(db, scdClient)

// Fetch job by ID
job, err := jobRepo.GetJobRemote(ctx, "job_ckbk6oo4hn7pacdgcz9f")
if err != nil {
    log.Fatalf("Failed to get job: %v", err)
}

// Update job status
updatedJob, err := jobRepo.UpdateJobStatusRemote(ctx, job.ID, "extended")
if err != nil {
    log.Fatalf("Failed to update job: %v", err)
}

// Get active jobs for a company
jobs, err := jobRepo.GetActiveJobsForCompanyRemote(ctx, "comp_example123")
if err != nil {
    log.Fatalf("Failed to get active jobs: %v", err)
}

// Get job version history
history, err := jobRepo.GetJobHistoryRemote(ctx, job.ID)
if err != nil {
    log.Fatalf("Failed to get job history: %v", err)
}
```

### Working with Timelog Entities

```go
// Create timelog repository
timelogRepo := repository.NewTimelogRepository(db, scdClient)

// Fetch timelog by ID
timelog, err := timelogRepo.GetTimelogRemote(ctx, "tl_AAABk__7Gd2t3TqM-Bdm8kNQ")
if err != nil {
    log.Fatalf("Failed to get timelog: %v", err)
}

// Adjust timelog duration
adjustedDuration := timelog.Duration - (timelog.Duration / 10)
adjustedTimelog, err := timelogRepo.AdjustTimelogRemote(ctx, timelog.ID, adjustedDuration)
if err != nil {
    log.Fatalf("Failed to adjust timelog: %v", err)
}

// Get timelogs for a job
timelogs, err := timelogRepo.GetTimelogsForJobRemote(ctx, timelog.JobUID)
if err != nil {
    log.Fatalf("Failed to get job timelogs: %v", err)
}
```

### Working with Payment Line Items

```go
// Create payment repository
paymentRepo := repository.NewPaymentRepository(db, scdClient)

// Fetch payment by ID
payment, err := paymentRepo.GetPaymentLineItemRemote(ctx, "li_EEEFo__1Ln8A0XyQ-FhiprsU")
if err != nil {
    log.Fatalf("Failed to get payment: %v", err)
}

// Mark payment as paid
if payment.Status != "paid" {
    paidPayment, err := paymentRepo.MarkAsPaidRemote(ctx, payment.ID)
    if err != nil {
        log.Fatalf("Failed to mark payment as paid: %v", err)
    }
}

// Get payments for a job
payments, err := paymentRepo.GetPaymentLineItemsForJobRemote(ctx, payment.JobUID)
if err != nil {
    log.Fatalf("Failed to get job payments: %v", err)
}

// Get total amount for a contractor
totalAmount, err := paymentRepo.GetTotalAmountForContractorRemote(ctx, "cont_example456",
    time.Now().AddDate(0, -1, 0).UnixNano()/1000000,
    time.Now().UnixNano()/1000000)
if err != nil {
    log.Fatalf("Failed to get total amount: %v", err)
}
```

## Running the Examples

The library includes example applications that demonstrate common SCD operations. All examples now support command-line arguments for more flexibility.

### Job Example

```bash
# From project root
cd cmd/examples/job
go run main.go -id job_ckbk6oo4hn7pacdgcz9f
```

**Command Line Arguments:**

- `-id` (required): Job ID to query

**Example Usage:**

```bash
# Query a specific job
go run main.go -id job_ckbk6oo4hn7pacdgcz9f

# With help
go run main.go -h
```

This example demonstrates:

- Fetching an existing job by ID
- Updating job status
- Getting active jobs for a company
- Retrieving job version history

### Timelog Example

```bash
# From project root
cd cmd/examples/timelog
go run main.go -id tl_AAABk__7Gd2t3TqM-Bdm8kNQ
```

**Command Line Arguments:**

- `-id` (required): Timelog ID to query
- `-adjust` (optional): Absolute duration value to set in milliseconds

**Example Usage:**

```bash
# Just query a timelog
go run main.go -id tl_AAABk__7Gd2t3TqM-Bdm8kNQ

# Query and adjust duration to 82304 milliseconds
go run main.go -id tl_AAABk__7Gd2t3TqM-Bdm8kNQ -adjust 82304

# With help
go run main.go -h
```

This example demonstrates:

- Fetching an existing timelog by ID
- Adjusting timelog duration
- Getting timelogs for a job
- Retrieving timelog version history

### Payment Example

```bash
# From project root
cd cmd/examples/payment
go run main.go -id li_EEEFo__1Ln8A0XyQ-FhiprsU
```

**Command Line Arguments:**

- `-id` (required): Payment Line Item ID to query
- `-contractor` (optional): Contractor ID for total amount query
- `-start` (optional): Start time for period queries in RFC3339 format
- `-end` (optional): End time for period queries in RFC3339 format

**Example Usage:**

```bash
# Basic payment line item query
go run main.go -id li_EEEFo__1Ln8A0XyQ-FhiprsU

# With contractor ID
go run main.go -id li_EEEFo__1Ln8A0XyQ-FhiprsU -contractor cont_e0nhseq682vkoc4d

# With time period (last month)
go run main.go -id li_EEEFo__1Ln8A0XyQ-FhiprsU -start 2025-03-21T00:00:00Z -end 2025-04-21T00:00:00Z

# Complete example with all parameters
go run main.go -id li_EEEFo__1Ln8A0XyQ-FhiprsU -contractor cont_e0nhseq682vkoc4d -start 2025-03-21T00:00:00Z -end 2025-04-21T00:00:00Z

# With help
go run main.go -h
```

This example demonstrates:

- Fetching an existing payment line item by ID
- Marking a payment as paid
- Getting payment line items for a job
- Retrieving payment line item version history
- Calculating total amount for a contractor within a specific time period

## Prerequisites

To run the examples, you'll need:

1. A PostgreSQL database running on localhost:5432
2. The SCD service running on localhost:50051
3. Go 1.16 or later

## Database Setup

The examples expect a PostgreSQL database with the following schema:

```sql
-- Create SCD tables
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
```

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT
