#!/bin/bash

cd "$(dirname "$0")"
cat ../config/env ../config/version > .env

CMD="docker-compose"
if ! command -v docker-compose &> /dev/null
then
    echo "docker-compose not found, let's try 'docker compose'"
    CMD="docker compose"
fi

$CMD exec yaci-cli /app/yaci-cli.sh $*
