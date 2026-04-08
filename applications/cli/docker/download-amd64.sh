file=cardano-node-10.6.2-linux-amd64.tar.gz
wget https://github.com/IntersectMBO/cardano-node/releases/download/10.6.2/cardano-node-10.6.2-linux-amd64.tar.gz

mkdir /app/cardano-bin

tar zxvf $file -C /app/cardano-bin

mv /app/cardano-bin/bin/* /app/cardano-bin/
rm -rf /app/cardano-bin/bin

rm $file
