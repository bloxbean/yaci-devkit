file=cardano-node-1.35.5-linux.tar.gz
wget https://update-cardano-mainnet.iohk.io/cardano-node-releases/cardano-node-1.35.5-linux.tar.gz

mkdir cardano-bin

tar zxvf $file -C cardano-bin

rm $file
