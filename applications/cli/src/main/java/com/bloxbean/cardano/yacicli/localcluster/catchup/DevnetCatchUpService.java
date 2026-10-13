package com.bloxbean.cardano.yacicli.localcluster.catchup;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yacicli.common.AnsiColors;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.common.Tuple;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.NodeMode;
import com.bloxbean.cardano.yacicli.localcluster.service.ClusterUtilService;
import com.bloxbean.cardano.yacicli.localcluster.yano.RelaySyncWaiter;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoBootstrapService;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoCompanionService;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoRunMode;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoService;
import com.bloxbean.cardano.yacicli.util.progress.ConsoleProgress;
import com.bloxbean.cardano.yacicli.util.progress.ProgressTask;
import com.bloxbean.cardano.yacicli.util.progress.Step;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

/**
 * Catches a devnet up to wall clock after a pause longer than the stability window (laptop lid closed, devnet
 * stopped for days), so developers keep the same chain instead of resetting it. See ADR-0017.
 * <p>
 * Companion: the Haskell node restarts as a relay, Yano follows its chain, Yano then backfills sparse empty
 * blocks to wall clock (one per forecast window plus every epoch boundary), the relay adopts them, and the node
 * restarts as the block producer, now inside its forecast window.
 * <p>
 * Yano-only: Yano starts on its chain without forging, backfills to wall clock, then restarts as the live producer.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DevnetCatchUpService {
    private static final long SYNC_STALL_TIMEOUT_MS = 30_000;
    private static final int MAX_PUMP_ROUNDS = 20;
    private static final Duration YANO_READY_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration NODE_READY_TIMEOUT = Duration.ofSeconds(90);

    private final YanoService yanoService;
    private final YanoBootstrapService yanoBootstrapService;
    private final YanoCompanionService yanoCompanionService;
    // ObjectProvider: ClusterUtilService -> ClusterService -> ClusterStartService -> this would be a constructor cycle
    private final ObjectProvider<ClusterUtilService> clusterUtilServiceProvider;

    // Catch up automatically: on start when the chain is behind, and from the watchdog when a running devnet stalls
    @Value("${devnet.auto.catch.up:true}")
    private boolean autoCatchUp = true;

    private final AtomicBoolean inProgress = new AtomicBoolean(false);
    // Wall clock, replaceable in tests
    LongSupplier clock = System::currentTimeMillis;
    // The thread running the current catch-up, and whether stop/reset asked it to give up
    private volatile Thread runner;
    private volatile boolean cancelled;

    /**
     * Outcome of a catch-up.
     *
     * @param success        the chain is at wall clock and the producer is forging
     * @param message        what happened, for the console and the admin API
     * @param fromSlot       tip slot before the catch-up
     * @param toSlot         tip slot after it
     * @param blocksProduced blocks Yano backfilled
     * @param durationMillis wall time the catch-up took
     */
    public record Result(boolean success, String message, long fromSlot, long toSlot, long blocksProduced,
                         long durationMillis) {
        static Result failed(String message) {
            return new Result(false, message, -1, -1, 0, 0);
        }

        static Result nothingToDo(String message, long tipSlot) {
            return new Result(true, message, tipSlot, tipSlot, 0, 0);
        }
    }

    public boolean isAutoCatchUp() {
        return autoCatchUp;
    }

    public boolean isInProgress() {
        return inProgress.get();
    }

    /**
     * Cancel a running catch-up and wait for it to give up, so a stop or reset does not race it: the catch-up's
     * thread is interrupted (its waits, Yano HTTP calls and sleeps all stop), it starts no further process, and its
     * clean-up does not restart the node. Whatever it already started is in the process list the stop then stops.
     *
     * @return true when no catch-up is running any more
     */
    public boolean cancelAndAwait(Duration timeout) throws InterruptedException {
        if (!inProgress.get())
            return true;
        cancelled = true;
        Thread thread = runner;
        if (thread != null && thread != Thread.currentThread())
            thread.interrupt();
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (inProgress.get() && System.currentTimeMillis() < deadline)
            Thread.sleep(100);
        return !inProgress.get();
    }

    /** Claim the catch-up slot for the current thread. */
    private boolean begin(Consumer<String> writer) {
        if (!inProgress.compareAndSet(false, true)) {
            writer.accept(error("A catch-up is already running"));
            return false;
        }
        cancelled = false;
        runner = Thread.currentThread();
        return true;
    }

    private void end() {
        runner = null;
        // A cancel interrupts this thread; do not leak the flag into whatever the thread does next
        if (cancelled)
            Thread.interrupted();
        inProgress.set(false);
    }

    /** Called before anything that starts a process: once stop/reset cancelled the catch-up, nothing is started. */
    private void checkCancelled() throws InterruptedException {
        if (cancelled || Thread.currentThread().isInterrupted())
            throw new InterruptedException("catch-up cancelled");
    }

    /** Why this devnet cannot be caught up, or empty when it can. */
    public Optional<String> unsupportedReason(ClusterInfo clusterInfo) {
        NodeMode mode = clusterInfo.getNodeMode();
        if (clusterInfo.isLocalMultiNodeEnabled())
            return Optional.of("catch-up does not support local multi-node devnets yet");
        if (mode == NodeMode.COMPANION) {
            if (clusterInfo.getActiveSlotsCoeff() < 1.0)
                return Optional.of("catch-up needs activeSlotsCoeff 1 in companion mode (block time equal to slot length): "
                        + "Yano's catch-up on an existing chain does not check VRF eligibility, which the Haskell node requires below 1");
            return Optional.empty();
        }
        if (mode == NodeMode.YANO_ONLY)
            return Optional.empty();
        return Optional.of("catch-up supports companion and yano-only devnets; this one runs in "
                + (mode == null ? "haskell-only" : mode.getValue()) + " mode");
    }

    /**
     * The chain's lag behind wall clock, read from the Haskell node (companion) or Yano (yano-only).
     *
     * @return the lag, or null when the tip cannot be read
     */
    public ChainLag probeLag(ClusterInfo clusterInfo) {
        Tuple<Long, Point> tip = clusterInfo.getNodeMode() == NodeMode.YANO_ONLY
                ? yanoBootstrapService.getNodeTip(clusterInfo.getYanoHttpPort())
                : haskellTip(clusterInfo);
        if (tip == null || tip._2 == null)
            return null;
        return ChainLag.of(clusterInfo, tip._2.getSlot(), clock.getAsLong());
    }

    /**
     * {@link #probeLag(ClusterInfo)} for a node that has just started: retries until the tip can be read.
     *
     * @return the lag, or null when the tip could not be read within the timeout
     */
    public ChainLag probeLagAfterStart(ClusterInfo clusterInfo, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            ChainLag lag = probeLag(clusterInfo);
            if (lag != null || System.currentTimeMillis() >= deadline)
                return lag;
            Thread.sleep(1000);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Companion
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Catch a companion devnet up to wall clock. The Haskell node must be running (as producer or relay); on
     * return it runs as the block producer again, with the original topology, whether or not the catch-up worked.
     */
    public Result catchUpCompanion(ClusterInfo clusterInfo, Path clusterFolder, NodeControl node, ChainLag lag,
                                   Consumer<String> writer) {
        if (!begin(writer))
            return Result.failed("A catch-up is already running");
        try {
            return doCatchUpCompanion(clusterInfo, clusterFolder, node, lag, writer);
        } finally {
            end();
        }
    }

    private Result doCatchUpCompanion(ClusterInfo clusterInfo, Path clusterFolder, NodeControl node, ChainLag lag,
                                      Consumer<String> writer) {
        long startedAt = clock.getAsLong();
        int httpPort = clusterInfo.getYanoHttpPort();
        long epochLength = clusterInfo.getEpochLength();
        ProgressTask task = ConsoleProgress.task(null, 4, writer);
        printBanner(lag, task.logWriter());
        // The node and Yano restarts inside a catch-up are not news to the user; warnings and errors still are
        Consumer<String> quiet = problemsOnly(task.logWriter());

        long blocksProduced = 0;
        long yanoTipSlot = lag.tipSlot();
        boolean handedBack = false;
        Step step = null;
        try {
            // 1. Haskell node as a relay, peering with Yano: it serves its chain to Yano and pulls Yano's backfill
            step = task.step("Haskell node → relay");
            checkCancelled();
            if (!node.stopNode(quiet))
                return failed(step, "Could not stop the Haskell node");
            yanoCompanionService.updateTopologyForYanoPeering(clusterInfo, clusterFolder, quiet);
            checkCancelled();
            if (!node.startNode(true, quiet))
                return failed(step, "Could not start the Haskell node as a relay");
            Tuple<Long, Point> relayTip = waitForHaskellTip(clusterInfo, NODE_READY_TIMEOUT);
            if (relayTip == null)
                return failed(step, "The Haskell relay did not answer tip queries");
            long haskellTipSlot = relayTip._2.getSlot();
            step.done(String.format("tip slot %,d", haskellTipSlot));

            // 2. Yano follows the Haskell chain. It already holds the bootstrap prefix, so only the blocks forged
            //    since the handover are fetched.
            step = task.step("Yano following the chain");
            yanoService.stopQuietly();
            checkCancelled();
            if (!yanoService.start(clusterInfo, clusterFolder, YanoRunMode.FOLLOW, quiet)
                    || !yanoBootstrapService.waitForNodeTip(httpPort, YANO_READY_TIMEOUT))
                return failed(step, "Yano did not start to follow the Haskell node (see 'yano-logs')");
            Tuple<Long, Point> yanoStart = yanoBootstrapService.getNodeTip(httpPort);
            long followFrom = yanoStart != null ? yanoStart._2.getSlot() : 0;
            final Step following = step;
            RelaySyncWaiter.Outcome followed = new RelaySyncWaiter(SYNC_STALL_TIMEOUT_MS, 0)
                    .awaitSlot(haskellTipSlot, epochLength, () -> yanoBootstrapService.getNodeTip(httpPort),
                            (slot, height, blocksPerSecond) -> following.progress(slot - followFrom,
                                    haskellTipSlot - followFrom, slotDetail(slot, haskellTipSlot, blocksPerSecond)));
            if (!followed.synced())
                return failed(step, "Yano could not sync the Haskell chain: " + followed.reason());
            yanoService.stopQuietly();
            step.done(String.format("at slot %,d", haskellTipSlot));

            // 3. Yano backfills to wall clock and the relay adopts it. The long block time keeps Yano from forging
            //    a live block at the wall-clock slot before the backfill runs. Repeat until the relay is within a
            //    quarter of the forecast window; the rest is for the producer restart.
            step = task.step("Backfill to wall clock");
            checkCancelled();
            if (!yanoService.start(clusterInfo, clusterFolder, YanoRunMode.CATCH_UP, quiet)
                    || !yanoBootstrapService.waitForNodeTip(httpPort, YANO_READY_TIMEOUT))
                return failed(step, "Yano did not start as a block producer (see 'yano-logs')");
            long slack = Math.max(2, lag.forecastWindowSlots() / 4);
            RelaySyncWaiter relayWaiter = new RelaySyncWaiter(SYNC_STALL_TIMEOUT_MS, 0);
            final Step backfilling = step;
            for (int round = 1; ; round++) {
                checkCancelled();
                JsonNode caughtUp = yanoBootstrapService.catchUp(httpPort);
                if (caughtUp == null)
                    return failed(step, "Yano's catch-up call failed (see 'yano-logs')");
                yanoTipSlot = caughtUp.path("new_slot").asLong(yanoTipSlot);
                blocksProduced += caughtUp.path("blocks_produced").asLong(0);

                final long target = yanoTipSlot;
                final long produced = blocksProduced;
                RelaySyncWaiter.Outcome adopted = relayWaiter.awaitSlot(target, epochLength,
                        () -> haskellTip(clusterInfo),
                        (slot, height, blocksPerSecond) -> backfilling.progress(slot - haskellTipSlot,
                                target - haskellTipSlot, String.format("%,d blocks, relay at slot %,d", produced, slot)));
                if (!adopted.synced())
                    return failed(step, "The Haskell relay did not sync Yano's backfill: " + adopted.reason());

                long lagNow = ChainLag.wallClockSlot(clusterInfo.getStartTime(), clusterInfo.getSlotLength(),
                        clock.getAsLong()) - yanoTipSlot;
                if (lagNow <= slack)
                    break;
                if (round >= MAX_PUMP_ROUNDS)
                    return failed(step, "The Haskell relay could not keep up with wall clock (still " + lagNow
                            + " slots behind)");
            }
            step.done(String.format("%,d blocks to slot %,d", blocksProduced, yanoTipSlot));

            // 4. Hand block production back to the Haskell node, as the first-run handover does: Yano stops, the
            //    original topology is restored, and the node restarts as the producer and forges on top.
            step = task.step("Haskell node back as block producer");
            yanoService.stopQuietly();
            yanoCompanionService.restoreOriginalTopology(clusterFolder, quiet);
            checkCancelled();
            // Only a successful restart counts as handed back; otherwise the recovery below retries it
            if (!node.stopNode(quiet) || !node.startNode(false, quiet))
                return failed(step, "Could not restart the Haskell node as the block producer");
            handedBack = true;

            step.detail("waiting for the first block");
            Tuple<Long, Point> forged = waitForHaskellSlotAbove(clusterInfo, yanoTipSlot, forgeTimeout(clusterInfo));
            long duration = clock.getAsLong() - startedAt;
            if (forged == null) {
                String message = "The chain is caught up, but the Haskell node has not forged yet. "
                        + "If it does not, run 'catch-up' again.";
                step.fail("no block yet");
                task.log(warn(message));
                return new Result(false, message, lag.tipSlot(), yanoTipSlot, blocksProduced, duration);
            }
            step.done(String.format("forging at slot %,d", forged._2.getSlot()));

            String message = resumedMessage(clusterInfo, lag, forged._2.getSlot(), duration);
            task.log(success(message));
            return new Result(true, message, lag.tipSlot(), forged._2.getSlot(), blocksProduced, duration);
        } catch (InterruptedException e) {
            return failed(step, cancelled ? "Catch-up cancelled" : "Catch-up interrupted");
        } catch (IOException | RuntimeException e) {
            log.error("Catch-up failed", e);
            return failed(step, "Catch-up failed: " + e.getMessage());
        } finally {
            // A cancel interrupts this thread: clear it so the clean-up's own waits (stopping Yano) still work
            Thread.interrupted();
            if (!handedBack) {
                // Never leave the devnet half-way: Yano stopped, original topology. Then the Haskell node back as
                // the producer, unless a stop/reset cancelled the catch-up: that stops whatever is running and must
                // not find the node restarted behind its back.
                yanoService.stopQuietly();
                yanoCompanionService.restoreOriginalTopology(clusterFolder, quiet);
                if (!cancelled && !(node.stopNode(quiet) && node.startNode(false, quiet)))
                    writer.accept(error("Could not restart the Haskell node as the block producer. "
                            + "Run 'stop' and 'start' to recover."));
            }
        }
    }

    private Result failed(Step step, String message) {
        // A step fails as a side effect of a cancel (an interrupted wait, a Yano start cut short): say so
        if (cancelled)
            message = "Catch-up cancelled";
        if (step != null)
            step.fail(message);
        return Result.failed(message);
    }

    private static String slotDetail(long slot, long target, long blocksPerSecond) {
        return String.format("slot %,d / %,d%s", slot, target, blocksPerSecond > 0 ? ", " + blocksPerSecond + " blocks/s" : "");
    }

    // ---------------------------------------------------------------------------------------------------------
    // Yano-only
    // ---------------------------------------------------------------------------------------------------------

    /**
     * Start Yano for an existing yano-only devnet.
     * <p>
     * With auto catch-up, Yano first runs without forging, the time that passed is backfilled, and only then the
     * live producer starts, so every skipped epoch gets its boundary. If the backfill fails while the gap crosses an
     * epoch boundary, live production is not started: its first block would jump over the missed epochs.
     * <p>
     * With auto catch-up off, a chain that is too far behind is left with Yano running but not forging (like a
     * stalled companion producer), and the user is told to run {@code catch-up}.
     *
     * @return true when Yano is running
     */
    public boolean startYanoOnly(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) {
        if (!begin(writer))
            return false;
        try {
            if (!autoCatchUp)
                return startYanoOnlyWithoutCatchUp(clusterInfo, clusterFolder, writer);

            YanoBackfill backfill = backfillYanoOnly(clusterInfo, clusterFolder, writer);
            if (!backfill.liveStartAllowed()) {
                writer.accept(error(backfill.message()));
                return false;
            }
            checkCancelled();
            return yanoService.start(clusterInfo, clusterFolder, YanoRunMode.LIVE, writer);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            end();
        }
    }

    /**
     * Catch a yano-only devnet up on request ({@code catch-up}, the admin API), whatever
     * {@code devnet.auto.catch.up} says: Yano is stopped, the gap backfilled, and the live producer started.
     */
    public Result catchUpYanoOnly(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) {
        if (!begin(writer))
            return Result.failed("A catch-up is already running");
        long startedAt = clock.getAsLong();
        try {
            yanoService.stopQuietly();
            YanoBackfill backfill = backfillYanoOnly(clusterInfo, clusterFolder, writer);
            if (!backfill.liveStartAllowed())
                return Result.failed(backfill.message());
            checkCancelled();
            if (!yanoService.start(clusterInfo, clusterFolder, YanoRunMode.LIVE, writer))
                return Result.failed("Yano did not start as the live producer (see 'yano-logs')");
            long duration = clock.getAsLong() - startedAt;
            return new Result(true, backfill.message(), backfill.fromSlot(), backfill.toSlot(), backfill.blocks(), duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.failed("Catch-up cancelled");
        } finally {
            end();
        }
    }

    private boolean startYanoOnlyWithoutCatchUp(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer)
            throws InterruptedException {
        int httpPort = clusterInfo.getYanoHttpPort();
        // Look at the tip with Yano serving but never forging, as the companion start does with its stalled producer.
        // Live production starts only once the tip is known and the lag is safe: an unknown lag is no permission.
        if (!yanoService.start(clusterInfo, clusterFolder, YanoRunMode.IDLE, problemsOnly(writer))
                || !yanoBootstrapService.waitForNodeTip(httpPort, YANO_READY_TIMEOUT)) {
            yanoService.stopQuietly();
            writer.accept(error("Yano did not start to check the chain tip (see 'yano-logs'); not starting block "
                    + "production, so no missed epoch is skipped. Retry with 'start'."));
            return false;
        }
        Tuple<Long, Point> tip = yanoBootstrapService.getNodeTip(httpPort);
        if (tip == null || tip._2 == null) {
            yanoService.stopQuietly();
            writer.accept(error("Could not read the chain tip from Yano; not starting block production. "
                    + "Retry with 'start'."));
            return false;
        }
        ChainLag lag = ChainLag.of(clusterInfo, tip._2.getSlot(), clock.getAsLong());
        if (!lag.liveStartSafe()) {
            writer.accept(warn("The chain is %s behind wall clock%s. Yano is running but not producing blocks; "
                            + "run 'catch-up' to bring it to wall clock.", lag.idleTimeText(),
                    lag.epochsToCross() > 0 ? String.format(" (%s)", epochs(lag.epochsToCross())) : ""));
            return true;
        }
        yanoService.stopQuietly();
        checkCancelled();
        return yanoService.start(clusterInfo, clusterFolder, YanoRunMode.LIVE, writer);
    }

    /**
     * Outcome of a yano-only backfill.
     *
     * @param liveStartAllowed the live producer can start: the chain is at wall clock, or the gap stays inside one
     *                         epoch so its first block skips no boundary
     */
    record YanoBackfill(boolean liveStartAllowed, String message, long fromSlot, long toSlot, long blocks) {
    }

    /** Backfill a stopped yano-only chain to wall clock with Yano in catch-up mode. Yano is stopped again on return. */
    private YanoBackfill backfillYanoOnly(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer)
            throws InterruptedException {
        int httpPort = clusterInfo.getYanoHttpPort();
        long startedAt = clock.getAsLong();
        try {
            checkCancelled();
            if (!yanoService.start(clusterInfo, clusterFolder, YanoRunMode.CATCH_UP, problemsOnly(writer))
                    || !yanoBootstrapService.waitForNodeTip(httpPort, YANO_READY_TIMEOUT))
                return new YanoBackfill(false, "Yano did not start to check the chain tip (see 'yano-logs'); "
                        + "not starting block production, so no missed epoch is skipped. Retry with 'start' or 'catch-up'.",
                        -1, -1, 0);
            Tuple<Long, Point> tip = yanoBootstrapService.getNodeTip(httpPort);
            if (tip == null)
                return new YanoBackfill(false, "Could not read the chain tip from Yano; not starting block production. "
                        + "Retry with 'start' or 'catch-up'.", -1, -1, 0);
            ChainLag lag = ChainLag.of(clusterInfo, tip._2.getSlot(), clock.getAsLong());
            if (lag.lagSlots() == 0)
                return new YanoBackfill(true, "The chain is at wall clock", lag.tipSlot(), lag.tipSlot(), 0);

            // Short stops need no banner: the backfill is a block or two
            boolean report = lag.needsCatchUpOnStart();
            ProgressTask task = report ? ConsoleProgress.task(null, 1, writer) : null;
            if (report)
                printBanner(lag, task.logWriter());
            Step step = report ? task.step("Backfill to wall clock") : null;
            long toSlot = lag.tipSlot();
            long blocks = 0;
            long slack = lag.yanoHandoffSlackSlots();
            // Yano backfills to the wall clock it reads when the call comes in, so a long backfill ends behind the
            // current one. Repeat until the gap left for the live producer's restart is small.
            for (int round = 1; ; round++) {
                checkCancelled();
                JsonNode caughtUp = yanoBootstrapService.catchUp(httpPort);
                checkCancelled();
                // Measure again after every call: it may have taken minutes, and epochs may have passed meanwhile
                if (caughtUp == null)
                    return gaveUp(step, "Yano's catch-up call failed",
                            ChainLag.of(clusterInfo, toSlot, clock.getAsLong()));
                toSlot = caughtUp.path("new_slot").asLong(toSlot);
                blocks += caughtUp.path("blocks_produced").asLong(0);
                ChainLag now = ChainLag.of(clusterInfo, toSlot, clock.getAsLong());
                if (now.lagSlots() <= slack)
                    break;
                if (round >= MAX_PUMP_ROUNDS)
                    return gaveUp(step, String.format("Yano's backfill could not keep up with wall clock (still %,d "
                            + "slots behind)", now.lagSlots()), now);
            }
            String message = resumedMessage(clusterInfo, lag, toSlot, clock.getAsLong() - startedAt);
            if (report) {
                step.done(String.format("%,d blocks to slot %,d", blocks, toSlot));
                task.log(success(message));
            }
            return new YanoBackfill(true, message, lag.tipSlot(), toSlot, blocks);
        } finally {
            // A cancel interrupts this thread: keep the flag for the caller, but let stopping Yano wait properly
            boolean interrupted = Thread.interrupted();
            yanoService.stopQuietly();
            if (interrupted)
                Thread.currentThread().interrupt();
        }
    }

    /**
     * A yano-only backfill that stopped short of wall clock. Inside one epoch the first live block skips no boundary,
     * so starting is harmless; otherwise live production is not started.
     */
    private YanoBackfill gaveUp(Step step, String why, ChainLag now) {
        if (step != null)
            step.fail(why);
        if (now.epochsToCross() == 0)
            return new YanoBackfill(true, why + "; the gap is inside one epoch", now.tipSlot(), now.tipSlot(), 0);
        return new YanoBackfill(false, String.format("%s (see 'yano-logs'). Not starting block production: its first "
                + "block would skip %s. Retry with 'catch-up'.", why, epochs(now.epochsToCross())),
                now.tipSlot(), now.tipSlot(), 0);
    }

    // ---------------------------------------------------------------------------------------------------------

    private void printBanner(ChainLag lag, Consumer<String> writer) {
        writer.accept(header(AnsiColors.CYAN_BOLD, String.format("⏸  Devnet was idle for %s (tip slot %,d, now slot %,d%s).",
                lag.idleTimeText(), lag.tipSlot(), lag.wallClockSlot(),
                lag.epochsToCross() > 0 ? String.format(", %s behind", epochs(lag.epochsToCross())) : "")));
        writer.accept(info("Catching up to wall clock with Yano (sparse backfill, about %,d blocks) ...",
                lag.estimatedBackfillBlocks()));
    }

    private static String resumedMessage(ClusterInfo clusterInfo, ChainLag lag, long tipSlot, long durationMillis) {
        long epoch = clusterInfo.getEpochLength() > 0 ? tipSlot / clusterInfo.getEpochLength() : 0;
        return String.format("Devnet resumed in %ds at slot %,d, epoch %,d. Your contracts, wallets and stake are "
                        + "unchanged%s.", Math.max(1, durationMillis / 1000), tipSlot, epoch,
                lag.epochsToCross() > 0 ? String.format("; %s passed", epochs(lag.epochsToCross())) : "");
    }

    private static String epochs(long count) {
        return String.format("%,d epoch%s", count, count == 1 ? "" : "s");
    }

    /** Forward only warnings and errors: the Yano and node restarts inside a catch-up are not news to the user. */
    static Consumer<String> problemsOnly(Consumer<String> writer) {
        return msg -> {
            if (msg != null && (msg.contains("🔴") || msg.contains("❗")))
                writer.accept(msg);
        };
    }

    private Tuple<Long, Point> haskellTip(ClusterInfo clusterInfo) {
        // Before the socket exists a tip query only prints "Find tip error" on the console
        if (clusterInfo.getSocketPath() != null && !Files.exists(Path.of(clusterInfo.getSocketPath())))
            return null;
        if (CommandContext.INSTANCE.getEra() == null && clusterInfo.getEra() != null)
            CommandContext.INSTANCE.setEra(clusterInfo.getEra());
        return clusterUtilServiceProvider.getObject().getTip(msg -> {});
    }

    private Tuple<Long, Point> waitForHaskellTip(ClusterInfo clusterInfo, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            Tuple<Long, Point> tip = haskellTip(clusterInfo);
            if (tip != null && tip._2 != null)
                return tip;
            Thread.sleep(1000);
        }
        return null;
    }

    private Tuple<Long, Point> waitForHaskellSlotAbove(ClusterInfo clusterInfo, long slot, Duration timeout)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            Tuple<Long, Point> tip = haskellTip(clusterInfo);
            if (tip != null && tip._2 != null && tip._2.getSlot() > slot)
                return tip;
            Thread.sleep(1000);
        }
        return null;
    }

    /**
     * Time to wait for the first forged block: the node has to open its ChainDB first, which took up to 30 s on a
     * busy machine, so at least two minutes.
     */
    private static Duration forgeTimeout(ClusterInfo clusterInfo) {
        double blockTime = clusterInfo.getBlockTime() > 0 ? clusterInfo.getBlockTime() : clusterInfo.getSlotLength();
        return Duration.ofMillis(Math.max(120_000, Math.round(blockTime * 20_000)));
    }
}
