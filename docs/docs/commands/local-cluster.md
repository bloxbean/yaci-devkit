---
sidebar_label: 'Local Cluster'
sidebar_position: 2
---

# Local Cluster Mode

:::info

This feature is currently supported only on **Linux** and **MacOS**

:::

**Need your own Cardano private network during development ?**

Using Local Cluster Mode commands, you can easily create and manage one or more 3-nodes local private network(s) on your local machine.
These networks are created from a pre-define cluster template. The current version of Yaci CLI allows to customize ``slotLength``
and ``node ports``. 

:::info

Make sure you have copied ``cardano-node`` binary to ``$user_home/.yaci-cli/bin`` folder. This folder
will be automatically created when you run yaci-cli first time.

Set executable permission for ``cardano-node`` binary. (if required)

:::

## 1. Create a Cluster

Create a cluster with ``create-cluster`` command. 

```shell
yaci-cli:>create-cluster mydevcluster

```

The above command will create a cluster with name ``mydevcluster``. The cluster folder can be found at
```$user_home/.yaci-cli/local-clusters/mydevcluster```

If the cluster creation is successful, you will see the cluster details like node ports and socket file locations.

Output (Example):

```shell
[Success] Create cluster folder !!!
[Success] Slot length updated in genesis.json
[Success] Update ports
[Success] Create Cluster : mydevcluster

###### Node Details ######
[Node ports] 3001 - 3002 - 3003
[Node Socket Paths] 
$userhome/.yaci-cli/local-clusters/mydevcluster/node-spo1/node.sock
$userhome/.yaci-cli/local-clusters/mydevcluster/node-spo2/node.sock
$userhome/.yaci-cli/local-clusters/mydevcluster/node-spo3/node.sock
```

### Create Cluster - Customization

#### a. Ports

By default, create-cluster command uses ``3000``, ``3001``, ``3002`` as node ports. To start nodes with different ports, you
can use ``--ports`` option.

```shell
yaci-cli:>create-cluster mydevcluster --ports 5001 5002 5003
```
The above command will create a cluster with ports 5001, 5002 and 5003.

#### b. Slot Length
By default, slotLength is set to 1 (1 sec). But if you want to create a cluster with different slot length,
you can do that using ``--slotLength`` option during cluster creation.

```shell
yaci-cli:>create-cluster mydevcluster --slotLength 0.2
```
The above command will create a cluster with 200ms slot length and hence shorter block time.

## 2. List Clusters

To see the available clusters, use ``list-clusters`` command.

```shell
yaci-cli:>list-clusters 
Available Clusters:
mydevcluster
democluster
```

## 3. Enter Cluster Mode

To start/stop and use other cluster mode commands, you need to enter the cluster mode for a cluster.
Use ``cluster`` command to enter cluster mode.


```shell
yaci-cli:>cluster mydevcluster
```

Output (Example) :

```shell
[Success] Switched to mydevcluster
local-cluster:mydevcluster>
```

## 4. Start a cluster
Once you are in cluster mode for a cluster, you can start the cluster with ``start`` command.

```shell
local-cluster:mydevcluster>start
```

If sucessful, you should see success message with a swagger-ui url. Using swagger-ui, you can interact
with the cluster. But currently only few endpoints are there. (Current epoch, address utxos etc.)
You can also interact with the cluster through ``cardano-cli``.

```shell
local-cluster:mydevcluster>start
[Success] Update Start time
[Success] Starting node from directory : $userhome/.yaci-cli/local-clusters/mydevcluster/node-spo1
[Success] Starting node from directory : $userhome/.yaci-cli/local-clusters/mydevcluster/node-spo2
[Success] Starting node from directory : $userhome/.yaci-cli/local-clusters/mydevcluster/node-spo3
[Info] Swagger Url to interact with the cluster's node : http://localhost:50056/swagger-ui.html
```

## 5. Show recent logs
To show recent logs, use ``logs`` command. It shows recent logs (last few hundreds lines) from each nodes.

```shell
local-cluster:mydevcluster>logs
```

## 6. Show default accounts

Using ``show-default-accounts`` command, you can see the available default accounts and utxos.

```shell
local-cluster:mydevcluster>show-default-accounts
```

Output:
```shell
Address
addr_test1vzs0r2nae22szlq3ul3h8t4u7rz9dr850mqjh98caddm4zcypzjjt
Utxos
347e9d3c72ddc52bdb43cf1c44d31507983c102e870398b9205fb8ed125f7e64#0 : [Amount(unit=lovelace, quantity=300000000000)]

Address
addr_test1vpf8vv32c7yzgdqh8hxxgsvstannw6ym6vymdzkckds5lkq4jtrmx
Utxos
650aae802cf0f3f5b214b6669caf823c34ff3648d749a8367bceed4bf64b2ae6#0 : [Amount(unit=lovelace, quantity=300000000000)]

Address
addr_test1vzaf27s0la5pvqsm9ta8jq97af506y8j678mtdjdurfr08qkeh3t3
Utxos
c59295f675c40a068596c0b816c997a8a2f564d6c090d5242444cf79d0ddec48#0 : [Amount(unit=lovelace, quantity=300000000000)]
```

## 7. Top-up a new address with test Ada
To topup a new address with some test Ada, you can use ``topup`` command. It uses one of the default account
and sends ada to the provided address.

```shell
local-cluster:mydevcluster>topup <address> --value <ada value>
```

Example:

```shell
local-cluster:mydevcluster>topup addr_test1qqwpl7h3g84mhr36wpetk904p7fchx2vst0z696lxk8ujsjyruqwmlsm344gfux3nsj6njyzj3ppvrqtt36cp9xyydzqzumz82 --value 5000
```

## 8. Check Utxos at an Address

Use ``utxos`` command to check utxos at an address.

```shell
local-cluster:mydevcluster>utxos <address>
```

Example:

```shell
local-cluster:mydevcluster>utxos addr_test1qqwpl7h3g84mhr36wpetk904p7fchx2vst0z696lxk8ujsjyruqwmlsm344gfux3nsj6njyzj3ppvrqtt36cp9xyydzqzumz82
f25ba08f473abbda5741a1c749a4eb067698a6342fe6be2aa87f5acbf0261307#0 : [Amount(unit=lovelace, quantity=5000000000)]
```

## 9. Check Tip
Use ``tip`` command to get the latest tip (Block no, Slot, Block Hash)

```shell
local-cluster:mydevcluster>tip
```

Output (Example):

```shell
local-cluster:mydevcluster>tip
[Block#] 2
[Slot#] 75
[Block Hash] e3f06194a390f486f8fad6ebe051bdf3f6066a23cd2d264fa11c492c488d2a3b
```

:::info
If the server is still starting up, the ``tip`` command may show the following error. In that case, just wait for
sometime and then try again.

```shell
[ERROR] Find tip error : Timeout on blocking read for 5000000000 NANOSECONDS
```
:::

## 10. Tail a running cluster
``ltail`` command is similar to ``tail`` command. But instead of connecting to a remote node, ``ltail`` 
command connects to currently running local cluster. This command can be used only in cluster mode.

Output (Example):

```shell
local-cluster:mydevcluster>ltail

=========================Connection Info=========================
Host          : localhost
Port          : 3001
ProtocolMagic : 42

2022-10-13 23:26:30,345 1633 [           main] INFO  c.b.c.y.c.p.chainsync.n2n.ChainsyncAgent - Trying to find the point [slot=0, hash='null']


Block : 178

Outputs
--------------------------------------------------------------------------------
Transactions  : 0
Block Size    : 0 KB


[ / ] Waiting for next block...
```

:::caution
**Known Issue:**
If you try to exit from ``ltail`` command using ``ctrl + c``, it doesn't exit gracefully and it stops the running cluster.
So you need to exit from yaci-cli to restart the cluster again.

To **avoid** this, it is recommended to start ``ltail`` command in a **separate** terminal after entering to the same cluster mode.

:::

## 11. Stop Cluster

Use ``stop`` command to stop the cluster.

```shell
local-cluster:mydevcluster>stop
```

If successful, you should see similar output

```shell
[Info] Trying to stop the running cluster ...
[Process] 2440
[Process] 2439
[Info] Stopping node process : Process[pid=2437, exitValue="not exited"]
[Success] Killed : Process[pid=2437, exitValue=143]
[Process] 2443
[Process] 2442
[Info] Stopping node process : Process[pid=2438, exitValue="not exited"]
[Success] Killed : Process[pid=2438, exitValue=143]
[Process] 2446
[Process] 2445
[Info] Stopping node process : Process[pid=2441, exitValue="not exited"]
[Success] Killed : Process[pid=2441, exitValue=143]
```

## 12. Get Cluster Info

Use ``info`` command to get information about the cluster.

```shell
local-cluster:mydevcluster>info
```

Output (Example):

```shell
###### Node Details ######
[Node ports] 3001 - 3002 - 3003
[Node Socket Paths] 
/Users/xxx/.yaci-cli/local-clusters/mydevcluster/node-spo1/node.sock
/Users/xxx/.yaci-cli/local-clusters/mydevcluster/node-spo2/node.sock
/Users/xxx/.yaci-cli/local-clusters/mydevcluster/node-spo3/node.sock
```

## 13. Delete a Cluster

Use ``delete-cluster`` command to delete an existing cluster.

```shell
local-cluster:mydevcluster>delete-cluster mydevcluster
```


