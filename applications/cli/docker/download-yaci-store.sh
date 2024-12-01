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


version=0.1.1-graalvm-preview2
file=yaci-store-${version}-linux-musl-x64-n2c.zip
wget https://github.com/bloxbean/yaci-store/releases/download/rel-graal-${version}/$file

mkdir /app/store
unzip $file -d /app/store

cp /app/store/yaci-store-${version}-n2c/yaci-store /app/store/yaci-store
rm -rf /app/store/yaci-store-${version}-n2c

chmod +x /app/store/yaci-store

rm $file
