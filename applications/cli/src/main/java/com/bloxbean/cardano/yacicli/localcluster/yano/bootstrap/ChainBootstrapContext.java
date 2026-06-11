package com.bloxbean.cardano.yacicli.localcluster.yano.bootstrap;

import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;

import java.nio.file.Path;

/**
 * Context passed to each {@link ChainBootstrapStep} during the past-bootstrap window
 * (after the epoch shift, before catch-up to wall-clock). Steps use this to submit
 * initial transactions / governance actions against Yano while it is producing blocks
 * in the past.
 */
public record ChainBootstrapContext(ClusterInfo clusterInfo, Path clusterFolder, int yanoHttpPort) {
}
