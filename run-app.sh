#!/bin/bash

# BlueCone Application Startup Script
# This script ensures the application runs with Java 21

# Set Java 21 as JAVA_HOME
export JAVA_HOME=/Users/zhenpengmu/Library/Java/JavaVirtualMachines/ms-21.0.9/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH

echo "=========================================="
echo "BlueCone Application Startup"
echo "=========================================="
echo "Using Java: $(java -version 2>&1 | head -1)"
echo "JAVA_HOME: $JAVA_HOME"
echo "=========================================="

# Navigate to project directory
cd "$(dirname "$0")"

# Check if compiled
if [ ! -f "app-application/target/classes/com/bluecone/app/Application.class" ]; then
    echo "Application not compiled. Compiling now..."
    mvn clean install -DskipTests -Dmaven.test.skip=true -pl app-application -am
    if [ $? -ne 0 ]; then
        echo "Compilation failed!"
        exit 1
    fi
fi

echo ""
echo "Starting application..."
echo "=========================================="

# Run the application
cd app-application
java -jar target/bluecone-app.jar --spring.profiles.active=prod

# Or use Maven to run
# mvn spring-boot:run -Dspring-boot.run.profiles=prod

