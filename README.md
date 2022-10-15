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
