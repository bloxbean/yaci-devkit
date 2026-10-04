#!/bin/bash

set -e

{{BIN_FOLDER}}/cardano-cli genesis create-cardano \
  --genesis-dir {{genesisKeysHome}} \
  --gen-genesis-keys {{nGenesisKeys}} \
  --gen-utxo-keys {{nGenesisUtxoKeys}} \
  --supply {{genesisUtxoSupply}} \
  {{^mainnet}}
  --testnet-magic {{protocolMagic}} \
  {{/mainnet}}
  {{#mainnet}}
  --mainnet \
  {{/mainnet}}
  --slot-coefficient {{activeSlotsCoeff}} \
  --byron-template ../genesis-templates/spec/byron.json \
  --shelley-template ../genesis-templates/spec/shelley.json \
  --alonzo-template ../genesis-templates/spec/alonzo.json \
  --conway-template ../genesis-templates/spec/conway.json \
  --security-param {{securityParam}} \
  --slot-length {{slotLength}} \
  --node-config-template ../genesis-templates/spec/config.json
