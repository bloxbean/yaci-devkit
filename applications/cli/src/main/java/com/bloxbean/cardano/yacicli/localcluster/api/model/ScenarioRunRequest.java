package com.bloxbean.cardano.yacicli.localcluster.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioRunRequest {
    /** A DevKit L3 scenario, CCL TxPlan, or CCL TxFlow document in YAML. */
    private String yaml;
    /** When true, return a runId immediately and execute in the background. */
    private boolean async;
}
