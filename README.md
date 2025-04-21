# Mercor SCD Abstraction

A solution for handling Slowly Changing Dimensions (SCD) data across multiple applications.

## Overview

This project provides a unified approach to working with SCD tables where (id, version) pairs represent unique keys, with a uid column as primary key. The solution abstracts away the complexity of SCD handling, allowing developers to:

- Query latest versions transparently
- Create new versions automatically on updates
- Use familiar ORM patterns without worrying about SCD implementation details
- Optimize database queries for performance

## Architecture

The solution consists of two main components:

1. **Core SCD Service** (Spring Boot)
   - Centralized SCD query handling
   - gRPC API for cross-language compatibility
   - Optimized query execution
   - Version tracking and management

2. **Language-specific Client Libraries**
   - Go client with GORM integration
   - (Django client planned for future implementation)

## SCD Data Structure

The system works with tables following this SCD pattern:

```
| id      | version | uid     | ... other columns ... |
|---------|---------|---------|------------------------|
| job_123 | 1       | uid_456 | (original data)        |
| job_123 | 2       | uid_789 | (updated data)         |
```

Where:
- `(id, version)` pair uniquely identifies each record
- `uid` serves as the primary key
- The highest version for a given id represents the current state

## Features

- **Transparent version handling**: Access latest versions without explicit version conditions
- **Automatic versioning**: Updates create new versions rather than modifying existing records
- **History tracking**: Access full version history when needed
- **Optimized queries**: Performance-tuned for large datasets
- **Cross-language support**: Language-agnostic core service with native client libraries

## Example Usage

### Go (with GORM)

```go
// Create job repository
jobRepo := repository.NewJobRepository(db, scdClient)

// Fetch job by ID (gets latest version automatically)
job, err := jobRepo.GetJobRemote(ctx, "job_123")

// Update job status (creates new version automatically)
updatedJob, err := jobRepo.UpdateJobStatusRemote(ctx, job.ID, "extended")

// Get active jobs for a company (filters on latest versions)
jobs, err := jobRepo.GetActiveJobsForCompanyRemote(ctx, "company_456")
```

## Technical Stack

- **Core Service**: Java 17, Spring Boot, gRPC, PostgreSQL
- **Go Client**: Go 1.16+, GORM
- **API Protocol**: Protocol Buffers (protobuf)
- **Database**: PostgreSQL 12+

## Benefits

- **Reduced code duplication**: Common SCD handling logic centralized
- **Improved query performance**: Optimized data access patterns
- **Consistent versioning**: Standardized approach across the application
- **Simplified development**: Abstract away SCD complexity with familiar ORM patterns

## Project Status

- Core Service: Implemented
- Go Client: Implemented
- Django Client: Planned
