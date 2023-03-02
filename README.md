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
## Update env file to fund test accounts

Update ```env``` file to include your test Cardano addresses to automatically topup Ada.

```
topup_addresses=<address1>:<ada_value>,<address2><ada_value>
```

**Example**

```
topup_addresses=addr_test1qzlwg5c3mpr0cz5td0rvr5rvcgf02al05cqgd2wzv7pud6chpzk4elx4jh2f7xtftjrdxddr88wg6sfszu8r3gktpjtqrr00q9:20000,addr_test1qqwpl7h3g84mhr36wpetk904p7fchx2vst0z696lxk8ujsjyruqwmlsm344gfux3nsj6njyzj3ppvrqtt36cp9xyydzqzumz82:10000
```

**Note:** You can also use topup command to fund your test addressses later.

## To start yaci-cli

Once the docker compose is up, start Yaci CLI using ```yaci-cli.sh```

```shell
$> ./yaci-cli.sh
```

## To stop DevKit

```shell
$> ./stop.sh
```

## Yaci CLI - Few Key Commands

This section explains a few key commands specific to Yaci CLI. For more details, please check https://yaci-cli.bloxbean.com.

### Create a default cluster

```
yaci-cli:>create-cluster
```
To overwrite data or reset the existing default cluster, use the "-o" flag

```
yaci-cli:>create-cluster -o
```

Now, you should be in the "local-cluster" context. To start the dev cluster, use the "start" command.
```
local-cluster:default>start
```

**To reset cluster's data**

```
local-cluster:default>reset
```

**To stop**

```
local-cluster:default>stop
```





