package com.bloxbean.cardano.yacicli.localcluster.yacistore;

import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
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

        storeProperties.put("store.cardano.host", "localhost");
        storeProperties.put("store.cardano.port", String.valueOf(clusterInfo.getNodePort()));
        storeProperties.put("store.cardano.protocol-magic", String.valueOf(clusterInfo.getProtocolMagic()));

        storeProperties.put("store.cardano.n2c-node-socket-path", clusterInfo.getSocketPath());
        storeProperties.put("store.cardano.submit-api-url", "http://localhost:" + clusterInfo.getSubmitApiPort() + "/api/submit/tx");
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

        storeProperties.put("store.epoch.endpoints.epoch.local.enabled", "true");

        storeProperties.put("spring.batch.job.enabled", "false");

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
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            error("Error creating Yaci Store configuration file: " + e.getMessage());
            return false;
        }

    }
}
