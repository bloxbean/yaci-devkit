package com.bloxbean.cardano.yacicli.commands.localcluster;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.transaction.spec.PlutusData;
import com.bloxbean.cardano.client.transaction.spec.serializers.PlutusDataJsonConverter;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.commands.common.RootLogService;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterDeleted;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterStarted;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterStopped;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.FirstRunDone;
import com.bloxbean.cardano.yacicli.commands.localcluster.service.AccountService;
import com.bloxbean.cardano.yacicli.commands.localcluster.service.ClusterUtilService;
import com.bloxbean.cardano.yacicli.common.AnsiColors;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.common.ShellHelper;
import com.bloxbean.cardano.yacicli.common.Tuple;
import com.bloxbean.cardano.yacicli.output.DefaultOutputFormatter;
import com.bloxbean.cardano.yacicli.output.OutputFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@ShellComponent
@ShellCommandGroup(Groups.CLUSTER_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class ClusterCommands {
    public static final String CUSTER_NAME = "custer_name";
    private final ClusterService localClusterService;
    private final RootLogService rootLogService;
    private final ClusterUtilService clusterUtilService;
    private final AccountService accountService;
    private final ShellHelper shellHelper;
    private final ApplicationEventPublisher publisher;

    @ShellMethod(value = "List local clusters (Babbage)", key = "list-clusters")
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
    public void startLocalClusterContext(@ShellOption(value = {"-n", "--name"}, defaultValue = "default", help = "Cluster Name") String clusterName) {
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

    @ShellMethod(value = "Create a local cluster (Babbage)", key = "create-cluster")
    public void createCluster(@ShellOption(value = {"-n", "--name"}, defaultValue = "default", help = "Cluster Name") String clusterName,
                              @ShellOption(value = {"--port"}, help = "Node port (Used with --create option only)", defaultValue = "3001") int port,
                              @ShellOption(value = {"--submit-api-port"}, help = "Submit Api Port", defaultValue = "8090") int submitApiPort,
                              @ShellOption(value = {"-s", "--slot-length"}, help = "Slot Length in sec. (0.1 to ..)", defaultValue = "1") double slotLength,
                              @ShellOption(value = {"-b", "--block-time"}, help = "Block time in sec. (1 - 20)", defaultValue = "1") double blockTime,
                              @ShellOption(value = {"-e", "--epoch-length"}, help = "No of slots in an epoch", defaultValue = "500") int epochLength,
                              @ShellOption(value = {"-o", "--overwrite"}, defaultValue = "false", help = "Overwrite existing cluster directory. default: false") boolean overwrite,
                              @ShellOption(value = {"--start"}, defaultValue = "false", help = "Automatically start the cluster after create. default: false") boolean start
    ) {

        try {
            if (blockTime < 1 || blockTime > 20) {
                writeLn(error("Block time should be between 1 and 20"));
                return;
            }

            if (slotLength > blockTime) {
                writeLn(error("Slot length should be less than block time"));
                return;
            }

            if (epochLength < 20) {
                writeLn(error("Epoch length should be 20 or more"));
                return;
            }

            long protocolMagic = 42; //always 42 for now.

            //stop any cluster if running
            localClusterService.stopCluster(msg -> writeLn(msg));
            publisher.publishEvent(new ClusterStopped(clusterName));

            boolean success = localClusterService.createClusterFolder(clusterName, port, submitApiPort, slotLength, blockTime,
                    epochLength,
                    protocolMagic, overwrite, (msg) -> writeLn(msg));

            if (success) {
                printClusterInfo(clusterName);

                //change to Local Cluster Context
                CommandContext.INSTANCE.setCurrentMode(CommandContext.Mode.LOCAL_CLUSTER);
                CommandContext.INSTANCE.setProperty(CUSTER_NAME, clusterName);

                if (start)
                    startLocalCluster();
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
        writeLn("");
        writeLn(header(AnsiColors.CYAN_BOLD, "###### Node Details ######"));
        writeLn(successLabel("Node port", String.valueOf(clusterInfo.getNodePort())));
        writeLn(successLabel("Node Socket Paths", ""));
        writeLn(clusterInfo.getSocketPath());
        writeLn(successLabel("Submit Api Port", String.valueOf(clusterInfo.getSubmitApiPort())));
        writeLn(successLabel("Protocol Magic", String.valueOf(clusterInfo.getProtocolMagic())));
        writeLn(successLabel("Block Time", String.valueOf(clusterInfo.getBlockTime())) + " sec");
        writeLn(successLabel("Slot Length", String.valueOf(clusterInfo.getSlotLength())) + " sec");
        writeLn(successLabel("Start Time", String.valueOf(clusterInfo.getStartTime())));
    }

    @ShellMethod(value = "Delete a local cluster", key = "delete-cluster")
    public void deleteLocalCluster(@ShellOption(value = {"-n", "--name"}, help = "Cluster Name") String clusterName) {
        try {
            localClusterService.deleteCluster(clusterName, (msg) -> {
                writeLn(msg);
            });
            publisher.publishEvent(new ClusterDeleted(clusterName));
        } catch (IOException e) {
            if (log.isDebugEnabled())
                log.error("Delete error", e);
            writeLn(error("Deletion failed for cluster: %s", clusterName));
        }
    }

    @ShellMethod(value = "Start a local cluster (Babbage)", key = "start")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void startLocalCluster() {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);
        localClusterService.startCluster(clusterName);

        if (localClusterService.isFirstRunt(clusterName)) {
            publisher.publishEvent(new FirstRunDone(clusterName));
            publisher.publishEvent(new ClusterStarted(clusterName));
        } else {
            writeLn("Let's wait for the next block to make sure server started properly !!!");
            //Wait for 1 block to make sure server started properly
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            boolean success = clusterUtilService.waitForNextBlocks(1, msg -> writeLn(msg));
            writeLn(infoLabel("OK", "Server Started"));
            if (success)
                publisher.publishEvent(new ClusterStarted(clusterName));
        }

    }

    @ShellMethod(value = "Stop the running local cluster (Babbage)", key = "stop")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void stopLocalCluster() {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);
        localClusterService.stopCluster(msg -> writeLn(msg));

        publisher.publishEvent(new ClusterStopped(clusterName));
    }

    @ShellMethod(value = "Reset local cluster. Delete data and logs folder and restart.", key = "reset")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void resetLocalCluster() {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);
        localClusterService.stopCluster(msg -> writeLn(msg));
        publisher.publishEvent(new ClusterStopped(clusterName));

        try {
            localClusterService.deleteClusterDataFolder(clusterName, (msg) -> {
                writeLn(msg);
            });
            publisher.publishEvent(new ClusterDeleted(clusterName));

            startLocalCluster();
        } catch (IOException e) {
            if (log.isDebugEnabled())
                log.error("Delete error", e);
            writeLn(error("Deletion failed for cluster db & logs: %s", clusterName));
        }
    }

    @ShellMethod(value = "Show recent logs for running cluster", key = "logs")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void logsLocalCluster() {
        localClusterService.logs(msg -> writeLn(msg));
    }

    @ShellMethod(value = "Show recent logs for submit api process", key = "submit-api-logs")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void logsSubmitApi() {
        localClusterService.submitApiLogs(msg -> writeLn(msg));
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
        } catch (Exception e) {
            writeLn(error(e.getMessage()));
        }
    }

    @ShellMethod(value = "Show available utxos at default accounts", key = "show-default-accounts")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void listDefaultAccounts() {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);

        Map<String, List<Utxo>> utxosMap = accountService.getUtxosAtDefaultAccounts(clusterName, msg -> writeLn(msg));
        utxosMap.entrySet().forEach(entry -> {
            writeLn(header(AnsiColors.CYAN_BOLD, "Address"));
            writeLn(entry.getKey());
            writeLn(header(AnsiColors.CYAN_BOLD, "Utxos"));
            entry.getValue().forEach(utxo -> {
                writeLn(utxo.getTxHash() + "#" + utxo.getOutputIndex() + " : " + utxo.getAmount());
            });
            writeLn("");
        });
    }

    @ShellMethod(value = "Topup account", key = "topup")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void topUp(@ShellOption(value = {"-a", "--address"}, help = "Receiver address") String address,
                      @ShellOption(value = {"-v", "--value"}, help = "Ada value") double adaValue) {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);

        accountService.topup(clusterName, address, adaValue, msg -> writeLn(msg));
        clusterUtilService.waitForNextBlocks(1, msg -> writeLn(msg));

        writeLn(info("Available utxos") + "\n");
        getUtxos(address, false);
    }

    @ShellMethod(value = "Get utxos at an address", key = "utxos")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void getUtxos(@ShellOption(value = {"-a", "--address"}, help = "Address") String address,
                         @ShellOption(value = {"--pretty-print-inline-datum"}, defaultValue = "false") boolean prettyPrintInlineDatum) {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);

        List<Utxo> utxos = accountService.getUtxos(clusterName, address, msg -> writeLn(msg));

        AtomicInteger index = new AtomicInteger(0);
        utxos.forEach(utxo -> {
            writeLn(index.incrementAndGet() + ". " + utxo.getTxHash() + "#" + utxo.getOutputIndex() + " : " + utxo.getAmount());
            if (utxo.getInlineDatum() != null) {
                if (prettyPrintInlineDatum) {
                    try {
                        writeLn("InlineDatum : \n" +
                                PlutusDataJsonConverter.toJson(PlutusData.deserialize(HexUtil.decodeHexString(utxo.getInlineDatum()))));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else
                    writeLn("InlineDatum: " + utxo.getInlineDatum());
            }
            if (utxo.getDataHash() != null)
                writeLn("DatumHash: " + utxo.getDataHash());

            if (utxo.getReferenceScriptHash() != null)
                writeLn("ReferenceScriptHash: " + utxo.getReferenceScriptHash());
            writeLn("--------------------------------------------------------------------------------------");
        });
    }

    @ShellMethod(value = "Get tip/current block number", key = "tip")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void getTip() {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);

        Tuple<Long, Point> tuple = clusterUtilService.getTip(msg -> writeLn(msg));
        writeLn(successLabel("Block#", String.valueOf(tuple._1)));
        writeLn(successLabel("Slot#", String.valueOf(tuple._2.getSlot())));
        writeLn(successLabel("Block Hash", String.valueOf(tuple._2.getHash())));
    }

    public Availability localClusterCmdAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster modes");
    }

}
