package com.bloxbean.cardano.yacicli.localcluster;

import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.localcluster.service.DefaultAddressService;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;

@ShellComponent
@ShellCommandGroup(Groups.NODE_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class DefaultAddressCommands {
    private final DefaultAddressService defaultAddressService;

    @ShellMethod(value = "Show default addresses", key = "default-addresses")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void listDefaultAddresses() {
        defaultAddressService.printDefaultAddresses(false);
    }

    public Availability localClusterCmdAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster modes");
    }
}
