package com.bloxbean.cardano.yacicli.commands.localcluster;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.commands.common.RootLogService;
import com.bloxbean.cardano.yacicli.commands.localcluster.config.GenesisConfig;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterDeleted;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterStarted;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterStopped;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.FirstRunDone;
import com.bloxbean.cardano.yacicli.commands.localcluster.profiles.GenesisProfile;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.bloxbean.cardano.yacicli.commands.localcluster.ClusterConfig.CLUSTER_NAME;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@ShellComponent
@ShellCommandGroup(Groups.NODE_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class ClusterCommands {
    private final ClusterService localClusterService;
    private final RootLogService rootLogService;
    private final ClusterUtilService clusterUtilService;
    private final AccountService accountService;
    private final ShellHelper shellHelper;
    private final ApplicationEventPublisher publisher;
    private final ClusterPortInfoHelper clusterUrlPrinter;
    private final GenesisConfig genesisConfig;

    @ShellMethod(value = "List devnet nodes. Use `list-nodes`. Deprecated: `list-clusters`", key = {"list-nodes", "list-clusters"})
    public void listLocalClusters() {
        try {
            List<String> clusters = localClusterService.listClusters();
            writeLn("Available DevNet nodes:");
            clusters.forEach(cluster -> writeLn(cluster));
        } catch (Exception e) {
            writeLn(error("DevNet listing failed. " + e.getMessage()));
        }
    }

    @ShellMethod(value = "Enter local devnet node mode. Use `node` or `devnet` command.", key = {"node", "devnet"})
    public void startLocalClusterContext(@ShellOption(value = {"-n", "--name"}, defaultValue = "default", help = "Node Name") String clusterName) {
        try {
            if (CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER) {
                localClusterService.stopCluster(msg -> writeLn(msg));
            }

            localClusterService.startClusterContext(clusterName, msg -> writeLn(msg));

            var clusterInfo = localClusterService.getClusterInfo(clusterName);
            CommandContext.INSTANCE.setCurrentMode(CommandContext.Mode.LOCAL_CLUSTER);
            CommandContext.INSTANCE.setProperty(CLUSTER_NAME, clusterName);
            CommandContext.INSTANCE.setEra(clusterInfo.getEra());
            writeLn(success("Switched to %s", clusterName));
        } catch (Exception e) {
            writeLn(error(e.getMessage()));
        }
    }

    @ShellMethod(value = "Create a local devnet node. Use `create-node`. Deprecated: `create-node`", key = {"create-node", "create-cluster"})
    public void createCluster(@ShellOption(value = {"-n", "--name"}, defaultValue = "default", help = "Node Name") String clusterName,
                              @ShellOption(value = {"--port"}, help = "Node port (Used with --create option only)", defaultValue = "3001") int port,
                              @ShellOption(value = {"--submit-api-port"}, help = "Submit Api Port", defaultValue = "8090") int submitApiPort,
                              @ShellOption(value = {"-s", "--slot-length"}, help = "Slot Length in sec. (0.1 to ..)", defaultValue = "1") double slotLength,
                              @ShellOption(value = {"-b", "--block-time"}, help = "Block time in sec. (1 - 20)", defaultValue = "1") double blockTime,
                              @ShellOption(value = {"-e", "--epoch-length"}, help = "No of slots in an epoch", defaultValue = "500") int epochLength,
                              @ShellOption(value = {"-o", "--overwrite"}, defaultValue = "false", help = "Overwrite existing node directory. default: false") boolean overwrite,
                              @ShellOption(value = {"--start"}, defaultValue = "false", help = "Automatically start the node after create. default: false") boolean start,
                              @ShellOption(value = {"--era"}, defaultValue = "babbage",  help = "Era (babbage, conway)") String era,
                              @ShellOption(value = {"--genesis-profile",}, defaultValue = ShellOption.NULL, help = "Use a pre-defined genesis profile (Options: zero_fee)") GenesisProfile genesisProfile,
                              @ShellOption(value = {"--generate-new-keys"}, defaultValue = "false", help = "Generate new genesis keys, pool keys instead of default keys") boolean generateNewKeys,
                              @ShellOption(value = {"--no-genesis-keys"}, defaultValue = "1", help = "No of genesis keys to generate") int nGenesisKeys
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

            //Era check
            Era nodeEra;
            if (era == null || era.isEmpty())
                nodeEra = Era.Babbage;
            else if (era.equalsIgnoreCase("babbage"))
                nodeEra = Era.Babbage;
            else if (era.equalsIgnoreCase("conway"))
                nodeEra = Era.Conway;
            else {
                writeLn(error("Invalid era. Valid values are babbage, conway"));
                return;
            }

            long protocolMagic = genesisConfig.getProtocolMagic();

            //stop any cluster if running
            localClusterService.stopCluster(msg -> writeLn(msg));
            publisher.publishEvent(new ClusterStopped(clusterName));

            ClusterInfo clusterInfo = ClusterInfo.builder()
                    .nodePort(port)
                    .submitApiPort(submitApiPort)
                    .slotLength(slotLength)
                    .blockTime(blockTime)
                    .epochLength(epochLength)
                    .protocolMagic(protocolMagic)
                    .p2pEnabled(true)
                    .masterNode(true)
                    .isBlockProducer(true)
                    .era(nodeEra)
                    .genesisProfile(genesisProfile)
                    .build();

            boolean success = localClusterService.createNodeClusterFolder(clusterName, clusterInfo, overwrite, generateNewKeys, nGenesisKeys, (msg) -> writeLn(msg));

            if (success) {
                printClusterInfo(clusterName);

                //change to Local Cluster Context
                CommandContext.INSTANCE.setCurrentMode(CommandContext.Mode.LOCAL_CLUSTER);
                CommandContext.INSTANCE.setProperty(CLUSTER_NAME, clusterName);
                CommandContext.INSTANCE.setEra(nodeEra);

                if (start)
                    startLocalCluster();
            }
        } catch (Exception e) {
            log.error("Error", e);
            writeLn(error(e.getMessage()));
        }
    }

    @ShellMethod(value = "Get devnet info", key = "info")
    @ShellMethodAvailability("localClusterCmdAvailability")
    private void getClusterInfo() {
        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
        try {
            printClusterInfo(clusterName);
        } catch (Exception e) {
            writeLn(error(e.getMessage()));
        }
    }

    private void printClusterInfo(String clusterName) throws IOException {
        ClusterInfo clusterInfo = localClusterService.getClusterInfo(clusterName);
        clusterUrlPrinter.printUrls(clusterName, clusterInfo);
    }

    @ShellMethod(value = "Delete a local devnet node. Use `delete-node`. Deprecated: `delete-cluster`", key = {"delete-node", "delete-cluster"})
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void deleteLocalCluster(@ShellOption(value = {"-n", "--name"}, help = "Node Name") String nodeName) {
        if (nodeName == null ||  nodeName.isEmpty()) {
            writeLn(error("Node name is required"));
            return;
        }
        try {
            localClusterService.deleteCluster(nodeName, (msg) -> {
                writeLn(msg);
            });
            publisher.publishEvent(new ClusterDeleted(nodeName));
        } catch (IOException e) {
            if (log.isDebugEnabled())
                log.error("Delete error", e);
            writeLn(error("Deletion failed for devnet node: %s", nodeName));
        }
    }

    @ShellMethod(value = "Start local devnet", key = "start")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void startLocalCluster() {
        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
        boolean started = localClusterService.startCluster(clusterName);
        if (!started)
            return;

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

    @ShellMethod(value = "Stop the running local devnet", key = "stop")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void stopLocalCluster() {
        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
        localClusterService.stopCluster(msg -> writeLn(msg));

        publisher.publishEvent(new ClusterStopped(clusterName));
    }

    @ShellMethod(value = "Reset local devnet. Delete data and logs folder and restart.", key = "reset")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void resetLocalCluster() {
        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
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

    @ShellMethod(value = "Show recent logs for running devnet node", key = "logs")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void logsLocalCluster() {
        localClusterService.logs(msg -> writeLn(msg));
    }

    @ShellMethod(value = "Show recent logs for submit api process", key = "submit-api-logs")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void logsSubmitApi() {
        localClusterService.submitApiLogs(msg -> writeLn(msg));
    }

    @ShellMethod(value = "Tail local devnet", key = "ltail")
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
        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
        OutputFormatter outputFormatter = new DefaultOutputFormatter(shellHelper);
        try {
            localClusterService.ltail(clusterName, showMint, showInputs, showMetadata, showDatumhash, showInlineDatum, grouping, outputFormatter);
        } catch (Exception e) {
            writeLn(error(e.getMessage()));
        }
    }

    @ShellMethod(value = "Show available utxos at faucet addresses", key = "show-faucet-addresses")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void showFaucetAddresses() {
        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
        Era era = CommandContext.INSTANCE.getEra();

        Map<String, List<Utxo>> utxosMap = accountService.getUtxosAtDefaultAccounts(clusterName, era, msg -> writeLn(msg));
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

    @ShellMethod(value = "Get tip/current block number", key = "tip")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void getTip() {
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
