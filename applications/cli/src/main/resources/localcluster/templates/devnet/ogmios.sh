#!/bin/bash

set -e

${OGMIOS_BIN} --node-socket node/node.sock --node-config node/configuration.json  --host 0.0.0.0 --port ${OGMIOS_PORT}
