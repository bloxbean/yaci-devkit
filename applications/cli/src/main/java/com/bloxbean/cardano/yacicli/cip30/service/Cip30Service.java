package com.bloxbean.cardano.yacicli.cip30.service;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.MultiAsset;
import com.bloxbean.cardano.client.transaction.spec.TransactionInput;
import com.bloxbean.cardano.client.transaction.spec.TransactionOutput;
import com.bloxbean.cardano.client.transaction.spec.Value;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yacicli.cip30.dto.WalletAccountDto;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.*;

/**
 * CIP-30 Service for handling CBOR serialization of wallet data.
 * All CIP-30 compliant CBOR encoding happens in this service using cardano-client-lib.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class Cip30Service {
    private final WalletAccountService walletAccountService;
    private final ClusterService clusterService;

    // Cached backend service to avoid creating new instances on every request
    private volatile BackendService cachedBackendService;

    /**
     * Get network ID for CIP-30
     * Yaci DevKit is always testnet (0)
     */
    public int getNetworkId() {
        return 0; // Testnet
    }

    /**
     * Get balance as CIP-30 CBOR hex
     * @param accountId The account ID
     * @return CBOR hex of Value type, or null if account not found
     */
    public String getBalanceCbor(String accountId) {
        Optional<WalletAccountDto> accountOpt = walletAccountService.getAccount(accountId);
        if (accountOpt.isEmpty()) {
            log.warn("Account not found: {}", accountId);
            return null;
        }

        WalletAccountDto account = accountOpt.get();

        try {
            BackendService backendService = getBackendService();
            if (backendService == null) {
                log.warn("Backend service not available");
                return serializeValue(Value.builder().coin(BigInteger.ZERO).build());
            }

            // Get UTXOs and sum up the balance
            List<Utxo> utxos = backendService.getUtxoService()
                    .getUtxos(account.getAddress(), 100, 1)
                    .getValue();

            if (utxos == null || utxos.isEmpty()) {
                return serializeValue(Value.builder().coin(BigInteger.ZERO).build());
            }

            // Aggregate all amounts from all UTXOs
            List<Amount> allAmounts = utxos.stream()
                    .flatMap(utxo -> utxo.getAmount().stream())
                    .toList();

            Value value = buildValueFromAmounts(allAmounts);
            return serializeValue(value);

        } catch (Exception e) {
            log.error("Error getting balance for account {}: {}", accountId, e.getMessage());
            return serializeValue(Value.builder().coin(BigInteger.ZERO).build());
        }
    }

    /**
     * Get UTXOs as CIP-30 CBOR hex array with optional amount filtering
     * @param accountId The account ID
     * @param amountCborHex Optional CBOR hex of Value type for minimum amount filter
     * @return List of CBOR hex strings for TransactionUnspentOutput, or null if amount cannot be satisfied
     */
    public List<String> getUtxosCbor(String accountId, String amountCborHex) {
        // If no amount specified, return all UTXOs
        if (amountCborHex == null || amountCborHex.isBlank()) {
            return getUtxosCbor(accountId);
        }

        // Get all UTXOs first
        List<String> allUtxos = getUtxosCbor(accountId);
        if (allUtxos == null || allUtxos.isEmpty()) {
            return null; // No UTXOs available
        }

        try {
            // Parse the requested amount
            byte[] amountCborBytes = HexUtil.decodeHexString(amountCborHex);
            Value requestedValue = Value.deserialize((co.nstant.in.cbor.model.Map) CborSerializationUtil.deserialize(amountCborBytes));

            // Get account for address
            Optional<WalletAccountDto> accountOpt = walletAccountService.getAccount(accountId);
            if (accountOpt.isEmpty()) {
                return null;
            }

            // Get actual UTXO objects to check their values
            BackendService backendService = getBackendService();
            if (backendService == null) {
                return null;
            }

            List<Utxo> utxos = backendService.getUtxoService()
                    .getUtxos(accountOpt.get().getAddress(), 100, 1)
                    .getValue();

            if (utxos == null || utxos.isEmpty()) {
                return null;
            }

            // Simple greedy selection: add UTXOs until we satisfy the amount
            List<String> selectedUtxos = new ArrayList<>();
            BigInteger accumulatedLovelace = BigInteger.ZERO;
            Map<String, BigInteger> accumulatedAssets = new HashMap<>();

            BigInteger requestedLovelace = requestedValue.getCoin();
            Map<String, BigInteger> requestedAssets = extractAssetsFromValue(requestedValue);

            for (int i = 0; i < utxos.size(); i++) {
                Utxo utxo = utxos.get(i);

                // Add this UTXO to accumulated total
                for (Amount amt : utxo.getAmount()) {
                    if ("lovelace".equals(amt.getUnit())) {
                        accumulatedLovelace = accumulatedLovelace.add(amt.getQuantity());
                    } else {
                        String unit = amt.getUnit();
                        BigInteger current = accumulatedAssets.getOrDefault(unit, BigInteger.ZERO);
                        accumulatedAssets.put(unit, current.add(amt.getQuantity()));
                    }
                }

                selectedUtxos.add(allUtxos.get(i));

                // Check if we've satisfied the requested amount
                if (isSatisfied(accumulatedLovelace, accumulatedAssets, requestedLovelace, requestedAssets)) {
                    return selectedUtxos;
                }
            }

            // If we've gone through all UTXOs and still haven't satisfied the amount, return null
            return null;

        } catch (Exception e) {
            log.error("Error filtering UTXOs by amount: {}", e.getMessage());
            // On error, return all UTXOs (graceful degradation)
            return allUtxos;
        }
    }

    /**
     * Get UTXOs as CIP-30 CBOR hex array
     * @param accountId The account ID
     * @return List of CBOR hex strings for TransactionUnspentOutput
     */
    public List<String> getUtxosCbor(String accountId) {
        Optional<WalletAccountDto> accountOpt = walletAccountService.getAccount(accountId);
        if (accountOpt.isEmpty()) {
            log.warn("Account not found: {}", accountId);
            return Collections.emptyList();
        }

        WalletAccountDto account = accountOpt.get();

        try {
            BackendService backendService = getBackendService();
            if (backendService == null) {
                log.warn("Backend service not available");
                return Collections.emptyList();
            }

            List<Utxo> utxos = backendService.getUtxoService()
                    .getUtxos(account.getAddress(), 100, 1)
                    .getValue();

            if (utxos == null || utxos.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> cborUtxos = new ArrayList<>();
            for (Utxo utxo : utxos) {
                String cborHex = serializeUtxoToCbor(utxo, account.getAddress());
                if (cborHex != null) {
                    cborUtxos.add(cborHex);
                }
            }
            return cborUtxos;

        } catch (Exception e) {
            log.error("Error getting UTXOs for account {}: {}", accountId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Get used addresses as CIP-30 CBOR hex array
     * @param accountId The account ID
     * @return List of address bytes as hex strings
     */
    public List<String> getUsedAddressesCbor(String accountId) {
        Optional<WalletAccountDto> accountOpt = walletAccountService.getAccount(accountId);
        if (accountOpt.isEmpty()) {
            return Collections.emptyList();
        }

        WalletAccountDto account = accountOpt.get();
        return List.of(addressToCborHex(account.getAddress()));
    }

    /**
     * Get change address as CIP-30 CBOR hex
     * @param accountId The account ID
     * @return Address bytes as hex string
     */
    public String getChangeAddressCbor(String accountId) {
        Optional<WalletAccountDto> accountOpt = walletAccountService.getAccount(accountId);
        if (accountOpt.isEmpty()) {
            return null;
        }

        return addressToCborHex(accountOpt.get().getAddress());
    }

    /**
     * Get reward addresses as CIP-30 CBOR hex array
     * @param accountId The account ID
     * @return List of stake address bytes as hex strings
     */
    public List<String> getRewardAddressesCbor(String accountId) {
        Optional<WalletAccountDto> accountOpt = walletAccountService.getAccount(accountId);
        if (accountOpt.isEmpty()) {
            return Collections.emptyList();
        }

        WalletAccountDto account = accountOpt.get();
        if (account.getStakeAddress() == null || account.getStakeAddress().isBlank()) {
            return Collections.emptyList();
        }

        return List.of(addressToCborHex(account.getStakeAddress()));
    }

    /**
     * Get collateral UTXOs as CIP-30 CBOR hex array.
     * Returns a single UTXO with at least 5 ADA for Plutus script collateral.
     * @param accountId The account ID
     * @return List of CBOR hex strings for TransactionUnspentOutput, or null if no suitable UTXO
     */
    public List<String> getCollateralCbor(String accountId) {
        Optional<WalletAccountDto> accountOpt = walletAccountService.getAccount(accountId);
        if (accountOpt.isEmpty()) {
            log.warn("Account not found: {}", accountId);
            return null;
        }

        WalletAccountDto account = accountOpt.get();

        try {
            BackendService backendService = getBackendService();
            if (backendService == null) {
                log.warn("Backend service not available");
                return null;
            }

            List<Utxo> utxos = backendService.getUtxoService()
                    .getUtxos(account.getAddress(), 100, 1)
                    .getValue();

            if (utxos == null || utxos.isEmpty()) {
                return null;
            }

            // Find a pure-ADA UTXO with at least 5 ADA (5_000_000 lovelace) for collateral
            BigInteger minCollateral = BigInteger.valueOf(5_000_000);
            for (Utxo utxo : utxos) {
                boolean isPureAda = utxo.getAmount().size() == 1
                        && "lovelace".equalsIgnoreCase(utxo.getAmount().get(0).getUnit());
                if (isPureAda && utxo.getAmount().get(0).getQuantity().compareTo(minCollateral) >= 0) {
                    String cborHex = serializeUtxoToCbor(utxo, account.getAddress());
                    if (cborHex != null) {
                        return List.of(cborHex);
                    }
                }
            }

            // Fallback: return the first UTXO that has enough ADA (even if it has native tokens)
            for (Utxo utxo : utxos) {
                BigInteger lovelace = utxo.getAmount().stream()
                        .filter(a -> "lovelace".equalsIgnoreCase(a.getUnit()))
                        .map(Amount::getQuantity)
                        .findFirst()
                        .orElse(BigInteger.ZERO);
                if (lovelace.compareTo(minCollateral) >= 0) {
                    String cborHex = serializeUtxoToCbor(utxo, account.getAddress());
                    if (cborHex != null) {
                        return List.of(cborHex);
                    }
                }
            }

            log.warn("No suitable collateral UTXO found for account {}", accountId);
            return null;

        } catch (Exception e) {
            log.error("Error getting collateral for account {}: {}", accountId, e.getMessage());
            return null;
        }
    }

    /**
     * Convert bech32 address to hex-encoded raw address bytes.
     * CIP-30 address methods (getUsedAddresses, getChangeAddress, getRewardAddresses)
     * return hex-encoded raw address bytes, NOT CBOR-wrapped bytes.
     * Only complex types like Value and TransactionUnspentOutput use CBOR encoding.
     */
    public String addressToCborHex(String bech32Address) {
        try {
            Address address = new Address(bech32Address);
            return HexUtil.encodeHexString(address.getBytes());
        } catch (Exception e) {
            log.error("Error converting address to hex: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Decode hex-encoded address bytes back to bech32 (for demo/display purposes).
     */
    public String decodeAddressFromCbor(String cborHex) {
        try {
            byte[] addressBytes = HexUtil.decodeHexString(cborHex);
            Address address = new Address(addressBytes);
            return address.toBech32();
        } catch (Exception e) {
            log.error("Error decoding address from CBOR: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Decode CBOR hex value to human-readable format
     */
    public Map<String, Object> decodeValueFromCbor(String cborHex) {
        try {
            byte[] valueBytes = HexUtil.decodeHexString(cborHex);
            Value value = Value.deserialize(CborSerializationUtil.deserialize(valueBytes));

            Map<String, Object> result = new HashMap<>();
            result.put("lovelace", value.getCoin().toString());

            if (value.getMultiAssets() != null && !value.getMultiAssets().isEmpty()) {
                List<Map<String, Object>> assets = new ArrayList<>();
                for (MultiAsset multiAsset : value.getMultiAssets()) {
                    String policyId = multiAsset.getPolicyId();
                    for (Asset assetEntry : multiAsset.getAssets()) {
                        Map<String, Object> asset = new HashMap<>();
                        asset.put("policyId", policyId);
                        asset.put("assetName", assetEntry.getName());
                        asset.put("quantity", assetEntry.getValue().toString());
                        assets.add(asset);
                    }
                }
                result.put("assets", assets);
            }

            return result;
        } catch (Exception e) {
            log.error("Error decoding value from CBOR: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    // ========== Helper Methods ==========

    /**
     * Serialize a UTXO to CIP-30 CBOR format (TransactionUnspentOutput)
     */
    private String serializeUtxoToCbor(Utxo utxo, String address) {
        try {
            // Build TransactionInput
            TransactionInput input = TransactionInput.builder()
                    .transactionId(utxo.getTxHash())
                    .index(utxo.getOutputIndex())
                    .build();

            // Build Value from amounts
            Value value = buildValueFromAmounts(utxo.getAmount());

            // Build TransactionOutput
            TransactionOutput output = TransactionOutput.builder()
                    .address(address)
                    .value(value)
                    .build();

            // Serialize as [input, output] array (CBOR)
            // CIP-30 TransactionUnspentOutput format: [transaction_input, transaction_output]
            co.nstant.in.cbor.model.Array utxoArray = new co.nstant.in.cbor.model.Array();
            utxoArray.add(input.serialize());
            utxoArray.add(output.serialize());

            byte[] cborBytes = CborSerializationUtil.serialize(utxoArray);
            return HexUtil.encodeHexString(cborBytes);

        } catch (Exception e) {
            log.error("Error serializing UTXO to CBOR: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Build a Value object from a list of amounts
     */
    private Value buildValueFromAmounts(List<Amount> amounts) {
        BigInteger lovelace = BigInteger.ZERO;
        Map<String, Map<String, BigInteger>> assetMap = new HashMap<>();

        if (amounts == null) {
            return Value.builder().coin(lovelace).build();
        }

        for (Amount amount : amounts) {
            String unit = amount.getUnit();
            BigInteger quantity = amount.getQuantity();

            if ("lovelace".equalsIgnoreCase(unit)) {
                lovelace = lovelace.add(quantity);
            } else {
                // Native token: unit = policyId (56 chars) + assetNameHex
                if (unit.length() >= 56) {
                    String policyId = unit.substring(0, 56);
                    String assetNameHex = unit.substring(56);

                    assetMap.computeIfAbsent(policyId, k -> new HashMap<>())
                            .merge(assetNameHex, quantity, BigInteger::add);
                }
            }
        }

        Value.ValueBuilder builder = Value.builder().coin(lovelace);

        if (!assetMap.isEmpty()) {
            List<MultiAsset> multiAssets = new ArrayList<>();
            for (Map.Entry<String, Map<String, BigInteger>> entry : assetMap.entrySet()) {
                List<Asset> assets = new ArrayList<>();
                for (Map.Entry<String, BigInteger> assetEntry : entry.getValue().entrySet()) {
                    assets.add(new Asset(assetEntry.getKey(), assetEntry.getValue()));
                }
                multiAssets.add(MultiAsset.builder()
                        .policyId(entry.getKey())
                        .assets(assets)
                        .build());
            }
            builder.multiAssets(multiAssets);
        }

        return builder.build();
    }

    /**
     * Serialize a Value to CBOR hex
     */
    private String serializeValue(Value value) {
        try {
            byte[] cborBytes = CborSerializationUtil.serialize(value.serialize());
            return HexUtil.encodeHexString(cborBytes);
        } catch (Exception e) {
            log.error("Error serializing Value to CBOR: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get the backend service (Yaci Store) for queries.
     * Uses lazy initialization with caching to avoid creating new instances on every request.
     */
    private BackendService getBackendService() {
        // Double-checked locking for thread-safe lazy initialization
        if (cachedBackendService == null) {
            synchronized (this) {
                if (cachedBackendService == null) {
                    try {
                        String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
                        var clusterInfo = clusterService.getClusterInfo(clusterName);
                        if (clusterInfo == null) {
                            log.error("Cluster {} not found", clusterName);
                            return null;
                        }

                        var yaciStorePort = clusterInfo.getYaciStorePort();
                        String backendUrl = "http://localhost:" + yaciStorePort + "/api/v1/";
                        cachedBackendService = new BFBackendService(backendUrl, "dummy_key");
                        log.info("Initialized cached BackendService for {}", backendUrl);
                    } catch (Exception e) {
                        log.error("Error getting backend service: {}", e.getMessage());
                        return null;
                    }
                }
            }
        }
        return cachedBackendService;
    }

    /**
     * Extract assets from a Value object as a map of unit -> quantity
     */
    private Map<String, BigInteger> extractAssetsFromValue(Value value) {
        Map<String, BigInteger> assets = new HashMap<>();

        if (value.getMultiAssets() != null) {
            for (MultiAsset multiAsset : value.getMultiAssets()) {
                String policyId = multiAsset.getPolicyId();
                for (Asset asset : multiAsset.getAssets()) {
                    String unit = policyId + asset.getName();
                    assets.put(unit, asset.getValue());
                }
            }
        }

        return assets;
    }

    /**
     * Check if accumulated value satisfies requested value
     */
    private boolean isSatisfied(BigInteger accumulatedLovelace, Map<String, BigInteger> accumulatedAssets,
                                BigInteger requestedLovelace, Map<String, BigInteger> requestedAssets) {
        // Check lovelace
        if (accumulatedLovelace.compareTo(requestedLovelace) < 0) {
            return false;
        }

        // Check all requested assets
        for (Map.Entry<String, BigInteger> entry : requestedAssets.entrySet()) {
            String unit = entry.getKey();
            BigInteger requestedQuantity = entry.getValue();
            BigInteger accumulatedQuantity = accumulatedAssets.getOrDefault(unit, BigInteger.ZERO);

            if (accumulatedQuantity.compareTo(requestedQuantity) < 0) {
                return false;
            }
        }

        return true;
    }
}
