file=cardano-node-1.35.5-linux.tar.gz
wget https://update-cardano-mainnet.iohk.io/cardano-node-releases/cardano-node-1.35.5-linux.tar.gz

mkdir /app/cardano-bin

tar zxvf $file -C /app/cardano-bin

rm $file
