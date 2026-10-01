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
     * Wait for a running catch-up to finish, so a stop or reset does not pull processes from under it.
     *
     * @return true when no catch-up is running any more
     */
    public boolean awaitIdle(Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (inProgress.get() && System.currentTimeMillis() < deadline)
            Thread.sleep(500);
        return !inProgress.get();
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
        return ChainLag.of(clusterInfo, tip._2.getSlot(), System.currentTimeMillis());
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
        if (!inProgress.compareAndSet(false, true))
            return Result.failed("A catch-up is already running");
        try {
            return doCatchUpCompanion(clusterInfo, clusterFolder, node, lag, writer);
        } finally {
            inProgress.set(false);
        }
    }

    private Result doCatchUpCompanion(ClusterInfo clusterInfo, Path clusterFolder, NodeControl node, ChainLag lag,
                                      Consumer<String> writer) {
        long startedAt = System.currentTimeMillis();
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
            if (!node.stopNode(quiet))
                return failed(step, "Could not stop the Haskell node");
            yanoCompanionService.updateTopologyForYanoPeering(clusterInfo, clusterFolder, quiet);
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
            if (!yanoService.start(clusterInfo, clusterFolder, YanoRunMode.CATCH_UP, quiet)
                    || !yanoBootstrapService.waitForNodeTip(httpPort, YANO_READY_TIMEOUT))
                return failed(step, "Yano did not start as a block producer (see 'yano-logs')");
            long slack = Math.max(2, lag.forecastWindowSlots() / 4);
            RelaySyncWaiter relayWaiter = new RelaySyncWaiter(SYNC_STALL_TIMEOUT_MS, 0);
            final Step backfilling = step;
            for (int round = 1; ; round++) {
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
                        System.currentTimeMillis()) - yanoTipSlot;
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
            boolean restarted = node.stopNode(quiet) && node.startNode(false, quiet);
            handedBack = true;
            if (!restarted)
                return failed(step, "Could not restart the Haskell node as the block producer");

            step.detail("waiting for the first block");
            Tuple<Long, Point> forged = waitForHaskellSlotAbove(clusterInfo, yanoTipSlot, forgeTimeout(clusterInfo));
            long duration = System.currentTimeMillis() - startedAt;
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
            Thread.currentThread().interrupt();
            return failed(step, "Catch-up interrupted");
        } catch (IOException | RuntimeException e) {
            log.error("Catch-up failed", e);
            return failed(step, "Catch-up failed: " + e.getMessage());
        } finally {
            if (!handedBack) {
                // Never leave the devnet half-way: Yano stopped, original topology, Haskell node as producer
                yanoService.stopQuietly();
                yanoCompanionService.restoreOriginalTopology(clusterFolder, quiet);
                if (node.stopNode(quiet))
                    node.startNode(false, quiet);
            }
        }
    }

    private static Result failed(Step step, String message) {
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
     * Start Yano for an existing yano-only devnet. With auto catch-up it first runs Yano without forging, backfills
     * whatever time has passed, and only then starts the live producer, so every skipped epoch gets its boundary.
     */
    public boolean startYanoOnly(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) {
        if (!autoCatchUp)
            return yanoService.start(clusterInfo, clusterFolder, YanoRunMode.LIVE, writer);

        if (!inProgress.compareAndSet(false, true)) {
            writer.accept(error("A catch-up is already running"));
            return false;
        }
        try {
            catchUpYanoOnly(clusterInfo, clusterFolder, writer);
        } finally {
            inProgress.set(false);
        }
        return yanoService.start(clusterInfo, clusterFolder, YanoRunMode.LIVE, writer);
    }

    /** Backfill a stopped yano-only chain to wall clock. Yano is stopped again on return. */
    private void catchUpYanoOnly(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) {
        int httpPort = clusterInfo.getYanoHttpPort();
        long startedAt = System.currentTimeMillis();
        try {
            if (!yanoService.start(clusterInfo, clusterFolder, YanoRunMode.CATCH_UP, problemsOnly(writer))
                    || !yanoBootstrapService.waitForNodeTip(httpPort, YANO_READY_TIMEOUT)) {
                writer.accept(warn("Could not check the chain tip before starting Yano; starting without catch-up"));
                return;
            }
            Tuple<Long, Point> tip = yanoBootstrapService.getNodeTip(httpPort);
            if (tip == null)
                return;
            ChainLag lag = ChainLag.of(clusterInfo, tip._2.getSlot(), System.currentTimeMillis());
            if (lag.lagSlots() == 0)
                return;

            // Short stops need no banner: the backfill is a block or two
            boolean report = lag.needsCatchUpOnStart();
            ProgressTask task = report ? ConsoleProgress.task(null, 1, writer) : null;
            if (report)
                printBanner(lag, task.logWriter());
            Step step = report ? task.step("Backfill to wall clock") : null;
            JsonNode caughtUp = yanoBootstrapService.catchUp(httpPort);
            if (caughtUp == null) {
                String message = "Yano's catch-up call failed (see 'yano-logs'); the first block will skip the gap";
                if (step != null)
                    step.fail(message);
                else
                    writer.accept(error(message));
                return;
            }
            if (report) {
                long toSlot = caughtUp.path("new_slot").asLong();
                step.done(String.format("%,d blocks to slot %,d", caughtUp.path("blocks_produced").asLong(), toSlot));
                task.log(success(resumedMessage(clusterInfo, lag, toSlot, System.currentTimeMillis() - startedAt)));
            }
        } finally {
            yanoService.stopQuietly();
        }
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
