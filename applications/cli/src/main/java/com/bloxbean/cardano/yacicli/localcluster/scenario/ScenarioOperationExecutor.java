package com.bloxbean.cardano.yacicli.localcluster.scenario;

import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.serialization.TxPlan;
import com.bloxbean.cardano.client.quicktx.signing.SignerRegistry;
import com.bloxbean.cardano.client.txflow.TxFlow;
import com.bloxbean.cardano.client.txflow.exec.ConfirmationConfig;
import com.bloxbean.cardano.client.txflow.exec.FlowExecutor;
import com.bloxbean.cardano.client.txflow.result.FlowResult;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.service.LocalBackendServiceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScenarioOperationExecutor {
    static final String TXPLAN = "TxPlan";
    static final String TXFLOW = "TxFlow";
    private static final String DEFAULT_CLUSTER_NAME = "default";

    private final LocalBackendServiceProvider backendServiceProvider;
    private final ScenarioSignerRegistryFactory signerRegistryFactory;

    public ScenarioResult runTxPlan(String yaml, Consumer<String> writer) {
        return run(yaml, TXPLAN, writer);
    }

    public ScenarioResult runTxFlow(String yaml, Consumer<String> writer) {
        return run(yaml, TXFLOW, writer);
    }

    public ScenarioValidationResult validateTxPlan(String yaml) {
        try {
            TxPlan.from(yaml);
            return ScenarioValidationResult.ok(TXPLAN);
        } catch (Exception e) {
            return ScenarioValidationResult.failed(TXPLAN, List.of("Invalid TxPlan YAML: " + e.getMessage()));
        }
    }

    public ScenarioValidationResult validateTxFlow(String yaml) {
        try {
            TxFlow.fromYaml(yaml);
            return ScenarioValidationResult.ok(TXFLOW);
        } catch (Exception e) {
            return ScenarioValidationResult.failed(TXFLOW, List.of("Invalid TxFlow YAML: " + e.getMessage()));
        }
    }

    private ScenarioResult run(String yaml, String type, Consumer<String> writer) {
        if (yaml == null || yaml.isBlank()) {
            return ScenarioResult.failed(type, "Scenario YAML is empty");
        }

        String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
        if (clusterName == null || clusterName.isBlank()) {
            clusterName = DEFAULT_CLUSTER_NAME;
        }
        Optional<BackendService> backend = backendServiceProvider.getBackendService(clusterName);
        if (backend.isEmpty()) {
            return ScenarioResult.failed(type,
                    "Devnet backend not available. Make sure a devnet is running with the indexer enabled.");
        }

        SignerRegistry registry = signerRegistryFactory.create();
        try {
            return TXFLOW.equals(type)
                    ? runFlow(yaml, backend.get(), registry)
                    : runPlan(yaml, backend.get(), registry, writer);
        } catch (Exception e) {
            log.error("Scenario operation execution error", e);
            return ScenarioResult.failed(type, "Scenario execution error: " + e.getMessage());
        }
    }

    private ScenarioResult runPlan(String yaml, BackendService backend, SignerRegistry registry, Consumer<String> writer) {
        TxPlan plan = TxPlan.from(yaml);
        Result<String> result = new QuickTxBuilder(backend)
                .compose(plan, registry)
                .completeAndWait(writer);

        return result.isSuccessful()
                ? ScenarioResult.ok(TXPLAN, List.of(result.getValue()))
                : ScenarioResult.failed(TXPLAN, result.getResponse());
    }

    private ScenarioResult runFlow(String yaml, BackendService backend, SignerRegistry registry) {
        TxFlow flow = TxFlow.fromYaml(yaml);
        FlowExecutor executor = FlowExecutor.create(backend).withSignerRegistry(registry);
        if (flow.getExecutionSettings() == null || flow.getExecutionSettings().getConfirmationConfig() == null) {
            executor.withConfirmationConfig(ConfirmationConfig.devnet());
        }
        try (executor) {
            FlowResult result = executor.executeSync(flow);
            if (result.isSuccessful()) {
                return ScenarioResult.ok(TXFLOW, result.getTransactionHashes());
            }
            String reason = result.getFailedStep()
                    .map(step -> "Failed step: " + step)
                    .orElse("TxFlow execution failed");
            return ScenarioResult.failed(TXFLOW, reason);
        }
    }
}
