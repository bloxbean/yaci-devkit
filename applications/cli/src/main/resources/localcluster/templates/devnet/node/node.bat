@echo off
setlocal enabledelayedexpansion

"${BIN_FOLDER}\cardano-node.exe" run ^
  --config configuration.yaml ^
  --topology topology.json ^
  --database-path "db" ^
  --socket-path "\\.\pipe\node.sock" ^
  --shelley-kes-key .\pool-keys\kes.skey ^
  --shelley-vrf-key .\pool-keys\vrf.skey ^
  --byron-delegation-certificate .\pool-keys\byron-delegation.cert ^
  --byron-signing-key .\pool-keys\byron-delegate.key ^
  --shelley-operational-certificate .\pool-keys\opcert.cert ^
  --port ${port} >> node.log 2>&1

