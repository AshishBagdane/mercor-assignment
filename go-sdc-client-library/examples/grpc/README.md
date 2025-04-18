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
