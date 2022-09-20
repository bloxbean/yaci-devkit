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
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClusterConfig {
    public final static String CLUSTER_INFO_FILE = "cluster-info.json";
    public final static String NODE_FOLDER_PREFIX = "node-spo";
    public final static long PROTOCOL_MAGIC = 42;

    @Value("${local.cluster.home:#{null}}")
    private String clusterHome;

    @Value("${cardano.cli.path: #{null}}")
    private String cardanoCliPath;

    public String getClusterHome() {
        if (!StringUtils.hasLength(clusterHome))
            return Path.of(YACI_CLI_HOME, "local-clusters").toAbsolutePath().toString();

        return clusterHome;
    }

    public String getCLIBinFolder() {
        if (!StringUtils.hasLength(clusterHome))
            return Path.of(YACI_CLI_HOME, "bin").toAbsolutePath().toString();

        return clusterHome;
    }

    public String getCardanoCliPath() {
        return cardanoCliPath;
    }
}
