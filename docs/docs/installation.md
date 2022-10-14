---
sidebar_position: 2
---

# Installation

Download the latest yaci-cli Jar from [GitHub](https://github.com/bloxbean/yaci-cli/) 

**Supported platforms :** All platforms

**Requirement :**  Java 11 and above

[//]: # (a. Native Binary)

[//]: # ()
[//]: # (Supported Platforms :  Ubuntu &#40;amd64&#41;, Mac OS &#40;Intel / M1&#41;)

## How to Run ?

```shell
$> java -jar yaci-cli-<version>.jar
```
Once you start yaci-cli, you should see a prompt "yaci-cli:>"

To use ``local-cluster`` features, you need to copy ``cardano-node`` binary to ``$userhome/.yaci-cli/bin`` folder. This folder
will be automatically created when you run yaci-cli first time.

#### To see help message

```
yaci-cli:> help

yaci-cli:> help <command>

yaci-cli:> help tail
```
