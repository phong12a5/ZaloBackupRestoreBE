#!/bin/bash

# Function to print a header
print_header() {
  echo "========================================"
  echo "$1"
  echo "========================================"
}

# Step 1: Build the Maven project
print_header "Building Maven Project"
./build.sh -a
if [ $? -ne 0 ]; then
  echo "Maven build failed. Exiting..."
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