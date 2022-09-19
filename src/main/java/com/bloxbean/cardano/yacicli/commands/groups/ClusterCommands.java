package com.bloxbean.cardano.yacicli.commands.groups;

import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.*;

import java.io.IOException;
import java.util.List;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.error;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.writeLn;

@ShellComponent
@ShellCommandGroup(Groups.CLUSTER_CMD_GROUP)
@Slf4j
public class ClusterCommands {
    public static final String CUSTER_NAME = "custer_name";
    private final ClusterService localClusterService;

    public ClusterCommands(ClusterService clusterService) {
        this.localClusterService = clusterService;
    }

    @ShellMethod(value = "List local cluster (Babbage)", key = "list-clusters")
    public void listLocalClusters() {
        try {
            List<String> clusters = localClusterService.listClusters();
            writeLn("Available Clusters:");
            clusters.forEach(cluster -> writeLn(cluster));
        } catch (Exception e) {
            error("Cluster listing failed. " + e.getMessage());
        }
    }

    @ShellMethod(value = "Enter local cluster mode(Babbage)", key = "cluster")
    public String startLocalClusterContext(@ShellOption(value = {"-n", "--name"}, help = "Cluster Name") String clusterName) {
        try {
            localClusterService.startClusterContext(clusterName, msg -> writeLn(msg));

            CommandContext.INSTANCE.setCurrentMode(CommandContext.Mode.LOCAL_CLUSTER);
            CommandContext.INSTANCE.setProperty(CUSTER_NAME, clusterName);
        } catch (Exception e) {
            error(e.getMessage());
        }

        return "Local Cluster Mode";
    }

    @ShellMethod(value = "Create local cluster mode(Babbage)", key = "create-cluster")
    public void createCluster(@ShellOption(value = {"-n", "--name"}, help = "Cluster Name") String clusterName,
                                           @ShellOption(value = {"--ports"}, help ="Node ports (Used with --create option only)", defaultValue = "3001, 3002, 3003", arity = 3) int[] ports,
                                           @ShellOption(value = {"--clean"}, defaultValue = "false", help = "Remove existing db folder and clean start. default: false") boolean clean,
                                           @ShellOption(value = {"-o", "--overwrite"}, defaultValue = "false", help = "Overwrite existing cluster directory. default: false") boolean overwrite

    ) {

        try {
            if (ports.length != 3)
                error("Please provide 3 ports for 3 node cluster");

            boolean success = localClusterService.createClusterFolder(clusterName, ports, clean, overwrite, (msg) -> writeLn(msg));

            if (success) {
                CommandContext.INSTANCE.setCurrentMode(CommandContext.Mode.LOCAL_CLUSTER);
                CommandContext.INSTANCE.setProperty(CUSTER_NAME, clusterName);
            }
        } catch (Exception e) {
            error(e.getMessage());
        }
    }

    @ShellMethod(value = "Delete local cluster", key = "delete-cluster")
    public void deleteLocalCluster(@ShellOption(value = {"-n", "--name"}, help = "Cluster Name") String clusterName) {
        try {
            localClusterService.deleteCluster(clusterName, (msg) -> {writeLn(msg);});
        } catch (IOException e) {
            if (log.isDebugEnabled())
                log.error("Delete error", e);
            error("Deletion failed for cluster: %s", clusterName);
        }
    }

    @ShellMethod(value = "Start local cluster (Babbage)", key = "start")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void startLocalCluster() {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);
        localClusterService.startCluster(clusterName);
    }

    @ShellMethod(value = "Stop local cluster (Babbage)", key = "stop")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void stopLocalCluster() {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);
        localClusterService.stopCluster(msg -> writeLn(msg));
    }

    @ShellMethod(value = "Logs local cluster", key = "logs")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void logsLocalCluster() {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);
        localClusterService.logs(msg -> writeLn(msg));
    }

    @ShellMethod(value = "Exit local cluster (Babbage)", key = "exit-cluster")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void exitLocalClusterMode() {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);
        localClusterService.stopCluster(msg -> writeLn(msg));

        CommandContext.INSTANCE.setCurrentMode(CommandContext.Mode.REGULAR);
    }

    public Availability localClusterCmdAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster modes");
    }
}
