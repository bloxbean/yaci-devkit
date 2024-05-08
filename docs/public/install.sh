#!/bin/bash

# Function to check if a command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Check for required commands: curl and unzip
if ! command_exists curl; then
    echo "Error: curl is not installed. Please install curl and try again."
    exit 1
fi

if ! command_exists unzip; then
    echo "Error: unzip is not installed. Please install unzip and try again."
    exit 1
fi

# Determine version and artifact based on user input or fetch latest
if [ -z "$1" ]; then
    if ! command_exists jq; then
        echo "Error: jq is not installed. Please install jq and try again."
        exit 1
    fi
    # Fetch the latest release version from GitHub
    VERSION=$(curl -s "https://api.github.com/repos/bloxbean/yaci-devkit/releases/latest" | jq -r '.tag_name')
    ARTIFACT=$(curl -s "https://api.github.com/repos/bloxbean/yaci-devkit/releases/latest" | jq -r '.assets[0].name')
    echo "No version specified. Fetching latest version: $VERSION"
else
    VERSION=$1
    if [[ "$VERSION" == v* ]]; then
            echo "Error: Version parameter should not start with 'v'. Please provide the version number without 'v', e.g., 0.5.0."
            exit 1
    fi
    ARTIFACT="yaci-devkit-$VERSION.zip"
    echo "Fetching specified version: $VERSION"
fi

if [[ "$VERSION" == "null" ]] || [[ -z "$VERSION" ]]; then
    echo "Latest version not found. Please try again later or specify a version."
    exit 1
fi

if [[ "$ARTIFACT" == "null" ]] || [[ -z "$ARTIFACT" ]]; then
    echo "File not found for latest release"
    exit 1
fi


# Configuration
INSTALL_DIR="$HOME/.yaci-devkit"
REPO_URL="https://github.com/bloxbean/yaci-devkit/releases/download/v$VERSION/$ARTIFACT"
SCRIPT_NAME="devkit.sh"

echo "Downloading from: $REPO_URL"

# Check for existing installation
if [ -d "$INSTALL_DIR" ]; then
    echo "Existing installation detected at $INSTALL_DIR"
    read -p "Do you want to delete the existing installation and proceed? (y/n): " response
    if [[ "$response" == "y" ]]; then
        echo "Removing existing installation..."
        rm -rf "$INSTALL_DIR"
    else
        echo "Installation aborted."
        exit 1
    fi
fi

# Create the install directory if it doesn't exist
mkdir -p $INSTALL_DIR

# Download the latest scripts
echo "Downloading the DevKit scripts..."
if ! curl -L $REPO_URL -o "$INSTALL_DIR/devkit.zip"; then
    echo "Error downloading the DevKit scripts. Please check your connection and the URL."
    exit 1
fi

# Unzip and remove the archive
echo "Installing..."
if ! unzip "$INSTALL_DIR/devkit.zip" -d $INSTALL_DIR; then
    echo "Error unzipping the file. Please check the downloaded file and your unzip tool."
    rm "$INSTALL_DIR/devkit.zip"
    exit 1
fi
rm "$INSTALL_DIR/devkit.zip"

# Move and set up new installation
mkdir -p "$INSTALL_DIR/bin"
mv "$INSTALL_DIR/yaci-devkit-$VERSION"/* "$INSTALL_DIR/."
rm -rf "$INSTALL_DIR/yaci-devkit-$VERSION"  # Clean up leftover directory

# Create a symbolic link for easier access
ln -s "$INSTALL_DIR/bin/$SCRIPT_NAME" "$INSTALL_DIR/bin/devkit"

# Make scripts executable
chmod +x $INSTALL_DIR/bin/*.sh

echo "Yaci DevKit version $VERSION installed to $INSTALL_DIR"
echo "Trying to add the install directory to the PATH in .bashrc. If you encounter any issues, please add the following line manually:"
echo 'export PATH="$HOME/.yaci-devkit/bin:$PATH"'

# Add the install directory to the PATH in .bashrc
echo "Updating environment..."
if ! grep -q 'export PATH="$HOME/.yaci-devkit/bin:$PATH"' "$HOME/.bashrc"; then
    echo 'export PATH="$HOME/.yaci-devkit/bin:$PATH"' >> "$HOME/.bashrc"
    echo "Please log out and log back in to refresh your environment."
fi

echo "Installation complete."
