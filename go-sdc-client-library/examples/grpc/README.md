# gRPC-Based SCD Client Example

This example demonstrates how to use the SCD client library with gRPC to work with versioned entities.

## Prerequisites

1. A running SCD service (default: localhost:9090)
2. PostgreSQL database
3. Go 1.24 or later

## Environment Variables

Set the following environment variables to configure the example:

### SCD Service Configuration

- `SCD_HOST` - Hostname of the SCD service (default: localhost)
- `SCD_PORT` - Port of the SCD service (default: 9090)

### Database Configuration

- `DB_HOST` - PostgreSQL host (default: localhost)
- `DB_PORT` - PostgreSQL port (default: 5432)
- `DB_USER` - PostgreSQL username (default: user)
- `DB_PASSWORD` - PostgreSQL password (default: password)
- `DB_NAME` - PostgreSQL database name (default: employment)
- `DB_SSL_MODE` - PostgreSQL SSL mode (default: disable)

## Running the Example

```bash
go run examples/grpc/main.go
```

## What This Example Demonstrates

### Direct Client API

1. Job Operations

   - Get latest version
   - Get version history
   - Query by conditions
   - Update
   - Batch operations

2. Timelog Operations

   - Get latest version
   - Query by job
   - Update duration

3. Payment Line Item Operations
   - Query by contractor/job
   - Mark as paid

### GORM Integration

4. Job Operations with GORM

   - Create
   - Find
   - Update
   - Soft delete

5. Timelog Operations with GORM

   - Create
   - Find by job
   - Adjust duration

6. Payment Line Item Operations with GORM

   - Create
   - Find by job
   - Mark as paid

7. Version History Queries
   - Query all versions
   - Compare changes between versions

## Key Features Demonstrated

- Transparent versioning with GORM
- Direct client API for low-level operations
- Versioned entity management
- Automatic version tracking
- History querying with `db.History()`
- SCD operations across different entity types

## Safe Update Pattern

To prevent optimistic locking failures when updating entities, always use the safe update pattern:

1. Fetch the latest version of the entity using `GetLatestVersion` before updating
2. Apply your changes while preserving the correct version number
3. Send the update request

### Example: Safe Update Pattern

```go
// Define only the fields you want to update
updateData := map[string]interface{}{
    "id":     entityID,
    "status": "completed",
    "rate":   85.0,
}

// Use the safe update utility function
updatedEntity, err := UpdateEntitySafely(ctx, client, entityType, updateData)
if err != nil {
    // Handle error
} else {
    // Use updated entity
}
```

The `UpdateEntitySafely` function:

1. Fetches the latest version of the entity
2. Applies your update data with the correct version
3. Performs the update operation

### Batch Updates

For batch updates, use the `BatchUpdateEntitiesSafely` function that fetches the latest version of each entity before updating:

```go
entitiesToUpdate := []map[string]interface{}{
    {
        "id":     entity1ID,
        "status": "active",
    },
    {
        "id":     entity2ID,
        "rate":   75.0,
    },
}

batchResult, err := BatchUpdateEntitiesSafely(ctx, client, entityType, entitiesToUpdate)
```

## Example Types

### Job Examples

Examples of job operations including:

- Creating a job
- Updating a job's status and rate
- Querying jobs by company, contractor, or rate
- Batch operations

### Timelog Examples

Examples of timelog operations including:

- Creating a timelog
- Adjusting a timelog
- Querying timelogs by job, time period, or duration
- Batch operations

### Payment Line Item Examples

Examples of payment line item operations including:

- Creating a payment line item
- Marking as paid
- Querying payment line items by job, timelog, or amount
- Batch operations

## Implementation Details

Each example demonstrates:

1. Direct gRPC calls using the Client API
2. GORM integration with the SCD plugin

For production use, always implement proper error handling and retry mechanisms.
