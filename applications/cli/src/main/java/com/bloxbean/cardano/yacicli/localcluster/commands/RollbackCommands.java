package com.bloxbean.cardano.yacicli.localcluster.commands;

import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.service.RollbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.*;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@ShellComponent
@ShellCommandGroup(Groups.ROLLBACK_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class RollbackCommands {
    private final RollbackService rollbackService;
    private final ApplicationEventPublisher publisher;

    @ShellMethod(value = "Set rollback point", key = "set-rollback-point")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void setRollbackPoint() {
        rollbackService.setRollbackPoint(msg -> writeLn(msg));
    }

    @ShellMethod(value = "Rollback to rollback point", key = "rollback")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void rollback() {
        rollbackService.rollback(msg -> writeLn(msg));
    }

    public Availability localClusterCmdAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster modes");
    }
}
