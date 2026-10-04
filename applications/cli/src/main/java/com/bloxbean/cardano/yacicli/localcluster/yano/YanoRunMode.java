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
     * Block producer on an existing chain with the longest block time Yano accepts (about 24.8 days), so it forges
     * nothing on its own during a catch-up. Blocks are only produced through {@code /devnet/epochs/catch-up}, and none
     * can land at the wall-clock slot before it. Only for the duration of a catch-up: use {@link #IDLE} to keep a
     * chain paused.
     */
    CATCH_UP,
    /**
     * Serves the chain (n2n server and HTTP API) with no block producer and no client, so it never forges: a
     * yano-only chain that is behind wall clock, waiting for {@code catch-up}.
     */
    IDLE;

    /** Block time used in {@link #CATCH_UP}: Yano's maximum, so no live block is scheduled during a catch-up. */
    public static final long CATCH_UP_BLOCK_TIME_MILLIS = Integer.MAX_VALUE;

    public boolean producesBlocks() {
        return this != FOLLOW && this != IDLE;
    }
}
