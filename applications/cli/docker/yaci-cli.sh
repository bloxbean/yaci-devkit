#!/bin/bash

# Check if yaci_cli_mode is set in the environment, if not set it to "java"
if [ -z "$yaci_cli_mode" ]; then
    yaci_cli_mode="java"
fi

# Check the value of yaci_cli_mode and run the appropriate command
if [ "$yaci_cli_mode" == "java" ]; then
    java -jar /app/yaci-cli.jar $*
elif [ "$yaci_cli_mode" == "native" ]; then
    /app/yaci-cli $*
else
    echo "Invalid mode. Please use 'java' or 'native'."
    exit 1
fi
