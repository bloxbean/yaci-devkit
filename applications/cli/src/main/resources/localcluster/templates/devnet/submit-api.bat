@echo off
setlocal enabledelayedexpansion

"${BIN_FOLDER}\cardano-submit-api.exe" --config submit-api-config.yaml ^
  --testnet-magic ${PROTOCOL_MAGIC} ^
  --socket-path node\node.sock ^
  --port ${SUBMIT_API_PORT} ^
  --listen-address 0.0.0.0 ^
  --metrics-port 0
