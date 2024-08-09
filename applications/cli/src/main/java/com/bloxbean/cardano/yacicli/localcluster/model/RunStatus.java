package com.bloxbean.cardano.yacicli.localcluster.model;

/**
 * Represents the run status of the cardano-node
 * @param stared
 * @param isFirstRun
 */
public record RunStatus(boolean stared, boolean isFirstRun) {
}
