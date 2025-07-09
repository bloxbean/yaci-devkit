package com.bloxbean.cardano.yacicli.localcluster.service;

import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.events.RollbackDone;
import com.bloxbean.cardano.yacicli.localcluster.peer.LocalPeerService;
import com.bloxbean.cardano.yacicli.localcluster.proxy.TcpProxyManager;
import com.bloxbean.cardano.yacicli.util.PortUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RollbackService {
    public static final String DB = "db";
    public static final String DB_ROLLBACK_POINT_FOLDER = "db_rollback_point";
    private final ClusterService clusterService;
    private final LocalPeerService localPeerService;
    private final ClusterUtilService clusterUtilService;
    private final ApplicationEventPublisher publisher;
    private final TcpProxyManager tcpProxyManager;

    public void takeDBSnapshot(Consumer<String> writer) {
        String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
        if (clusterName != null && !clusterName.equals("default")) {
            writer.accept(error("Rollback point cannot be set in a non-default cluster. Please use the default cluster."));
            return;
        }

        Path nodeFolder = clusterService.getNodeFolder(clusterName);
        if (nodeFolder == null || !nodeFolder.toFile().exists()) {
            writer.accept(error("Node folder not found for cluster: " + clusterName));
            return;
        }

        Path dbPath = nodeFolder.resolve(DB);
        if (!dbPath.toFile().exists()) {
            writer.accept(error("Database folder not found at: " + dbPath));
            return;
        }

        Path rollbackPath = nodeFolder.resolve(DB_ROLLBACK_POINT_FOLDER);
        try {
            FileUtils.copyDirectory(dbPath.toFile(), rollbackPath.toFile());
            writer.accept(info("Rollback point created successfully at: " + rollbackPath));
        } catch (Exception e) {
            writer.accept(error("Failed to create rollback point. Error: " + e.getMessage()));
            log.error("Error creating rollback point", e);
        }
    }


    public boolean rollbackToLastDBSnapshot(Consumer<String> writer) {

        String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
        if (clusterName != null && !clusterName.equals("default")) {
            writer.accept(error("Rollback cannot be performed in a non-default cluster. Please use the default cluster."));
            return false;
        }

        Path nodeFolder = clusterService.getNodeFolder(clusterName);
        if (nodeFolder == null || !nodeFolder.toFile().exists()) {
            writer.accept(error("Node folder not found for cluster: " + clusterName));
            return false;
        }

        Path dbPath = nodeFolder.resolve(DB);
        if (!dbPath.toFile().exists()) {
            writer.accept(error("Database folder not found at: " + dbPath));
            return false;
        }

        Path rollbackPath = nodeFolder.resolve(DB_ROLLBACK_POINT_FOLDER);
        if (!rollbackPath.toFile().exists()) {
            writer.accept(error("Rollback point not found at: " + rollbackPath));
            return false;
        }

        clusterService.stopClusterNode(writer);

        try {
            FileUtils.deleteDirectory(dbPath.toFile());
            FileUtils.copyDirectory(rollbackPath.toFile(), dbPath.toFile());
            FileUtils.deleteDirectory(rollbackPath.toFile());
            writer.accept(info("Rollback completed successfully. Database restored to rollback point at: " + rollbackPath));

            clusterService.startCluster(clusterName);
            publisher.publishEvent(new RollbackDone(clusterName));
            return true;
        } catch (Exception e) {
            writer.accept(error("Failed to rollback. Error: " + e.getMessage()));
            log.error("Error during rollback", e);
            return false;
        }
    }

    public boolean createForks(boolean restartNode, long waitInSecBeforeRestart, Consumer<String> writer) {
        String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
        if (clusterName != null && !clusterName.equals("default")) {
            writer.accept(error("Fork cannot be started in a non-default cluster. Please use the default cluster."));
            return false;
        }

        tcpProxyManager.stopAll();

        if (restartNode) {
            clusterService.stopClusterNode(writer);

            writer.accept(info("Waiting for %s seconds before starting the main node...", waitInSecBeforeRestart));
            try {
                Thread.sleep(waitInSecBeforeRestart * 1000);
            } catch (InterruptedException e) {
            }

            clusterService.startCluster(clusterName);
        }

        writer.accept(info("Fork started successfully for cluster: " + clusterName));

        return true;
    }

    public boolean joinForks(Consumer<String> writer) {
        String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
        if (clusterName != null && !clusterName.equals("default")) {
            writer.accept(error("Join forks can't be done in a non-default cluster. Please use the default cluster."));
            return false;
        }

        if (!PortUtil.isPortAvailable(4001)) {
            writer.accept(error("Port 4001 is already in use. Please stop any process using this port before starting the TCP proxy to simulate rollback."));
            return false;
        }
        if (!PortUtil.isPortAvailable(4002)) {
            writer.accept(error("Port 4002 is already in use. Please stop any process using this port before starting the TCP proxy to simulate rollback."));
            return false;
        }

        if (!PortUtil.isPortAvailable(4003)) {
            writer.accept(error("Port 4003 is already in use. Please stop any process using this port before starting the TCP proxy to simulate rollback."));
            return false;
        }

        try {
            tcpProxyManager.startProxy(4001, "127.0.0.1", 3001);
            tcpProxyManager.startProxy(4002, "127.0.0.1", 3002);
            tcpProxyManager.startProxy(4003, "127.0.0.1", 3003);

            writer.accept(success("Started proxies to join forks..."));
            return true;
        } catch (IOException e) {
            writer.accept(error("Failed to start TCP proxy for joining forks. Error: " + e.getMessage()));
            return false;
        }

    }
}
