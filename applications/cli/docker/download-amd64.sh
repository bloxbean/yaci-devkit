file=cardano-node-8.7.3-linux.tar.gz
wget https://github.com/IntersectMBO/cardano-node/releases/download/8.7.3/cardano-node-8.7.3-linux.tar.gz

mkdir /app/cardano-bin

tar zxvf $file -C /app/cardano-bin

rm $file
