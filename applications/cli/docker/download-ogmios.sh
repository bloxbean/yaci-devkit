#!/bin/bash

# Assign the architecture based on user input and determine the correct suffix
case $1 in
    amd64)
        SUFFIX="x86_64"
        ;;
    arm64)
        SUFFIX="aarch64"
        ;;
    *)
        echo "Error: Invalid architecture specified. Use 'amd64' or 'arm64'."
        exit 1
        ;;
esac


version=v6.5.0
file=ogmios-${version}-${SUFFIX}-linux.zip
wget https://github.com/CardanoSolutions/ogmios/releases/download/${version}/$file

mkdir /app/ogmios
unzip $file -d /app/ogmios

chmod +x /app/ogmios/bin/ogmios

rm $file
