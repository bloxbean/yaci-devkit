package com.bloxbean.cardano.yacicli.localcluster.yano;

import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.yano.bootstrap.ChainBootstrapContext;
import com.bloxbean.cardano.yacicli.localcluster.yano.bootstrap.ChainBootstrapRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

/**
 * First-run "time-travel" bootstrap for yano-only mode: start Yano in the past, shift epochs back,
 * run the generic chain bootstrap steps (cost-model governance, future customizations), catch up to
 * wall-clock so protocol params are enacted instantly, then sync the shifted genesis into the cluster
 * {@code node/genesis} dir so Yaci Store's slot/epoch/time math matches Yano.
 *
 * <p>This is the clean subset of {@link YanoCompanionService#bootstrap} without the Haskell node /
 * handover (there is no Haskell node in yano-only).
 *
 * <p>On full success a {@link ClusterConfig#YANO_BOOTSTRAP_COMPLETE_MARKER} marker is written so the
 * run is recognised as completed; an interrupted/failed bootstrap leaves no marker and is retried
 * (with stale Yano data cleaned) on the next start.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YanoTimeTravelBootstrap {

    private final YanoService yanoService;
    private final YanoBootstrapService yanoBootstrapService;
    private final ChainBootstrapRunner chainBootstrapRunner;
    private final YanoCompanionService yanoCompanionService;

    public boolean bootstrapYanoOnly(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) {
        int httpPort = clusterInfo.getYanoHttpPort();

        // 0. Clean any stale Yano data/config from a previous incomplete bootstrap so the
        //    past-time-travel start begins from a clean chain.
        cleanStaleYanoState(clusterFolder, writer);

        // 1. Start Yano in past-time-travel mode
        if (!yanoService.start(clusterInfo, clusterFolder, true, writer)) {
            writer.accept(error("Failed to start Yano."));
            return false;
        }

        // 2. Wait for Yano HTTP API
        if (!yanoBootstrapService.waitForReady(httpPort, writer)) {
            writer.accept(error("Yano HTTP API not ready."));
            yanoService.stop();
            return false;
        }

        // 3. Shift genesis back N epochs to create room for governance enactment
        writer.accept(info("Shifting genesis back %d epochs for protocol param enactment...",
                ClusterConfig.YANO_BOOTSTRAP_EPOCH_SHIFT));
        if (!yanoBootstrapService.shiftEpochs(httpPort, ClusterConfig.YANO_BOOTSTRAP_EPOCH_SHIFT, writer)) {
            writer.accept(error("Failed to shift epochs."));
            yanoService.stop();
            return false;
        }

        // 4. Run initial chainstate bootstrap steps (cost-model governance, etc.).
        //    yano-only has no later fallback, so a failed step is fatal (no partial-params chain).
        boolean bootstrapStepsOk = chainBootstrapRunner.runAll(
                new ChainBootstrapContext(clusterInfo, clusterFolder, httpPort), writer);
        if (!bootstrapStepsOk) {
            writer.accept(error("Chain bootstrap steps failed; yano-only devnet cannot start with incomplete protocol params."));
            yanoService.stop();
            return false;
        }

        // 5. Catch up to wall-clock (enacts params across the shifted epoch boundaries)
        writer.accept(info("Catching up to wall-clock time..."));
        if (!yanoBootstrapService.catchUpToWallClock(httpPort, writer)) {
            writer.accept(error("Failed to catch up to wall-clock."));
            yanoService.stop();
            return false;
        }

        // 6. Sync shifted genesis into node/genesis so the store's slot/epoch/time math matches Yano
        try {
            yanoCompanionService.syncShiftedGenesisToClusterNodeDir(clusterInfo, clusterFolder, writer);
        } catch (IOException e) {
            writer.accept(error("Failed to sync shifted genesis: " + e.getMessage()));
            yanoService.stop();
            return false;
        }

        // 7. Let Yano settle at wall-clock before the store starts indexing
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 8. Mark the bootstrap as complete (only now is a restart safe to skip the bootstrap).
        try {
            Path marker = clusterFolder.resolve(ClusterConfig.NODE_FOLDER_PREFIX)
                    .resolve(ClusterConfig.YANO_BOOTSTRAP_COMPLETE_MARKER);
            Files.writeString(marker, Instant.now().toString());
        } catch (IOException e) {
            // Non-fatal: the chain is up. Worst case the next start re-bootstraps.
            log.warn("Could not write yano bootstrap marker: {}", e.getMessage());
        }

        writer.accept(success("Yano-only time-travel bootstrap complete."));
        return true;
    }

    private void cleanStaleYanoState(Path clusterFolder, Consumer<String> writer) {
        Path[] stale = {
                clusterFolder.resolve(ClusterConfig.NODE_FOLDER_PREFIX).resolve("yano"),
                clusterFolder.resolve("yano-config")
        };
        for (Path p : stale) {
            if (p.toFile().exists()) {
                try {
                    FileUtils.deleteDirectory(p.toFile());
                    writer.accept(info("Cleaned stale Yano state: %s", p.getFileName()));
                } catch (IOException e) {
                    log.warn("Could not clean stale Yano state {}: {}", p, e.getMessage());
                }
            }
        }
    }
}
