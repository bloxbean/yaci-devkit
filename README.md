# Yaci DevKit

A set of dev tools for development on Cardano.

Yaci DevKit docker-compose has following components

1. [Yaci CLI](https://github.com/bloxbean/yaci-cli)       - A CLI to create/manage a local cluster and other utilities
2. [Yaci Store](https://github.com/bloxbean/yaci-store)   - A lightweight indexer with H2 DB
3. [Yaci Viewer](https://github.com/bloxbean/yaci-viewer) - A minimal blockchain data viewer for developers

Yaci DevKit provides API endpoints that can be used in your off-chain code (e.g., a Java app) to post transactions to your local cluster.

Urls
--------------------------------------------
Yaci Viewer                   : http://localhost:5173   (Browser)

Yaci Store Swagger UI         : http://localhost:8080/swagger-ui.html  (Browser)

Yaci Local Cluster Swagger UI : http://localhost:10000/swagger-ui.html  (Browser)

Yaci Store Api URL            : http://localhost:8080/api/v1/   (Can be used in Java app with Blockfrost backend)

Yaci Local Cluster Api URL    : http://localhost:10000/local-cluster/api/   (Can be used in Java app with Blockfrost backend)

# How to Run

## Pre-requisites

- Docker Compose

## Clone this Git repo

```shell
$> git clone https://github.com/bloxbean/yaci-devkit.git
```

## To start the DevKit docker composer

```shell
$> ./start.sh
```

## To start yaci-cli

Once the docker compose is up, start Yaci CLI using ```yaci-cli.sh```
```shell
$> ./yaci-cli.sh
```

## To stop DevKit

```shell
$> ./stop.sh
```
