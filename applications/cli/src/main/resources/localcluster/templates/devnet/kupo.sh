#!/bin/bash

set -e

${KUPO_BIN} \
  --ogmios-host 0.0.0.0 \
  --ogmios-port ${OGMIOS_PORT} \
  --host 0.0.0.0 \
  --port ${KUPO_PORT} \
  --since origin \
  --match "*" \
  --workdir ${KUPO_DB_DIR}
