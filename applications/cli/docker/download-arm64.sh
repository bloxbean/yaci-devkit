file=cardano-8_9_2-aarch64-static-musl-ghc_964.tar.zst
dir=cardano-8_9_2-aarch64-static-musl-ghc_964
wget https://github.com/armada-alliance/cardano-node-binaries/raw/main/static-binaries/$file?raw=true -O - | tar -I zstd -xv

#unzip $file
mv $dir cardano-node
mv cardano-node /app/cardano-bin/
rm $file

submit_api_file=cardano-submit-api-3_2_2.zip
wget https://github.com/armada-alliance/cardano-node-binaries/raw/main/static-binaries/cardano-submit-api/$submit_api_file

unzip $submit_api_file
mv cardano-submit-api /app/cardano-bin/

rm $submit_api_file
