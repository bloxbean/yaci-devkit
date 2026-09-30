package com.bloxbean.cardano.yacicli.localcluster.yano;

/**
 * How DevKit runs Yano.
 */
public enum YanoRunMode {
    /** Block producer at wall clock (yano-only, yano-primary, restarts). */
    LIVE,
    /** Block producer that starts in the past; the chain is shifted and caught up through the devnet API (first run). */
    PAST_TIME_TRAVEL,
    /** Follows the Haskell node's chain (client on, producer off) so Yano's chainstate reaches the Haskell tip. */
    FOLLOW,
    /**
     * Block producer on an existing chain with a very long block time, so it forges nothing on its own. Blocks are
     * only produced through {@code /devnet/epochs/catch-up}, and none can land at the wall-clock slot before it.
     */
    CATCH_UP;

    /** Block time used in {@link #CATCH_UP}: long enough that no live block is ever scheduled during a catch-up. */
    public static final long CATCH_UP_BLOCK_TIME_MILLIS = 3_600_000L;

    public boolean producesBlocks() {
        return this != FOLLOW;
    }
}
