package com.bloxbean.cardano.yacicli.localcluster.scenario;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScenarioRunner {
    private static final Set<String> COMMON_KEYS = Set.of("id", "name", "description", "expect_failure", "continue_on_failure");
    private static final Set<String> ACTION_KEYS = Set.of(
            "tx", "flow", "advance", "wait", "assert", "topup", "snapshot", "restore", "rollback", "reset",
            "repeat", "for_each", "parallel", "group", "log"
    );
    private static final Pattern EXPR = Pattern.compile("\\$\\{([^}]+)}");

    private final ScenarioOperationExecutor operationExecutor;
    private final ScenarioControlService controlService;

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService parallelExecutor = Executors.newFixedThreadPool(
            Math.max(2, Runtime.getRuntime().availableProcessors()));

    public ScenarioResult run(String yaml, Consumer<String> writer) {
        long startedAt = System.currentTimeMillis();
        List<ScenarioStepResult> results = Collections.synchronizedList(new ArrayList<>());
        try {
            controlService.ensureContext();
            JsonNode scenario = scenarioNode(yaml);
            ScenarioContext ctx = new ScenarioContext();
            ctx.variables.putAll(toMap(scenario.path("variables")));

            JsonNode steps = scenario.path("steps");
            if (!steps.isArray()) {
                return ScenarioResult.failed("Scenario", "scenario.steps must be an array");
            }

            boolean success = executeSteps(steps, ctx, results, writer);
            List<String> txHashes = results.stream()
                    .filter(step -> step.getTxHashes() != null)
                    .flatMap(step -> step.getTxHashes().stream())
                    .toList();

            return ScenarioResult.builder()
                    .type("Scenario")
                    .status(success ? "completed" : "failed")
                    .success(success)
                    .scenarioName(text(scenario.get("name")))
                    .txHashes(txHashes)
                    .steps(new ArrayList<>(results))
                    .finalState(safeFinalState())
                    .message(success ? "Scenario executed successfully" : "Scenario failed")
                    .build();
        } catch (Exception e) {
            log.error("L3 scenario execution failed", e);
            ScenarioStepResult failure = ScenarioStepResult.builder()
                    .index(results.size() + 1)
                    .action("scenario")
                    .status("failed")
                    .message(e.getMessage())
                    .startedAt(startedAt)
                    .completedAt(System.currentTimeMillis())
                    .build();
            results.add(failure);
            return ScenarioResult.builder()
                    .type("Scenario")
                    .status("failed")
                    .success(false)
                    .steps(new ArrayList<>(results))
                    .message("Scenario execution error: " + e.getMessage())
                    .build();
        }
    }

    public ScenarioValidationResult validate(String yaml) {
        List<String> errors = new ArrayList<>();
        try {
            JsonNode scenario = scenarioNode(yaml);
            ScenarioContext ctx = new ScenarioContext();
            ctx.variables.putAll(toMap(scenario.path("variables")));
            JsonNode steps = scenario.path("steps");
            if (!steps.isArray()) {
                errors.add("scenario.steps must be an array");
            } else {
                validateSteps(steps, "scenario.steps", errors, ctx);
            }
        } catch (Exception e) {
            errors.add(e.getMessage());
        }
        return errors.isEmpty()
                ? ScenarioValidationResult.ok("Scenario")
                : ScenarioValidationResult.failed("Scenario", errors);
    }

    @PreDestroy
    void shutdown() {
        parallelExecutor.shutdownNow();
    }

    private Map<String, Object> safeFinalState() {
        try {
            return controlService.stateSummary();
        } catch (Exception e) {
            log.debug("Could not collect scenario final state", e);
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("final_state_available", false);
            state.put("error", e.getMessage());
            return state;
        }
    }

    private boolean executeSteps(JsonNode steps, ScenarioContext ctx, List<ScenarioStepResult> results, Consumer<String> writer) {
        boolean success = true;
        for (JsonNode step : steps) {
            boolean stepSuccess = executeStep(step, ctx, results, writer);
            if (!stepSuccess) {
                success = false;
                if (!bool(step.get("continue_on_failure"))) {
                    break;
                }
            }
        }
        return success;
    }

    private boolean executeStep(JsonNode step, ScenarioContext ctx, List<ScenarioStepResult> results, Consumer<String> writer) {
        long startedAt = System.currentTimeMillis();
        String action = actionKey(step);
        String id = resolvedText(step.get("id"), ctx);
        String name = resolvedText(step.get("name"), ctx);

        ScenarioStepResult.ScenarioStepResultBuilder builder = ScenarioStepResult.builder()
                .index(results.size() + 1)
                .id(id)
                .name(name)
                .action(action)
                .startedAt(startedAt);

        try {
            ScenarioStepResult result = switch (action) {
                case "tx", "flow" -> operationStep(action, step.get(action), step, ctx, builder, writer);
                case "advance", "wait" -> advanceStep(step.get(action), ctx, builder, writer);
                case "assert" -> assertStep(step.get(action), ctx, builder, writer);
                case "topup" -> topupStep(step.get(action), ctx, builder, writer);
                case "snapshot" -> snapshotStep(step.get(action), ctx, builder, writer);
                case "restore" -> restoreStep(step.get(action), ctx, builder, writer);
                case "rollback" -> rollbackStep(step.get(action), ctx, builder, writer);
                case "reset" -> resetStep(builder, writer);
                case "repeat" -> repeatStep(step.get(action), ctx, results, builder, writer);
                case "for_each" -> forEachStep(step.get(action), ctx, results, builder, writer);
                case "parallel" -> parallelStep(step.get(action), ctx, results, builder, writer);
                case "group" -> groupStep(step.get(action), ctx, results, builder, writer);
                case "log" -> logStep(step.get(action), ctx, builder, writer);
                default -> throw new IllegalArgumentException("Unsupported scenario action: " + action);
            };

            if (bool(step.get("expect_failure")) && !"tx".equals(action) && !"flow".equals(action)) {
                result = applyExpectedFailure(result);
            }
            result.setCompletedAt(System.currentTimeMillis());
            if (id != null && !id.isBlank()) {
                ctx.steps.put(id, stepOutput(result));
            }
            results.add(result);
            return isContinuableSuccess(result);
        } catch (Exception e) {
            ScenarioStepResult result = builder
                    .status("failed")
                    .message(e.getMessage())
                    .completedAt(System.currentTimeMillis())
                    .outputs(Map.of("status", "failed", "error", e.getMessage()))
                    .build();
            if (id != null && !id.isBlank()) {
                ctx.steps.put(id, stepOutput(result));
            }
            results.add(result);
            return false;
        }
    }

    private ScenarioStepResult operationStep(String action, JsonNode payload, JsonNode step, ScenarioContext ctx,
                                             ScenarioStepResult.ScenarioStepResultBuilder builder,
                                             Consumer<String> writer) throws Exception {
        boolean expectFailure = bool(step.get("expect_failure"));
        String operationYaml = operationYaml(payload, ctx);
        ScenarioResult result = "flow".equals(action)
                ? operationExecutor.runTxFlow(operationYaml, writer)
                : operationExecutor.runTxPlan(operationYaml, writer);

        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("status", result.isSuccess() ? "success" : "failed");
        outputs.put("message", result.getMessage());
        if (!result.isSuccess()) {
            outputs.put("error", result.getMessage());
        }
        outputs.put("tx_hashes", result.getTxHashes());
        if (result.getTxHashes() != null && !result.getTxHashes().isEmpty()) {
            outputs.put("tx_hash", result.getTxHashes().get(0));
        }

        if (expectFailure) {
            if (result.isSuccess()) {
                outputs.put("status", "failed");
                outputs.put("error", "Expected operation failure, but it succeeded");
                return builder.status("failed")
                        .message("Expected operation failure, but it succeeded")
                        .txHashes(result.getTxHashes())
                        .outputs(outputs)
                        .build();
            }
            outputs.put("status", "expected_failure");
            outputs.putIfAbsent("error", result.getMessage());
            return builder.status("expected_failure")
                    .message(result.getMessage())
                    .txHashes(result.getTxHashes())
                    .outputs(outputs)
                    .build();
        }

        return builder.status(result.isSuccess() ? "success" : "failed")
                .message(result.getMessage())
                .txHashes(result.getTxHashes())
                .outputs(outputs)
                .build();
    }

    private ScenarioStepResult advanceStep(JsonNode payload, ScenarioContext ctx,
                                           ScenarioStepResult.ScenarioStepResultBuilder builder,
                                           Consumer<String> writer) {
        Map<String, Object> request = toResolvedMap(payload, ctx);
        boolean ok = controlService.advance(request, writer);
        return simple(builder, ok, ok ? "Advance completed" : "Advance failed", Map.of("request", request));
    }

    private ScenarioStepResult topupStep(JsonNode payload, ScenarioContext ctx,
                                         ScenarioStepResult.ScenarioStepResultBuilder builder,
                                         Consumer<String> writer) {
        Map<String, Object> request = toResolvedMap(payload, ctx);
        String accountOrAddress = stringValue(request.getOrDefault("account", request.get("address")));
        double ada = toBigDecimal(request.get("ada")).doubleValue();
        boolean ok = controlService.topup(accountOrAddress, ada, writer);
        Map<String, Object> outputs = new LinkedHashMap<>();
        outputs.put("account_or_address", accountOrAddress);
        outputs.put("address", controlService.resolveAddress(accountOrAddress));
        outputs.put("ada", ada);
        return simple(builder, ok, ok ? "Topup completed" : "Topup failed", outputs);
    }

    private ScenarioStepResult snapshotStep(JsonNode payload, ScenarioContext ctx,
                                            ScenarioStepResult.ScenarioStepResultBuilder builder,
                                            Consumer<String> writer) {
        Map<String, Object> request = toResolvedMap(payload, ctx);
        String name = stringValue(request.getOrDefault("name", "scenario-snapshot"));
        boolean ok = controlService.snapshot(name, writer);
        return simple(builder, ok, ok ? "Snapshot created" : "Snapshot failed", Map.of("name", name));
    }

    private ScenarioStepResult restoreStep(JsonNode payload, ScenarioContext ctx,
                                           ScenarioStepResult.ScenarioStepResultBuilder builder,
                                           Consumer<String> writer) {
        Map<String, Object> request = toResolvedMap(payload, ctx);
        String name = stringValue(request.getOrDefault("snapshot", request.getOrDefault("name", "scenario-snapshot")));
        boolean ok = controlService.restore(name, writer);
        return simple(builder, ok, ok ? "Snapshot restored" : "Restore failed", Map.of("name", name));
    }

    private ScenarioStepResult rollbackStep(JsonNode payload, ScenarioContext ctx,
                                            ScenarioStepResult.ScenarioStepResultBuilder builder,
                                            Consumer<String> writer) {
        Map<String, Object> request = toResolvedMap(payload, ctx);
        long blocks = toBigDecimal(request.getOrDefault("blocks", request.getOrDefault("count", 0))).longValue();
        boolean ok = controlService.rollback(blocks, writer);
        return simple(builder, ok, ok ? "Rollback completed" : "Rollback failed", Map.of("blocks", blocks));
    }

    private ScenarioStepResult applyExpectedFailure(ScenarioStepResult result) {
        Map<String, Object> outputs = new LinkedHashMap<>();
        if (result.getOutputs() != null) {
            outputs.putAll(result.getOutputs());
        }

        if (isContinuableSuccess(result)) {
            String message = "Expected step failure, but it succeeded";
            outputs.put("status", "failed");
            outputs.put("error", message);
            result.setStatus("failed");
            result.setMessage(message);
        } else {
            outputs.put("status", "expected_failure");
            outputs.putIfAbsent("error", result.getMessage());
            result.setStatus("expected_failure");
        }
        result.setOutputs(outputs);
        return result;
    }

    private ScenarioStepResult resetStep(ScenarioStepResult.ScenarioStepResultBuilder builder, Consumer<String> writer) {
        boolean ok = controlService.reset(writer);
        return simple(builder, ok, ok ? "Reset completed" : "Reset failed", Map.of());
    }

    private ScenarioStepResult repeatStep(JsonNode payload, ScenarioContext ctx, List<ScenarioStepResult> results,
                                          ScenarioStepResult.ScenarioStepResultBuilder builder,
                                          Consumer<String> writer) {
        Map<String, Object> request = toResolvedMap(payload, ctx);
        int times = toBigDecimal(request.getOrDefault("times", 0)).intValue();
        JsonNode nested = payload.path("steps");
        boolean ok = true;
        Object previous = ctx.variables.get("index");
        for (int i = 0; i < times; i++) {
            ctx.variables.put("index", i);
            ok = executeSteps(nested, ctx, results, writer) && ok;
        }
        if (previous != null) {
            ctx.variables.put("index", previous);
        } else {
            ctx.variables.remove("index");
        }
        return simple(builder, ok, ok ? "Repeat completed" : "Repeat failed", Map.of("times", times));
    }

    private ScenarioStepResult forEachStep(JsonNode payload, ScenarioContext ctx, List<ScenarioStepResult> results,
                                           ScenarioStepResult.ScenarioStepResultBuilder builder,
                                           Consumer<String> writer) {
        Object resolvedItems = resolveValue(toJava(payload.path("items")), ctx);
        List<?> items = resolvedItems instanceof List<?> list ? list : List.of();
        String as = resolvedText(payload.path("as"), ctx);
        if (as == null || as.isBlank()) {
            as = "item";
        }
        JsonNode nested = payload.path("steps");
        boolean ok = true;
        Object previous = ctx.variables.get(as);
        for (Object item : items) {
            ctx.variables.put(as, item);
            ok = executeSteps(nested, ctx, results, writer) && ok;
        }
        if (previous != null) {
            ctx.variables.put(as, previous);
        } else {
            ctx.variables.remove(as);
        }
        return simple(builder, ok, ok ? "for_each completed" : "for_each failed", Map.of("count", items.size(), "as", as));
    }

    private ScenarioStepResult parallelStep(JsonNode payload, ScenarioContext ctx, List<ScenarioStepResult> results,
                                            ScenarioStepResult.ScenarioStepResultBuilder builder,
                                            Consumer<String> writer) throws Exception {
        JsonNode nested = payload.path("steps");
        if (!nested.isArray()) {
            return simple(builder, false, "parallel.steps must be an array", Map.of());
        }
        List<Callable<List<ScenarioStepResult>>> tasks = new ArrayList<>();
        for (JsonNode child : nested) {
            tasks.add(() -> {
                List<ScenarioStepResult> local = new ArrayList<>();
                executeStep(child, ctx.copyVariables(), local, writer);
                return local;
            });
        }
        boolean ok = true;
        List<Future<List<ScenarioStepResult>>> futures = parallelExecutor.invokeAll(tasks);
        for (Future<List<ScenarioStepResult>> future : futures) {
            List<ScenarioStepResult> childResults = future.get();
            for (ScenarioStepResult result : childResults) {
                ok = isContinuableSuccess(result) && ok;
                result.setIndex(results.size() + 1);
                results.add(result);
            }
        }
        return simple(builder, ok, ok ? "Parallel completed" : "Parallel failed", Map.of("branches", tasks.size()));
    }

    private ScenarioStepResult groupStep(JsonNode payload, ScenarioContext ctx, List<ScenarioStepResult> results,
                                         ScenarioStepResult.ScenarioStepResultBuilder builder,
                                         Consumer<String> writer) {
        JsonNode nested = payload.path("steps");
        boolean ok = executeSteps(nested, ctx, results, writer);
        String groupName = resolvedText(payload.path("name"), ctx);
        return simple(builder, ok, ok ? "Group completed" : "Group failed", Map.of("name", groupName));
    }

    private ScenarioStepResult logStep(JsonNode payload, ScenarioContext ctx,
                                       ScenarioStepResult.ScenarioStepResultBuilder builder,
                                       Consumer<String> writer) {
        String message;
        if (payload != null && payload.isObject() && payload.has("message")) {
            message = resolvedText(payload.get("message"), ctx);
        } else {
            message = stringValue(resolveValue(toJava(payload), ctx));
        }
        writer.accept(message);
        return simple(builder, true, message, Map.of("message", message));
    }

    private ScenarioStepResult assertStep(JsonNode payload, ScenarioContext ctx,
                                          ScenarioStepResult.ScenarioStepResultBuilder builder,
                                          Consumer<String> writer) {
        Map<String, Object> assertion = toResolvedMap(payload, ctx);
        try {
            assertPredicate(assertion, ctx, writer);
            return simple(builder, true, "Assertion passed", Map.of("assertion", assertion));
        } catch (AssertionError e) {
            return simple(builder, false, e.getMessage(), Map.of("assertion", assertion));
        }
    }

    private void assertPredicate(Map<String, Object> assertion, ScenarioContext ctx, Consumer<String> writer) {
        if (assertion.containsKey("tx")) {
            assertTx(toStringObjectMap(assertion.get("tx")), ctx);
            return;
        }
        if (assertion.containsKey("account") || assertion.containsKey("address")) {
            assertBalance(assertion, writer);
            return;
        }
        if (assertion.containsKey("epoch")) {
            int expected = toBigDecimal(assertion.get("epoch")).intValue();
            int actual = controlService.latestEpoch();
            if (actual != expected) {
                throw new AssertionError("Expected epoch " + expected + " but got " + actual);
            }
            return;
        }
        if (assertion.containsKey("protocol_param")) {
            assertProtocolParams(toStringObjectMap(assertion.get("protocol_param")));
            return;
        }
        if (assertion.containsKey("asset_supply")) {
            assertAssetSupply(toStringObjectMap(assertion.get("asset_supply")));
            return;
        }
        if (assertion.containsKey("datum_at")) {
            assertDatumAt(toStringObjectMap(assertion.get("datum_at")), writer);
            return;
        }
        throw new AssertionError("Unsupported assertion predicate: " + assertion.keySet());
    }

    private void assertTx(Map<String, Object> txAssert, ScenarioContext ctx) {
        String ref = stringValue(txAssert.get("ref"));
        Map<String, Object> step = ctx.steps.get(ref);
        if (step == null) {
            throw new AssertionError("Unknown tx step ref: " + ref);
        }
        String expected = stringValue(txAssert.getOrDefault("status", "success"));
        String actual = stringValue(step.get("status"));
        if (!statusMatches(expected, actual)) {
            throw new AssertionError("Expected tx step " + ref + " status " + expected + " but got " + actual);
        }
        if (txAssert.containsKey("error_contains")) {
            String needle = stringValue(txAssert.get("error_contains"));
            String error = stringValue(step.get("error"));
            if (error == null || !error.contains(needle)) {
                throw new AssertionError("Expected tx step " + ref + " error to contain: " + needle);
            }
        }
    }

    private void assertBalance(Map<String, Object> assertion, Consumer<String> writer) {
        String target = stringValue(assertion.getOrDefault("account", assertion.get("address")));
        List<Utxo> utxos = controlService.utxos(target, writer);
        BigInteger lovelace = BigInteger.ZERO;
        for (Utxo utxo : utxos) {
            for (Amount amount : utxo.getAmount()) {
                if (LOVELACE.equals(amount.getUnit())) {
                    lovelace = lovelace.add(amount.getQuantity());
                }
            }
        }
        if (assertion.containsKey("min_ada")) {
            BigInteger expected = adaToLovelace(toBigDecimal(assertion.get("min_ada")));
            if (lovelace.compareTo(expected) < 0) {
                throw new AssertionError("Expected at least " + expected + " lovelace at " + target + " but got " + lovelace);
            }
        }
        if (assertion.containsKey("min_lovelace")) {
            BigInteger expected = toBigDecimal(assertion.get("min_lovelace")).toBigInteger();
            if (lovelace.compareTo(expected) < 0) {
                throw new AssertionError("Expected at least " + expected + " lovelace at " + target + " but got " + lovelace);
            }
        }
        if (assertion.containsKey("ada")) {
            BigInteger expected = adaToLovelace(toBigDecimal(assertion.get("ada")));
            if (!lovelace.equals(expected)) {
                throw new AssertionError("Expected " + expected + " lovelace at " + target + " but got " + lovelace);
            }
        }
        if (assertion.containsKey("lovelace")) {
            BigInteger expected = toBigDecimal(assertion.get("lovelace")).toBigInteger();
            if (!lovelace.equals(expected)) {
                throw new AssertionError("Expected " + expected + " lovelace at " + target + " but got " + lovelace);
            }
        }
        if (assertion.containsKey("utxo_count")) {
            int expected = toBigDecimal(assertion.get("utxo_count")).intValue();
            if (utxos.size() != expected) {
                throw new AssertionError("Expected " + expected + " UTxOs at " + target + " but got " + utxos.size());
            }
        }
        if (assertion.containsKey("has_asset")) {
            assertHasAsset(target, utxos, toStringObjectMap(assertion.get("has_asset")));
        }
    }

    private void assertHasAsset(String target, List<Utxo> utxos, Map<String, Object> spec) {
        String unit = stringValue(spec.get("unit"));
        String policy = stringValue(spec.get("policy"));
        String name = stringValue(spec.get("name"));
        String nameHex = stringValue(spec.get("name_hex"));
        BigInteger min = spec.containsKey("min") ? toBigDecimal(spec.get("min")).toBigInteger() : BigInteger.ONE;
        BigInteger total = BigInteger.ZERO;
        for (Utxo utxo : utxos) {
            for (Amount amount : utxo.getAmount()) {
                String amountUnit = amount.getUnit();
                boolean matches = unit != null
                        ? unit.equals(amountUnit)
                        : assetUnitMatches(amountUnit, policy, name, nameHex);
                if (matches) {
                    total = total.add(amount.getQuantity());
                }
            }
        }
        if (total.compareTo(min) < 0) {
            throw new AssertionError("Expected asset at " + target + " with minimum " + min + " but got " + total);
        }
    }

    private void assertProtocolParams(Map<String, Object> expected) {
        Map<String, Object> actual = controlService.protocolParamsAsMap();
        Map<String, Object> normalized = new LinkedHashMap<>();
        actual.forEach((k, v) -> normalized.put(normalize(k), v));
        expected.forEach((k, v) -> {
            Object actualValue = normalized.get(normalize(k));
            if (actualValue == null) {
                throw new AssertionError("Protocol parameter not found: " + k);
            }
            if (!valueEquals(v, actualValue)) {
                throw new AssertionError("Protocol parameter " + k + " expected " + v + " but got " + actualValue);
            }
        });
    }

    private void assertAssetSupply(Map<String, Object> spec) {
        String unit = stringValue(spec.get("unit"));
        if (unit == null) {
            String policy = stringValue(spec.get("policy"));
            String name = stringValue(spec.get("name"));
            String nameHex = stringValue(spec.get("name_hex"));
            if (policy == null) {
                throw new AssertionError("asset_supply requires unit or policy");
            }
            unit = policy + assetNameSuffix(name, nameHex);
        }
        BigInteger actual = controlService.assetSupply(unit);
        if (spec.containsKey("min")) {
            BigInteger min = toBigDecimal(spec.get("min")).toBigInteger();
            if (actual.compareTo(min) < 0) {
                throw new AssertionError("Expected asset supply " + unit + " >= " + min + " but got " + actual);
            }
        }
        if (spec.containsKey("quantity")) {
            BigInteger expected = toBigDecimal(spec.get("quantity")).toBigInteger();
            if (!actual.equals(expected)) {
                throw new AssertionError("Expected asset supply " + unit + " = " + expected + " but got " + actual);
            }
        }
    }

    private boolean assetUnitMatches(String amountUnit, String policy, String name, String nameHex) {
        if (amountUnit == null || policy == null || !amountUnit.startsWith(policy)) {
            return false;
        }
        if (name == null && nameHex == null) {
            return true;
        }
        for (String suffix : assetNameSuffixes(name, nameHex)) {
            if (amountUnit.equals(policy + suffix)) {
                return true;
            }
        }
        return false;
    }

    private String assetNameSuffix(String name, String nameHex) {
        if (nameHex != null) {
            return nameHex.toLowerCase(Locale.ROOT);
        }
        return name != null
                ? HexFormat.of().formatHex(name.getBytes(StandardCharsets.UTF_8))
                : "";
    }

    private List<String> assetNameSuffixes(String name, String nameHex) {
        if (nameHex != null) {
            return List.of(nameHex.toLowerCase(Locale.ROOT));
        }
        if (name == null) {
            return List.of("");
        }
        List<String> suffixes = new ArrayList<>();
        suffixes.add(HexFormat.of().formatHex(name.getBytes(StandardCharsets.UTF_8)));
        if (isHex(name)) {
            suffixes.add(name.toLowerCase(Locale.ROOT));
        }
        return suffixes;
    }

    private boolean isHex(String value) {
        return value != null && value.length() % 2 == 0 && value.matches("(?i)[0-9a-f]+");
    }

    private void assertDatumAt(Map<String, Object> spec, Consumer<String> writer) {
        String datumHash = stringValue(spec.getOrDefault("hash", spec.get("data_hash")));
        if (datumHash != null && !spec.containsKey("account") && !spec.containsKey("address")) {
            String json = String.valueOf(controlService.scriptDatum(datumHash));
            if (spec.containsKey("json_contains")) {
                String expected = stringValue(spec.get("json_contains"));
                if (!json.contains(expected)) {
                    throw new AssertionError("Expected datum " + datumHash + " JSON to contain " + expected);
                }
            }
            if (spec.containsKey("json_equals")) {
                String expected = stringValue(spec.get("json_equals"));
                if (!json.equals(expected)) {
                    throw new AssertionError("Expected datum " + datumHash + " JSON to equal " + expected + " but got " + json);
                }
            }
            return;
        }

        String target = stringValue(spec.getOrDefault("account", spec.get("address")));
        if (target == null) {
            throw new AssertionError("datum_at requires hash/data_hash or account/address");
        }
        List<Utxo> utxos = controlService.utxos(target, writer);
        boolean found = false;
        for (Utxo utxo : utxos) {
            boolean matches = true;
            if (datumHash != null) {
                matches = datumHash.equals(utxo.getDataHash());
            }
            if (matches && spec.containsKey("inline_contains")) {
                String inline = utxo.getInlineDatum();
                matches = inline != null && inline.contains(stringValue(spec.get("inline_contains")));
            }
            if (matches && spec.containsKey("inline_equals")) {
                matches = Objects.equals(utxo.getInlineDatum(), stringValue(spec.get("inline_equals")));
            }
            if (matches) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new AssertionError("Expected datum predicate at " + target + " but no matching UTxO was found");
        }
    }

    private String operationYaml(JsonNode payload, ScenarioContext ctx) throws Exception {
        Object javaPayload = toJava(payload);
        if (javaPayload instanceof String path) {
            return readOperationFile(stringValue(resolveValue(path, ctx)), ctx, Map.of());
        }
        if (javaPayload instanceof Map<?, ?> map) {
            if (map.containsKey("file")) {
                Map<String, Object> with = map.containsKey("with")
                        ? toStringObjectMap(resolveValue(map.get("with"), ctx))
                        : Map.of();
                return readOperationFile(stringValue(resolveValue(map.get("file"), ctx)), ctx, with);
            }
            if (map.containsKey("inline")) {
                Object inline = resolveValue(map.get("inline"), ctx);
                return yamlMapper.writeValueAsString(inline);
            }
        }
        Object inline = resolveValue(javaPayload, ctx);
        return yamlMapper.writeValueAsString(inline);
    }

    private String readOperationFile(String file, ScenarioContext ctx, Map<String, Object> with) throws Exception {
        Path base = Path.of("").toAbsolutePath().normalize();
        List<Path> roots = operationFileRoots(base);
        Path path = resolveOperationFile(file, roots);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Scenario operation file not found: " + file);
        }
        String content = Files.readString(path);
        if (with.isEmpty()) {
            return content;
        }
        ScenarioContext child = ctx.copyVariables();
        child.variables.putAll(with);
        Object parsed = yamlMapper.readValue(content, Object.class);
        Object resolved = resolveValue(parsed, child);
        return yamlMapper.writeValueAsString(resolved);
    }

    private List<Path> operationFileRoots(Path base) {
        List<Path> roots = new ArrayList<>();
        roots.add(base);
        findWorkspaceRoot(base).ifPresent(root -> {
            if (!roots.contains(root)) {
                roots.add(root);
            }
        });
        return roots;
    }

    private java.util.Optional<Path> findWorkspaceRoot(Path base) {
        Path current = base;
        while (current != null) {
            boolean hasBuildRoot = Files.exists(current.resolve("settings.gradle"))
                    || Files.exists(current.resolve("settings.gradle.kts"));
            boolean hasDevkitLayout = Files.isDirectory(current.resolve("applications").resolve("cli"));
            if (Files.isDirectory(current.resolve("examples")) && (hasBuildRoot || hasDevkitLayout)) {
                return java.util.Optional.of(current.normalize());
            }
            current = current.getParent();
        }
        return java.util.Optional.empty();
    }

    private Path resolveOperationFile(String file, List<Path> roots) {
        Path requested = Path.of(file);
        if (requested.isAbsolute()) {
            Path path = requested.normalize();
            if (!isUnderAnyRoot(path, roots)) {
                throw new IllegalArgumentException("Scenario file references must stay under " + roots);
            }
            return path;
        }

        for (Path root : roots) {
            Path path = root.resolve(requested).normalize();
            if (isUnderAnyRoot(path, roots) && Files.exists(path)) {
                return path;
            }
        }

        Path fallback = roots.get(0).resolve(requested).normalize();
        if (!isUnderAnyRoot(fallback, roots)) {
            throw new IllegalArgumentException("Scenario file references must stay under " + roots);
        }
        return fallback;
    }

    private boolean isUnderAnyRoot(Path path, List<Path> roots) {
        for (Path root : roots) {
            if (path.startsWith(root)) {
                return true;
            }
        }
        return false;
    }

    private void validateSteps(JsonNode steps, String path, List<String> errors, ScenarioContext ctx) {
        int i = 0;
        for (JsonNode step : steps) {
            String stepPath = path + "[" + i + "]";
            try {
                String action = actionKey(step);
                validateStepPayload(action, step.get(action), stepPath, errors, ctx);
            } catch (Exception e) {
                errors.add(stepPath + ": " + e.getMessage());
            }
            i++;
        }
    }

    private void validateStepPayload(String action, JsonNode payload, String path, List<String> errors, ScenarioContext ctx) {
        switch (action) {
            case "tx", "flow" -> validateOperationPayload(action, payload, path, errors, ctx);
            case "advance", "wait" -> validateAdvancePayload(payload, path, errors);
            case "assert" -> validateAssertPayload(payload, path, errors);
            case "topup" -> validateTopupPayload(payload, path, errors);
            case "snapshot" -> validateOptionalMap(payload, path, errors);
            case "restore" -> validateOptionalMap(payload, path, errors);
            case "rollback" -> validateRequiredMap(payload, path, errors, "blocks", "count");
            case "reset", "log" -> {
            }
            case "repeat" -> {
                validateRequiredMap(payload, path, errors, "times");
                JsonNode nested = payload.path("steps");
                if (!nested.isArray()) {
                    errors.add(path + ".repeat.steps must be an array");
                } else {
                    validateSteps(nested, path + ".repeat.steps", errors, ctx.copyVariables());
                }
            }
            case "for_each" -> {
                validateRequiredMap(payload, path, errors, "items");
                JsonNode nested = payload.path("steps");
                if (!nested.isArray()) {
                    errors.add(path + ".for_each.steps must be an array");
                } else {
                    validateSteps(nested, path + ".for_each.steps", errors, ctx.copyVariables());
                }
            }
            case "parallel", "group" -> {
                JsonNode nested = payload.path("steps");
                if (!nested.isArray()) {
                    errors.add(path + "." + action + ".steps must be an array");
                } else {
                    validateSteps(nested, path + "." + action + ".steps", errors, ctx.copyVariables());
                }
            }
            default -> errors.add(path + ": Unsupported scenario action: " + action);
        }
    }

    private void validateOperationPayload(String action, JsonNode payload, String path, List<String> errors, ScenarioContext ctx) {
        try {
            String operationYaml = operationYaml(payload, ctx);
            ScenarioValidationResult result = "flow".equals(action)
                    ? operationExecutor.validateTxFlow(operationYaml)
                    : operationExecutor.validateTxPlan(operationYaml);
            if (!result.isValid()) {
                result.getErrors().forEach(error -> errors.add(path + "." + action + ": " + error));
            }
        } catch (Exception e) {
            errors.add(path + "." + action + ": " + e.getMessage());
        }
    }

    private void validateAdvancePayload(JsonNode payload, String path, List<String> errors) {
        if (payload == null || !payload.isObject()) {
            errors.add(path + ": advance/wait must be a map");
            return;
        }
        boolean hasAmount = payload.has("blocks") || payload.has("slots") || payload.has("seconds")
                || payload.has("epochs") || payload.has("until");
        if (!hasAmount) {
            errors.add(path + ": advance/wait requires blocks, slots, seconds, epochs, or until");
        }
        if (payload.has("until") && !payload.path("until").isObject()) {
            errors.add(path + ".until must be a map");
        }
    }

    private void validateAssertPayload(JsonNode payload, String path, List<String> errors) {
        if (payload == null || !payload.isObject()) {
            errors.add(path + ": assert must be a map");
            return;
        }
        boolean supported = payload.has("tx") || payload.has("account") || payload.has("address")
                || payload.has("epoch") || payload.has("protocol_param") || payload.has("asset_supply")
                || payload.has("datum_at");
        if (!supported) {
            errors.add(path + ": unsupported assertion predicate: " + payload);
        }
    }

    private void validateTopupPayload(JsonNode payload, String path, List<String> errors) {
        if (payload == null || !payload.isObject()) {
            errors.add(path + ": topup must be a map");
            return;
        }
        if (!payload.has("account") && !payload.has("address")) {
            errors.add(path + ": topup requires account or address");
        }
        if (!payload.has("ada")) {
            errors.add(path + ": topup requires ada");
        }
    }

    private void validateRequiredMap(JsonNode payload, String path, List<String> errors, String... alternatives) {
        if (payload == null || !payload.isObject()) {
            errors.add(path + ": step payload must be a map");
            return;
        }
        boolean present = false;
        for (String key : alternatives) {
            present = present || payload.has(key);
        }
        if (!present) {
            errors.add(path + ": missing one of required keys: " + List.of(alternatives));
        }
    }

    private void validateOptionalMap(JsonNode payload, String path, List<String> errors) {
        if (payload != null && !payload.isMissingNode() && !payload.isObject()) {
            errors.add(path + ": step payload must be a map");
        }
    }

    private String actionKey(JsonNode step) {
        if (step == null || !step.isObject()) {
            throw new IllegalArgumentException("Scenario step must be a map");
        }
        Set<String> actions = new LinkedHashSet<>();
        step.fieldNames().forEachRemaining(key -> {
            if (!COMMON_KEYS.contains(key)) {
                actions.add(key);
            }
        });
        if (actions.size() != 1) {
            throw new IllegalArgumentException("Scenario step must contain exactly one action key. Found: " + actions);
        }
        String action = actions.iterator().next();
        if (!ACTION_KEYS.contains(action)) {
            throw new IllegalArgumentException("Unknown scenario action: " + action);
        }
        return action;
    }

    private JsonNode scenarioNode(String yaml) throws Exception {
        JsonNode root = yamlMapper.readTree(yaml);
        if (root == null || !root.has("scenario")) {
            throw new IllegalArgumentException("Expected top-level scenario:");
        }
        JsonNode scenario = root.get("scenario");
        if (!scenario.isObject()) {
            throw new IllegalArgumentException("scenario must be a map");
        }
        return scenario;
    }

    private Object resolveValue(Object value, ScenarioContext ctx) {
        if (value instanceof String s) {
            return resolveString(s, ctx);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> resolved = new LinkedHashMap<>();
            map.forEach((k, v) -> resolved.put(String.valueOf(k), resolveValue(v, ctx)));
            return resolved;
        }
        if (value instanceof List<?> list) {
            List<Object> resolved = new ArrayList<>();
            list.forEach(v -> resolved.add(resolveValue(v, ctx)));
            return resolved;
        }
        return value;
    }

    private Object resolveString(String value, ScenarioContext ctx) {
        Matcher exact = EXPR.matcher(value);
        if (exact.matches()) {
            return expressionValue(exact.group(1).trim(), ctx);
        }
        Matcher matcher = EXPR.matcher(value);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            Object replacement = expressionValue(matcher.group(1).trim(), ctx);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(replacement)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Object expressionValue(String expr, ScenarioContext ctx) {
        List<String> parts = pathTokens(expr);
        if (parts.isEmpty()) {
            return null;
        }
        if ("steps".equals(parts.get(0))) {
            if (parts.size() < 3) {
                return null;
            }
            Map<String, Object> step = ctx.steps.get(parts.get(1));
            Object current = step;
            return pathValue(current, parts, 2);
        }
        return pathValue(ctx.variables, parts, 0);
    }

    private Object pathValue(Map<String, Object> values, String path) {
        List<String> parts = pathTokens(path);
        return pathValue(values, parts, 0);
    }

    private Object pathValue(Object current, List<String> parts, int start) {
        for (int i = start; i < parts.size(); i++) {
            String part = parts.get(i);
            if (current instanceof Map<?, ?> map) {
                current = map.get(part);
            } else if (current instanceof List<?> list) {
                current = list.get(Integer.parseInt(part));
            } else {
                return null;
            }
        }
        return current;
    }

    private List<String> pathTokens(String path) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (ch == '.') {
                addPathToken(tokens, token);
            } else if (ch == '[') {
                addPathToken(tokens, token);
                int end = path.indexOf(']', i);
                if (end < 0) {
                    throw new IllegalArgumentException("Unclosed expression bracket in: " + path);
                }
                String bracket = path.substring(i + 1, end).trim();
                if ((bracket.startsWith("'") && bracket.endsWith("'"))
                        || (bracket.startsWith("\"") && bracket.endsWith("\""))) {
                    bracket = bracket.substring(1, bracket.length() - 1);
                }
                if (!bracket.isBlank()) {
                    tokens.add(bracket);
                }
                i = end;
            } else {
                token.append(ch);
            }
        }
        addPathToken(tokens, token);
        return tokens;
    }

    private void addPathToken(List<String> tokens, StringBuilder token) {
        if (!token.isEmpty()) {
            tokens.add(token.toString());
            token.setLength(0);
        }
    }

    private Map<String, Object> stepOutput(ScenarioStepResult result) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("status", result.getStatus());
        output.put("error", result.getMessage());
        output.put("tx_hashes", result.getTxHashes());
        if (result.getTxHashes() != null && !result.getTxHashes().isEmpty()) {
            output.put("tx_hash", result.getTxHashes().get(0));
        }
        output.put("outputs", result.getOutputs());
        return output;
    }

    private ScenarioStepResult simple(ScenarioStepResult.ScenarioStepResultBuilder builder, boolean ok,
                                      String message, Map<String, Object> outputs) {
        Map<String, Object> finalOutputs = new LinkedHashMap<>(outputs);
        finalOutputs.putIfAbsent("status", ok ? "success" : "failed");
        if (!ok) {
            finalOutputs.putIfAbsent("error", message);
        }
        return builder.status(ok ? "success" : "failed")
                .message(message)
                .outputs(finalOutputs)
                .build();
    }

    private boolean isContinuableSuccess(ScenarioStepResult result) {
        return "success".equals(result.getStatus()) || "expected_failure".equals(result.getStatus());
    }

    private Map<String, Object> toResolvedMap(JsonNode node, ScenarioContext ctx) {
        Object resolved = resolveValue(toJava(node), ctx);
        return toStringObjectMap(resolved);
    }

    private Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return new LinkedHashMap<>();
        }
        return toStringObjectMap(toJava(node));
    }

    private Object toJava(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return objectMapper.convertValue(node, Object.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toStringObjectMap(Object value) {
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }
        throw new IllegalArgumentException("Expected map but got: " + value);
    }

    private String resolvedText(JsonNode node, ScenarioContext ctx) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return stringValue(resolveValue(toJava(node), ctx));
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private boolean bool(JsonNode node) {
        return node != null && !node.isNull() && node.asBoolean(false);
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof BigInteger bi) return new BigDecimal(bi);
        if (value instanceof Number n) return new BigDecimal(String.valueOf(n));
        return new BigDecimal(String.valueOf(value));
    }

    private BigInteger adaToLovelace(BigDecimal ada) {
        return ada.multiply(BigDecimal.valueOf(1_000_000L)).toBigInteger();
    }

    private boolean statusMatches(String expected, String actual) {
        if ("success".equals(expected)) {
            return "success".equals(actual);
        }
        if ("failed".equals(expected) || "failure".equals(expected)) {
            return "failed".equals(actual) || "expected_failure".equals(actual);
        }
        if ("expected_failure".equals(expected)) {
            return "expected_failure".equals(actual);
        }
        return Objects.equals(expected, actual);
    }

    private String normalize(String key) {
        return key == null ? "" : key.replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
    }

    private boolean valueEquals(Object expected, Object actual) {
        try {
            return toBigDecimal(expected).compareTo(toBigDecimal(actual)) == 0;
        } catch (Exception ignored) {
            return Objects.equals(String.valueOf(expected), String.valueOf(actual));
        }
    }

    private static class ScenarioContext {
        private final Map<String, Object> variables = new ConcurrentHashMap<>();
        private final Map<String, Map<String, Object>> steps;

        private ScenarioContext() {
            this.steps = new ConcurrentHashMap<>();
        }

        private ScenarioContext(Map<String, Map<String, Object>> steps) {
            this.steps = steps;
        }

        private ScenarioContext copyVariables() {
            ScenarioContext copy = new ScenarioContext(this.steps);
            copy.variables.putAll(this.variables);
            // Parallel branches share the step-output map so later assertions can see branch outputs.
            return copy;
        }
    }
}
