package com.bloxbean.cardano.yacicli.localcluster.yacistore;

import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

        Properties storeProperties = new Properties();
        storeProperties.setProperty("server.port", String.valueOf(clusterInfo.getYaciStorePort()));

        storeProperties.setProperty("store.cardano.host", "localhost");
        storeProperties.setProperty("store.cardano.port", String.valueOf(clusterInfo.getNodePort()));
        storeProperties.setProperty("store.cardano.protocol-magic", String.valueOf(clusterInfo.getProtocolMagic()));

        storeProperties.setProperty("store.cardano.n2c-node-socket-path", clusterInfo.getSocketPath());
        storeProperties.setProperty("store.cardano.submit-api-url", "http://localhost:" + clusterInfo.getSubmitApiPort() + "/api/submit/tx");
        storeProperties.setProperty("store.cardano.ogmios-url", "http://localhost:" + clusterInfo.getOgmiosPort());
        storeProperties.setProperty("spring.datasource.url", "jdbc:h2:file:" + nodeFolder + "/yaci_store/storedb");
        storeProperties.setProperty("spring.datasource.username", "sa");
        storeProperties.setProperty("spring.datasource.password", "password");

        storeProperties.setProperty("logging.file.name", "./logs/yaci-store.log");
        storeProperties.setProperty("spring.jpa.properties.hibernate.jdbc.batch_size", "20");
        storeProperties.setProperty("spring.jpa.properties.hibernate.order_inserts", "true");

        storeProperties.setProperty("store.cardano.byron-genesis-file", nodeFolder + "/genesis/byron-genesis.json");
        storeProperties.setProperty("store.cardano.shelley-genesis-file", nodeFolder + "/genesis/shelley-genesis.json");
        storeProperties.setProperty("store.cardano.alonzo-genesis-file", nodeFolder + "/genesis/alonzo-genesis.json");
        storeProperties.setProperty("store.cardano.conway-genesis-file", nodeFolder + "/genesis/conway-genesis.json");

        storeProperties.setProperty("store.blocks.epoch-calculation-interval", "3600");

        storeProperties.setProperty("store.account.enabled", "true");
        storeProperties.setProperty("store.account.api-enabled", "true");
        storeProperties.setProperty("store.account.balance-aggregation-enabled", "true");
        storeProperties.setProperty("store.account.history-cleanup-enabled", "false");

        storeProperties.setProperty("store.live.enabled", "true");

        storeProperties.setProperty("store.epoch.endpoints.epoch.local.enabled", "true");

        storeProperties.setProperty("spring.batch.job.enabled", "false");

        Path yaciStoreConfigPath = Path.of(clusterConfig.getYaciStoreBinPath(), "config", "application.properties");
        if (yaciStoreConfigPath.toFile().exists()) {
            return true;
        }

        Path configFolder = yaciStoreConfigPath.getParent();
        if (!configFolder.toFile().exists()) {
            configFolder.toFile().mkdirs();
        }

        try (BufferedWriter writer = Files.newBufferedWriter(yaciStoreConfigPath)) {
            for (String key : storeProperties.stringPropertyNames()) {
                String value = storeProperties.getProperty(key);
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
