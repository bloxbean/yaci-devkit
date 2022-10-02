# yaci-cli
A CLI to stream data from a Cardano Node. 

<i>More features will be added in future releases.</i>

This CLI uses [Yaci](https://github.com/bloxbean/yaci-core), a mini protocol implementation in java, to interact with a remote Cardano node.

You can find native binaries for Ubuntu (amd64), Mac (Intel, M1) in download section. 

For all other platforms, please use the generic JAR file.

**Note:**
**This is a command line tool. It works best in a terminal with Dark background. For terminal with light background, use "--color-mode light" option to change the color scheme**


# How to Use:

## Installation Steps

#### 1. Go to the download section to download the yaci-cli binary. There are two kinds of distributions available.

**b. Generic Jar**
   
     Supported platforms : All platforms
   
     Requirement :  **Java 11**

**a. Native Binary**

     Supported Platforms :  Ubuntu (amd64), Mac OS (Intel / M1)

#### 2. For Generic Jar

You need **Java 11** to run yaci-cli jar file.

```
$> java -jar yaci-cli-0.0.2.jar
```

#### 3. For Native Binary

- Download platform specific binary if available.
- Rename the downloaded file to yaci-cli and change the permission
- Run yaci-cli from a terminal
- On Mac OS, you may need to allow this app to run (From System Preferences -> Security & Privacy)

```
$> mv yaci-cli-<os>-<arch>-<version>  yaci-cli

$> chmod +x yaci-cli

$> ./yaci-cli
```

## How to use ?

Once you start yaci-cli, you should see a prompt "yaci-cli:>"

#### 1. To see help message

```
yaci-cli:> help

yaci-cli:> help <command>

yaci-cli:> help tail
```

#### 2. For terminal with light background, use --color-mode option with "tail" command

```
yaci-cli:> tail --color-mode light
```

#### 2. Stream from a Cardano node (tail)

Use "tail" command to stream from a Cardano node. The tail command supports :-

a> Stream from a public network (mainnet, legacy_testnet, prepod, preview) using a public relay

b> Stream from a public network using your own Cardano node

c> Stream from a private network


**Stream from a public network using public relay**

Specify the network name to stream using a public relay. The supported networks are  mainnet / legacy_testnet / prepod/ preview

Default network: mainnet

```
yaci-cli> tail    

yaci-cli> tail --network legacy_testnet

yaci-cli> tail --network prepod

yaci-cli> tail --network preview
```

**Stream from a public network using your own Cardano node**

```
yaci-cli> tail --network tail --network mainnet --host <Cardano Node Host> --port <Cardano Node Port)

```

**Stream from a private network**

To stream data from a private network, please provide host, port, protocol magic, known host, known port

Example:

```
yaci-cli:>tail --host localhost --port 30000 --protocol-magic 1 --known-slot 7055961 --known-blockhash f5753d8e7df48ed77eb1bc886e9b42c629e8a885ee88cfc994c127d2dff19641
```


## Build from source

```
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
./gradlew clean build -PskipSigning
```

## Build Native Image

- Install GraalVM and set the path

```
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8
./gradlew clean build nativeCompile -PskipSigning
```
