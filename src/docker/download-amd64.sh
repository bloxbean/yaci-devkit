file=cardano-node-8.1.2-linux.tar.gz
wget https://github.com/input-output-hk/cardano-node/releases/download/8.1.2/cardano-node-8.1.2-linux.tar.gz

mkdir /app/cardano-bin

tar zxvf $file -C /app/cardano-bin

rm $file
