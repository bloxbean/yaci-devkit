package com.bloxbean.cardano.yacicli.localcluster.scenario;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScenarioRunnerTest {

    private final ScenarioOperationExecutor operationExecutor = mock(ScenarioOperationExecutor.class);
    private final ScenarioControlService controlService = mock(ScenarioControlService.class);
    private final ScenarioRunner runner = new ScenarioRunner(operationExecutor, controlService);

    @Test
    void validatesL3ScenarioShape() {
        String yaml = """
                scenario:
                  name: demo
                  steps:
                    - log: hello
                    - group:
                        name: nested
                        steps:
                          - log: inside
                """;

        ScenarioValidationResult result = runner.validate(yaml);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validationFailsForUnknownAction() {
        String yaml = """
                scenario:
                  steps:
                    - unknown: {}
                """;

        ScenarioValidationResult result = runner.validate(yaml);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(error -> assertThat(error).contains("Unknown scenario action"));
    }

    @Test
    void runsLogOnlyScenario() {
        when(controlService.stateSummary()).thenReturn(Map.of("cluster", "default"));
        String yaml = """
                scenario:
                  name: log-demo
                  variables:
                    message: hello
                  steps:
                    - log: "${message}"
                      id: first
                    - assert:
                        tx: { ref: first, status: success }
                """;

        ScenarioResult result = runner.run(yaml, msg -> {});

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getType()).isEqualTo("Scenario");
        assertThat(result.getSteps()).hasSize(2);
    }

    @Test
    void resolvesExampleOperationFilesFromWorkspaceRoot() throws Exception {
        when(operationExecutor.runTxPlan(anyString(), any())).thenReturn(ScenarioResult.ok("TxPlan", java.util.List.of("txhash")));
        when(controlService.stateSummary()).thenReturn(Map.of("cluster", "default"));

        String yaml = Files.readString(Path.of("../../examples/scenarios/l3-payment-assert.yaml"));

        ScenarioResult result = runner.run(yaml, msg -> {});

        assertThat(result.isSuccess()).as("%s", result).isTrue();
        assertThat(result.getTxHashes()).containsExactly("txhash");
    }

    @Test
    void expectedFailureAppliesToControlSteps() {
        when(controlService.rollback(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(false);
        when(controlService.stateSummary()).thenReturn(Map.of("cluster", "default"));
        String yaml = """
                scenario:
                  name: expected-rollback-failure
                  steps:
                    - rollback: { blocks: 1 }
                      expect_failure: true
                      id: rb
                    - assert:
                        tx: { ref: rb, status: expected_failure }
                """;

        ScenarioResult result = runner.run(yaml, msg -> {});

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSteps()).extracting(ScenarioStepResult::getStatus)
                .containsExactly("expected_failure", "success");
    }

    @Test
    void finalStateFailureDoesNotFailSuccessfulScenario() {
        doThrow(new IllegalStateException("node unavailable")).when(controlService).stateSummary();
        String yaml = """
                scenario:
                  steps:
                    - log: ok
                """;

        ScenarioResult result = runner.run(yaml, msg -> {});

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getFinalState()).containsEntry("final_state_available", false);
    }

    @Test
    void expectedFailureNormalizesOperationStepStatus() {
        when(operationExecutor.runTxPlan(anyString(), any())).thenReturn(ScenarioResult.failed("TxPlan", "script failed"));
        when(controlService.stateSummary()).thenReturn(Map.of("cluster", "default"));
        String yaml = """
                scenario:
                  steps:
                    - tx:
                        inline:
                          transaction: []
                      expect_failure: true
                      id: tx1
                    - assert:
                        tx: { ref: tx1, status: failed }
                """;

        ScenarioResult result = runner.run(yaml, msg -> {});

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSteps().get(0).getStatus()).isEqualTo("expected_failure");
    }

    @Test
    void resolvesBracketStepOutputExpressions() {
        when(controlService.stateSummary()).thenReturn(Map.of("cluster", "default"));
        String yaml = """
                scenario:
                  steps:
                    - log: hello
                      id: first
                    - log: "${steps.first.outputs[message]}"
                """;

        ScenarioResult result = runner.run(yaml, msg -> {});

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getSteps().get(1).getMessage()).isEqualTo("hello");
    }

    @Test
    void supportsRawLovelaceBalanceAssertions() {
        when(controlService.utxos(eq("account://acc1"), any())).thenReturn(List.of(
                Utxo.builder()
                        .amount(List.of(Amount.lovelace(BigInteger.valueOf(5_000_000L))))
                        .build()));
        when(controlService.stateSummary()).thenReturn(Map.of("cluster", "default"));
        String yaml = """
                scenario:
                  steps:
                    - assert:
                        account: account://acc1
                        min_lovelace: 5000000
                """;

        ScenarioResult result = runner.run(yaml, msg -> {});

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void assetAssertionsEncodeHumanAssetNames() {
        String policy = "abcd";
        when(controlService.utxos(eq("account://acc1"), any())).thenReturn(List.of(
                Utxo.builder()
                        .amount(List.of(new Amount(policy + "4c50", BigInteger.TEN)))
                        .build()));
        when(controlService.stateSummary()).thenReturn(Map.of("cluster", "default"));
        String yaml = """
                scenario:
                  steps:
                    - assert:
                        account: account://acc1
                        has_asset:
                          policy: abcd
                          name: LP
                          min: 10
                """;

        ScenarioResult result = runner.run(yaml, msg -> {});

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void assetSupplyEncodesHumanAssetNames() {
        when(controlService.assetSupply("abcd4c50")).thenReturn(BigInteger.TEN);
        when(controlService.stateSummary()).thenReturn(Map.of("cluster", "default"));
        String yaml = """
                scenario:
                  steps:
                    - assert:
                        asset_supply:
                          policy: abcd
                          name: LP
                          quantity: 10
                """;

        ScenarioResult result = runner.run(yaml, msg -> {});

        assertThat(result.isSuccess()).isTrue();
        verify(controlService).assetSupply("abcd4c50");
    }

    @Test
    void validationResolvesReferencedOperationFiles() {
        String yaml = """
                scenario:
                  steps:
                    - tx:
                        file: examples/scenarios/does-not-exist.yaml
                """;

        ScenarioValidationResult result = runner.validate(yaml);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrors()).anySatisfy(error -> assertThat(error).contains("file not found"));
    }
}
