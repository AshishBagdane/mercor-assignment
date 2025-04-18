# Go SCD Client Library Usage Guide

This document provides detailed instructions on how to use the SCD Client Library with GORM.

## Installation

To install the library, use the following command:

```bash
go get github.com/mercor-ai/go-sdc-client-library
go get gorm.io/driver/postgres
```

## Basic Usage

### Initializing the SCD Client

The library provides several ways to initialize the SCD client with configurable host and port:

```go
import "github.com/mercor-ai/go-sdc-client-library/pkg/client"

// Initialize with default settings (localhost:9090)
scdClient, err := client.NewClient()
if err != nil {
    // Handle error
}
defer scdClient.Close()

// Initialize with specific host but default port (example.com:9090)
scdClient, err := client.NewClientWithTarget("example.com")
if err != nil {
    // Handle error
}
defer scdClient.Close()

// Initialize with specific host and port (scd.example.com:8080)
scdClient, err := client.NewClientWithHostPort("scd.example.com", 8080)
if err != nil {
    // Handle error
}
defer scdClient.Close()

// Or with custom configuration
config := client.ClientConfig{
    Timeout:    30 * time.Second,
    MaxRetries: 3,
    RetryDelay: 500 * time.Millisecond,
    Host:       "custom.example.com",
    Port:       9000,
}
scdClient, err := client.NewClientWithConfig(config)
if err != nil {
    // Handle error
}
defer scdClient.Close()
```

You can also use environment variables to configure the SCD service:

```go
// Read configuration from environment
scdHost := os.Getenv("SCD_HOST")
if scdHost == "" {
    scdHost = client.DefaultSCDHost // "localhost"
}

scdPortStr := os.Getenv("SCD_PORT")
scdPort := client.DefaultSCDPort // 9090
if scdPortStr != "" {
    if port, err := strconv.Atoi(scdPortStr); err == nil {
        scdPort = port
    }
}

scdClient, err := client.NewClientWithHostPort(scdHost, scdPort)
```

### Setting Up PostgreSQL with GORM

```go
import (
    "github.com/mercor-ai/go-sdc-client-library/pkg/gorm"
    "gorm.io/driver/postgres"
    gormLib "gorm.io/gorm"
)

// PostgreSQL connection string
dsn := "host=localhost user=postgres password=postgres dbname=scd_db port=5432 sslmode=disable TimeZone=UTC"

// Initialize GORM with SCD integration and PostgreSQL
db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{
    SCDClient: scdClient,
    Config: &gormLib.Config{
        // Standard GORM configuration options
    },
})
if err != nil {
    // Handle error
}
```

You can also configure PostgreSQL with environment variables:

```go
// Get PostgreSQL connection details from environment variables
dbHost := getEnv("DB_HOST", "localhost")
dbPort := getEnv("DB_PORT", "5432")
dbUser := getEnv("DB_USER", "user")
dbPass := getEnv("DB_PASSWORD", "password")
dbName := getEnv("DB_NAME", "employment")
sslMode := getEnv("DB_SSL_MODE", "disable")

// Helper function for environment variables
func getEnv(key, fallback string) string {
    if value, exists := os.LookupEnv(key); exists {
        return value
    }
    return fallback
}

// Create PostgreSQL connection string
dsn := fmt.Sprintf("host=%s user=%s password=%s dbname=%s port=%s sslmode=%s TimeZone=UTC",
    dbHost, dbUser, dbPass, dbName, dbPort, sslMode)

// Initialize GORM with PostgreSQL
db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{
    SCDClient: scdClient,
})
```

### Defining SCD-aware Models

```go
import "github.com/mercor-ai/go-sdc-client-library/pkg/model"

// Define a model that uses SCD versioning
type Job struct {
    model.SCDModel      // Embed SCD fields (ID, Version, UID, CreatedAt, UpdatedAt)
    Status      string  `json:"status"`
    Rate        float64 `json:"rate"`
    Title       string  `json:"title"`
    CompanyID   string  `json:"company_id"`
}

// Implement table name method
func (Job) TableName() string {
    return "jobs"
}

// Implement entity type method
func (Job) EntityType() string {
    return "job"
}
```

### Basic CRUD Operations

```go
// Create
job := &Job{
    Status:    "active",
    Rate:      50.0,
    Title:     "Software Engineer",
    CompanyID: "company-123",
}
if err := db.Create(job).Error; err != nil {
    // Handle error
}

// Read (automatically gets the latest version)
var retrievedJob Job
if err := db.First(&retrievedJob, "id = ?", "job-123").Error; err != nil {
    // Handle error
}

// Update (creates a new version)
retrievedJob.Status = "completed"
if err := db.Save(&retrievedJob).Error; err != nil {
    // Handle error
}

// Delete (soft delete in SCD by creating new version with deleted status)
if err := db.Delete(&retrievedJob).Error; err != nil {
    // Handle error
}
```

### Version History and Querying

```go
import "github.com/mercor-ai/go-sdc-client-library/pkg/query"

// Get version history for an entity
var jobHistory []Job
if err := db.History().Find(&jobHistory, "id = ?", "job-123").Error; err != nil {
    // Handle error
}

// Query with scopes
var activeJobs []Job
if err := db.Scopes(query.LatestOnly).Where("status = ?", "active").Find(&activeJobs).Error; err != nil {
    // Handle error
}

// Query with date range
var recentJobs []Job
if err := db.Scopes(query.WithDateRange("created_at", startTime, endTime)).Find(&recentJobs).Error; err != nil {
    // Handle error
}
```

### Batch Operations

```go
import "github.com/mercor-ai/go-sdc-client-library/pkg/query"

// Process entities in batches to avoid memory issues with large datasets
err := query.BatchScope(100, func(tx *gorm.DB) error {
    // Process a batch of up to 100 records
    var jobs []Job
    if err := tx.Find(&jobs).Error; err != nil {
        return err
    }

    // Process each job in the batch
    for _, job := range jobs {
        // Do something with the job
    }

    return nil
})(db)
if err != nil {
    // Handle error
}

// Batch update
err = query.BatchScope(100, func(tx *gorm.DB) error {
    return tx.Model(&Job{}).Where("company_id = ?", "company-123").Update("status", "completed").Error
})(db)
if err != nil {
    // Handle error
}
```

### Direct SCD Client Usage

For more complex operations or when you need to bypass GORM, you can use the SCD client directly:

```go
import "context"

// Get the latest version of an entity
ctx := context.Background()
jobData, err := scdClient.GetLatestVersion(ctx, client.EntityTypeJob, "job-123")
if err != nil {
    // Handle error
}

// Get version history
jobVersions, err := scdClient.GetVersionHistory(ctx, client.EntityTypeJob, "job-123")
if err != nil {
    // Handle error
}

// Query entities with conditions
conditions := map[string]interface{}{
    "status": "active",
    "company_id": "company-123",
}
options := client.QueryOptions{
    LatestVersionOnly: true,
    Limit: 10,
    Offset: 0,
    SortBy: "created_at",
    SortDirection: "desc",
}
results, err := scdClient.Query(ctx, client.EntityTypeJob, conditions, options)
if err != nil {
    // Handle error
}
```

## Advanced Usage

### Custom Model Methods

You can extend your model with custom methods to perform common operations:

```go
// Update status and create a new version
func (j *Job) UpdateStatus(db *gorm.DB, status string) error {
    j.Status = status
    return db.Save(j).Error
}

// Mark job as completed
func (j *Job) Complete(db *gorm.DB) error {
    return j.UpdateStatus(db, "completed")
}
```

### Transactions

```go
// Use transactions to ensure consistency
err := db.Transaction(func(tx *gorm.DB) error {
    // Create a new job
    job := &Job{
        Status:    "active",
        Rate:      50.0,
        Title:     "Software Engineer",
        CompanyID: "company-123",
    }
    if err := tx.Create(job).Error; err != nil {
        return err
    }

    // Create related entities
    // ...

    return nil
})
if err != nil {
    // Handle error
}
```

### Custom Query Scopes

You can define custom query scopes for common filtering patterns:

```go
// Define a scope for active jobs
func ActiveJobs(db *gorm.DB) *gorm.DB {
    return db.Where("status = ?", "active")
}

// Define a scope for jobs with rate above threshold
func JobsWithRateAbove(rate float64) func(db *gorm.DB) *gorm.DB {
    return func(db *gorm.DB) *gorm.DB {
        return db.Where("rate > ?", rate)
    }
}

// Use the scopes
var jobs []Job
if err := db.Scopes(query.LatestOnly, ActiveJobs, JobsWithRateAbove(30.0)).Find(&jobs).Error; err != nil {
    // Handle error
}
```

## Best Practices

1. **Always use transactions** for operations that affect multiple entities to ensure consistency.
2. **Use the History() method** when you need to query version history, and avoid it otherwise for better performance.
3. **Define custom scopes** for common query patterns to keep your code DRY.
4. **Implement EntityType() method** on your models to ensure proper integration with the SCD service.
5. **Close the SCD client** when you're done with it to free up resources.
6. **Use batch operations** for processing large datasets to avoid memory issues.
7. **Handle errors properly** at each step, especially when interacting with the SCD service.
8. **Configure the SCD client** with appropriate timeout and retry settings for your environment.
9. **Use environment variables** for database and SCD service configuration in production.

## Troubleshooting

### Common Issues

1. **Connection failures**: Ensure the SCD service is running and accessible at the specified address.
2. **Version conflicts**: If you see errors about version conflicts, ensure you're working with the latest version of the entity.
3. **Missing entity types**: Make sure your models implement the EntityType() method.
4. **Performance issues**: Use batch operations and appropriate indexes for large datasets.
5. **Database issues**: Ensure your PostgreSQL server is running and accessible with the specified credentials.

### Debugging

For debugging, you can enable GORM logging:

```go
db, err := gorm.Open(postgres.Open(dsn), &gorm.Config{
    SCDClient: scdClient,
    Config: &gormLib.Config{
        Logger: logger.Default.LogMode(logger.Info),
    },
})
```

You can also check the connection to your SCD service:

```go
// Print the SCD service target
fmt.Println("Connected to SCD service at", scdClient.GetTarget())
```

You can use the direct SCD client methods to inspect entities and their versions when troubleshooting.
