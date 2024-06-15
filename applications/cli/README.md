# yaci-cli
A CLI tool aims to help developers building on Cardano. 

[https://yaci-cli.bloxbean.com/](https://yaci-cli.bloxbean.com/)

### Key Features
- Stream blockchain data from a Cardano node
- Create and manage a local private Cardano network (3 nodes cluster)  (Supported on Linux and MacOS)

<i>More features will be added in future releases.</i>

This CLI uses [Yaci](https://github.com/bloxbean/yaci-core), a mini protocol implementation in java, to interact with a remote/local Cardano node.

**Note:**
**This is a command line tool. It works best in a terminal with Dark background. For terminal with light background, use "--color-mode light" option to change the color scheme**


### For more details, check this [**User Guide**](https://yaci-cli.bloxbean.com/docs/intro)

## Installation Steps

- Go to the download section to download the yaci-cli Jar.

- Requirement :  Java 11 and above

- To run


```
$> java -jar yaci-cli-<version>.jar
```

## Run using Docker

### 1. Run without exposing ports to host

```
docker run -it  bloxbean/yaci-cli:<version>
```

### 2. Run with exposing ports to host

**env**

While using local cluster support, create a file called "env" if you want to enable Yaci Store support and fund your test ada address with some Ada.

```
yaci_store_enabled=true
topup_addresses=<address1>:<ada_amount>,<address2>:<ada_amount>
```

**Docker command**

```
docker run -it -v ~/clusters:/clusters --env-file env -p 3001:3001 -p 8090:8090 -p 10000:10000 -p 8080:8080 bloxbean/yaci-cli:<version>
```

- 3001 - Local Cluster Node Port
- 8090 - Submit Api port
- 10000 - Local Cluster Api endpoint port
- 8080 - Yaci Store Api endpoint port





## How to Use ?

### Stream from a public network using public relay

Specify the network name to stream using a public relay. The supported networks are  mainnet / legacy_testnet / prepod/ preview

Default network: mainnet

```
yaci-cli> tail --network <network>
```

### Stream from a public network using your own Cardano node

```
yaci-cli> tail --network tail --network mainnet --host <Cardano Node Host> --port <Cardano Node Port)

```

### Stream from a private network

To stream data from a private network, please provide host, port, protocol magic, known host, known port

Example:

```
yaci-cli:>tail --host localhost --port 30000 --protocol-magic 1 --known-slot 7055961 --known-blockhash f5753d8e7df48ed77eb1bc886e9b42c629e8a885ee88cfc994c127d2dff19641
```

### Create and start a cluster
```shell
yaci-cli>create-cluster mydevcluster
local-cluster:mydevcluster>start
```

### Build from source

```
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
./gradlew clean build -PskipSigning
```

### Docker build

```shell
docker build --build-arg="APP_VERSION=<app_version>" -t bloxbean/yaci-cli:<version> . 
```

### Docker multi-arch build and push
```shell
docker buildx build --build-arg="APP_VERSION=<app_version>" --push --platform linux/amd64,linux/arm64 --tag bloxbean/yaci-cli:<version> . 
```

