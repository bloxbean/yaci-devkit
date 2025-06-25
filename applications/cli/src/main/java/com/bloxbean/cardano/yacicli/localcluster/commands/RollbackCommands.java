package com.bloxbean.cardano.yacicli.localcluster.commands;

import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.localcluster.service.RollbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.*;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@ShellComponent
@ShellCommandGroup(Groups.ROLLBACK_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class RollbackCommands {
    private final RollbackService rollbackService;

    @ShellMethod(value = "Set DB snapshot for rollback", key = "take-db-snapshot")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void takeDBSnapshot() {
        rollbackService.takeDBSnapshot(msg -> writeLn(msg));
    }

    @ShellMethod(value = "Rollback to last db snapshot", key = "rollback-to-db-snapshot")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void rollbackToLastDBSnapshot() {
        rollbackService.rollbackToLastDBSnapshot(msg -> writeLn(msg));
    }

    @ShellMethod(value = "Create forks for rollback", key = "create-forks")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void createForks(
            @ShellOption(value = {"--restart-node"}, defaultValue = "false", help = "Use 'true' to restart the main node with a specified delay.") boolean restartNode,
            @ShellOption(value = {"--wait-time"}, defaultValue = "10", help = "Wait time before starting the node after detaching main node from peer nodes") long waitTime) {
        rollbackService.createForks(restartNode, waitTime, msg -> writeLn(msg));
    }

    @ShellMethod(value = "Join forks for rollback", key = "join-forks")
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void joinForks() {
        rollbackService.joinForks(msg -> writeLn(msg));
    }

    public Availability localClusterCmdAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster modes");
    }
}
