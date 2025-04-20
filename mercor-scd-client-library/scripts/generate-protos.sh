#!/bin/bash

# Exit on error
set -e

# Get the root directory of the project
ROOT_DIR=$(git rev-parse --show-toplevel)
PROTO_DIR="${ROOT_DIR}/proto"
API_DIR="${ROOT_DIR}/api"

# Make sure directories exist
mkdir -p ${API_DIR}

# Generate code for each proto directory
for dir in common core job paymentlineitems timelog; do
  echo "Generating code for ${dir}..."
  
  # Create output directory
  mkdir -p ${API_DIR}/${dir}
  
  # Find all proto files in the directory
  find ${PROTO_DIR}/${dir} -name "*.proto" -type f | while read -r file; do
    echo "Processing ${file}..."
    
    # Run protoc
    protoc -I=${PROTO_DIR} \
      --go_out=${API_DIR} \
      --go_opt=paths=source_relative \
      --go-grpc_out=${API_DIR} \
      --go-grpc_opt=paths=source_relative \
      ${file}
  done
done

echo "Proto code generation complete!"
