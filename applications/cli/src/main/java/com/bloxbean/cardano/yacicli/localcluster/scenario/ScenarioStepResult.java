package com.bloxbean.cardano.yacicli.localcluster.scenario;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ScenarioStepResult {
    private int index;
    private String id;
    private String name;
    private String action;
    private String status;
    private String message;
    private List<String> txHashes;
    private Map<String, Object> outputs;
    private long startedAt;
    private long completedAt;

    public boolean isSuccessLike() {
        return "success".equals(status) || "expected_failure".equals(status);
    }
}
