#!/bin/bash

# This is the wrapper script for starting and stopping services and running yaci-cli.sh

# Check if an argument is provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 start|stop"
    exit 1
fi

# Process the argument
case $1 in
    start)
        echo "Attempting to start the service..."
        ./start.sh

        # Check if start.sh was successful
        if [ $? -eq 0 ]; then
            echo "start.sh executed successfully. Running yaci-cli.sh..."
            ./yaci-cli.sh
        else
            echo "start.sh failed. Not executing yaci-cli.sh."
        fi
        ;;
    stop)
        echo "Stopping the service..."
        ./stop.sh
        ;;
    *)
        echo "Invalid argument: $1"
        echo "Usage: $0 start|stop"
        exit 1
        ;;
esac
