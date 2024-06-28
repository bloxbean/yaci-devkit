#!/bin/bash

set -e

#Generate Cold Keys and a Cold_counter:
echo "\nGenerating pool certificate"
${BIN_FOLDER}/cardano-cli node issue-op-cert \
                          --kes-verification-key-file kes.vkey \
                          --cold-signing-key-file cold.skey \
                          --operational-certificate-issue-counter opcert.counter \
                          --kes-period $1 \
                          --out-file opcert.cert

