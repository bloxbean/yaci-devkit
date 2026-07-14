package com.bloxbean.cardano.yacicli.localcluster.scenario;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Declarative scenario engine. Direct TxPlan/TxFlow documents are delegated to CCL; top-level
 * {@code scenario:} documents are interpreted by DevKit's L3 runner.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ScenarioService {
    static final String SCENARIO = "Scenario";
    static final String TXPLAN = ScenarioOperationExecutor.TXPLAN;
    static final String TXFLOW = ScenarioOperationExecutor.TXFLOW;
    private static final int MAX_ASYNC_RUNS = 100;
    private static final long ASYNC_RUN_TTL_MS = 60 * 60 * 1000L;

    private final ScenarioOperationExecutor operationExecutor;
    private final ScenarioRunner scenarioRunner;
    private final ScenarioControlService controlService;

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(4);
    private final Map<String, ScenarioResult> asyncRuns = new ConcurrentHashMap<>();
    private final Map<String, Long> asyncRunUpdatedAt = new ConcurrentHashMap<>();

    public ScenarioResult run(String yaml) {
        return run(yaml, log::info);
    }

    public ScenarioResult run(String yaml, Consumer<String> writer) {
        if (yaml == null || yaml.isBlank()) {
            return ScenarioResult.failed(TXPLAN, "Scenario YAML is empty");
        }

        try {
            return switch (detectType(yaml)) {
                case SCENARIO -> scenarioRunner.run(yaml, writer);
                case TXFLOW -> operationExecutor.runTxFlow(yaml, writer);
                case TXPLAN -> operationExecutor.runTxPlan(yaml, writer);
                default -> ScenarioResult.failed("Unknown", "Unknown YAML document. Expected top-level scenario:, flow:, or transaction:.");
            };
        } catch (Exception e) {
            log.error("Scenario execution error", e);
            return ScenarioResult.failed("Scenario", "Scenario execution error: " + e.getMessage());
        }
    }

    public ScenarioResult runAsync(String yaml, Consumer<String> writer) {
        evictAsyncRuns();
        String runId = UUID.randomUUID().toString();
        ScenarioResult running = ScenarioResult.running(detectTypeQuietly(yaml), runId);
        asyncRuns.put(runId, running);
        asyncRunUpdatedAt.put(runId, System.currentTimeMillis());
        asyncExecutor.submit(() -> {
            ScenarioResult result = run(yaml, writer);
            result.setRunId(runId);
            asyncRuns.put(runId, result);
            asyncRunUpdatedAt.put(runId, System.currentTimeMillis());
        });
        return running;
    }

    public ScenarioResult status(String runId) {
        evictAsyncRuns();
        ScenarioResult result = asyncRuns.get(runId);
        return result != null ? result : ScenarioResult.failed("Scenario", "Unknown runId: " + runId);
    }

    @PreDestroy
    void shutdown() {
        asyncExecutor.shutdownNow();
    }

    private void evictAsyncRuns() {
        long now = System.currentTimeMillis();
        asyncRunUpdatedAt.forEach((runId, updatedAt) -> {
            ScenarioResult result = asyncRuns.get(runId);
            if (result == null || (!"running".equals(result.getStatus()) && now - updatedAt > ASYNC_RUN_TTL_MS)) {
                asyncRuns.remove(runId);
                asyncRunUpdatedAt.remove(runId);
            }
        });

        int excess = asyncRuns.size() - MAX_ASYNC_RUNS;
        if (excess <= 0) {
            return;
        }
        asyncRunUpdatedAt.entrySet().stream()
                .filter(entry -> {
                    ScenarioResult result = asyncRuns.get(entry.getKey());
                    return result == null || !"running".equals(result.getStatus());
                })
                .sorted(Comparator.comparingLong(Map.Entry::getValue))
                .limit(excess)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(runId -> {
                    asyncRuns.remove(runId);
                    asyncRunUpdatedAt.remove(runId);
                });
    }

    public ScenarioValidationResult validate(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            return ScenarioValidationResult.failed("Unknown", List.of("Scenario YAML is empty"));
        }

        String type = detectTypeQuietly(yaml);
        return switch (type) {
            case SCENARIO -> scenarioRunner.validate(yaml);
            case TXFLOW -> operationExecutor.validateTxFlow(yaml);
            case TXPLAN -> operationExecutor.validateTxPlan(yaml);
            default -> ScenarioValidationResult.failed("Unknown", List.of("Unknown YAML document. Expected top-level scenario:, flow:, or transaction:."));
        };
    }

    public String schema() {
        return """
                {
                  "type": "object",
                  "oneOf": [
                    { "required": ["scenario"] },
                    { "required": ["flow"] },
                    { "required": ["transaction"] }
                  ],
                  "properties": {
                    "scenario": {
                      "type": "object",
                      "required": ["steps"],
                      "properties": {
                        "name": { "type": "string" },
                        "description": { "type": "string" },
                        "variables": { "type": "object" },
                        "steps": {
                          "type": "array",
                          "items": { "$ref": "#/$defs/step" }
                        }
                      }
                    }
                  },
                  "$defs": {
                    "step": {
                      "type": "object",
                      "description": "Exactly one action key plus optional id/name/description/expect_failure/continue_on_failure.",
                      "properties": {
                        "id": { "type": "string" },
                        "name": { "type": "string" },
                        "description": { "type": "string" },
                        "expect_failure": { "type": "boolean" },
                        "continue_on_failure": { "type": "boolean" },
                        "tx": { "description": "CCL TxPlan: string file path, {file,with}, {inline}, or inline CCL document." },
                        "flow": { "description": "CCL TxFlow: string file path, {file,with}, {inline}, or inline CCL document." },
                        "advance": { "description": "{blocks|slots|seconds|epochs:N} or {until:{block|slot|epoch:N}}" },
                        "wait": { "description": "Alias for advance." },
                        "assert": { "description": "Predicates: {tx:{ref,status,error_contains}}, {account|address, min_ada|ada|min_lovelace|lovelace|utxo_count|has_asset}, {epoch}, {protocol_param}, {asset_supply}, or {datum_at}. ADA fields are ADA; lovelace fields are raw lovelace." },
                        "topup": { "description": "{account|address, ada}; ada is denominated in ADA." },
                        "snapshot": { "description": "{name}" },
                        "restore": { "description": "{snapshot|name}" },
                        "rollback": { "description": "{blocks}" },
                        "reset": { "description": "{}" },
                        "repeat": { "description": "{times, steps}" },
                        "for_each": { "description": "{items, as, steps}" },
                        "parallel": { "description": "{steps}" },
                        "group": { "description": "{name, steps}" },
                        "log": { "description": "string or object with message" }
                      }
                    },
                    "assetPredicate": {
                      "description": "Asset predicates accept unit, or policy plus name (UTF-8 asset name) / name_hex (already hex-encoded asset name)."
                    },
                    "ccl": {
                      "description": "TxPlan and TxFlow bodies are cardano-client-lib YAML documents. DevKit delegates them to TxPlan.from(...) and TxFlow.fromYaml(...); use validate to run those parsers over inline or referenced files."
                    }
                  }
                }
                """;
    }

    public List<Map<String, Object>> accounts() {
        return controlService.accounts();
    }

    public Map<String, Object> stateSummary() {
        return controlService.stateSummary();
    }

    public String examples() {
        return """
                scenario:
                  name: payment-with-assert
                  steps:
                    - tx: examples/scenarios/payment.yaml
                      id: pay
                    - assert:
                        tx: { ref: pay, status: success }
                    - advance: { blocks: 1 }
                ---
                scenario:
                  name: repeat-topup
                  variables: { amount: 10 }
                  steps:
                    - for_each:
                        items: [account://acc1, account://acc2]
                        as: receiver
                        steps:
                          - topup: { account: "${receiver}", ada: "${amount}" }
                    - assert: { account: account://acc1, min_ada: 10 }
                """;
    }

    private String detectTypeQuietly(String yaml) {
        try {
            return detectType(yaml);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private String detectType(String yaml) throws Exception {
        JsonNode root = yamlMapper.readTree(yaml);
        if (root == null || !root.isObject()) {
            return "Unknown";
        }
        if (root.has("scenario")) {
            return SCENARIO;
        }
        if (root.has("flow")) {
            return TXFLOW;
        }
        if (root.has("transaction")) {
            return TXPLAN;
        }
        return "Unknown";
    }
}
