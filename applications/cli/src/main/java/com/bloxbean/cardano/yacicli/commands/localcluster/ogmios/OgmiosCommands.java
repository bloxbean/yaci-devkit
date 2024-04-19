package com.bloxbean.cardano.yacicli.commands.localcluster.ogmios;

import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@ShellComponent
@ShellCommandGroup(Groups.OGMIOS_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class OgmiosCommands {
    private final OgmiosService ogmiosService;

    @ShellMethod(value = "Show recent Ogmios logs", key = "ogmios-logs")
    @ShellMethodAvailability("ogmiosEnableAvailability")
    public void showOgmiosLogs() {
        ogmiosService.showLogs(msg -> writeLn(msg));
    }

    @ShellMethod(value = "Show recent Kupo logs", key = "kupo-logs")
    @ShellMethodAvailability("ogmiosEnableAvailability")
    public void showKupoLogs() {
        ogmiosService.showLogs(msg -> writeLn(msg));
    }

    @ShellMethod(value = "Enable Ogmios & Kupo")
    public void enableOgmios() {
        ogmiosService.setEnableOgmios(true);
        writeLn(infoLabel("OK", "Ogmios/Kupo Status: Enable"));
    }

    @ShellMethod(value = "Disble Ogmios & Kupo")
    public void disableOgmios() {
        ogmiosService.setEnableOgmios(false);
        writeLn(infoLabel("OK", "Ogmios/Kupo Status: Disable"));
    }

    @ShellMethod(value = "Check if Ogmios and Kupo are enabled or disabled")
    public void checkOgmiosStatus() {
        if (ogmiosService.isEnableOgmios())
            writeLn(info("Ogmios/Kupo Status: Enable"));
        else
            writeLn(info("Ogmios/Kupo Status: disable"));
    }

    public Availability ogmiosEnableAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster modes");
    }
}
