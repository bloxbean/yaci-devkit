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

# Kupo release tags are major.minor (v2.12) while the asset carries the full version (v2.12.0)
version=v2.12.0
tag=v2.12
file=kupo-${version}-${ARCH}-linux.zip
url=https://github.com/CardanoSolutions/kupo/releases/download/${tag}/${file}

wget "${url}"

rm -rf /app/kupo
mkdir -p /app/kupo
unzip "${file}" -d /app/kupo

chmod +x /app/kupo/bin/kupo

rm "${file}"
