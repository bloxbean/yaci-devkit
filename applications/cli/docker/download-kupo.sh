#!/bin/bash

# Assign the architecture based on user input and determine the correct suffix
case $1 in
    amd64)
        ARCH="x86_64"
        ;;
    arm64)
        ARCH="aarch64"
        ;;
    *)
        echo "Error: Invalid architecture specified. Use 'amd64' or 'arm64'."
        exit 1
        ;;
esac

file=kupo-v2.9.0-$ARCH-linux.zip
wget https://github.com/CardanoSolutions/kupo/releases/download/v2.9/$file

mkdir /app/kupo
unzip $file -d /app/kupo

chmod +x /app/kupo/bin/kupo

rm $file
