package com.bloxbean.cardano.yacicli.localcluster.yacistore;

import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.NodeMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.error;

/**
 * This class is used to build Yaci Store configuration
 * It's currently used for non-docker mode to build the configuration. For docker mode, store-application.properties in the docker folder
 * is used.
 */
@Component
@RequiredArgsConstructor
public class YaciStoreConfigBuilder {
    private final ClusterConfig clusterConfig;

    public boolean build(ClusterInfo clusterInfo) {
        Path nodeSocketPath = Path.of(clusterInfo.getSocketPath());
        String nodeFolder = nodeSocketPath.getParent().toFile().getAbsolutePath();

        Map<String, String> storeProperties = new LinkedHashMap();
        storeProperties.put("server.port", String.valueOf(clusterInfo.getYaciStorePort()));

        boolean yanoOnly = NodeMode.YANO_ONLY == clusterInfo.getNodeMode();

        storeProperties.put("store.cardano.host", "localhost");
        if (yanoOnly) {
            // Connect to Yano's N2N port instead of Haskell node
            storeProperties.put("store.cardano.port", String.valueOf(clusterInfo.getYanoServerPort()));
        } else {
            storeProperties.put("store.cardano.port", String.valueOf(clusterInfo.getNodePort()));
            storeProperties.put("store.cardano.n2c-node-socket-path", clusterInfo.getSocketPath());
        }
        storeProperties.put("store.cardano.protocol-magic", String.valueOf(clusterInfo.getProtocolMagic()));

        if (yanoOnly) {
            // Yano's HTTP API handles tx submission
            storeProperties.put("store.cardano.submit-api-url", "http://localhost:" + clusterInfo.getYanoHttpPort() + "/api/v1/tx/submit");
        } else {
            storeProperties.put("store.cardano.submit-api-url", "http://localhost:" + clusterInfo.getSubmitApiPort() + "/api/submit/tx");
        }
        storeProperties.put("store.cardano.ogmios-url", "http://localhost:" + clusterInfo.getOgmiosPort());
        storeProperties.put("spring.datasource.url", "jdbc:h2:file:" + nodeFolder + "/yaci_store/storedb;MV_STORE=TRUE;AUTO_SERVER=TRUE;AUTO_RECONNECT=TRUE;LOCK_TIMEOUT=120000");
        storeProperties.put("spring.datasource.username", "sa");
        storeProperties.put("spring.datasource.password", "password");

        storeProperties.put("logging.file.name", "./logs/yaci-store.log");
        storeProperties.put("spring.jpa.properties.hibernate.jdbc.batch_size", "20");
        storeProperties.put("spring.jpa.properties.hibernate.order_inserts", "true");

        storeProperties.put("store.cardano.byron-genesis-file", nodeFolder + "/genesis/byron-genesis.json");
        storeProperties.put("store.cardano.shelley-genesis-file", nodeFolder + "/genesis/shelley-genesis.json");
        storeProperties.put("store.cardano.alonzo-genesis-file", nodeFolder + "/genesis/alonzo-genesis.json");
        storeProperties.put("store.cardano.conway-genesis-file", nodeFolder + "/genesis/conway-genesis.json");

        storeProperties.put("store.blocks.epoch-calculation-interval", "3600");

        storeProperties.put("store.account.enabled", "true");
        storeProperties.put("store.account.api-enabled", "true");
        storeProperties.put("store.account.balance-aggregation-enabled", "true");
        storeProperties.put("store.account.history-cleanup-enabled", "false");

        storeProperties.put("store.adapot.enabled", "true");
        storeProperties.put("store.adapot.api-enabled", "true");
        storeProperties.put("store.governance-aggr.enabled", "true");
        storeProperties.put("store.governance-aggr.api-enabled", "true");

        storeProperties.put("store.live.enabled", "true");

        if (yanoOnly) {
            // Explicitly disable N2C-based local epoch endpoint — no Haskell node socket in yano-only mode.
            // The n2c profile (baked into Yaci Store binary) sets this to true by default,
            // so we must explicitly override it to false.
            storeProperties.put("store.epoch.endpoints.epoch.local.enabled", "false");
        } else {
            storeProperties.put("store.epoch.endpoints.epoch.local.enabled", "true");
        }

        storeProperties.put("spring.batch.job.enabled", "false");

        if (yanoOnly) {
            // Disable MCP server in yano-only mode — McpDAppRegistryService fails with custom
            // protocol magic (NetworkType.fromProtocolMagic returns null for devnet magic 42)
            storeProperties.put("yaci.store.mcp-server.enabled", "false");
        }

        Path yaciStoreConfigPath = Path.of(clusterConfig.getYaciStoreBinPath(), "config", "application.properties");

        Path configFolder = yaciStoreConfigPath.getParent();
        if (!configFolder.toFile().exists()) {
            configFolder.toFile().mkdirs();
        }

        try (BufferedWriter writer = Files.newBufferedWriter(yaciStoreConfigPath)) {
            for (String key : storeProperties.keySet()) {
                String value = storeProperties.get(key);
                writer.write(key + "=" + value);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            error("Error creating Yaci Store configuration file: " + e.getMessage());
            return false;
        }

        // In yano-only mode, write a profile-specific override to disable N2C local epoch endpoint.
        // The n2c profile (baked into Yaci Store binary) bundles application-n2c.properties which sets
        // store.epoch.endpoints.epoch.local.enabled=true. Profile-specific properties inside the binary
        // override non-profile application.properties. An external application-n2c.properties in the
        // config/ folder takes precedence over the bundled one.
        if (yanoOnly) {
            Path n2cOverridePath = configFolder.resolve("application-n2c.properties");
            try (BufferedWriter writer = Files.newBufferedWriter(n2cOverridePath)) {
                writer.write("store.epoch.endpoints.epoch.local.enabled=false");
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
                error("Error creating N2C override config: " + e.getMessage());
            }
        }

        return true;

    }
}
