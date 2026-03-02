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
import com.bloxbean.cardano.yacicli.localcluster.service.AccountService;
import com.bloxbean.cardano.yacicli.localcluster.service.ClusterUtilService;
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

    private final RestTemplate restTemplate = new RestTemplate();

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
}
