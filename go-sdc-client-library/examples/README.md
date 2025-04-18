# SCD Client Library Examples

This directory contains example applications demonstrating how to use the SCD Client Library.

## Basic Example

The basic example in the `basic` directory demonstrates how to:

1. Initialize the SCD client
2. Set up a GORM database with SCD integration
3. Perform basic CRUD operations with versioning
4. Query version history
5. Use the query helpers for filtering
6. Perform batch operations
7. Use the SCD client directly for more complex operations

### Running the Basic Example

```bash
# Make sure you have a running SCD service on localhost:50051
cd examples/basic
go run main.go
```

## Advanced Examples

More examples will be added in the future:

- **Distributed Transactions**: How to handle transactions across multiple services
- **Custom Model Extensions**: How to extend the base SCD model with custom functionality
- **High-Volume Processing**: Strategies for handling large datasets efficiently
- **Service Integration**: Examples of integrating with other services

## Testing Notes

The examples use PostgreSQL for data persistence. Make sure you have PostgreSQL set up correctly using the environment variables as specified in the example code.

These examples assume that you have a running SCD service. In a real environment, you would need to provide the correct address and potentially authentication credentials for the SCD service.
