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
     * Build and write application.properties for Yano.
     *
     * @param clusterInfo   cluster configuration
     * @param yanoConfigDir resolved path to Yano's devnet config (genesis files, keys)
     * @param yanoDataDir   resolved path for Yano's chainstate storage
     * @param pastTimeTravelMode whether past-time-travel mode is enabled
     * @return true if config was written successfully
     */
    public boolean build(ClusterInfo clusterInfo, Path yanoConfigDir, Path yanoDataDir,
                         boolean pastTimeTravelMode) {
        Map<String, String> props = new LinkedHashMap<>();

        // Quarkus profile
        props.put("quarkus.profile", "devnet");
        props.put("quarkus.http.port", String.valueOf(clusterInfo.getYanoHttpPort()));

        // Network
        props.put("yaci.node.remote.protocol-magic", String.valueOf(clusterInfo.getProtocolMagic()));
        props.put("yaci.node.server.port", String.valueOf(clusterInfo.getYanoServerPort()));

        // Genesis files
        props.put("yaci.node.genesis.shelley-genesis-file", yanoConfigDir.resolve("shelley-genesis.json").toAbsolutePath().toString());
        props.put("yaci.node.genesis.byron-genesis-file", yanoConfigDir.resolve("byron-genesis.json").toAbsolutePath().toString());
        props.put("yaci.node.genesis.alonzo-genesis-file", yanoConfigDir.resolve("alonzo-genesis.json").toAbsolutePath().toString());
        props.put("yaci.node.genesis.conway-genesis-file", yanoConfigDir.resolve("conway-genesis.json").toAbsolutePath().toString());
        props.put("yaci.node.genesis.protocol-parameters-file", yanoConfigDir.resolve("protocol-param.json").toAbsolutePath().toString());

        // Block producer keys
        props.put("yaci.node.block-producer.vrf-skey-file", yanoConfigDir.resolve("vrf.skey").toAbsolutePath().toString());
        props.put("yaci.node.block-producer.kes-skey-file", yanoConfigDir.resolve("kes.skey").toAbsolutePath().toString());
        props.put("yaci.node.block-producer.opcert-file", yanoConfigDir.resolve("opcert.cert").toAbsolutePath().toString());

        // Storage — inside the node folder so it gets cleaned up with create-node -o
        props.put("yaci.node.storage.path", yanoDataDir.toAbsolutePath().toString());

        // Past-time-travel mode
        if (pastTimeTravelMode) {
            props.put("yaci.node.block-producer.past-time-travel-mode", "true");
        }

        // Write to {yanoHome}/config/application.properties
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
