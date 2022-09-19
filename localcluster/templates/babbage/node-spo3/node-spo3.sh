cardano-node run \
--config ../configuration.yaml \
--topology topology.json \
--database-path db \
--socket-path node.sock \
--shelley-kes-key kes.skey \
--shelley-vrf-key vrf.skey \
--byron-delegation-certificate byron-delegation.cert \
--byron-signing-key byron-delegate.key \
--shelley-operational-certificate opcert.cert \
--port 3003 \
| tee -a 'node.log'
