#!/bin/bash

set -e

#Generate pool registration certificate
#Parameters:
# $1 pledege: The amount of ADA that will be pledged to the pool
# $2 cost: The cost per epoch in lovelace
# $3 margin: The pool operator's margin
# $4 metadataurl: Metadata URL
# $5 metadatahash: Metadata Hash
echo "Generating pool registration certificate"
${BIN_FOLDER}/cardano-cli latest stake-pool registration-certificate \
--cold-verification-key-file cold.vkey \
--vrf-verification-key-file vrf.vkey \
--pool-pledge $1 \
--pool-cost $2 \
--pool-margin $3 \
--pool-reward-account-verification-key-file stake.vkey \
--pool-owner-stake-verification-key-file stake.vkey \
--testnet-magic ${protocolMagic} \
--metadata-url $4 \
--metadata-hash $5 \
--pool-relay-ipv4 $6 \
--pool-relay-port $7 \
--out-file pool-registration.cert
