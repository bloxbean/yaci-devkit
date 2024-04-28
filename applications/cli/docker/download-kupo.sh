#!/bin/bash

ARCH=$1

file=kupo-2.8.0-$ARCH-Linux.tar.gz
wget https://github.com/CardanoSolutions/kupo/releases/download/v2.8/$file

mkdir /app/kupo

tar zxvf $file -C /app/kupo
chmod +x /app/kupo/bin/kupo

rm $file
