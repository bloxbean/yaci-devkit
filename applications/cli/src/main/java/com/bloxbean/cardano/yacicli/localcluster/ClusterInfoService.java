package com.bloxbean.cardano.yacicli.localcluster;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.bloxbean.cardano.yacicli.localcluster.ClusterConfig.NODE_FOLDER_PREFIX;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClusterInfoService {

    private final ClusterConfig clusterConfig;
    private final ObjectMapper objectMapper;

    public ClusterInfo getClusterInfo(String clusterName) throws IOException {
        Path clusterFolder = getClusterFolder(clusterName);
        if (!Files.exists(clusterFolder)) {
            throw new IllegalStateException("Cluster not found : " + clusterName);
        }

        String clusterInfoPath = clusterFolder.resolve(ClusterConfig.CLUSTER_INFO_FILE).toAbsolutePath().toString();
        try {
            return objectMapper.readValue(new File(clusterInfoPath), ClusterInfo.class);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public Path getClusterFolder(String clusterName) {
        return Path.of(clusterConfig.getClusterHome(), clusterName);
    }

    public void saveClusterInfo(Path clusterFolder, ClusterInfo clusterInfo) throws IOException {
        if (!Files.exists(clusterFolder)) {
            throw new IllegalStateException("Cluster folder not found - "  + clusterFolder);
        }

        String socketPath = clusterFolder.resolve(NODE_FOLDER_PREFIX).resolve("node.sock").toString();
        clusterInfo.setSocketPath(socketPath);

        String clusterInfoPath = clusterFolder.resolve(ClusterConfig.CLUSTER_INFO_FILE).toAbsolutePath().toString();
        objectMapper.writer(new DefaultPrettyPrinter()).writeValue(new File(clusterInfoPath), clusterInfo);
    }
}
