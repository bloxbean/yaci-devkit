package com.bloxbean.cardano.yacicli.localcluster.catchup;

import java.util.function.Consumer;

/**
 * Stops and starts the devnet's Haskell node process, as a relay (no block producer keys) or as the block producer.
 * Implemented by {@link com.bloxbean.cardano.yacicli.localcluster.ClusterStartService}, which owns the process.
 */
public interface NodeControl {

    /** Stop the running node, gracefully so its ChainDB stays clean. Returns false if it could not be stopped. */
    boolean stopNode(Consumer<String> writer);

    /** Start the node as a relay (no forging) or as the block producer. Returns false if it did not start. */
    boolean startNode(boolean asRelay, Consumer<String> writer);
}
