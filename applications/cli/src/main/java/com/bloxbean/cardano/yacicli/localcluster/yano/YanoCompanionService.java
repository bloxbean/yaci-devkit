package com.bloxbean.cardano.yacicli.localcluster.yano;

import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.bloxbean.cardano.yacicli.common.AnsiColors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.bloxbean.cardano.client.crypto.Blake2bUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HexFormat;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

/**
 * Orchestrates the Yano companion mode bootstrap:
 * 1. Start Yano with past-time-travel
 * 2. Shift epochs back to create room for governance enactment
 * 3. Fund test accounts via Yano
 * 4. Submit cost model governance proposals (TODO: Phase 4)
 * 5. Catch up to wall-clock time
 * 6. Update topology so Haskell node syncs from Yano
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YanoCompanionService {
    private static final int BOOTSTRAP_EPOCH_SHIFT = 3;

    private final YanoService yanoService;
    private final YanoBootstrapService yanoBootstrapService;
    private final YanoGovernanceService yanoGovernanceService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Run the full companion bootstrap sequence before the Haskell node starts.
     * Returns true if bootstrap succeeded and Haskell node should proceed.
     */
    public boolean bootstrap(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) {
        int httpPort = clusterInfo.getYanoHttpPort();

        writer.accept(header(AnsiColors.CYAN_BOLD, "Starting Yano companion bootstrap..."));

        // 1. Start Yano with past-time-travel mode
        boolean started = yanoService.start(clusterInfo, clusterFolder, true, writer);
        if (!started) {
            writer.accept(error("Failed to start Yano. Falling back to haskell-only mode."));
            return false;
        }

        // 2. Wait for Yano HTTP API
        if (!yanoBootstrapService.waitForReady(httpPort, writer)) {
            writer.accept(error("Yano HTTP API not ready. Falling back to haskell-only mode."));
            yanoService.stop();
            return false;
        }

        // 3. Shift genesis back N epochs to create room for governance
        writer.accept(info("Shifting genesis back %d epochs for protocol param enactment...", BOOTSTRAP_EPOCH_SHIFT));
        if (!yanoBootstrapService.shiftEpochs(httpPort, BOOTSTRAP_EPOCH_SHIFT, writer)) {
            writer.accept(error("Failed to shift epochs. Falling back to haskell-only mode."));
            yanoService.stop();
            return false;
        }

        // 4. Submit governance proposals (cost model update) via Yano
        writer.accept(info("Submitting governance proposals via Yano..."));
        boolean governanceDone = yanoGovernanceService.submitCostModelGovernance(clusterInfo, clusterFolder, writer);
        if (!governanceDone) {
            writer.accept(warn("Governance proposals failed. Cost models will need manual update after startup."));
        }

        // 5. Catch up to wall-clock time
        writer.accept(info("Catching up to wall-clock time..."));
        if (!yanoBootstrapService.catchUpToWallClock(httpPort, writer)) {
            writer.accept(error("Failed to catch up to wall-clock. Falling back to haskell-only mode."));
            yanoService.stop();
            return false;
        }

        // 6. Sync shifted genesis files back to Haskell node
        try {
            syncYanoGenesisToHaskellNode(clusterInfo, clusterFolder, writer);
        } catch (IOException e) {
            writer.accept(error("Failed to sync genesis: " + e.getMessage()));
            yanoService.stop();
            return false;
        }

        // 6b. Query and log Yano's epoch nonce for comparison with Haskell
        try {
            var nonceInfo = yanoBootstrapService.getEpochNonce(httpPort);
            if (nonceInfo != null) {
                writer.accept(info("Yano epoch nonce: epoch=%s, nonce=%s",
                        nonceInfo.path("epoch").asText("?"),
                        nonceInfo.path("nonce").asText("?")));
            }
        } catch (Exception e) {
            log.debug("Could not query Yano epoch nonce: {}", e.getMessage());
        }

        // 7. Wait for a few blocks so Yano has a tip for Haskell to sync from
        writer.accept(info("Waiting for Yano to produce a few blocks at wall-clock time..."));
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 8. Update topology so Haskell node peers with Yano
        try {
            updateTopologyForYanoPeering(clusterInfo, clusterFolder, writer);
        } catch (IOException e) {
            writer.accept(error("Failed to update topology: " + e.getMessage()));
            yanoService.stop();
            return false;
        }

        writer.accept(success("Yano companion bootstrap complete. Starting Haskell node..."));
        return true;
    }

    /**
     * Copy Yano's shifted shelley-genesis.json back to the Haskell node's genesis dir,
     * and update byron-genesis.json startTime to match.
     */
    public void syncYanoGenesisToHaskellNode(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) throws IOException {
        Path yanoConfigDir = clusterFolder.resolve("yano-config").resolve("network").resolve("devnet");
        Path haskellGenesisDir = clusterFolder.resolve("node").resolve("genesis");

        // 1. Copy Yano's updated shelley-genesis.json back to Haskell node
        Path yanoShelley = yanoConfigDir.resolve("shelley-genesis.json");
        Path haskellShelley = haskellGenesisDir.resolve("shelley-genesis.json");
        Files.copy(yanoShelley, haskellShelley, StandardCopyOption.REPLACE_EXISTING);

        // 2. Read the shifted systemStart and update byron-genesis.json startTime to match
        ObjectNode shelleyJson = (ObjectNode) objectMapper.readTree(haskellShelley.toFile());
        String systemStart = shelleyJson.get("systemStart").asText();
        long epochSeconds = Instant.parse(systemStart).getEpochSecond();

        Path byronGenesis = haskellGenesisDir.resolve("byron-genesis.json");
        ObjectNode byronJson = (ObjectNode) objectMapper.readTree(byronGenesis.toFile());
        byronJson.put("startTime", epochSeconds);
        objectMapper.writer(new DefaultPrettyPrinter()).writeValue(byronGenesis.toFile(), byronJson);

        // 3. Update ClusterInfo with the shifted start time
        clusterInfo.setStartTime(epochSeconds);

        // 4. Compute and log the genesis hash (= initial epoch nonce)
        byte[] genesisBytes = Files.readAllBytes(haskellShelley);
        byte[] genesisHash = Blake2bUtil.blake2bHash256(genesisBytes);
        String genesisHashHex = HexFormat.of().formatHex(genesisHash);
        writer.accept(info("Synced shifted genesis to Haskell node (systemStart: %s)", systemStart));
        writer.accept(info("Shelley genesis hash (epoch nonce): %s", genesisHashHex));
    }

    /**
     * Update the Haskell node's topology.json to peer with Yano.
     * Backs up original topology first.
     */
    public void updateTopologyForYanoPeering(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) throws IOException {
        Path topologyPath = clusterFolder.resolve("node").resolve("topology.json");
        Path topologyBackup = clusterFolder.resolve("node").resolve("topology-original.json");

        if (!Files.exists(topologyBackup) && Files.exists(topologyPath)) {
            Files.copy(topologyPath, topologyBackup, StandardCopyOption.REPLACE_EXISTING);
        }

        // Build topology pointing to Yano
        ObjectNode topology = objectMapper.createObjectNode();

        ArrayNode localRoots = objectMapper.createArrayNode();
        ObjectNode localRoot = objectMapper.createObjectNode();
        ArrayNode accessPoints = objectMapper.createArrayNode();
        ObjectNode accessPoint = objectMapper.createObjectNode();
        accessPoint.put("address", "127.0.0.1");
        accessPoint.put("port", clusterInfo.getYanoServerPort());
        accessPoints.add(accessPoint);
        localRoot.set("accessPoints", accessPoints);
        localRoot.put("valency", 1);
        localRoots.add(localRoot);
        topology.set("localRoots", localRoots);

        ArrayNode publicRoots = objectMapper.createArrayNode();
        topology.set("publicRoots", publicRoots);

        topology.put("useLedgerAfterSlot", -1);

        objectMapper.writer(new DefaultPrettyPrinter()).writeValue(topologyPath.toFile(), topology);
        writer.accept(info("Updated topology.json to peer with Yano (port %d)", clusterInfo.getYanoServerPort()));
    }

    /**
     * Hand over block production from Yano to the Haskell node.
     * Waits for Haskell to sync Yano's chain, then stops Yano and restores topology.
     *
     * @param clusterInfo   cluster configuration
     * @param clusterFolder cluster folder path
     * @param writer        console output
     */
    public void performHandover(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) {
        writer.accept(info("Waiting for Haskell node to sync from Yano..."));

        try {
            // Wait for node socket to appear (Haskell node is ready)
            Path socketPath = Path.of(clusterInfo.getSocketPath());
            for (int i = 0; i < 30; i++) {
                if (socketPath.toFile().exists()) break;
                Thread.sleep(1000);
            }

            // Wait for Haskell relay to sync Yano's chain.
            // With epoch-length 600 and 3 epoch shift, Yano produces ~1800 blocks.
            // Syncing locally typically takes 10-20 seconds.
            // Stability window = 3k/f = 300 slots, so we have ~300s of margin.
            writer.accept(info("Waiting 30s for relay to sync Yano's chain..."));
            Thread.sleep(30000);

            // Stop Yano — relay has synced the chain to its db
            writer.accept(info("Stopping Yano..."));
            yanoService.stop();
            writer.accept(success("Yano stopped."));

            // Restore original topology (remove Yano peer)
            restoreOriginalTopology(clusterFolder, writer);

        } catch (Exception e) {
            log.error("Error during Yano handover", e);
            writer.accept(warn("Yano handover error: " + e.getMessage() + ". Yano may still be running."));
        }
    }

    /**
     * Restore the original topology.json (before Yano peering was added).
     */
    private void restoreOriginalTopology(Path clusterFolder, Consumer<String> writer) {
        try {
            Path topologyPath = clusterFolder.resolve("node").resolve("topology.json");
            Path topologyBackup = clusterFolder.resolve("node").resolve("topology-original.json");

            if (Files.exists(topologyBackup)) {
                Files.copy(topologyBackup, topologyPath, StandardCopyOption.REPLACE_EXISTING);
                writer.accept(info("Restored original topology (removed Yano peer)."));
            }
        } catch (IOException e) {
            log.warn("Could not restore original topology", e);
        }
    }

}
