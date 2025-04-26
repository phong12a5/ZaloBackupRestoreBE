#!/bin/bash

# Default values
BUILD_ALL=false
BUILD_SERVICE=true
SERVICE_TO_BUILD=""

# --- Use getopt to parse options ---
# --- Process parsed options ---
usage() {
    echo "Usage: $0 [-a | --all] [-s | --skip] [service_name]"
    echo "Options:"
    echo "  -a, --all       Build all services (default if no service name is provided)"
    echo "  -s, --skip      Skip building the service"
    exit 1
}

while getopts ":as" opt; do
    case $opt in
        a)
            BUILD_ALL=true
            ;;
        s)
            BUILD_SERVICE=false
            ;;
        \?)
            echo "Invalid option: -$OPTARG" >&2
            usage
            ;;
        :)
            echo "Option -$OPTARG requires an argument." >&2
            usage
            ;;
    esac
done
shift $((OPTIND-1)) # Shift processed options away
# --- End of option processing ---

# --- Process non-option arguments (service name) ---
if [ "$BUILD_ALL" = false ]; then
    # If -a is not set, a service name is required
    if [ $# -eq 0 ]; then
        echo "Error: Service name is required when '-a' is not specified." >&2
        usage # Call the usage function which exits
    else
        # If -a is not set AND arguments exist, take the first as service name
        SERVICE_TO_BUILD=$1
        echo "Service specified: $SERVICE_TO_BUILD"
        if [ $# -gt 1 ];then
            echo "Warning: Multiple service names provided, only using the first one ('$SERVICE_TO_BUILD')."
        fi
    fi
elif [ "$BUILD_ALL" = true ]; then
    # If -a is set
    if [ $# -gt 0 ]; then
        # If -a is set, ignore any service name provided
        echo "Warning: Service name '$1' provided but '-a' option is set. Building all services."
    else
        # -a is set and no other arguments, proceed to build all
        echo "Building all services..."
    fi
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
  if [ "$BUILD_SERVICE" = true ]; then
    echo "Building service..."
    build_service "./api-gateway"
    build_service "./auth-service"
    build_service "./user-service"
    echo "All services built successfully!"
  fi
elif [ -n "$SERVICE_TO_BUILD" ]; then
  echo "Building specific service: $SERVICE_TO_BUILD..."
  if [ -d "./$SERVICE_TO_BUILD" ]; then
    # if is frontend or mongoDB, skip build
    if [ "$SERVICE_TO_BUILD" = "frontend" ] || [ "$SERVICE_TO_BUILD" = "mongoDB" ]; then
      echo "Skipping build for $SERVICE_TO_BUILD."
    else
      if [ "$BUILD_SERVICE" = true ]; then
        build_service "./$SERVICE_TO_BUILD"
        echo "$SERVICE_TO_BUILD built successfully!"
      fi
    fi
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