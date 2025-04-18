# Go SCD Client Library

A Go client library for interacting with the Slowly Changing Dimension (SCD) service.

## Overview

This client library provides a convenient way to interact with the SCD service from Go applications. It offers both a direct client API for low-level operations and a GORM integration for seamless database operations with versioning support.

## Prerequisites

- Go 1.16 or later
- Protocol Buffers compiler (protoc)
- SCD service instance

## Installation

```bash
go get github.com/mercor-ai/go-sdc-client-library
```

## Generating the gRPC Client Code

Before using this library, you need to generate the Go code from the Protocol Buffers definition file. Run the included script:

```bash
# Make the script executable
chmod +x scripts/generate-proto.sh

# Run the script
./scripts/generate-proto.sh
```

This will generate the necessary Go code in the `internal/proto/gen` directory.

## Usage

### Direct Client API

```go
package main

import (
    "context"
    "fmt"
    "log"

    "github.com/mercor-ai/go-sdc-client-library/pkg/client"
)

func main() {
    // Create a new client with default configuration
    c, err := client.NewClient()
    if err != nil {
        log.Fatalf("Failed to create client: %v", err)
    }
    defer c.Close()

    // Get the latest version of a job
    ctx := context.Background()
    jobID := "job_123"
    job, err := c.GetLatestVersion(ctx, client.EntityTypeJob, jobID)
    if err != nil {
        log.Fatalf("Failed to get job: %v", err)
    }

    fmt.Printf("Job: %v\n", job)
}
```

### GORM Integration

```go
package main

import (
    "fmt"
    "log"

    "github.com/mercor-ai/go-sdc-client-library/pkg/client"
    scdGorm "github.com/mercor-ai/go-sdc-client-library/pkg/gorm"
    "github.com/mercor-ai/go-sdc-client-library/pkg/model"
    "gorm.io/driver/postgres"
)

// Define a job entity
type Job struct {
    model.SCDModel         // Embed SCD fields
    Status         string  `json:"status"`
    Rate           float64 `json:"rate"`
    Title          string  `json:"title"`
    CompanyID      string  `json:"company_id"`
    ContractorID   string  `json:"contractor_id"`
}

func main() {
    // Create a client
    scdClient, err := client.NewClient()
    if err != nil {
        log.Fatalf("Failed to create SCD client: %v", err)
    }
    defer scdClient.Close()

    // Create PostgreSQL connection
    dsn := "host=localhost user=postgres password=postgres dbname=jobs port=5432 sslmode=disable"

    // Initialize GORM with SCD plugin
    db, err := scdGorm.Open(postgres.Open(dsn), &scdGorm.Config{
        SCDClient: scdClient,
    })
    if err != nil {
        log.Fatalf("Failed to connect to database: %v", err)
    }

    // Create a new job
    job := Job{
        Title:        "Software Engineer",
        Rate:         50.0,
        Status:       "active",
        CompanyID:    "comp_123",
        ContractorID: "cont_456",
    }

    // Save the job (this will automatically handle versioning)
    db.Create(&job)

    // Update the job
    job.Rate = 55.0
    db.Save(&job)

    // Get job history
    var versions []Job
    db.History().Where("id = ?", job.ID).Find(&versions)

    for i, v := range versions {
        fmt.Printf("Version %d: %s, Rate: %.2f\n", i+1, v.Status, v.Rate)
    }
}
```

## Features

- Transparent versioning of entities
- Automatic version tracking
- History queries
- Batch operations
- GORM integration for seamless database operations
- Retry logic with configurable timeouts

## Examples

See the `examples` directory for more detailed usage examples.

## Running the Examples

The library includes several example applications to demonstrate its functionality. Here's how to run them:

### Prerequisites

Before running the examples, make sure:

1. You have the SCD service running (either locally or remotely)
2. You have generated the gRPC client code (see [Generating the gRPC Client Code](#generating-the-grpc-client-code))
3. You have PostgreSQL running if you want to use the GORM integration examples

### Environment Configuration

Configure the examples using environment variables:

```bash
# SCD Service configuration
export SCD_HOST=localhost
export SCD_PORT=9090

# Database configuration (for GORM examples)
export DB_HOST=localhost
export DB_PORT=5432
export DB_USER=user
export DB_PASSWORD=password
export DB_NAME=employment
export DB_SSL_MODE=disable
```

### Running Examples

To run all examples:

```bash
cd examples/grpc
go run main.go job_example.go timelog_example.go payment_example.go utility.go client_demo.go -all
```

To run specific examples:

```bash
# For job examples only
go run main.go job_example.go timelog_example.go payment_example.go utility.go client_demo.go -job

# For timelog examples only
go run main.go job_example.go timelog_example.go payment_example.go utility.go client_demo.go -timelog

# For payment examples only
go run main.go job_example.go timelog_example.go payment_example.go utility.go client_demo.go -payment

# For a simple test of the gRPC client
go run main.go job_example.go timelog_example.go payment_example.go utility.go client_demo.go -test
```

### Example Output

Each example demonstrates different operations:

1. **Job Examples**: Creating, updating, and querying job entities
2. **Timelog Examples**: Creating timelog records, adjusting durations, and querying time periods
3. **Payment Examples**: Creating payment records, querying by status/amount, and processing payments

The examples show both direct client API usage and GORM integration.

## Configuration

The SCD client can be configured in multiple ways:

```go
// Using environment variables
scdHost := os.Getenv("SCD_HOST")  // Defaults to "localhost" if not set
scdPort := 9090                    // Default port
if portStr := os.Getenv("SCD_PORT"); portStr != "" {
    if port, err := strconv.Atoi(portStr); err == nil {
        scdPort = port
    }
}
scdClient, err := client.NewClientWithHostPort(scdHost, scdPort)

// Using the configuration struct
config := client.ClientConfig{
    Host:       "scd-service.example.com",
    Port:       8080,
    Timeout:    30 * time.Second,
    MaxRetries: 3,
    RetryDelay: 500 * time.Millisecond,
}
scdClient, err := client.NewClientWithConfig(config)
```

### Database Configuration

The library uses PostgreSQL for data persistence. You can configure your PostgreSQL connection through environment variables:

```go
// Get PostgreSQL connection details from environment variables
dbHost := getEnv("DB_HOST", "localhost")
dbPort := getEnv("DB_PORT", "5432")
dbUser := getEnv("DB_USER", "user")
dbPass := getEnv("DB_PASSWORD", "password")
dbName := getEnv("DB_NAME", "employment")
sslMode := getEnv("DB_SSL_MODE", "disable")

// Create PostgreSQL connection string
dsn := fmt.Sprintf("host=%s user=%s password=%s dbname=%s port=%s sslmode=%s TimeZone=UTC",
    dbHost, dbUser, dbPass, dbName, dbPort, sslMode)

// Initialize GORM with PostgreSQL
db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{
    SCDClient: scdClient,
})
```

## Documentation

For more detailed documentation, see the [docs](./docs) directory.

## License

MIT
