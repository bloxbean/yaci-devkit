file=kupo-2.7.2-amd64-Linux.tar.gz
wget https://github.com/CardanoSolutions/kupo/releases/download/v2.7/$file

mkdir /app/kupo

tar zxvf $file -C /app/kupo
chmod +x /app/kupo/bin/kupo

rm $file
