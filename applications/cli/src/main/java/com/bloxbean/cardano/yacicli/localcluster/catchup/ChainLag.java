package com.bloxbean.cardano.yacicli.localcluster.catchup;

import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;

import java.time.Duration;

/**
 * How far a devnet's chain tip is behind wall clock, measured against the stability window.
 * <p>
 * A Haskell block producer can only forge while its ledger view can be forecast to the current slot, i.e. while
 * the tip is less than {@code floor(3k/f)} slots behind wall clock (the forecast window). A devnet that was paused
 * for longer (laptop lid closed, devnet stopped) stays stuck until it is caught up.
 *
 * @param tipSlot             slot of the chain tip
 * @param wallClockSlot       slot of the current wall-clock time
 * @param forecastWindowSlots {@code floor(3k/f)}
 * @param epochLength         slots per epoch
 * @param slotLengthSeconds   slot length in seconds
 */
public record ChainLag(long tipSlot, long wallClockSlot, long forecastWindowSlots, long epochLength,
                       double slotLengthSeconds) {

    public static ChainLag of(ClusterInfo clusterInfo, long tipSlot, long nowMillis) {
        return new ChainLag(tipSlot,
                wallClockSlot(clusterInfo.getStartTime(), clusterInfo.getSlotLength(), nowMillis),
                forecastWindowSlots(clusterInfo.getSecurityParam(), clusterInfo.getActiveSlotsCoeff()),
                clusterInfo.getEpochLength(),
                clusterInfo.getSlotLength());
    }

    /** {@code floor(3k/f)}, the slots a node can forecast its ledger view ahead of its tip. */
    public static long forecastWindowSlots(long securityParam, double activeSlotsCoeff) {
        if (securityParam <= 0 || activeSlotsCoeff <= 0)
            return 0;
        return (long) Math.floor(3 * securityParam / activeSlotsCoeff);
    }

    /** The slot at {@code nowMillis} for a chain whose slot 0 started at {@code startTimeSeconds}. */
    public static long wallClockSlot(long startTimeSeconds, double slotLengthSeconds, long nowMillis) {
        if (slotLengthSeconds <= 0)
            return 0;
        long slotLengthMillis = Math.round(slotLengthSeconds * 1000);
        return Math.max(0, (nowMillis - startTimeSeconds * 1000) / slotLengthMillis);
    }

    public long lagSlots() {
        return Math.max(0, wallClockSlot - tipSlot);
    }

    /** The producer can no longer forge: the tip is outside the forecast window. */
    public boolean stalled() {
        return forecastWindowSlots > 0 && lagSlots() >= forecastWindowSlots;
    }

    /**
     * Whether a (re)start should catch up before handing the chain to the producer: the lag is past three
     * quarters of the forecast window, which leaves the remaining quarter for the node to start and forge.
     */
    public boolean needsCatchUpOnStart() {
        return forecastWindowSlots > 0 && lagSlots() * 4 > forecastWindowSlots * 3;
    }

    /**
     * Yano-only: whether the live producer can start at wall clock with no backfill. Its first block processes one
     * epoch boundary as usual but jumps over any further ones ("epoch boundary 3 → 6"), which leaves those epochs
     * without protocol parameters and rewards. A securityParam set by hand can make the forecast window longer than
     * an epoch, so the lag threshold alone does not rule that out.
     */
    public boolean liveStartSafe() {
        return !needsCatchUpOnStart() && epochsToCross() <= 1;
    }

    /**
     * Yano-only: how close a backfill must get to wall clock before the live producer starts. The Yano restart in
     * between must not let the gap cross a second epoch boundary, so at most half an epoch, and at most a quarter of
     * the forecast window, as in companion mode.
     */
    public long yanoHandoffSlackSlots() {
        long slack = forecastWindowSlots > 0 ? forecastWindowSlots / 4 : epochLength / 2;
        if (epochLength > 0)
            slack = Math.min(slack, epochLength / 2);
        return Math.max(2, slack);
    }

    public long tipEpoch() {
        return epochLength > 0 ? tipSlot / epochLength : 0;
    }

    public long wallClockEpoch() {
        return epochLength > 0 ? wallClockSlot / epochLength : 0;
    }

    /** Epoch boundaries between the tip and wall clock. */
    public long epochsToCross() {
        return Math.max(0, wallClockEpoch() - tipEpoch());
    }

    /**
     * Rough number of blocks a sparse backfill places: one per {@code window - 1} slots (Yano's automatic
     * spacing for empty blocks), one at each epoch start, and one at the target.
     */
    public long estimatedBackfillBlocks() {
        if (lagSlots() == 0)
            return 0;
        long interval = Math.max(1, forecastWindowSlots - 1);
        return lagSlots() / interval + epochsToCross() + 1;
    }

    public Duration idleTime() {
        return Duration.ofMillis(Math.round(lagSlots() * slotLengthSeconds * 1000));
    }

    /** Idle time in the largest two units, e.g. {@code 3d 4h}, {@code 2h 5m}, {@code 7m 12s}, {@code 40s}. */
    public String idleTimeText() {
        return durationText(idleTime());
    }

    static String durationText(Duration d) {
        long days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        if (days > 0)
            return days + "d " + hours + "h";
        if (hours > 0)
            return hours + "h " + minutes + "m";
        if (minutes > 0)
            return minutes + "m " + seconds + "s";
        return seconds + "s";
    }
}
