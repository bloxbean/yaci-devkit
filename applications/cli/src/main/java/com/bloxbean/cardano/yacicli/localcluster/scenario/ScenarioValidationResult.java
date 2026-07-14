package com.bloxbean.cardano.yacicli.localcluster.scenario;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ScenarioValidationResult {
    private String type;
    private boolean valid;
    @Builder.Default
    private List<String> errors = new ArrayList<>();
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    public static ScenarioValidationResult ok(String type) {
        return ScenarioValidationResult.builder()
                .type(type)
                .valid(true)
                .build();
    }

    public static ScenarioValidationResult failed(String type, List<String> errors) {
        return ScenarioValidationResult.builder()
                .type(type)
                .valid(false)
                .errors(errors)
                .build();
    }
}
