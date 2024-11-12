package com.bloxbean.cardano.yacicli.localcluster.yacistore;

import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
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
    private final YaciStoreService yaciStoreService;
    private final YaciStoreCustomDbHelper yaciStoreCustomDbHelper;

    @ShellMethod(value = "Show recent Yaci Store logs", key = "yaci-store-logs")
    @ShellMethodAvailability("yaciStoreEnableAvailability")
    public void showLogs() {
        yaciStoreService.showLogs(msg -> writeLn(msg));
    }

    @ShellMethod(value = "Enable Yaci Store")
    public void enableYaciStore() {
        yaciStoreService.setEnableYaciStore(true);
        writeLn(infoLabel("OK", "Yaci Store Status: Enable"));
    }

    @ShellMethod(value = "Disble Yaci Store")
    public void disableYaciStore() {
        yaciStoreService.setEnableYaciStore(false);
        writeLn(infoLabel("OK", "Yaci Store Status: Disable"));
    }

    @ShellMethod(value = "Check if Yaci Store is enable or disable")
    public void checkYaciStoreStatus() {
        if (yaciStoreService.isEnableYaciStore())
            writeLn(info("Yaci Store Status: Enable"));
        else
            writeLn(info("Yaci Store Status: disable"));
    }

    @ShellMethod(value = "Drop yaci store db or schema (for external Postgres)", key = "yaci-store-drop-db")
    public void dropYaciStoreDb() {
        String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
        yaciStoreCustomDbHelper.dropDatabase(clusterName);
    }

    public Availability yaciStoreEnableAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster modes");
    }
}
