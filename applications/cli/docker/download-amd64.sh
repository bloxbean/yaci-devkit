file=cardano-node-11.0.1-linux-amd64.tar.gz
wget https://github.com/IntersectMBO/cardano-node/releases/download/11.0.1/cardano-node-11.0.1-linux-amd64.tar.gz

mkdir /app/cardano-bin

tar zxvf $file -C /app/cardano-bin

mv /app/cardano-bin/bin/* /app/cardano-bin/
rm -rf /app/cardano-bin/bin

rm $file
