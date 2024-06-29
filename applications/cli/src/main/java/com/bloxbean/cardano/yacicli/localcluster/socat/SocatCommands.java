package com.bloxbean.cardano.yacicli.localcluster.socat;

import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.writeLn;

@ShellComponent
@ShellCommandGroup(Groups.NODE_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class SocatCommands {
    private final SocatService socatService;

    @ShellMethod(value = "Show recent socat logs", key = "socat-logs")
    @ShellMethodAvailability("nodeContextAvailability")
    public void showSocatLogs() {
        socatService.showLogs(msg -> writeLn(msg));
    }


    public Availability nodeContextAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster modes");
    }
}
