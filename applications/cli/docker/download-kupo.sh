#!/bin/bash
set -euo pipefail

# PV11 workaround: IntersectMBO currently publishes the cardano-node 11.0.1
# compatible Kupo release only as a Linux x86_64 tarball. We bundle it for
# arm64 Docker images too so Apple Silicon Docker Desktop can run it through
# emulation. This may fail at runtime on native Linux arm64 hosts without
# x86_64 binfmt/qemu support. Keep the original CardanoSolutions downloader in
# download-kupo.sh.original for reverting once upstream publishes the needed
# assets again.
case "${1:-}" in
    amd64)
        ARCH="x86_64"
        ;;
    arm64)
        echo "Warning: IntersectMBO Kupo v2.11.0.1 has no Linux arm64 asset."
        echo "Bundling the Linux x86_64 artifact for arm64 Docker image compatibility on Docker Desktop."
        ARCH="x86_64"
        ;;
    *)
        echo "Error: Invalid architecture specified. Use 'amd64' or 'arm64'."
        exit 1
        ;;
esac

version=v2.11.0.1
file=kupo-${version}-${ARCH}-linux.tar.gz
url=https://github.com/IntersectMBO/kupo/releases/download/${version}/${file}

wget "${url}"

rm -rf /app/kupo
mkdir -p /app/kupo
tar -xzf "${file}" -C /app/kupo

chmod +x /app/kupo/bin/kupo

rm "${file}"
