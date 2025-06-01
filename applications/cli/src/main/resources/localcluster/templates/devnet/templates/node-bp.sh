{{BIN_FOLDER}}/cardano-node run \
--config configuration.json \
--topology topology.json \
--database-path db \
--socket-path node.sock \
--shelley-kes-key ../../../pool-keys/{{NODE_NAME}}/kes.skey \
--shelley-vrf-key ../../../pool-keys/{{NODE_NAME}}/vrf.skey \
--shelley-operational-certificate ../../../pool-keys/{{NODE_NAME}}/opcert.cert \
--port {{port}} \
| tee -a 'node.log'
