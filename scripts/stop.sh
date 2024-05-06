#!/bin/bash

cd "$(dirname "$0")"

ENV_FILE="../config/env"
VERSION_FILE="../config/version"

CMD="docker-compose"
if ! command -v docker-compose &> /dev/null
then
    echo "docker-compose not found, let's try 'docker compose'"
    CMD="docker compose"
fi

$CMD --env-file $ENV_FILE --env-file $VERSION_FILE kill
