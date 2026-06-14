#!/bin/bash
set -euo pipefail

case "${1:-}" in
    amd64)
        ARCH="x64"
        ;;
    arm64)
        ARCH="arm64"
        ;;
    *)
        echo "Error: Invalid architecture specified. Use 'amd64' or 'arm64'."
        exit 1
        ;;
esac

VERSION="${2:-0.1.0-pre6}"
TAG="${3:-v${VERSION}}"

file="yano-native-${VERSION}-linux-${ARCH}.zip"
url="https://github.com/bloxbean/yano/releases/download/${TAG}/${file}"

wget "${url}"

rm -rf /app/yano /tmp/yano
mkdir -p /app/yano /tmp/yano
unzip -q "${file}" -d /tmp/yano

yano_root="$(find /tmp/yano -mindepth 1 -maxdepth 1 -type d | head -n 1)"
if [ -z "${yano_root}" ] || [ ! -f "${yano_root}/yano" ]; then
    echo "Error: yano binary not found in ${file}"
    find /tmp/yano -maxdepth 3 -type f
    exit 1
fi

cp -R "${yano_root}/." /app/yano/
chmod +x /app/yano/yano
find /app/yano -type f -name '*.sh' -exec chmod +x {} +

if [ ! -f /app/yano/yano.git.properties ]; then
    {
        echo "release.tag=${TAG}"
        echo "release.file=${file}"
    } > /app/yano/yano.git.properties
fi

rm -rf "${file}" /tmp/yano
