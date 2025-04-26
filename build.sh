#!/bin/bash

# Default values
BUILD_ALL=false
SERVICE_TO_BUILD=""

# --- Use getopt to parse options ---
# Define short options: 'a' (no argument needed)
# Define long options: 'all' (no argument needed)
# ' -- ' separates options from non-option arguments
# "$@" passes all script arguments to getopt
TEMP=$(getopt -o a --long all -n 'mvn-build-all.sh' -- "$@")

# Check if getopt ran successfully
if [ $? != 0 ]; then
    echo "Terminating..." >&2
    exit 1
fi

# Note the quotes around '$TEMP': they are essential!
eval set -- "$TEMP"
# --- End of getopt parsing setup ---

# --- Process parsed options ---
while true; do
    case "$1" in
        -a | --all) # Handle -a or --all
            BUILD_ALL=true
            shift # Consume the option
            ;;
        --) # End of options
            shift # Consume '--'
            break # Exit loop
            ;;
        *) # Should not happen with getopt
            echo "Internal error!"
            exit 1
            ;;
    esac
done
# --- End of option processing ---

# --- Process non-option arguments (service name) ---
# Any remaining arguments ($1, $2, etc.) are non-option arguments
if [ $# -gt 0 ] && [ "$BUILD_ALL" = false ]; then
    # If we are not building all, the first non-option arg is the service name
    SERVICE_TO_BUILD=$1
    echo "Service specified: $SERVICE_TO_BUILD"
    # You might want to add a check here if more than one service name is provided
    if [ $# -gt 1 ]; then
        echo "Warning: Multiple service names provided, only using the first one ('$SERVICE_TO_BUILD')."
    fi
elif [ $# -eq 0 ] && [ "$BUILD_ALL" = false ]; then
    # No arguments and -a not specified: default to build all
    echo "No specific service or '-a' option provided. Building all services (default)."
    BUILD_ALL=true
fi
# --- End of argument processing ---


# Function to build a service
build_service() {
  echo "Building $1..."
  cd "$1" || exit 1
  mvn clean package -DskipTests
  if [ $? -ne 0 ]; then
    echo "Failed to build $1. Exiting..."
    exit 1
  fi
  cd - > /dev/null || exit 1
}

# Build specific service or all services
if [ "$BUILD_ALL" = true ]; then
  echo "Building all services..."
  build_service "./api-gateway"
  build_service "./auth-service"
  build_service "./user-service"
  echo "All services built successfully!"
elif [ -n "$SERVICE_TO_BUILD" ]; then
  echo "Building specific service: $SERVICE_TO_BUILD..."
  if [ -d "./$SERVICE_TO_BUILD" ]; then
    build_service "./$SERVICE_TO_BUILD"
    echo "$SERVICE_TO_BUILD built successfully!"
  else
    echo "Error: Service directory './$SERVICE_TO_BUILD' not found."
    exit 1
  fi
else
    # This case should not be reached with the current logic
    echo "Internal logic error: No build action determined."
    exit 1
fi


# Restart docker-compose after build
echo "Restarting Docker containers..."
if [ "$BUILD_ALL" = true ]; then
  docker-compose down

  echo "Building all services with Docker Compose..."
  docker-compose -f docker-compose.yml build --no-cache
  docker-compose -f docker-compose.yml up -d
else
  docker-compose down $SERVICE_TO_BUILD

  echo "Building specific service with Docker Compose..."
  docker-compose up -d --no-deps --build "$SERVICE_TO_BUILD"
fi
echo "Docker containers are up and running!"