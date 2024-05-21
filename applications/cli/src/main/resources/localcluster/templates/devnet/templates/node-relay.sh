{{BIN_FOLDER}}/cardano-node run \
--config configuration.yaml \
--topology topology.json \
--database-path db \
--socket-path node.sock \
--port {{port}} \
| tee -a 'node.log'
