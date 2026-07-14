package com.bloxbean.cardano.yacicli.localcluster.commands;

import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.commands.common.Groups;
import com.bloxbean.cardano.yacicli.localcluster.scenario.ScenarioResult;
import com.bloxbean.cardano.yacicli.localcluster.scenario.ScenarioService;
import com.bloxbean.cardano.yacicli.localcluster.scenario.ScenarioValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.Availability;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import java.nio.file.Files;
import java.nio.file.Path;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@ShellComponent
@ShellCommandGroup(Groups.TXN_CMD_GROUP)
@RequiredArgsConstructor
@Slf4j
public class ScenarioCommands {
    private final ScenarioService scenarioService;

    @ShellMethod(value = "Run a declarative DevKit scenario / TxPlan / TxFlow YAML against the devnet", key = {"run-scenario", "run"})
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void runScenario(@ShellOption(value = {"-f", "--file"}, help = "Path to the scenario YAML file") String file,
                            @ShellOption(value = {"--async"}, defaultValue = "false", help = "Run scenario asynchronously") boolean async) {
        try {
            Path path = Path.of(file);
            if (!Files.exists(path)) {
                writeLn(error("Scenario file not found: " + file));
                return;
            }

            String yaml = Files.readString(path);
            writeLn(infoLabel("Scenario", "Running " + file));

            ScenarioResult result = async
                    ? scenarioService.runAsync(yaml, msg -> writeLn(msg))
                    : scenarioService.run(yaml, msg -> writeLn(msg));
            if ("running".equals(result.getStatus())) {
                writeLn(success("Scenario started. RunId: " + result.getRunId()));
            } else if (result.isSuccess()) {
                writeLn(success("Scenario executed successfully (" + result.getType() + ")"));
                if (result.getTxHashes() != null) {
                    result.getTxHashes().forEach(h -> writeLn(info("Tx: " + h)));
                }
            } else {
                writeLn(error("Scenario failed: " + result.getMessage()));
            }
        } catch (Exception e) {
            log.error("Error running scenario", e);
            writeLn(error("Error running scenario: " + e.getMessage()));
        }
    }

    @ShellMethod(value = "Validate a declarative DevKit scenario / TxPlan / TxFlow YAML", key = {"validate-scenario"})
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void validateScenario(@ShellOption(value = {"-f", "--file"}, help = "Path to the scenario YAML file") String file) {
        try {
            String yaml = Files.readString(Path.of(file));
            ScenarioValidationResult result = scenarioService.validate(yaml);
            if (result.isValid()) {
                writeLn(success("Scenario YAML is valid (" + result.getType() + ")"));
            } else {
                writeLn(error("Scenario YAML is invalid (" + result.getType() + ")"));
                result.getErrors().forEach(e -> writeLn(error(" - " + e)));
            }
        } catch (Exception e) {
            log.error("Error validating scenario", e);
            writeLn(error("Error validating scenario: " + e.getMessage()));
        }
    }

    @ShellMethod(value = "Get async scenario run status", key = {"scenario-status"})
    @ShellMethodAvailability("localClusterCmdAvailability")
    public void scenarioStatus(@ShellOption(value = {"--run-id"}, help = "Scenario run id") String runId) {
        ScenarioResult result = scenarioService.status(runId);
        writeLn(infoLabel("Status", result.getStatus()));
        writeLn(infoLabel("Message", result.getMessage()));
    }

    public Availability localClusterCmdAvailability() {
        return CommandContext.INSTANCE.getCurrentMode() == CommandContext.Mode.LOCAL_CLUSTER
                ? Availability.available()
                : Availability.unavailable("you are not in local-cluster mode");
    }
}
