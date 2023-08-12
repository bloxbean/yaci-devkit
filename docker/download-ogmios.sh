version=v5.6.0
file=ogmios-${version}-x86_64-linux.zip
wget https://github.com/CardanoSolutions/ogmios/releases/download/${version}/$file

mkdir /app/ogmios
unzip $file -d /app/ogmios

chmod +x /app/ogmios/bin/ogmios

rm $file
