package com.bloxbean.cardano.yacicli.cip30.controller;

import com.bloxbean.cardano.yacicli.cip30.config.WalletConfigurationProperties;
import com.bloxbean.cardano.yacicli.cip30.dto.*;
import com.bloxbean.cardano.yacicli.cip30.service.WalletSessionService;
import com.bloxbean.cardano.yacicli.cip30.service.YaciStoreProxyService;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/wallet")
@CrossOrigin(origins = {"http://localhost:*", "https://localhost:*", "http://127.0.0.1:*", "https://127.0.0.1:*"})
@ConditionalOnProperty(prefix = "yaci.devkit.wallet", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class WalletApiController {
    
    private final WalletConfigurationProperties walletConfig;
    private final YaciStoreProxyService proxyService;
    private final WalletSessionService sessionService;
    private final ObjectMapper objectMapper;
    
    /**
     * Enable wallet connection (CIP-30)
     */
    @PostMapping("/enable")
    public ResponseEntity<WalletResponse> enableWallet(@RequestBody(required = false) WalletRequest request) {
        log.info("Wallet enable requested: {}", request);
        
        // For development, always allow connection
        return ResponseEntity.ok(WalletResponse.builder()
                .enabled(true)
                .message("Yaci DevKit wallet enabled")
                .build());
    }
    
    /**
     * Get network information
     */
    @GetMapping("/network")
    public ResponseEntity<NetworkResponse> getNetwork() {
        try {
            // Try to get network info from Yaci Store
            Optional<JsonNode> networkInfo = proxyService.proxyGetRequest("/api/v1/network", JsonNode.class);
            
            if (networkInfo.isPresent()) {
                JsonNode data = networkInfo.get();
                return ResponseEntity.ok(NetworkResponse.builder()
                        .networkId(getNetworkId())
                        .networkName(walletConfig.getNetwork())
                        .currentEpoch(data.has("currentEpoch") ? data.get("currentEpoch").asInt() : null)
                        .currentSlot(data.has("currentSlot") ? data.get("currentSlot").asInt() : null)
                        .build());
            }
        } catch (Exception e) {
            log.error("Failed to get network info from Yaci Store", e);
        }
        
        // Fallback response
        return ResponseEntity.ok(NetworkResponse.builder()
                .networkId(getNetworkId())
                .networkName(walletConfig.getNetwork())
                .build());
    }
    
    /**
     * Get wallet addresses
     */
    @GetMapping("/addresses")
    public ResponseEntity<AddressesResponse> getAddresses() {
        List<String> addresses = sessionService.getWalletAddresses();
        List<String> stakeAddresses = sessionService.getStakeAddresses();
        
        return ResponseEntity.ok(AddressesResponse.builder()
                .addresses(addresses)
                .rewardAddresses(stakeAddresses)
                .count(addresses.size())
                .build());
    }
    
    /**
     * Get balance for a specific address (including native assets)
     */
    @GetMapping("/balance/{address}")
    public ResponseEntity<Map<String, Object>> getBalance(@PathVariable String address) {
        // Verify address belongs to wallet
        if (!sessionService.isWalletAddress(address)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "address", address,
                            "balance", "0",
                            "adaBalance", "0",
                            "assets", List.of()
                    ));
        }
        
        try {
            // Get amounts (ADA + native assets) from Yaci Store
            Optional<JsonNode> amountsData = proxyService.proxyGetRequest(
                    "/api/v1/addresses/" + address + "/amounts", JsonNode.class);
            
            if (amountsData.isPresent() && amountsData.get().isArray()) {
                JsonNode amounts = amountsData.get();
                
                String adaBalance = "0";
                String adaBalanceLovelace = "0";
                List<Map<String, Object>> nativeAssets = new ArrayList<>();
                
                // Process all amounts
                for (JsonNode amount : amounts) {
                    String unit = amount.has("unit") ? amount.get("unit").asText() : "";
                    String quantity = amount.has("quantity") ? amount.get("quantity").asText() : "0";
                    
                    if ("lovelace".equals(unit)) {
                        // ADA balance
                        adaBalanceLovelace = quantity;
                        BigDecimal lovelace = new BigDecimal(quantity);
                        BigDecimal ada = lovelace.divide(new BigDecimal("1000000"), 6, RoundingMode.DOWN);
                        adaBalance = ada.toPlainString();
                    } else if (!unit.isEmpty()) {
                        // Native asset
                        Map<String, Object> asset = new HashMap<>();
                        asset.put("unit", unit);
                        asset.put("quantity", quantity);
                        
                        // Try to get asset metadata if available
                        if (unit.length() > 56) {
                            String policyId = unit.substring(0, 56);
                            String assetName = unit.substring(56);
                            asset.put("policyId", policyId);
                            asset.put("assetName", assetName);
                            
                            // Convert hex asset name to ASCII if possible
                            try {
                                String asciiName = new String(HexUtil.decodeHexString(assetName));
                                if (asciiName.matches("^[\\x20-\\x7E]*$")) { // printable ASCII
                                    asset.put("assetNameAscii", asciiName);
                                }
                            } catch (Exception ignored) {
                                // Not valid hex or not printable, ignore
                            }
                        }
                        
                        nativeAssets.add(asset);
                    }
                }
                
                Map<String, Object> response = new HashMap<>();
                response.put("address", address);
                response.put("balance", adaBalanceLovelace);
                response.put("adaBalance", adaBalance);
                response.put("assets", nativeAssets);
                response.put("totalAssets", nativeAssets.size());
                
                return ResponseEntity.ok(response);
            }
            
            // Try fallback to basic balance endpoint
            Optional<JsonNode> balanceData = proxyService.proxyGetRequest(
                    "/api/v1/addresses/" + address + "/balance", JsonNode.class);
            
            if (balanceData.isPresent()) {
                JsonNode data = balanceData.get();
                String lovelaceBalance = data.has("balance") ? data.get("balance").asText() : "0";
                
                // Convert lovelace to ADA
                BigDecimal lovelace = new BigDecimal(lovelaceBalance);
                BigDecimal ada = lovelace.divide(new BigDecimal("1000000"), 6, RoundingMode.DOWN);
                
                Map<String, Object> response = new HashMap<>();
                response.put("address", address);
                response.put("balance", lovelaceBalance);
                response.put("adaBalance", ada.toPlainString());
                response.put("assets", List.of());
                response.put("totalAssets", 0);
                
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception e) {
            log.error("Failed to get balance for address: {}", address, e);
        }
        
        // Return zero balance if no data available (no fallback to default amounts)
        Map<String, Object> response = new HashMap<>();
        response.put("address", address);
        response.put("balance", "0");
        response.put("adaBalance", "0");
        response.put("assets", List.of());
        response.put("totalAssets", 0);
        response.put("error", "Unable to fetch balance from Yaci Store");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get UTXOs for an address
     */
    @GetMapping("/utxos/{address}")
    public ResponseEntity<List<UtxoResponse>> getUtxos(
            @PathVariable String address,
            @RequestParam(required = false, defaultValue = "50") int count,
            @RequestParam(required = false, defaultValue = "0") int page) {
        
        // Verify address belongs to wallet
        if (!sessionService.isWalletAddress(address)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ArrayList<>());
        }
        
        try {
            // Get UTXOs from Yaci Store
            String endpoint = String.format("/api/v1/addresses/%s/utxos?count=%d&page=%d", 
                    address, count, page);
            Optional<JsonNode> utxosData = proxyService.proxyGetRequest(endpoint, JsonNode.class);
            
            if (utxosData.isPresent() && utxosData.get().isArray()) {
                List<UtxoResponse> utxos = new ArrayList<>();
                
                for (JsonNode utxoNode : utxosData.get()) {
                    UtxoResponse utxo = UtxoResponse.builder()
                            .txHash(utxoNode.has("txHash") ? utxoNode.get("txHash").asText() : null)
                            .outputIndex(utxoNode.has("outputIndex") ? utxoNode.get("outputIndex").asInt() : 0)
                            .address(address)
                            .amount(utxoNode.has("amount") ? utxoNode.get("amount").asText() : "0")
                            .datum(utxoNode.has("datum") ? utxoNode.get("datum").asText() : null)
                            .datumHash(utxoNode.has("datumHash") ? utxoNode.get("datumHash").asText() : null)
                            .inlineDatum(utxoNode.has("inlineDatum") ? utxoNode.get("inlineDatum").asText() : null)
                            .referenceScriptHash(utxoNode.has("referenceScriptHash") ? 
                                    utxoNode.get("referenceScriptHash").asText() : null)
                            .blockNumber(utxoNode.has("blockNumber") ? utxoNode.get("blockNumber").asLong() : null)
                            .slot(utxoNode.has("slot") ? utxoNode.get("slot").asLong() : null)
                            .build();
                    
                    utxos.add(utxo);
                }
                
                return ResponseEntity.ok(utxos);
            }
        } catch (Exception e) {
            log.error("Failed to get UTXOs for address: {}", address, e);
        }
        
        // Return empty list if no UTXOs found (no fallback to mock data)
        return ResponseEntity.ok(new ArrayList<>());
    }
    
    /**
     * Sign transaction (mock implementation for development)
     */
    @PostMapping("/sign-tx")
    public ResponseEntity<SignatureResponse> signTransaction(@RequestBody TransactionRequest request) {
        log.info("Sign transaction requested");
        
        if (walletConfig.isMockMode() || walletConfig.isDefaultMode()) {
            // Mock signature for development
            String mockWitnessSet = generateMockWitnessSet();
            
            return ResponseEntity.ok(SignatureResponse.builder()
                    .witnessSet(mockWitnessSet)
                    .signature("mock_signature_" + UUID.randomUUID().toString())
                    .publicKey("mock_public_key")
                    .build());
        }
        
        // In production, this would use actual key material to sign
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(SignatureResponse.builder()
                        .witnessSet(null)
                        .signature(null)
                        .publicKey(null)
                        .build());
    }
    
    /**
     * Sign data (mock implementation for development)
     */
    @PostMapping("/sign-data")
    public ResponseEntity<Map<String, String>> signData(@RequestBody Map<String, String> request) {
        String address = request.get("address");
        String payload = request.get("payload");
        
        log.info("Sign data requested for address: {}", address);
        
        if (!sessionService.isWalletAddress(address)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        
        if (walletConfig.isMockMode() || walletConfig.isDefaultMode()) {
            // Mock signature for development
            Map<String, String> result = new HashMap<>();
            result.put("signature", "845846a2" + UUID.randomUUID().toString().replace("-", ""));
            result.put("key", "a4010103272006215820" + UUID.randomUUID().toString().replace("-", ""));
            
            return ResponseEntity.ok(result);
        }
        
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
    }
    
    /**
     * Submit transaction
     */
    @PostMapping("/submit-tx")
    public ResponseEntity<Map<String, String>> submitTransaction(@RequestBody Map<String, String> request) {
        String txCbor = request.get("tx");
        
        log.info("Submit transaction requested");
        
        // Try to submit via Yaci Store
        try {
            Map<String, String> submitRequest = new HashMap<>();
            submitRequest.put("cborHex", txCbor);
            
            Optional<JsonNode> result = proxyService.proxyPostRequest(
                    "/api/v1/transactions/submit", submitRequest, JsonNode.class);
            
            if (result.isPresent()) {
                String txHash = result.get().has("txHash") ? 
                        result.get().get("txHash").asText() : 
                        UUID.randomUUID().toString();
                
                Map<String, String> response = new HashMap<>();
                response.put("txHash", txHash);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Failed to submit transaction", e);
        }
        
        // Mock response for development
        if (walletConfig.isMockMode()) {
            Map<String, String> response = new HashMap<>();
            response.put("txHash", UUID.randomUUID().toString().replace("-", ""));
            return ResponseEntity.ok(response);
        }
        
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Collections.singletonMap("error", "Transaction submission failed"));
    }
    
    /**
     * Helper method to get network ID
     */
    private Integer getNetworkId() {
        String network = walletConfig.getNetwork().toLowerCase();
        switch (network) {
            case "mainnet":
                return 1;
            case "testnet":
            case "preprod":
            case "preview":
            default:
                return 0;
        }
    }
    
    /**
     * Generate mock witness set for development
     */
    private String generateMockWitnessSet() {
        // This is a simplified mock witness set in CBOR hex format
        // In production, this would be properly generated from the transaction
        return "a10081825820" + UUID.randomUUID().toString().replace("-", "") + 
               "5840" + UUID.randomUUID().toString().replace("-", "") + 
               UUID.randomUUID().toString().replace("-", "");
    }
}