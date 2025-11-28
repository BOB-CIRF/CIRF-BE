#!/bin/bash

# Install Java 21 if not already installed
if ! command -v java &> /dev/null; then
    echo "Java not found. Installing Java 21..."
    sudo yum install java-21-amazon-corretto-headless -y
else
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$JAVA_VERSION" != "21" ]; then
        echo "Java $JAVA_VERSION found. Installing Java 21..."
        sudo yum install java-21-amazon-corretto-headless -y
    else
        echo "Java 21 is already installed."
    fi
fi

echo "Dependencies check completed."
