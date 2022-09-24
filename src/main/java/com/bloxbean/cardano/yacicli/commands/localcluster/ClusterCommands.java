package com.bloxbean.cardano.yacicli.commands.localcluster;

import ch.qos.logback.classic.Level;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.commands.common.RootLogService;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.common.ShellHelper;
import com.bloxbean.cardano.yacicli.output.DefaultOutputFormatter;
import com.bloxbean.cardano.yacicli.output.OutputFormatter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@ShellComponent
@ShellCommandGroup(Groups.CLUSTER_CMD_GROUP)
@Slf4j
public class ClusterCommands {
    public static final String CUSTER_NAME = "custer_name";
    private final ClusterService localClusterService;
    private final RootLogService rootLogService;

    private ShellHelper shellHelper;

    public ClusterCommands(ClusterService clusterService, RootLogService rootLogService, ShellHelper shellHelper) {
        this.localClusterService = clusterService;
        this.rootLogService = rootLogService;
        this.shellHelper = shellHelper;
    }

    @ShellMethod(value = "List local cluster (Babbage)", key = "list-clusters")
    public void listLocalClusters() {
        try {
            List<String> clusters = localClusterService.listClusters();
            writeLn("Available Clusters:");
            clusters.forEach(cluster -> writeLn(cluster));
        } catch (Exception e) {
            writeLn(error("Cluster listing failed. " + e.getMessage()));
        }
    }

    @ShellMethod(value = "Enter local cluster mode(Babbage)", key = "cluster")
    public void startLocalClusterContext(@ShellOption(value = {"-n", "--name"}, help = "Cluster Name") String clusterName) {
        try {
            if (CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER) {
                localClusterService.stopCluster(msg -> writeLn(msg));
            }

            localClusterService.startClusterContext(clusterName, msg -> writeLn(msg));

            CommandContext.INSTANCE.setCurrentMode(CommandContext.Mode.LOCAL_CLUSTER);
            CommandContext.INSTANCE.setProperty(CUSTER_NAME, clusterName);
            writeLn(success("Switched to %s", clusterName));
        } catch (Exception e) {
            writeLn(error(e.getMessage()));
        }
    }

    @ShellMethod(value = "Create local cluster mode(Babbage)", key = "create-cluster")
    public void createCluster(@ShellOption(value = {"-n", "--name"}, help = "Cluster Name") String clusterName,
                              @ShellOption(value = {"--ports"}, help = "Node ports (Used with --create option only)", defaultValue = "3001, 3002, 3003", arity = 3) int[] ports,
                              @ShellOption(value = {"-s", "--slotLength"}, help = "Slot Length (Valid values are 0.1 to 1)", defaultValue = "0.5", arity = 3) double slotLength,
                              @ShellOption(value = {"-o", "--overwrite"}, defaultValue = "false", help = "Overwrite existing cluster directory. default: false") boolean overwrite
    ) {

        try {
            if (ports.length != 3) {
                writeLn(error("Please provide 3 ports for 3 node cluster"));
                return;
            }

            boolean success = localClusterService.createClusterFolder(clusterName, ports, slotLength, overwrite, (msg) -> writeLn(msg));

            if (success) {
                printClusterInfo(clusterName);

                //change to Local Cluster Context
                CommandContext.INSTANCE.setCurrentMode(CommandContext.Mode.LOCAL_CLUSTER);
                CommandContext.INSTANCE.setProperty(CUSTER_NAME, clusterName);
            }
        } catch (Exception e) {
            log.error("Error", e);
            writeLn(error(e.getMessage()));
        }
    }

    @ShellMethod(value = "Get cluster info", key = "info")
    @ShellMethodAvailability("localClusterCmdAvailability")
    private void getClusterInfo() {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);
        try {
            printClusterInfo(clusterName);
        } catch (Exception e) {
            writeLn(error(e.getMessage()));
        }
    }

    private void printClusterInfo(String clusterName) throws IOException {
        ClusterInfo clusterInfo = localClusterService.getClusterInfo(clusterName);
        writeLn("\n###### Node Details ######");
        writeLn("Node ports : " + StringUtils.join(ArrayUtils.toObject(clusterInfo.getNodePorts()), " - "));
        writeLn("Node Socket Paths: ");
        for (String socketPath : clusterInfo.getSocketPaths())
            writeLn(socketPath);
    }

    @ShellMethod(value = "Delete local cluster", key = "delete-cluster")
    public void deleteLocalCluster(@ShellOption(value = {"-n", "--name"}, help = "Cluster Name") String clusterName) {
        try {
            localClusterService.deleteCluster(clusterName, (msg) -> {
                writeLn(msg);
            });
        } catch (IOException e) {
            if (log.isDebugEnabled())
                log.error("Delete error", e);
            writeLn(error("Deletion failed for cluster: %s", clusterName));
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

    @ShellMethod(value = "Show recent logs for running cluster", key = "logs")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void logsLocalCluster() {
        localClusterService.logs(msg -> writeLn(msg));
    }

    @ShellMethod(value = "Tail local cluster", key = "ltail")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void ltail(
            @ShellOption(value = {"-c", "--show-mint"}, defaultValue = "true", help = "Show mint outputs") boolean showMint,
            @ShellOption(value = {"-i", "--show-inputs"}, defaultValue = "false", help = "Show inputs") boolean showInputs,
            @ShellOption(value = {"-m", "--show-metadata"}, defaultValue = "true", help = "Show Metadata") boolean showMetadata,
            @ShellOption(value = {"-d", "--show-datumhash"}, defaultValue = "true", help = "Show DatumHash") boolean showDatumhash,
            @ShellOption(value = {"-l", "--show-inlinedatum"}, defaultValue = "true", help = "Show InlineDatum") boolean showInlineDatum,
            @ShellOption(value = {"--grouping"}, defaultValue = "true", help = "Enable/Disable grouping") boolean grouping,
            @ShellOption(value = {"--log-level"}, defaultValue = ShellOption.NULL, help = "Log level") String logLevel,
            @ShellOption(value = {"--color-mode"}, defaultValue = "dark", help = "Color mode (dark, light") String colorMode
    ) {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);
        OutputFormatter outputFormatter = new DefaultOutputFormatter(shellHelper);
        try {
            localClusterService.ltail(clusterName, showMint, showInputs, showMetadata, showDatumhash, showInlineDatum, grouping, outputFormatter);
        } catch (IOException e) {
            writeLn(error(e.getMessage()));
        }
    }

    @ShellMethod(value = "Show available utxos at default accounts", key = "show-default-accounts")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void listDefaultAccounts() {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);

        Level orgLevel = rootLogService.getLogLevel();
        if (!rootLogService.isDebugLevel())
            rootLogService.setLogLevel(Level.OFF);

        try {
            long protocolMagic = localClusterService.getClusterInfo(clusterName).getProtocolMagic();
            Path clusterFolder = localClusterService.getClusterFolder(clusterName);
            TopUpService topUpService = new TopUpService(clusterFolder, protocolMagic, msg -> {
            });
            Map<String, List<Utxo>> utxosMap = topUpService.getFundsAtGenesisKeys();

            utxosMap.entrySet().forEach(entry -> {
                writeLn("\nAddress : " + entry.getKey());
                writeLn("Utxos:");
                entry.getValue().forEach(utxo -> {
                    writeLn(utxo.getTxHash() + "#" + utxo.getOutputIndex() + " : " + utxo.getAmount());
                });
            });
            topUpService.shutdown();
        } catch (Exception e) {
            // if (log.isDebugEnabled())
            log.error("Error", e);
            writeLn(error("Topup error : " + e.getMessage()));
        } finally {
            rootLogService.setLogLevel(orgLevel);
        }
    }

    @ShellMethod(value = "Topup account", key = "topup")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void topUp(@ShellOption(value = {"-a", "--address"}, help = "Receiver address") String address,
                      @ShellOption(value = {"-v", "--value"}, help = "Ada value") double adaValue) {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);

        Level orgLevel = rootLogService.getLogLevel();
        if (!rootLogService.isDebugLevel())
            rootLogService.setLogLevel(Level.OFF);

        try {
            long protocolMagic = localClusterService.getClusterInfo(clusterName).getProtocolMagic();
            Path clusterFolder = localClusterService.getClusterFolder(clusterName);
            TopUpService topUpService = new TopUpService(clusterFolder, protocolMagic, msg -> writeLn(msg));

            topUpService.topUp(address, adaValue, msg -> writeLn(msg));
            topUpService.shutdown();
        } catch (Exception e) {
            // if (log.isDebugEnabled())
            log.error("Error", e);
            writeLn(error("Topup error : " + e.getMessage()));
        } finally {
            rootLogService.setLogLevel(orgLevel);
        }
    }

    @ShellMethod(value = "Get utxos at an address", key = "utxos")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void getUtxos(@ShellOption(value = {"-a", "--address"}, help = "Address") String address) {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);

        Level orgLevel = rootLogService.getLogLevel();
        if (!rootLogService.isDebugLevel())
            rootLogService.setLogLevel(Level.OFF);

        try {
            long protocolMagic = localClusterService.getClusterInfo(clusterName).getProtocolMagic();
            Path clusterFolder = localClusterService.getClusterFolder(clusterName);
            TopUpService topUpService = new TopUpService(clusterFolder, protocolMagic, msg -> writeLn(msg));

            List<Utxo> utxos = topUpService.getUtxos(address);
            writeLn("Utxos:");
            utxos.forEach(utxo -> {
                writeLn(utxo.getTxHash() + "#" + utxo.getOutputIndex() + " : " + utxo.getAmount());
            });
            topUpService.shutdown();
        } catch (Exception e) {
            // if (log.isDebugEnabled())
            log.error("Error", e);
            writeLn(error("Topup error : " + e.getMessage()));
        } finally {
            rootLogService.setLogLevel(orgLevel);
        }
    }

    public Availability localClusterCmdAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster modes");
    }

}
