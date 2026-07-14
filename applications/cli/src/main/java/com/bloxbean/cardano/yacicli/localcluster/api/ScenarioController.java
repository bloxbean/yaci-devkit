package com.bloxbean.cardano.yacicli.localcluster.api;

import com.bloxbean.cardano.yacicli.localcluster.api.model.ScenarioRunRequest;
import com.bloxbean.cardano.yacicli.localcluster.scenario.ScenarioResult;
import com.bloxbean.cardano.yacicli.localcluster.scenario.ScenarioService;
import com.bloxbean.cardano.yacicli.localcluster.scenario.ScenarioValidationResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Executes declarative L3 scenario, TxPlan, or TxFlow YAML against the devnet.
 */
@RestController
@RequestMapping(path = "/local-cluster/api/scenarios")
@RequiredArgsConstructor
@Tag(name = "Scenario API", description = "Run declarative DevKit scenario / TxPlan / TxFlow YAML against the devnet")
@Slf4j
public class ScenarioController {
    private final ScenarioService scenarioService;

    @PostMapping(path = "run")
    public ResponseEntity<ScenarioResult> run(@RequestBody ScenarioRunRequest request) {
        ScenarioResult result = request.isAsync()
                ? scenarioService.runAsync(request.getYaml(), msg -> log.debug(msg))
                : scenarioService.run(request.getYaml());
        if ("running".equals(result.getStatus())) {
            return ResponseEntity.accepted().body(result);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping(path = "validate")
    public ResponseEntity<ScenarioValidationResult> validate(@RequestBody ScenarioRunRequest request) {
        ScenarioValidationResult result = scenarioService.validate(request.getYaml());
        return result.isValid()
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @GetMapping(path = "status/{runId}")
    public ScenarioResult status(@PathVariable String runId) {
        return scenarioService.status(runId);
    }

    @GetMapping(path = "schema")
    public String schema() {
        return scenarioService.schema();
    }

    @GetMapping(path = "examples")
    public String examples() {
        return scenarioService.examples();
    }
}
