#!/bin/bash
set -e

# Check for protoc
if ! command -v protoc &> /dev/null; then
    echo "protoc is not installed. Please install Protocol Buffers."
    exit 1
fi

# Get the Go binary path
GOBIN=$(go env GOPATH)/bin
echo "Go binary path: $GOBIN"

# Add Go bin to PATH if not already there
if [[ ":$PATH:" != *":$GOBIN:"* ]]; then
    export PATH="$GOBIN:$PATH"
    echo "Added $GOBIN to PATH"
fi

# Check for protoc-gen-go
if ! command -v protoc-gen-go &> /dev/null; then
    echo "protoc-gen-go is not installed. Installing..."
    go install google.golang.org/protobuf/cmd/protoc-gen-go@latest
fi

# Check for protoc-gen-go-grpc
if ! command -v protoc-gen-go-grpc &> /dev/null; then
    echo "protoc-gen-go-grpc is not installed. Installing..."
    go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@latest
fi

# Verify protoc plugins are in path
echo "Checking if protoc plugins are in PATH:"
which protoc-gen-go || echo "protoc-gen-go not found in PATH"
which protoc-gen-go-grpc || echo "protoc-gen-go-grpc not found in PATH"

# Create the output directory if it doesn't exist
mkdir -p internal/proto/gen

# Generate the Go code
echo "Running protoc command..."
protoc \
    --proto_path=internal/proto \
    --go_out=internal/proto/gen \
    --go_opt=paths=source_relative \
    --go-grpc_out=internal/proto/gen \
    --go-grpc_opt=paths=source_relative \
    internal/proto/scd_service.proto

echo "Proto files generated successfully!" 