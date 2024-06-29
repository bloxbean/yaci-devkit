#!/bin/bash

set -e

#Generate Pool owner payment and Stake Keys
echo "\nGenerating payment and stake keys for pool"
${BIN_FOLDER}/cardano-cli address key-gen \
--verification-key-file payment.vkey \
--signing-key-file payment.skey

${BIN_FOLDER}/cardano-cli stake-address key-gen \
--verification-key-file stake.vkey \
--signing-key-file stake.skey

${BIN_FOLDER}/cardano-cli address build \
--payment-verification-key-file payment.vkey \
--stake-verification-key-file stake.vkey \
--out-file payment.addr \
--testnet-magic ${protocolMagic}

${BIN_FOLDER}/cardano-cli stake-address build \
--stake-verification-key-file stake.vkey \
--out-file stake.addr \
--testnet-magic ${protocolMagic}

echo "Pool owner payment address: " $(cat payment.addr)
echo "Pool owner stake address: " $(cat stake.addr)

#Generate Cold Keys and a Cold_counter:
echo "\nGenerating cold keys and cold counter for pool"
${BIN_FOLDER}/cardano-cli node key-gen \
--cold-verification-key-file cold.vkey \
--cold-signing-key-file cold.skey \
--operational-certificate-issue-counter-file opcert.counter

#Generate VRF keys
echo "\nGenerating VRF keys for pool"
${BIN_FOLDER}/cardano-cli node key-gen-VRF \
--verification-key-file vrf.vkey \
--signing-key-file vrf.skey


#Generate KES keys
echo "\nGenerate KES keys for pool"
${BIN_FOLDER}/cardano-cli node key-gen-KES \
--verification-key-file kes.vkey \
--signing-key-file kes.skey
