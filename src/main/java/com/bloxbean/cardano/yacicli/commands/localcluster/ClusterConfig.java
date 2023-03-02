package com.bloxbean.cardano.yacicli.commands.localcluster;

import lombok.AllArgsConstructor;
import lombok.Getter;
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
    public final static String CLUSTER_INFO_FILE = "cluster-info.json";
    public final static String NODE_FOLDER_PREFIX = "node-spo";
    public final static long PROTOCOL_MAGIC = 42;

    @Value("${local.cluster.home:#{null}}")
    private String clusterHome;

    @Value("${cardano.cli.path:#{null}}")
    private String cardanoCliPath;

    @Value("${yaci.store.folder:#{null}}")
    private String yaciStoreBinFolder;

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
}
