file=cardano-node-9.1.1-linux.tar.gz
wget https://github.com/IntersectMBO/cardano-node/releases/download/9.1.1/cardano-node-9.1.1-linux.tar.gz

mkdir /app/cardano-bin

tar zxvf $file -C /app/cardano-bin

mv /app/cardano-bin/bin/* /app/cardano-bin/
rm -rf /app/cardano-bin/bin

rm $file
