package com.bloxbean.cardano.yacicli.localcluster.service;

import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.events.RollbackDone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

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
    private final ClusterUtilService clusterUtilService;
    private final ApplicationEventPublisher publisher;

    public void setRollbackPoint(Consumer<String> writer) {
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


    public boolean rollback(Consumer<String> writer) {

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
}
