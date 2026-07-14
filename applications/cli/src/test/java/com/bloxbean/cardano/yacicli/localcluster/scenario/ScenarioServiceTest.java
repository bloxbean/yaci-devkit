package com.bloxbean.cardano.yacicli.localcluster.scenario;

import com.bloxbean.cardano.yacicli.localcluster.service.LocalBackendServiceProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScenarioServiceTest {

    private final LocalBackendServiceProvider backendProvider = mock(LocalBackendServiceProvider.class);
    private final ScenarioSignerRegistryFactory registryFactory = mock(ScenarioSignerRegistryFactory.class);
    private final ScenarioOperationExecutor operationExecutor = new ScenarioOperationExecutor(backendProvider, registryFactory);
    private final ScenarioRunner scenarioRunner = mock(ScenarioRunner.class);
    private final ScenarioControlService controlService = mock(ScenarioControlService.class);
    private final ScenarioService service = new ScenarioService(operationExecutor, scenarioRunner, controlService);

    @Test
    void emptyYamlFails() {
        ScenarioResult result = service.run("   ");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).containsIgnoringCase("empty");
    }

    @Test
    void failsClearlyWhenBackendUnavailable() {
        when(backendProvider.getBackendService(anyString())).thenReturn(Optional.empty());
        String txPlanYaml = "version: 1.0\ntransaction:\n  - tx:\n      from_ref: account://acc0\n";

        ScenarioResult result = service.run(txPlanYaml);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getType()).isEqualTo("TxPlan");
        assertThat(result.getMessage()).containsIgnoringCase("backend not available");
    }

    @Test
    void detectsTxFlowFormatFromTopLevelFlowKey() {
        // Backend unavailable short-circuits before execution, but the format is detected first.
        when(backendProvider.getBackendService(anyString())).thenReturn(Optional.empty());
        String txFlowYaml = "version: \"1.0\"\nflow:\n  id: demo\n  steps: []\n";

        ScenarioResult result = service.run(txFlowYaml);

        assertThat(result.getType()).isEqualTo("TxFlow");
    }

    @Test
    void treatsPlainTransactionYamlAsTxPlan() {
        when(backendProvider.getBackendService(anyString())).thenReturn(Optional.empty());
        String txPlanYaml = "version: 1.0\ntransaction:\n  - tx:\n      from_ref: account://acc0\n";

        ScenarioResult result = service.run(txPlanYaml);

        assertThat(result.getType()).isEqualTo("TxPlan");
    }

    @Test
    void routesTopLevelScenarioToL3Runner() {
        String l3 = "scenario:\n  name: demo\n  steps:\n    - log: hello\n";
        when(scenarioRunner.run(org.mockito.ArgumentMatchers.eq(l3), org.mockito.ArgumentMatchers.any()))
                .thenReturn(ScenarioResult.builder().type("Scenario").success(true).status("completed").build());

        ScenarioResult result = service.run(l3);

        assertThat(result.getType()).isEqualTo("Scenario");
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsUnknownTopLevelDocument() {
        ScenarioResult result = service.run("version: 1\nfoo: bar\n");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getType()).isEqualTo("Unknown");
        assertThat(result.getMessage()).contains("scenario");
    }
}
