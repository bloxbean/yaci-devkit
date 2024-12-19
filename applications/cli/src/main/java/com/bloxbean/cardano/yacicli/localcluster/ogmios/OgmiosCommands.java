package com.bloxbean.cardano.yacicli.localcluster.ogmios;

import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.localcluster.config.ApplicationConfig;
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
    private final ApplicationConfig appConfig;
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
    public void enableKupomios() {
        appConfig.setOgmiosEnabled(true);
        appConfig.setKupoEnabled(true);
        writeLn(infoLabel("OK", "Ogmios/Kupo Status: Enable"));
    }

    @ShellMethod(value = "Disble Ogmios & Kupo", key ={ "disable-kupomios", "disable-ogmios-kupo"})
    public void disableOgmiosKupo() {
        appConfig.setOgmiosEnabled(false);
        appConfig.setKupoEnabled(false);
        writeLn(infoLabel("OK", "Ogmios/Kupo Status: Disable"));
    }

    @ShellMethod(value = "Enable Ogmios")
    public void enableOgmios() {
        appConfig.setOgmiosEnabled(true);
        writeLn(infoLabel("OK", "Ogmios Status: Enable"));
    }

    @ShellMethod(value = "Disble Ogmios")
    public void disableOgmios() {
        appConfig.setOgmiosEnabled(false);
        writeLn(infoLabel("OK", "Ogmios Status: Disable"));
    }

    @ShellMethod(value = "Check if Ogmios is enabled or disabled")
    public void checkOgmiosStatus() {
        if (appConfig.isOgmiosEnabled())
            writeLn(info("Ogmios Status: Enable"));
        else
            writeLn(info("Ogmios Status: disable"));
    }

    @ShellMethod(value = "Check if Kupo is enabled or disabled")
    public void checkKupoStatus() {
        if (appConfig.isKupoEnabled())
            writeLn(info("Kupo Status: Enable"));
        else
            writeLn(info("Kupo Status: disable"));
    }

    public Availability ogmiosEnableAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster modes");
    }
}
