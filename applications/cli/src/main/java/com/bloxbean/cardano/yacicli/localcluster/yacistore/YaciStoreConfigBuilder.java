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

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.error;

/**
 * This class builds the Yaci Store configuration (config/application.properties next to the store
 * binary) at cluster start. It is used for both non-docker and docker modes so the correct node
 * endpoints (Yano vs Haskell), submit URL and epoch settings are applied per node mode.
 */
@Component
@RequiredArgsConstructor
public class YaciStoreConfigBuilder {
    private final ClusterConfig clusterConfig;

    public boolean build(ClusterInfo clusterInfo, String txEvaluatorMode) {
        Path nodeSocketPath = Path.of(clusterInfo.getSocketPath());
        String nodeFolder = nodeSocketPath.getParent().toFile().getAbsolutePath();

        Map<String, String> storeProperties = new LinkedHashMap<>();
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
        storeProperties.put("store.submit.tx-evaluator-mode", txEvaluatorMode);
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

        // yano-only runs the "all" native variant (the "all" Spring profile is active), which derives
        // protocol params via the epoch-aggr module. Write an external application-all.properties to
        // override the bundled one: the default epoch-aggr calculation interval (14400s = 4h) is far
        // too slow for short devnet epochs, so /epochs/latest/parameters would go stale after the first
        // couple of epochs. An external profile-specific file takes precedence over the bundled one.
        Path n2cOverridePath = configFolder.resolve("application-n2c.properties");
        Path allOverridePath = configFolder.resolve("application-all.properties");
        if (yanoOnly) {
            try (BufferedWriter writer = Files.newBufferedWriter(allOverridePath)) {
                writer.write("store.epoch-aggr.epoch-calculation-interval=1");
                writer.newLine();
                writer.write("store.submit.tx-evaluator-mode=" + txEvaluatorMode);
                writer.newLine();
            } catch (IOException e) {
                e.printStackTrace();
                // Fatal: without this override the all binary would run with bundled defaults
                // (4h epoch-aggr interval), leaving /epochs/parameters stale on a devnet.
                error("Error creating all-profile override config: " + e.getMessage());
                return false;
            }
            // The n2c profile is not active for the all binary; drop any stale n2c override.
            try {
                Files.deleteIfExists(n2cOverridePath);
            } catch (IOException e) {
                e.printStackTrace();
                error("Error removing stale n2c override config: " + e.getMessage());
                return false;
            }
        } else {
            // n2c modes: remove any stale yano-only overrides so they don't leak into the n2c profile
            // (e.g. after switching yano-only -> haskell-only without --overwrite).
            try {
                Files.deleteIfExists(allOverridePath);
                Files.deleteIfExists(n2cOverridePath);
            } catch (IOException e) {
                e.printStackTrace();
                error("Error removing stale Yano override config: " + e.getMessage());
                return false;
            }
        }

        return true;

    }
}
