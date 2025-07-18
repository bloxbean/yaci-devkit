package com.bloxbean.cardano.yacicli.commands.general;

import com.bloxbean.cardano.yacicli.commands.common.DownloadService;
import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.localcluster.ClusterCommands;
import com.bloxbean.cardano.yacicli.localcluster.config.ApplicationConfig;
import com.bloxbean.cardano.yacicli.localcluster.profiles.GenesisProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.*;

import java.util.ArrayList;
import java.util.Arrays;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@ShellComponent
@ShellCommandGroup(Groups.GENERAL_CMD_GROUP)
@RequiredArgsConstructor
public class DownloadCommand {
    private final ApplicationConfig applicationConfig;
    private final DownloadService downloadService;
    private final ClusterCommands clusterCommands;

    @ShellMethod(value = "Download", key = "download")
    @ShellMethodAvailability({"nonDockerCommandAvailability"})
    public boolean download(
            @ShellOption(value = {"--component", "-c"}, defaultValue = "all",  help = "Provide list of components separated by space. Components: node,ogmios,kupo,yaci-store,yaci-store-jar") String[] components,
            @ShellOption(value = {"-o", "--overwrite"}, defaultValue = "false", help = "Overwrite existing installation. default: false") boolean overwrite
            ) {

        try {
            if (components == null) {
                writeLn(error("Component is not provided. Please provide the component to download"));
                return false;
            }

            var componentList = Arrays.asList(components);
            boolean validComponent = false;

            if (componentList.contains("all") || componentList.contains("node")) {
                downloadService.downloadNode(overwrite);
                validComponent = true;
            }

            if (componentList.contains("all") || componentList.contains("yaci-store")) {
                downloadService.downloadYaciStoreNative(overwrite);
                validComponent = true;
            }

            if (componentList.contains("yaci-store-jar")) {
                downloadService.downloadYaciStoreJar(overwrite);
                validComponent = true;
            }

            if (componentList.contains("all") || componentList.contains("ogmios")) {
                downloadService.downloadOgmios(overwrite);
                validComponent = true;
            }

            if (componentList.contains("all") || componentList.contains("kupo")) {
                downloadService.downloadKupo(overwrite);
                validComponent = true;
            }

            if (!validComponent) {
                writeLn(error("Invalid components : " + componentList));
                return false;
            }

        } catch (Exception e) {
            writeLn(error("Error downloading component : " + e.getMessage()));
            return false;
        }

        return true;
    }

    @ShellMethod(value = "Download and Start DevNet with other selected components", key = "up")
    @ShellMethodAvailability({"nonDockerCommandAvailability"})
    public void downloadAndStart(
            @ShellOption(value = {"--component", "-c"}, defaultValue = "node",  help = "Provide list of components separated by space. Components: node,ogmios,kupo,yaci-store,yaci-store-jar") String[] components,
            @ShellOption(value = {"-o", "--overwrite"}, defaultValue = "false", help = "Overwrite existing installation. default: false") boolean overwrite,
            @ShellOption(value = {"-n", "--name"}, defaultValue = "default", help = "Node Name") String clusterName,
            @ShellOption(value = {"--port"}, help = "Node port (Used with --create option only)", defaultValue = "3001") int port,
            @ShellOption(value = {"--submit-api-port"}, help = "Submit Api Port", defaultValue = "8090") int submitApiPort,
            @ShellOption(value = {"-s", "--slot-length"}, help = "Slot Length in sec. (0.1 to ..)", defaultValue = "1") double slotLength,
            @ShellOption(value = {"-b", "--block-time"}, help = "Block time in sec. (1 - 20)", defaultValue = "1") double blockTime,
            @ShellOption(value = {"-e", "--epoch-length"}, help = "No of slots in an epoch", defaultValue = "600") int epochLength,
            @ShellOption(value = {"--genesis-profile"}, defaultValue = ShellOption.NULL,
                    help = "Use a pre-defined genesis profile (Options: zero_fee, zero_min_utxo_value, zero_fee_and_min_utxo_value)") GenesisProfile genesisProfile,
            @ShellOption(value = {"--enable-yaci-store"}, defaultValue = "false", help = "Enable Yaci Store. This will also enable Ogmios for Tx Evaluation") boolean enableYaciStore,
            @ShellOption(value = {"--enable-kupomios"}, defaultValue = "false", help= "Enable Ogmios and Kupo") boolean enableKupomios,
            @ShellOption(value = {"--interactive"}, defaultValue="false", help="To start in interactive mode when 'up' command is passed as an arg to yaci-cli") boolean interactive,
            @ShellOption(value = {"--tail"}, defaultValue="false", help="To tail the network when 'up' command is passed as an arg to yaci-cli. Only works in non-interactive mode.") boolean tail,
            @ShellOption(value = {"--enable-multi-node"}, defaultValue = "false", help="Create multiple local block producing nodes") boolean enableMultiNode,
            @ShellOption(value = {"--stake-ratio-factor"}, defaultValue = "5", help="The stake ratio between the primary node and two peers is only used when multi-node is enabled for rollback testing.") int stakeRatioFactor
    ) {

        if (components == null)
            components = new String[0];

        var componentList = new ArrayList<>(Arrays.asList(components));

        if (enableYaciStore) {
            applicationConfig.setYaciStoreEnabled(true);
            applicationConfig.setOgmiosEnabled(true);
        } else if (enableKupomios){
            applicationConfig.setOgmiosEnabled(true);
            applicationConfig.setKupoEnabled(true);
        }

        if (enableYaciStore) {
            if (!componentList.contains("yaci-store"))
                componentList.add("yaci-store");
            if (!componentList.contains("ogmios")) {
                componentList.add("ogmios");
            }
        } else if (enableKupomios) {
            if (!componentList.contains("ogmios"))
                componentList.add("ogmios");
            if (!componentList.contains("kupo"))
                componentList.add("kupo");
        }

        var status = download(componentList.toArray(new String[0]), overwrite);
        if (status) {
            clusterCommands.createCluster(clusterName, port, submitApiPort, slotLength, blockTime, epochLength,
                    true, true, null, genesisProfile, false, enableMultiNode, stakeRatioFactor);

            if (!interactive && tail)
                clusterCommands.ltail(true, true, true, true, true, true, null, null);
        }
    }

    public Availability nonDockerCommandAvailability() {
        return !applicationConfig.isDocker()
                ? Availability.available()
                : Availability.unavailable("This command is only supported in non-Docker mode.");
    }
}
