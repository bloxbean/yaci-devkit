package com.bloxbean.cardano.yacicli.localcluster.yano;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yacicli.common.Tuple;

import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Waits for the Haskell relay to copy Yano's bootstrap chain during the companion handover.
 * <p>
 * The wait is progress-based. It keeps polling while the relay's tip is still advancing and gives up
 * only when the tip has not moved for {@code stallTimeoutMs}, or when the optional overall ceiling
 * {@code maxWaitMs} is reached ({@code 0} = no ceiling). A fixed deadline cut the relay off mid-chain
 * on long epochs: Yano was stopped, and the producer restarted with a tip already outside its own
 * forecast horizon, so the chain was frozen from its first minute.
 */
final class RelaySyncWaiter {
    static final long POLL_INTERVAL_MS = 1_000L;
    static final long PROGRESS_REPORT_INTERVAL_MS = 5_000L;

    /** Reads the relay's tip: block height and point. Returns {@code null} when the relay is not reachable yet. */
    interface TipReader {
        Tuple<Long, Point> readTip();
    }

    /** Sleeps for the given number of milliseconds. */
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    /**
     * Result of one wait.
     *
     * @param synced true when the relay's tip reached the target epoch
     * @param epoch  epoch of the last tip seen, or -1 when no tip was ever read
     * @param slot   slot of the last tip seen, or -1
     * @param height block height of the last tip seen, or -1
     * @param reason why the wait ended when {@code synced} is false; {@code null} otherwise
     */
    record Outcome(boolean synced, long epoch, long slot, long height, String reason) {
    }

    private final long stallTimeoutMs;
    private final long maxWaitMs;
    private final LongSupplier clock;
    private final Sleeper sleeper;

    RelaySyncWaiter(long stallTimeoutMs, long maxWaitMs) {
        this(stallTimeoutMs, maxWaitMs, System::currentTimeMillis, Thread::sleep);
    }

    RelaySyncWaiter(long stallTimeoutMs, long maxWaitMs, LongSupplier clock, Sleeper sleeper) {
        if (stallTimeoutMs <= 0) {
            throw new IllegalArgumentException("stallTimeoutMs must be positive, got " + stallTimeoutMs);
        }
        if (maxWaitMs < 0) {
            throw new IllegalArgumentException("maxWaitMs must be zero (no ceiling) or positive, got " + maxWaitMs);
        }
        this.stallTimeoutMs = stallTimeoutMs;
        this.maxWaitMs = maxWaitMs;
        this.clock = clock;
        this.sleeper = sleeper;
    }

    /**
     * Poll the relay's tip until it reaches {@code targetEpoch}, the tip stalls, or the ceiling is hit.
     *
     * @param targetEpoch epoch the relay must reach (tip slot / epochLength >= targetEpoch)
     * @param epochLength slots per epoch; must be positive
     * @param tipReader   reads the relay's current tip
     * @param progress    receives a progress line about every {@link #PROGRESS_REPORT_INTERVAL_MS}
     */
    Outcome await(long targetEpoch, long epochLength, TipReader tipReader, Consumer<String> progress)
            throws InterruptedException {
        if (epochLength <= 0) {
            return new Outcome(false, -1, -1, -1, "epoch length is not known");
        }

        final long startedAt = clock.getAsLong();
        long lastProgressAt = startedAt;
        long lastReportAt = startedAt;
        long lastReportHeight = -1;
        long slot = -1;
        long height = -1;
        long epoch = -1;

        while (true) {
            Tuple<Long, Point> tip = tipReader.readTip();
            long now = clock.getAsLong();

            if (tip != null && tip._2 != null) {
                long tipSlot = tip._2.getSlot();
                long tipHeight = tip._1 != null ? tip._1 : -1;
                if (tipSlot > slot || tipHeight > height) {
                    lastProgressAt = now;
                }
                slot = tipSlot;
                height = tipHeight;
                epoch = slot / epochLength;

                if (epoch >= targetEpoch) {
                    return new Outcome(true, epoch, slot, height, null);
                }

                if (now - lastReportAt >= PROGRESS_REPORT_INTERVAL_MS) {
                    long elapsedMs = Math.max(1, now - lastReportAt);
                    long blocksSinceReport = lastReportHeight >= 0 && height >= 0 ? height - lastReportHeight : 0;
                    long blocksPerSecond = blocksSinceReport * 1000 / elapsedMs;
                    progress.accept(String.format(
                            "Relay sync in progress: epoch %d of %d, slot %d, height %d (%d blocks/s)",
                            epoch, targetEpoch, slot, height, blocksPerSecond));
                    lastReportAt = now;
                    lastReportHeight = height;
                }
            }

            long sinceProgress = now - lastProgressAt;
            if (sinceProgress >= stallTimeoutMs) {
                String where = slot >= 0 ? " at slot " + slot + " (epoch " + epoch + ")" : " before any tip was read";
                return new Outcome(false, epoch, slot, height,
                        "relay tip did not advance for " + (sinceProgress / 1000) + "s" + where);
            }
            if (maxWaitMs > 0 && now - startedAt >= maxWaitMs) {
                return new Outcome(false, epoch, slot, height,
                        "overall wait ceiling of " + (maxWaitMs / 1000) + "s reached");
            }

            long untilStall = stallTimeoutMs - sinceProgress;
            long untilCeiling = maxWaitMs > 0 ? maxWaitMs - (now - startedAt) : Long.MAX_VALUE;
            sleeper.sleep(Math.max(1, Math.min(POLL_INTERVAL_MS, Math.min(untilStall, untilCeiling))));
        }
    }
}
