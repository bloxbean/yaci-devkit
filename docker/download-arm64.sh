file=1_35_5.zip
wget https://github.com/armada-alliance/cardano-node-binaries/raw/main/static-binaries/$file

unzip $file
mv cardano-node cardano-bin
rm $file
