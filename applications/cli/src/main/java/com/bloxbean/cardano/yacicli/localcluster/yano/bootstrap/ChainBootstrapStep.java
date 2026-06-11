package com.bloxbean.cardano.yacicli.localcluster.yano.bootstrap;

import org.springframework.core.Ordered;

import java.util.function.Consumer;

/**
 * A pluggable initial-chainstate customization that runs during the Yano past-bootstrap
 * window (after the genesis epoch shift, before catch-up to wall-clock). This is the
 * generic extension point for "set up initial chain state" — e.g. cost-model governance,
 * funding accounts, deploying reference scripts, custom governance actions.
 *
 * <p>Register a step as a Spring bean; it is picked up automatically and applied to every
 * time-travel bootstrap (yano-only, companion, yano-primary). Steps run in {@link #getOrder()}
 * order (lowest first).
 */
public interface ChainBootstrapStep extends Ordered {

    /** Short identifier used in logs. */
    String name();

    /**
     * Apply the step. Implementations submit transactions/governance via Yano's HTTP API
     * (port in {@code context.yanoHttpPort()}) while Yano produces blocks in the past.
     *
     * @return true on success; false to signal the step did not complete (bootstrap continues
     *         with remaining steps, but the overall run is reported as not fully successful).
     */
    boolean apply(ChainBootstrapContext context, Consumer<String> writer);

    /** Lower runs first. Default is late so unspecified steps run after the built-ins. */
    @Override
    default int getOrder() {
        return 1000;
    }
}
