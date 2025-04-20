#!/bin/bash

# Function to build a service
build_service() {
  echo "Building $1..."
  cd $1 || exit
  mvn clean package -DskipTests
  if [ $? -ne 0 ]; then
    echo "Failed to build $1. Exiting..."
    exit 1
  fi
  cd - || exit
}

# Build auth-service
build_service "./auth-service"

# Build user-service
build_service "./user-service"

echo "All services built successfully!"