package com.bloxbean.cardano.yacicli.commands.general;

import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.commands.Quit;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@ShellComponent
public class ExitCommand implements Quit.Command {
    private ClusterService clusterService;

    public ExitCommand(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @ShellMethod(value = "Exit the shell / Exit cluster mode", key = {"quit", "exit", "terminate"})
    public void quit() {
        if (CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER) {
            clusterService.stopCluster(msg -> writeLn(msg));

            CommandContext.INSTANCE.setCurrentMode(CommandContext.Mode.REGULAR);
        } else {
            writeLn(info("Exiting yaci-cli"));
            writeLn(success("Bye!!!"));
            System.exit(0);
        }

    }
}

