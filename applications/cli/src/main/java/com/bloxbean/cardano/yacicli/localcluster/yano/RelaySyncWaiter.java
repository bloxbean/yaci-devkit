package com.bloxbean.cardano.yacicli.localcluster.yano;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yacicli.common.Tuple;

import java.util.function.Consumer;
import java.util.function.LongPredicate;
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
public final class RelaySyncWaiter {
    static final long POLL_INTERVAL_MS = 1_000L;
    static final long PROGRESS_REPORT_INTERVAL_MS = 5_000L;
    static final long DEFAULT_STALL_TIMEOUT_SECONDS = 30;
    // Largest value in seconds that still converts to milliseconds without overflowing
    private static final long MAX_SECONDS = Long.MAX_VALUE / 1000;

    /** Stall timeout to use for a configured value: a positive value as given, anything else the default. */
    public static long stallTimeoutSeconds(long configured) {
        return configured > 0 ? Math.min(configured, MAX_SECONDS) : DEFAULT_STALL_TIMEOUT_SECONDS;
    }

    /** Overall ceiling to use for a configured value: zero (no ceiling) or a positive value as given, negative is none. */
    public static long maxWaitSeconds(long configured) {
        return Math.min(Math.max(0, configured), MAX_SECONDS);
    }

    /** Reads the relay's tip: block height and point. Returns {@code null} when the relay is not reachable yet. */
    public interface TipReader {
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
    public record Outcome(boolean synced, long epoch, long slot, long height, String reason) {
    }

    private final long stallTimeoutMs;
    private final long maxWaitMs;
    private final LongSupplier clock;
    private final Sleeper sleeper;

    public RelaySyncWaiter(long stallTimeoutMs, long maxWaitMs) {
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
    public Outcome await(long targetEpoch, long epochLength, TipReader tipReader, Consumer<String> progress)
            throws InterruptedException {
        if (epochLength <= 0) {
            return new Outcome(false, -1, -1, -1, "epoch length is not known");
        }

        return awaitTip(slot -> slot / epochLength >= targetEpoch, epochLength,
                (slot, height, blocksPerSecond) -> String.format(
                        "Relay sync in progress: epoch %d of %d, slot %d, height %d (%d blocks/s)",
                        slot / epochLength, targetEpoch, slot, height, blocksPerSecond),
                tipReader, progress);
    }

    /**
     * Poll a tip until it reaches {@code targetSlot}, the tip stalls, or the ceiling is hit.
     *
     * @param targetSlot  slot the tip must reach
     * @param epochLength slots per epoch, only used for the epoch reported in the outcome
     * @param label       what is syncing, for the progress lines (e.g. "Yano sync")
     * @param tipReader   reads the current tip
     * @param progress    receives a progress line about every {@link #PROGRESS_REPORT_INTERVAL_MS}
     */
    public Outcome awaitSlot(long targetSlot, long epochLength, String label, TipReader tipReader,
                             Consumer<String> progress) throws InterruptedException {
        return awaitTip(slot -> slot >= targetSlot, Math.max(1, epochLength),
                (slot, height, blocksPerSecond) -> String.format(
                        "%s in progress: slot %d of %d, height %d (%d blocks/s)",
                        label, slot, targetSlot, height, blocksPerSecond),
                tipReader, progress);
    }

    private interface ProgressLine {
        String format(long slot, long height, long blocksPerSecond);
    }

    private Outcome awaitTip(LongPredicate reached, long epochLength, ProgressLine progressLine, TipReader tipReader,
                             Consumer<String> progress) throws InterruptedException {
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

                if (lastReportHeight < 0 && height >= 0) {
                    // First tip read: the baseline the first reported rate is measured from
                    lastReportHeight = height;
                    lastReportAt = now;
                }

                if (reached.test(slot)) {
                    return new Outcome(true, epoch, slot, height, null);
                }

                if (now - lastReportAt >= PROGRESS_REPORT_INTERVAL_MS) {
                    long elapsedMs = Math.max(1, now - lastReportAt);
                    long blocksSinceReport = lastReportHeight >= 0 && height >= 0 ? height - lastReportHeight : 0;
                    long blocksPerSecond = blocksSinceReport * 1000 / elapsedMs;
                    progress.accept(progressLine.format(slot, height, blocksPerSecond));
                    lastReportAt = now;
                    lastReportHeight = height;
                }
            }

            long sinceProgress = now - lastProgressAt;
            if (sinceProgress >= stallTimeoutMs) {
                String where = slot >= 0 ? " at slot " + slot + " (epoch " + epoch + ")" : " before any tip was read";
                return new Outcome(false, epoch, slot, height,
                        "tip did not advance for " + (sinceProgress / 1000) + "s" + where);
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
