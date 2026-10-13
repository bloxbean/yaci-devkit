package com.bloxbean.cardano.yacicli.localcluster.yano;

import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.error;

/**
 * Builds Yano's application.properties config file at {yanoHome}/config/.
 * Similar to YaciStoreConfigBuilder for yaci-store.
 * <p>
 * Quarkus picks up config/application.properties relative to the working directory.
 * Since YanoService sets the working directory to yanoHome, this file is automatically loaded.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YanoConfigBuilder {
    private final ClusterConfig clusterConfig;

    /**
     * Yano's configuration for a run mode. {@link YanoService} writes it to application.properties (see
     * {@link #write(Map)}) and passes the same values as environment variables, which the native binary honours
     * over the file.
     *
     * @param clusterInfo   cluster configuration
     * @param yanoConfigDir resolved path to Yano's devnet config (genesis files, keys)
     * @param yanoDataDir   resolved path for Yano's chainstate storage
     * @param yanoHistoryDir resolved path for Yano's history archive
     * @param runMode how Yano runs (see {@link YanoRunMode})
     * @param slotLeaderTimeTravel whether the past-time-travel backfill runs Praos slot-leader checks
     *                             (see {@link YanoService#slotLeaderTimeTravelEnabled(ClusterInfo)})
     * @param backfillBlockIntervalSlots slots between blocks in a backfill (past-time-travel or catch-up); 0 = automatic
     */
    public Map<String, String> properties(ClusterInfo clusterInfo, Path yanoConfigDir, Path yanoDataDir, Path yanoHistoryDir,
                                   YanoRunMode runMode, boolean slotLeaderTimeTravel, int backfillBlockIntervalSlots) {
        Map<String, String> props = new LinkedHashMap<>();

        // Quarkus profile
        props.put("quarkus.profile", "devnet");
        props.put("quarkus.http.port", String.valueOf(clusterInfo.getYanoHttpPort()));

        // Network
        props.put("yano.remote.protocol-magic", String.valueOf(clusterInfo.getProtocolMagic()));
        props.put("yano.server.port", String.valueOf(clusterInfo.getYanoServerPort()));

        // Genesis files
        props.put("yano.genesis.shelley-genesis-file", yanoConfigDir.resolve("shelley-genesis.json").toAbsolutePath().toString());
        props.put("yano.genesis.byron-genesis-file", yanoConfigDir.resolve("byron-genesis.json").toAbsolutePath().toString());
        props.put("yano.genesis.alonzo-genesis-file", yanoConfigDir.resolve("alonzo-genesis.json").toAbsolutePath().toString());
        props.put("yano.genesis.conway-genesis-file", yanoConfigDir.resolve("conway-genesis.json").toAbsolutePath().toString());
        props.put("yano.genesis.protocol-parameters-file", yanoConfigDir.resolve("protocol-param.json").toAbsolutePath().toString());

        // Block producer keys
        props.put("yano.block-producer.vrf-skey-file", yanoConfigDir.resolve("vrf.skey").toAbsolutePath().toString());
        props.put("yano.block-producer.kes-skey-file", yanoConfigDir.resolve("kes.skey").toAbsolutePath().toString());
        props.put("yano.block-producer.opcert-file", yanoConfigDir.resolve("opcert.cert").toAbsolutePath().toString());

        // Storage — inside the node folder so it gets cleaned up with create-node -o
        props.put("yano.storage.path", yanoDataDir.toAbsolutePath().toString());

        // History archive — Yano's bundled devnet profile enables the DuckLake projection
        // archive by default. DevKit reads nothing from Yano's history api, so it's turned off
        // here. The dir is pinned inside the node folder anyway: it defaults to ./history,
        // relative to the working directory, which is the shared yanoHome — an archive written
        // there outlives create-node -o and then fails the startup identity/coverage guards.
        props.put("yano.history.projection.enabled", "false");
        props.put("yano.history.dir", yanoHistoryDir.toAbsolutePath().toString());

        switch (runMode) {
            case PAST_TIME_TRAVEL -> {
                props.put("yano.block-producer.past-time-travel-mode", "true");
                if (slotLeaderTimeTravel) {
                    props.put("yano.block-producer.past-time-travel-slot-leader-mode", "true");
                }

                //Spacing for the catch-up from the shifted genesis to wall clock. 0 = automatic : Yano derives
                //the widest spacing that still fits inside the forecast window a Haskell relay can validate,
                //and keeps the epoch-boundary blocks needed for nonces. 1 is one block per slot (Yano's default),
                //which makes create time grow with epoch length.
                props.put("yano.block-producer.backfill-block-interval-slots", String.valueOf(backfillBlockIntervalSlots));
            }
            case CATCH_UP -> {
                //Producer on the existing chain that forges nothing on its own: blocks only come from
                //devnet/epochs/catch-up, so no live block can land at the wall-clock slot first
                props.put("yano.block-producer.block-time-millis", String.valueOf(YanoRunMode.CATCH_UP_BLOCK_TIME_MILLIS));
                props.put("yano.block-producer.backfill-block-interval-slots", String.valueOf(backfillBlockIntervalSlots));
            }
            case FOLLOW -> {
                //Follow the Haskell node's chain. Yano's dev mode requires a block producer, so it is off here.
                props.put("yano.dev-mode", "false");
                props.put("yano.block-producer.enabled", "false");
                props.put("yano.client.enabled", "true");
                props.put("yano.remote.host", "127.0.0.1");
                props.put("yano.remote.port", String.valueOf(clusterInfo.getNodePort()));
            }
            case IDLE -> {
                //Serve the chain without forging: no producer, no client. Yano's dev mode requires a producer.
                props.put("yano.dev-mode", "false");
                props.put("yano.block-producer.enabled", "false");
                props.put("yano.client.enabled", "false");
            }
            case LIVE -> {
            }
        }

        return props;
    }

    /**
     * Write the properties to {yanoHome}/config/application.properties.
     *
     * @return true if config was written successfully
     */
    public boolean write(Map<String, String> props) {
        Path configPath = Path.of(clusterConfig.getYanoHome(), "config", "application.properties");
        Path configFolder = configPath.getParent();
        if (!configFolder.toFile().exists()) {
            configFolder.toFile().mkdirs();
        }

        try (BufferedWriter writer = Files.newBufferedWriter(configPath)) {
            for (Map.Entry<String, String> entry : props.entrySet()) {
                writer.write(entry.getKey() + "=" + entry.getValue());
                writer.newLine();
            }
            return true;
        } catch (IOException e) {
            log.error("Error creating Yano configuration file", e);
            error("Error creating Yano configuration file: " + e.getMessage());
            return false;
        }
    }
}
