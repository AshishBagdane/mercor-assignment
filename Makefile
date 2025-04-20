.PHONY: generate test build examples clean

# Default target
all: generate build test

# Generate code from proto files
generate:
	@echo "Generating code from proto files..."
	@./scripts/generate-protos.sh

# Build the library
build:
	@echo "Building library..."
	@go build ./...

# Run tests
test:
	@echo "Running tests..."
	@go test ./...

# Build examples
examples:
	@echo "Building examples..."
	@for dir in cmd/examples/*; do \
		echo "Building $${dir}..."; \
		go build -o bin/$$(basename $${dir}) ./$${dir}; \
	done

# Clean generated files
clean:
	@echo "Cleaning generated files..."
	@rm -rf api/*
	@rm -rf bin/*

# Install dependencies
deps:
	@echo "Installing dependencies..."
	@go get -u google.golang.org/grpc
	@go get -u google.golang.org/protobuf
	@go get -u gorm.io/gorm
	@go get -u gorm.io/driver/postgres
