package com.bloxbean.cardano.yacicli.localcluster;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.file.Path;

import static com.bloxbean.cardano.yacicli.YaciCliConfig.YACI_CLI_HOME;

@Component
@NoArgsConstructor
@AllArgsConstructor
public class ClusterConfig {
    public static final String CLUSTER_NAME = "cluster_name";
    public final static String CLUSTER_INFO_FILE = "cluster-info.json";
    public final static String NODE_FOLDER_PREFIX = "node";
    public final static String NODE_RELAY_SCRIPT = "node-relay";
    public final static String NODE_BP_SCRIPT = "node-bp";

    @Value("${local.cluster.home:#{null}}")
    private String clusterHome;

    @Value("${pool.keys.home:#{null}}")
    private String poolKeysHome;

    @Value("${genesis.keys.home:#{null}}")
    private String genesisKeysHome;

    @Value("${cardano.cli.path:#{null}}")
    private String cardanoCliPath;

    @Value("${yaci.store.folder:#{null}}")
    private String yaciStoreBinFolder;

    @Value("${ogmios.folder:#{null}}")
    private String ogmiosFolder;

    @Value("${kupo.folder:#{null}}")
    private String kupoFolder;

    public String getClusterHome() {
        if (clusterHome == null || !StringUtils.hasLength(clusterHome.trim()))
            return Path.of(YACI_CLI_HOME, "local-clusters").toAbsolutePath().toString();

        return clusterHome;
    }

    public String getCLIBinFolder() {
        if (cardanoCliPath == null || !StringUtils.hasLength(cardanoCliPath.trim()))
            return Path.of(YACI_CLI_HOME, "bin").toAbsolutePath().toString();
        else
            return Path.of(cardanoCliPath).toAbsolutePath().toString();
    }

    public String getYaciStoreBinPath() {
        if (yaciStoreBinFolder == null || !StringUtils.hasLength(yaciStoreBinFolder.trim()))
            return Path.of(YACI_CLI_HOME, "bin").toAbsolutePath().toString();
        else
            return Path.of(yaciStoreBinFolder).toAbsolutePath().toString();
    }

    public String getPoolKeysHome() {
        if (poolKeysHome == null || !StringUtils.hasLength(poolKeysHome.trim()))
            return Path.of(YACI_CLI_HOME, "pool-keys").toAbsolutePath().toString();
        else
            return poolKeysHome;
    }

    public String getGenesisKeysHome() {
        if (genesisKeysHome == null || !StringUtils.hasLength(genesisKeysHome.trim()))
            return Path.of(YACI_CLI_HOME, "genesis-keys").toAbsolutePath().toString();
        else
            return genesisKeysHome;
    }

    public String getOgmiosHome() {
        if (ogmiosFolder == null || !StringUtils.hasLength(ogmiosFolder.trim()))
            return Path.of(YACI_CLI_HOME, "bin", "ogmios").toAbsolutePath().toString();
        else
            return Path.of(ogmiosFolder).toAbsolutePath().toString();
    }

    public String getKupoHome() {
        if (kupoFolder == null || !StringUtils.hasLength(kupoFolder.trim()))
            return Path.of(YACI_CLI_HOME, "bin", "kupo").toAbsolutePath().toString();
        else
            return Path.of(kupoFolder).toAbsolutePath().toString();
    }

    public Path getClusterFolder(String clusterName) {
        return Path.of(getClusterHome(), clusterName);
    }

    public Path getPoolKeysFolder(String clusterName) {
        return Path.of(getPoolKeysHome(), clusterName);
    }

    public Path getGenesisKeysFolder(String clusterName) {
        return Path.of(getGenesisKeysHome(), clusterName);
    }
}
