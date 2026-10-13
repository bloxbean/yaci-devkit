package com.bloxbean.cardano.yacicli.localcluster.api.model;

import com.bloxbean.cardano.yacicli.localcluster.catchup.ChainLag;

/**
 * How far the devnet's chain tip is behind wall clock.
 *
 * @param tipSlot             slot of the chain tip
 * @param wallClockSlot       slot of the current wall-clock time
 * @param lagSlots            slots the tip is behind wall clock
 * @param forecastWindowSlots {@code floor(3k/f)}: a Haskell producer cannot forge once the lag reaches it
 * @param stalled             the lag has reached the forecast window; the chain needs a catch-up
 * @param idleTime            the lag as time, e.g. {@code 3d 4h}
 * @param epochsBehind        epoch boundaries between the tip and wall clock
 */
public record ChainLagResponse(long tipSlot, long wallClockSlot, long lagSlots, long forecastWindowSlots,
                               boolean stalled, String idleTime, long epochsBehind) {

    public static ChainLagResponse of(ChainLag lag) {
        return new ChainLagResponse(lag.tipSlot(), lag.wallClockSlot(), lag.lagSlots(), lag.forecastWindowSlots(),
                lag.stalled(), lag.idleTimeText(), lag.epochsToCross());
    }
}
