package com.bloxbean.cardano.yacicli.localcluster.mcp;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.common.Tuple;
import com.bloxbean.cardano.yacicli.localcluster.ClusterCommands;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.scenario.ScenarioResult;
import com.bloxbean.cardano.yacicli.localcluster.scenario.ScenarioService;
import com.bloxbean.cardano.yacicli.localcluster.scenario.ScenarioValidationResult;
import com.bloxbean.cardano.yacicli.localcluster.service.AccountService;
import com.bloxbean.cardano.yacicli.localcluster.service.ClusterUtilService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DevnetMcpTools {
    private static final String DEFAULT_CLUSTER_NAME = "default";
    private static final String SUBMIT_API_URL = "http://localhost:8090/api/submit/tx";

    private final ClusterService clusterService;
    private final ClusterUtilService clusterUtilService;
    private final ClusterCommands clusterCommands;
    private final AccountService accountService;
    private final ScenarioService scenarioService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Tool(description = "Get devnet info and current tip (slot, block, epoch).")
    public String devnet_status() {
        try {
            ClusterInfo info = clusterService.getClusterInfo(DEFAULT_CLUSTER_NAME);

            CommandContext.INSTANCE.setProperty(ClusterConfig.CLUSTER_NAME, DEFAULT_CLUSTER_NAME);
            Tuple<Long, Point> tip = clusterUtilService.getTip(msg -> log.debug(msg));

            StringBuilder sb = new StringBuilder();
            sb.append("Devnet Info:\n");
            if (info != null) {
                sb.append("  Protocol Magic: ").append(info.getProtocolMagic()).append("\n");
                sb.append("  Era: ").append(info.getEra()).append("\n");
                sb.append("  Slot Length: ").append(info.getSlotLength()).append("s\n");
                sb.append("  Block Time: ").append(info.getBlockTime()).append("s\n");
                sb.append("  Epoch Length: ").append(info.getEpochLength()).append(" slots\n");
                sb.append("  Node Port: ").append(info.getNodePort()).append("\n");
            }
            if (tip != null) {
                sb.append("Tip:\n");
                sb.append("  Block: ").append(tip._1).append("\n");
                sb.append("  Slot: ").append(tip._2.getSlot()).append("\n");
                sb.append("  Hash: ").append(tip._2.getHash()).append("\n");
            } else {
                sb.append("Tip: unavailable (is the devnet running?)\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Error getting devnet status", e);
            return "Error getting devnet status: " + e.getMessage();
        }
    }

    @Tool(description = "Reset devnet to initial state. Wipes all transactions and returns to genesis.")
    public String devnet_reset() {
        try {
            CommandContext.INSTANCE.setProperty(ClusterConfig.CLUSTER_NAME, DEFAULT_CLUSTER_NAME);
            clusterCommands.resetLocalCluster();
            return "Devnet reset successfully.";
        } catch (Exception e) {
            log.error("Error resetting devnet", e);
            return "Reset failed: " + e.getMessage();
        }
    }

    @Tool(description = "Fund an address with ADA from the devnet faucet.")
    public String devnet_topup(
            @ToolParam(description = "Bech32 address to fund") String address,
            @ToolParam(description = "Amount of ADA to send") double adaAmount) {
        if (address == null || address.isEmpty()) {
            return "Error: address is required";
        }
        if (adaAmount <= 0) {
            return "Error: adaAmount must be positive";
        }

        try {
            CommandContext.INSTANCE.setProperty(ClusterConfig.CLUSTER_NAME, DEFAULT_CLUSTER_NAME);
            Era era = CommandContext.INSTANCE.getEra();

            boolean success = accountService.topup(DEFAULT_CLUSTER_NAME, era, address, adaAmount, msg -> log.debug(msg));
            if (success) {
                return "Topped up " + adaAmount + " ADA to " + address;
            } else {
                return "Topup failed for " + address;
            }
        } catch (Exception e) {
            log.error("Error topping up", e);
            return "Topup error: " + e.getMessage();
        }
    }

    @Tool(description = "Query UTxOs at a Cardano address on the devnet.")
    public String devnet_utxos(
            @ToolParam(description = "Bech32 address to query") String address) {
        if (address == null || address.isEmpty()) {
            return "Error: address is required";
        }

        try {
            CommandContext.INSTANCE.setProperty(ClusterConfig.CLUSTER_NAME, DEFAULT_CLUSTER_NAME);
            Era era = CommandContext.INSTANCE.getEra();

            List<Utxo> utxos = accountService.getUtxos(DEFAULT_CLUSTER_NAME, era, address, msg -> log.debug(msg));
            if (utxos == null || utxos.isEmpty()) {
                return "No UTxOs found at " + address;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("UTxOs at ").append(address).append(":\n");
            for (Utxo utxo : utxos) {
                sb.append("  ").append(utxo.getTxHash()).append("#").append(utxo.getOutputIndex()).append("\n");
                for (Amount amount : utxo.getAmount()) {
                    sb.append("    ").append(amount.getUnit()).append(": ").append(amount.getQuantity()).append("\n");
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Error querying UTxOs", e);
            return "Error querying UTxOs: " + e.getMessage();
        }
    }

    @Tool(description = "Submit a signed transaction (CBOR hex) to the devnet.")
    public String devnet_submit_tx(
            @ToolParam(description = "Transaction CBOR as hex string") String cborHex) {
        if (cborHex == null || cborHex.isEmpty()) {
            return "Error: cborHex is required";
        }

        byte[] cborBytes;
        try {
            cborBytes = HexFormat.of().parseHex(cborHex);
        } catch (IllegalArgumentException e) {
            return "Error: Invalid hex string: " + e.getMessage();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/cbor");
            HttpEntity<byte[]> entity = new HttpEntity<>(cborBytes, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    SUBMIT_API_URL, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return "Transaction submitted successfully.\n" + response.getBody();
            } else {
                return "Submit failed (HTTP " + response.getStatusCode() + "): " + response.getBody();
            }
        } catch (Exception e) {
            log.error("Error submitting transaction", e);
            return "Submit failed: " + e.getMessage();
        }
    }

    @Tool(description = "Run a declarative scenario (DevKit L3 scenario, cardano-client-lib TxPlan, or TxFlow YAML) against the devnet. "
            + "Reference signers as account://acc0 .. account://acc19 (the pre-funded default accounts) and policy://default "
            + "for minting, or use account://, wallet://, policy://, and custom refs configured in DevKit signer properties, "
            + "so no keys are needed in the YAML.")
    public String devnet_run_scenario(
            @ToolParam(description = "Scenario YAML in DevKit L3, cardano-client-lib TxPlan, or TxFlow format") String yaml) {
        if (yaml == null || yaml.isEmpty()) {
            return "Error: yaml is required";
        }

        try {
            CommandContext.INSTANCE.setProperty(ClusterConfig.CLUSTER_NAME, DEFAULT_CLUSTER_NAME);
            ScenarioResult result = scenarioService.run(yaml, msg -> log.debug(msg));
            return toJson(result);
        } catch (Exception e) {
            log.error("Error running scenario", e);
            return "Scenario error: " + e.getMessage();
        }
    }

    @Tool(description = "Run a declarative scenario asynchronously and return a runId for polling.")
    public String devnet_run_scenario_async(
            @ToolParam(description = "Scenario YAML in DevKit L3, cardano-client-lib TxPlan, or TxFlow format") String yaml) {
        if (yaml == null || yaml.isEmpty()) {
            return "Error: yaml is required";
        }

        try {
            CommandContext.INSTANCE.setProperty(ClusterConfig.CLUSTER_NAME, DEFAULT_CLUSTER_NAME);
            ScenarioResult result = scenarioService.runAsync(yaml, msg -> log.debug(msg));
            return toJson(result);
        } catch (Exception e) {
            log.error("Error starting scenario", e);
            return "Scenario error: " + e.getMessage();
        }
    }

    @Tool(description = "Validate a DevKit L3 scenario, cardano-client-lib TxPlan, or TxFlow YAML document without submitting transactions.")
    public String devnet_validate_scenario(
            @ToolParam(description = "Scenario YAML to validate") String yaml) {
        if (yaml == null || yaml.isEmpty()) {
            return "Error: yaml is required";
        }
        ScenarioValidationResult result = scenarioService.validate(yaml);
        return toJson(result);
    }

    @Tool(description = "Return the DevKit scenario grammar schema for AI authoring.")
    public String devnet_scenario_schema() {
        return scenarioService.schema();
    }

    @Tool(description = "Return example DevKit scenario YAML snippets for AI authoring.")
    public String devnet_scenario_examples() {
        return scenarioService.examples();
    }

    @Tool(description = "List default devnet account references and addresses.")
    public String devnet_accounts() {
        try {
            return scenarioService.accounts().toString();
        } catch (Exception e) {
            log.error("Error listing devnet accounts", e);
            return "Error listing accounts: " + e.getMessage();
        }
    }

    @Tool(description = "Return current devnet state summary: cluster, node mode, tip, slot, epoch.")
    public String devnet_state_summary() {
        try {
            return scenarioService.stateSummary().toString();
        } catch (Exception e) {
            log.error("Error getting devnet state summary", e);
            return "Error getting state summary: " + e.getMessage();
        }
    }

    @Tool(description = "Get async scenario run status by runId.")
    public String devnet_scenario_status(
            @ToolParam(description = "Run id returned by devnet_run_scenario_async") String runId) {
        if (runId == null || runId.isBlank()) {
            return "Error: runId is required";
        }
        ScenarioResult result = scenarioService.status(runId);
        return toJson(result);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
