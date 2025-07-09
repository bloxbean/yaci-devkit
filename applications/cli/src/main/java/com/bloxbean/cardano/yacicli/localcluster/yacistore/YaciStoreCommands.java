package com.bloxbean.cardano.yacicli.localcluster.yacistore;

import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.config.ApplicationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.*;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@ShellComponent
@ShellCommandGroup(Groups.YACI_STORE_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class YaciStoreCommands {
    private final ApplicationConfig appConfig;
    private final YaciStoreService yaciStoreService;
    private final YaciStoreCustomDbHelper yaciStoreCustomDbHelper;

    @ShellMethod(value = "Show recent Yaci Store logs", key = "yaci-store-logs")
    @ShellMethodAvailability("yaciStoreEnableAvailability")
    public void showLogs() {
        yaciStoreService.showLogs(msg -> writeLn(msg));
    }

    @ShellMethod(value = "Enable Yaci Store")
    public void enableYaciStore() {
        appConfig.setYaciStoreEnabled(true);
        writeLn(infoLabel("OK", "Yaci Store Status: Enable"));
    }

    @ShellMethod(value = "Disble Yaci Store")
    public void disableYaciStore() {
        appConfig.setYaciStoreEnabled(false);
        writeLn(infoLabel("OK", "Yaci Store Status: Disable"));
    }

    @ShellMethod(value = "Check if Yaci Store is enable or disable")
    public void checkYaciStoreStatus() {
        if (appConfig.isYaciStoreEnabled())
            writeLn(info("Yaci Store Status: Enable"));
        else
            writeLn(info("Yaci Store Status: disable"));
    }

    @ShellMethod(value = "Drop yaci store db or schema (for external Postgres)", key = "yaci-store-drop-db")
    public void dropYaciStoreDb() {
        String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
        yaciStoreCustomDbHelper.dropDatabase(clusterName);
    }

    @ShellMethod(value = "Start Yaci Store", key = "yaci-store-start")
    @ShellMethodAvailability("yaciStoreEnableAvailability")
    public void start() {
        if (!appConfig.isYaciStoreEnabled()) {
            appConfig.setYaciStoreEnabled(true);
        }

        String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
        yaciStoreService.start(clusterName, (msg) -> writeLn(msg));
    }

    @ShellMethod(value = "Stop Yaci Store", key = "yaci-store-stop")
    @ShellMethodAvailability("yaciStoreEnableAvailability")
    public void stop() {
        yaciStoreService.stop();
    }

    @ShellMethod(value = "Restart Yaci Store and sync from beginnning", key = "yaci-store-resync")
    @ShellMethodAvailability("yaciStoreEnableAvailability")
    public void resyncYaciStore() {
        if (!appConfig.isYaciStoreEnabled()) {
            writeLn(error("Yaci Store is not enabled. Please enable it first using 'enable-yaci-store' command."));
            return;
        }

        String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
        yaciStoreService.stopAndSyncFromBeginning(clusterName, msg -> writeLn(msg));
    }

    public Availability yaciStoreEnableAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster modes");
    }
}
