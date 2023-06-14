<div align="center">
<img src="static/DevKit.png" width="300">

<h4>A set of dev tools for development on Cardano</h4>
</div>

# What's Yaci DevKit

Say goodbye to lengthy iterations and hello to lightning-fast feedback loops. With **Yaci DevKit**, developers can accelerate 
their progress by leveraging a custom Cardano devnet that can be created and destroyed in a matter of seconds.

Powered by the user-friendly **Yaci CLI**, the devnet can be easily reset with a single command. This means you can iterate, 
experiment, and refine your code without any hassle. 

Tailor your devnet to suit your specific needs using the flexible configuration options available through the CLI. While 
the default devnet is optimized for speed, with a 1-second block time, 1-second slot, and 500 slots per epoch, you have the
freedom to customize these parameters. For instance, if you're focusing on stake registration, delegation, and reward withdrawal
transactions, creating a devnet with 30-second slots allows you to test these operations within a timeframe of 120-150 seconds.

But Yaci DevKit goes beyond just providing a devnet. It equips you with a lightweight chain indexer called **"Yaci Store"** and
a user-friendly browser-based viewer called **"Yaci Viewer"**. Seamlessly integrated into the DevKit, these components empower 
you to build and submit transactions effortlessly.

Deploying Yaci DevKit is a breeze with the Docker Compose package, making setup and configuration a hassle-free experience. 
Manage the entire process using simple commands such as "**start**," "**stop**," and "**reset**," giving you full control over your development
environment.

Yaci DevKit's chain indexer exposes required Blockfrost compatible API endpoints, enabling seamless integration with  client SDKs like 
**Cardano Client Lib (Java)** or **Lucid JS**. With the ability to effortlessly build and submit transactions to the Devnet through the
Blockfrost provider, your development possibilities are boundless.

# Components

Yaci DevKit docker-compose has following components

1. [Yaci CLI](https://github.com/bloxbean/yaci-cli)       - A CLI to create/manage a local cluster and other utilities
2. [Yaci Store](https://github.com/bloxbean/yaci-store)   - A lightweight indexer with H2 DB
3. [Yaci Viewer](https://github.com/bloxbean/yaci-viewer) - A minimal blockchain data viewer for developers
4. Cardano Node

Yaci DevKit provides API endpoints that can be used in your off-chain code (e.g., a Java app, Lucid JS app) to post transactions to your local cluster.

# Urls

**Yaci Viewer**                   : http://localhost:5173   (Browser)

**Yaci Store Swagger UI**         : http://localhost:8080/swagger-ui.html  (Browser)

**Yaci Store Api URL**            : http://localhost:8080/api/v1/  
<em>(Can be used in a Java app with Cardano Client Lib's Blockfrost backend or Javascript app with Lucid JS + Blockfrost provider as it exposes required BF compatible minimum apis for tx building and submission)</em>

# Component Versions
- [Yaci CLI](https://github.com/bloxbean/yaci-cli)    : v0.0.15
- [Yaci Store](https://github.com/bloxbean/yaci-store)  : v0.0.9
- [Yaci Viewer](https://github.com/bloxbean/yaci-viewer) : v0.0.5
- Cardano Node: 1_35_5

**Note:** Includes Cardano Node binaries for both amd64 and arm64. arm64 binary is from [Armada Alliance](https://github.com/armada-alliance/cardano-node-binaries)
  (Include both amd64 and arm64 binaries)

# How to Run

## Pre-requisites

- Docker Compose

## Clone this Git repo

```shell
git clone https://github.com/bloxbean/yaci-devkit.git
```

## To start the DevKit docker compose

```shell
./start.sh
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

**Important:** After updating env file, you need to restart the docker compose using ```./stop.sh``` and ```./start.sh```

**Note:** You can also use the topup command in Yaci CLI to fund your test addresses later.

## To start yaci-cli

Once the docker compose is up, start Yaci CLI using ```yaci-cli.sh```

```shell
./yaci-cli.sh
```

## To stop DevKit

```shell
./stop.sh
```

## Yaci CLI - Few Key Commands

This section explains a few key commands specific to Yaci CLI. For more details, please check https://yaci-cli.bloxbean.com.

### Create a default devnet

```
yaci-cli:>create-cluster
```
To overwrite data or reset the existing default devnet, use the "-o" flag.
Use --start flag to start the devnet after creation.

```
yaci-cli:>create-cluster -o
or,
yaci-cli:>create-cluster -o --start
```

To create devnet with a custom slots per epoch (By default 500 slots/epoch)

**For example:** Create and start a devnet with 30 slots per epoch

```
yaci-cli> create-cluster -o -e 30 --start

```

Now, you should be in the "local-cluster" context. To start the devnet, use the "start" command.

```
local-cluster:default>start
```

**Note** Now, with Yaci Viewer, you can conveniently check the devnet's data right from the browser. Simply open the following URL
in your browser to access the Yaci Viewer.

http://localhost:5173

### To reset cluster's data

If your devnet gets stuck or you simply want to reset the data and restart with the same configuration, simply use the command "reset".
It will restore your devnet to its initial state, allowing you to continue your development seamlessly.

```
local-cluster:default>reset
```

### To stop

```
local-cluster:default>stop
```

### To fund a new address

Easily fund your test account with ADA using the "topup" command.

```shell
local-cluster:default> topup <address> <ada value>
```

### To check utxos at an address

```shell
local-cluster:default> utxos <address>
```

### Docker Build

```shell
cd src
docker buildx build --platform linux/amd64,linux/arm64 --tag bloxbean/yaci-devkit:<version> . 
```




