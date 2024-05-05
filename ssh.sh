#!/bin/bash

cd "$(dirname "$0")"

CMD="docker-compose"
if ! command -v docker-compose &> /dev/null
then
    echo "docker-compose not found, let's try 'docker compose'"
    CMD="docker compose"
fi

$CMD --env-file env --env-file version exec yaci-cli /bin/bash

