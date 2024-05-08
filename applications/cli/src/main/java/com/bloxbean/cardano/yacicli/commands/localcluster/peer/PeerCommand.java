package com.bloxbean.cardano.yacicli.commands.localcluster.peer;

import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterCommands;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterStopped;
import com.bloxbean.cardano.yacicli.common.AnsiColors;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.*;

import java.io.IOException;
import java.math.BigInteger;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@ShellComponent
@ShellCommandGroup(Groups.NODE_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class PeerCommand {
    public static final String CUSTER_NAME = "cluster_name";
    private final ClusterService clusterService;
    private final ApplicationEventPublisher publisher;
    private final PeerService peerService;
    private final ClusterCommands clusterCommands;

    @Value("${bp.create.enabled:true}")
    private boolean bpCreateEnable;

    @ShellMethod(value = "Join a existing cluster", key = "join")
    public void joinCluster(
            @ShellOption(value = {"-a", "--admin-url"}, defaultValue = "http://localhost:10000", help = "Cluster Admin URL") String adminUrl,
            @ShellOption(value = {"--port-shift"}, help = "Node Id. 0, 1, 2, 3 ...", defaultValue = "0") int portShift,
            @ShellOption(value = {"-n", "--name"}, defaultValue = "default", help = "Node name") String nodeName,
            @ShellOption(value = {"-o", "--overwrite"}, defaultValue = "false", help = "Overwrite") boolean overwrite,
            @ShellOption(value = {"--bp"}, defaultValue = "false", help = "True if this is a block producing node, else false") boolean bp,
            @ShellOption(value = {"--start"}, defaultValue = "false", help = "Automatically start the node after create. default: false") boolean start,
            @ShellOption(value = {"--overwrite--pool-keys"}, defaultValue = "false", help = "Overwrite Pool Keys") boolean overwritePoolKeys,
            @ShellOption(value = {"--era"}, defaultValue = "babbage",  help = "Era (babbage, conway)") String era
    ) {
        try {
            //stop any cluster if running
            clusterService.stopCluster(msg -> writeLn(msg));
            publisher.publishEvent(new ClusterStopped(nodeName));

            if (bp && !bpCreateEnable) {
                writeLn(error("Block producing peer node creation is disabled as this feature is in experimental mode." +
                        " Please enable by setting bp.create.enabled=true in configuration file"));
                return;
            }

            if (bp)
                writeLn(info("Creating a new block producing node ..."));
            else
                writeLn(info("Creating a new relay node ..."));

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

            boolean success = peerService.addPeer(adminUrl, nodeName, portShift, bp, overwrite, overwritePoolKeys);
            if (success) {
                clusterService.startClusterContext(nodeName, msg -> writeLn(msg));
                printClusterInfo(nodeName);

                //change to Local Cluster Context
                CommandContext.INSTANCE.setCurrentMode(CommandContext.Mode.LOCAL_CLUSTER);
                CommandContext.INSTANCE.setProperty(CUSTER_NAME, nodeName);
                CommandContext.INSTANCE.setEra(nodeEra);
                writeLn(success("Switched to %s", nodeName));

                if (start)
                    clusterCommands.startLocalCluster();
            }

        } catch (Exception e) {
            log.error("Error", e);
            writeLn(error(e.getMessage()));
        }
    }

    @ShellMethod(value = "Register a pool", key = "register-pool")
    @ShellMethodAvailability("bpCommandAvailability")
    public void registerPool(
            @ShellOption(value = {"-t", "--ticker"}, defaultValue = "DEFAULT_POOL", help = "Pool Ticker") String poolTicker,
            @ShellOption(value = {"-m", "--metadata-hash"}, defaultValue = "6bf124f217d0e5a0a8adb1dbd8540e1334280d49ab861127868339f43b3948af", help = "Metadata Hash") String metadataHash,
            @ShellOption(value = {"-p", "--pledge"}, defaultValue = "10000000", help = "Pledge. Default 10,000,000 lovelace") BigInteger pledge,
            @ShellOption(value = {"-c", "--cpst"}, defaultValue = "340000000", help = "Cost in lovelace. Default 340 Ada") BigInteger cost,
            @ShellOption(value = {"--margin"}, defaultValue = "0.02", help = "Relay Host. Default 0.02") double margin,
            @ShellOption(value = {"-r", "--relay-host"}, defaultValue = "127.0.0.1", help = "Relay Host") String relayHost,
            @ShellOption(value = {"-p", "--relay-port"}, defaultValue = "0", help = "Relay Port") int relayPort
    ) {
        if (!bpCreateEnable) {
            writeLn(error("Block producing peer node creation is disabled as this feature is in experimental mode." +
                    " Please enable by setting bp.create.enabled=true in configuration file"));
            return;
        }

        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);

        PoolConfig poolConfig = PoolConfig.builder()
                .ticker(poolTicker)
                .metadataHash(metadataHash)
                .pledge(pledge)
                .cost(cost)
                .margin(margin)
                .relayHost(relayHost)
                .relayPort(relayPort)
                .build();

        peerService.registerPool(clusterName, poolConfig, msg -> writeLn(msg));
    }

    private void printClusterInfo(String clusterName) throws IOException {
        ClusterInfo clusterInfo = clusterService.getClusterInfo(clusterName);
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

    public Availability peerCommandAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster modes");
    }

    public Availability bpCommandAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER && bpCreateEnable
                ? Availability.available()
                : Availability.unavailable("This command is not supported yet");
    }
}
