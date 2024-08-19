#!/bin/bash

cd "$(dirname "$0")"

SCRIPT_DIR="../scripts"
CONFIG_DIR="../config"

# This is the wrapper script for starting and stopping services and running yaci-cli.sh

# Function to display help message
function display_help() {
    echo "Usage: $0 [option]"
    echo
    echo "Options:"
    echo "  start   Start the DevKit containers and CLI."
    echo "  stop    Stop the DevKit containers."
    echo "  cli     Query the Cardano node in the DevKit container using cardano-cli."
    echo "          Usage: $0 cli <command>. For example, $0 cli query tip"
    echo "  ssh     Establish an SSH connection to the DevKit container."
    echo "  info    Display information about the Dev Node"
    echo "  version Display the version of the DevKit"
    echo "  help    Display this help message."
    echo
    echo "Please provide one of the above options."
}

# Check if an argument is provided
if [ $# -eq 0 ]; then
    display_help
    exit 1
fi

# Process the argument
case $1 in
    start)
        echo "Attempting to start the service..."
        bash $SCRIPT_DIR/start.sh

        # Check if start.sh was successful
        if [ $? -eq 0 ]; then
            echo "start.sh executed successfully. Running yaci-cli.sh..."
            first_arg="$1"
            shift
            bash $SCRIPT_DIR/yaci-cli.sh "$@"
        else
            echo "start.sh failed. Not executing yaci-cli.sh."
        fi
        ;;
    stop)
        echo "Stopping the service..."
        bash $SCRIPT_DIR/stop.sh
        ;;
    ssh)
        echo "ssh to Devkit container..."
        bash $SCRIPT_DIR/ssh.sh
        ;;
    info)
        echo "Info of Devkit"
        bash $SCRIPT_DIR/info.sh
        ;;
    cli)
        echo "Run cardano-cli in Devkit container..."
        # Discard the first argument
        first_arg="$1"
        shift
        bash $SCRIPT_DIR//cardano-cli.sh "$@"
        ;;
    version)
       # Show version information
      if [ -f $CONFIG_DIR/version ]; then
          source $CONFIG_DIR/version
          echo "DevKit Version: $tag"
          [ -n "$revision" ] && echo "Revision: $revision"
      else
          echo "Version file not found."
      fi
      ;;
    help)
        display_help
        ;;
    *)
        echo "Invalid argument: $1"
        display_help
        exit 1
        ;;
esac
