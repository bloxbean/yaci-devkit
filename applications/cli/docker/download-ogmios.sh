#!/bin/bash
set -euo pipefail

# Assign the architecture based on user input and determine the correct suffix
case "${1:-}" in
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

version=v7.0.0
file=ogmios-${version}-${ARCH}-linux.zip
url=https://github.com/CardanoSolutions/ogmios/releases/download/${version}/${file}

wget "${url}"

rm -rf /app/ogmios
mkdir -p /app/ogmios
unzip "${file}" -d /app/ogmios

chmod +x /app/ogmios/bin/ogmios

rm "${file}"
