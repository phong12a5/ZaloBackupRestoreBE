#!/bin/bash

# Function to print a header
print_header() {
  echo "========================================"
  echo "$1"
  echo "========================================"
}

# Step 1: Build the Maven project
print_header "Building Maven Project"
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
  echo "Maven build failed. Exiting..."
  exit 1
fi

# Step 2: Build Docker images using Docker Compose
print_header "Building Docker Images"
docker-compose -f docker/docker-compose.yml build
if [ $? -ne 0 ]; then
  echo "Docker build failed. Exiting..."
  exit 1
fi

# Step 3: Stop all running containers if there are 1 or more
running containers
print_header "Stopping Running Containers"
running_containers=$(docker ps -q)
if [ -n "$running_containers" ]; then
  docker stop $running_containers
  if [ $? -ne 0 ]; then
    echo "Failed to stop running containers. Exiting..."
    exit 1
  fi
else
  echo "No running containers to stop."
fi

# Step 4: Restart containers using Docker Compose
print_header "Restarting Containers"
docker-compose -f docker/docker-compose.yml up -d
if [ $? -ne 0 ]; then
  echo "Failed to restart containers. Exiting..."
  exit 1
fi

sleep 1

#check mongoDB connection
print_header "Checking MongoDB Connection"
nc -zv localhost 27017
if [ $? -ne 0 ]; then
  echo "MongoDB connection failed. Exiting..."
  exit 1
fi

# check spring boot connection
print_header "Checking Spring Boot Connection"

#retry 3 times
for i in {1..3}; do
  curl http://localhost:8080/api/users
  if [ $? -eq 0 ]; then
    echo "Spring Boot connection successful."
    break
  else
    echo "Spring Boot connection failed. Retrying in 5 seconds..."
    sleep 3
  fi
done


print_header "All Steps Completed Successfully"