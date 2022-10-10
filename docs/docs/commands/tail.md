---
sidebar_position: 1
---

# tail

Once you start yaci-cli, you should see a prompt "yaci-cli:>"

## 1. Change background

For terminal with light background, use --color-mode option with "tail" command

```
yaci-cli:> tail --color-mode light
```

## 2. Stream from a Cardano node (tail)

Use "tail" command to stream from a Cardano node. The tail command supports :-

a> Stream from a public network (mainnet, legacy_testnet, prepod, preview) using a public relay

b> Stream from a public network using your own Cardano node

c> Stream from a private network


### 2.1 Stream from a public network using public relay

Specify the network name to stream using a public relay. The supported networks are  mainnet / legacy_testnet / prepod/ preview

Default network: mainnet

```
yaci-cli> tail    

yaci-cli> tail --network legacy_testnet

yaci-cli> tail --network prepod

yaci-cli> tail --network preview
```

### 2.2 Stream from a public network using your own Cardano node

```
yaci-cli> tail --network tail --network mainnet --host <Cardano Node Host> --port <Cardano Node Port)

```

### 2.3 Stream from a private network

To stream data from a private network, please provide host, port, protocol magic, known host, known port

Example:

```
yaci-cli:>tail --host localhost --port 30000 --protocol-magic 1 --known-slot 7055961 --known-blockhash f5753d8e7df48ed77eb1bc886e9b42c629e8a885ee88cfc994c127d2dff19641
```
