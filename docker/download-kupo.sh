file=kupo-2.5.1-amd64-Linux.tar.gz
wget https://github.com/CardanoSolutions/kupo/releases/download/v2.5/$file

mkdir /app/kupo

tar zxvf $file -C /app/kupo
chmod +x /app/kupo/bin/kupo

rm $file
