package com.bloxbean.cardano.yacicli.localcluster.scenario;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Outcome of executing a declarative scenario against the devnet.
 */
@Data
@Builder
public class ScenarioResult {
    /** Detected scenario format: "Scenario", "TxPlan" or "TxFlow". */
    private String type;
    /** Run lifecycle status: completed, failed, running. */
    private String status;
    private boolean success;
    private String runId;
    private String scenarioName;
    /** Submitted transaction hashes (one for TxPlan, possibly many for TxFlow). */
    private List<String> txHashes;
    private List<ScenarioStepResult> steps;
    private Map<String, Object> finalState;
    /** Human-readable message; the failure reason when {@code success} is false. */
    private String message;

    public static ScenarioResult ok(String type, List<String> txHashes) {
        return ScenarioResult.builder()
                .type(type)
                .status("completed")
                .success(true)
                .txHashes(txHashes)
                .message("Scenario executed successfully")
                .build();
    }

    public static ScenarioResult failed(String type, String message) {
        return ScenarioResult.builder()
                .type(type)
                .status("failed")
                .success(false)
                .message(message)
                .build();
    }

    public static ScenarioResult running(String type, String runId) {
        return ScenarioResult.builder()
                .type(type)
                .status("running")
                .success(false)
                .runId(runId)
                .message("Scenario is running")
                .build();
    }
}
