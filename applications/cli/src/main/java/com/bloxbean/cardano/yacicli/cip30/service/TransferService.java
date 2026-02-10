package com.bloxbean.cardano.yacicli.cip30.service;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.TransactionWitnessSet;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yacicli.cip30.dto.*;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferService {
    private final WalletAccountService walletAccountService;
    private final ClusterService clusterService;

    /**
     * Transfer ADA and/or native tokens from an account to a receiver address
     */
    public TransferResult transfer(TransferRequest request) {
        // Validate request
        if (request.getAccountId() == null || request.getAccountId().isBlank()) {
            return TransferResult.error("INVALID_ACCOUNT", "Account ID is required");
        }

        if (request.getReceiverAddress() == null || request.getReceiverAddress().isBlank()) {
            return TransferResult.error("INVALID_ADDRESS", "Receiver address is required");
        }

        if (request.getAmounts() == null || request.getAmounts().isEmpty()) {
            return TransferResult.error("INVALID_AMOUNT", "At least one transfer amount is required");
        }

        // Validate amounts
        for (TransferAmount amount : request.getAmounts()) {
            if (amount.getUnit() == null || amount.getUnit().isBlank()) {
                return TransferResult.error("INVALID_AMOUNT", "Amount unit is required");
            }
            if (amount.getQuantity() == null || amount.getQuantity().isBlank()) {
                return TransferResult.error("INVALID_AMOUNT", "Amount quantity is required");
            }
            try {
                BigInteger qty = new BigInteger(amount.getQuantity());
                if (qty.compareTo(BigInteger.ZERO) <= 0) {
                    return TransferResult.error("INVALID_AMOUNT", "Amount must be greater than zero");
                }
            } catch (NumberFormatException e) {
                return TransferResult.error("INVALID_AMOUNT", "Invalid quantity format: " + amount.getQuantity());
            }
        }

        // Get signing account
        Optional<Account> accountOpt = walletAccountService.getSigningAccount(request.getAccountId());
        if (accountOpt.isEmpty()) {
            return TransferResult.error("ACCOUNT_NOT_FOUND", "Account not found: " + request.getAccountId());
        }

        Account account = accountOpt.get();

        // Get backend service
        Optional<BackendService> backendServiceOpt = getBackendService();
        if (backendServiceOpt.isEmpty()) {
            return TransferResult.error("SERVICE_UNAVAILABLE", "Backend service not available. Make sure Yaci Store is running.");
        }

        BackendService backendService = backendServiceOpt.get();

        try {
            // Convert transfer amounts to cardano-client-lib Amount objects
            List<Amount> amounts = convertAmounts(request.getAmounts());

            // Build transaction
            QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

            Tx tx = new Tx()
                    .payToAddress(request.getReceiverAddress(), amounts)
                    .from(account.baseAddress());

            var result = quickTxBuilder
                    .compose(tx)
                    .withSigner(SignerProviders.signerFrom(account))
                    .completeAndWait(msg -> log.debug("Transfer tx: {}", msg));

            if (result.isSuccessful()) {
                log.info("Transfer successful. TxHash: {}", result.getValue());
                return TransferResult.success(result.getValue());
            } else {
                String errorMsg = result.getResponse();
                log.error("Transfer failed: {}", errorMsg);

                // Check for common error types
                if (errorMsg != null && errorMsg.toLowerCase().contains("insufficient")) {
                    return TransferResult.error("INSUFFICIENT_FUNDS", errorMsg);
                }
                if (errorMsg != null && errorMsg.toLowerCase().contains("min utxo")) {
                    return TransferResult.error("MIN_UTXO_NOT_MET", "Native token transfer requires minimum ADA. " + errorMsg);
                }

                return TransferResult.error("TRANSACTION_FAILED", errorMsg != null ? errorMsg : "Transaction failed");
            }
        } catch (Exception e) {
            log.error("Error during transfer", e);
            String errorMsg = e.getMessage();

            if (errorMsg != null && errorMsg.toLowerCase().contains("insufficient")) {
                return TransferResult.error("INSUFFICIENT_FUNDS", errorMsg);
            }

            return TransferResult.error("TRANSACTION_ERROR", errorMsg != null ? errorMsg : "An error occurred during transfer");
        }
    }

    /**
     * Build an unsigned transaction (without signing or submitting)
     */
    public BuildTxResponse buildTransaction(BuildTxRequest request) {
        if (request.getSenderAddress() == null || request.getSenderAddress().isBlank()) {
            return BuildTxResponse.error("INVALID_ADDRESS", "Sender address is required");
        }

        if (request.getReceiverAddress() == null || request.getReceiverAddress().isBlank()) {
            return BuildTxResponse.error("INVALID_ADDRESS", "Receiver address is required");
        }

        if (request.getAmounts() == null || request.getAmounts().isEmpty()) {
            return BuildTxResponse.error("INVALID_AMOUNT", "At least one transfer amount is required");
        }

        // Validate amounts
        for (TransferAmount amount : request.getAmounts()) {
            if (amount.getUnit() == null || amount.getUnit().isBlank()) {
                return BuildTxResponse.error("INVALID_AMOUNT", "Amount unit is required");
            }
            if (amount.getQuantity() == null || amount.getQuantity().isBlank()) {
                return BuildTxResponse.error("INVALID_AMOUNT", "Amount quantity is required");
            }
            try {
                BigInteger qty = new BigInteger(amount.getQuantity());
                if (qty.compareTo(BigInteger.ZERO) <= 0) {
                    return BuildTxResponse.error("INVALID_AMOUNT", "Amount must be greater than zero");
                }
            } catch (NumberFormatException e) {
                return BuildTxResponse.error("INVALID_AMOUNT", "Invalid quantity format: " + amount.getQuantity());
            }
        }

        // Get backend service
        Optional<BackendService> backendServiceOpt = getBackendService();
        if (backendServiceOpt.isEmpty()) {
            return BuildTxResponse.error("SERVICE_UNAVAILABLE", "Backend service not available. Make sure Yaci Store is running.");
        }

        BackendService backendService = backendServiceOpt.get();

        try {
            List<Amount> amounts = convertAmounts(request.getAmounts());

            QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

            Tx tx = new Tx()
                    .payToAddress(request.getReceiverAddress(), amounts)
                    .from(request.getSenderAddress());

            // Build without signing — use withSigner but only to build, then extract the unsigned tx
            Transaction transaction = quickTxBuilder
                    .compose(tx)
                    .build();

            String txCbor = transaction.serializeToHex();

            // Calculate tx hash from body
            byte[] txBodyBytes = CborSerializationUtil.serialize(transaction.getBody().serialize());
            String txHash = HexUtil.encodeHexString(Blake2bUtil.blake2bHash256(txBodyBytes));

            log.info("Built unsigned transaction. TxHash: {}", txHash);
            return BuildTxResponse.success(txCbor, txHash);

        } catch (Exception e) {
            log.error("Error building transaction", e);
            String errorMsg = e.getMessage();

            if (errorMsg != null && errorMsg.toLowerCase().contains("insufficient")) {
                return BuildTxResponse.error("INSUFFICIENT_FUNDS", errorMsg);
            }

            return BuildTxResponse.error("BUILD_ERROR", errorMsg != null ? errorMsg : "An error occurred while building the transaction");
        }
    }

    /**
     * Assemble a signed transaction from unsigned tx CBOR and witness set CBOR
     */
    public AssembleTxResponse assembleTx(AssembleTxRequest request) {
        if (request.getTxCbor() == null || request.getTxCbor().isBlank()) {
            return AssembleTxResponse.error("INVALID_TX", "Transaction CBOR is required");
        }

        if (request.getWitnessCbor() == null || request.getWitnessCbor().isBlank()) {
            return AssembleTxResponse.error("INVALID_WITNESS", "Witness set CBOR is required");
        }

        try {
            // Deserialize the unsigned transaction
            byte[] txBytes = HexUtil.decodeHexString(request.getTxCbor());
            Transaction unsignedTx = Transaction.deserialize(txBytes);

            // Deserialize the witness set
            byte[] witnessBytes = HexUtil.decodeHexString(request.getWitnessCbor());
            TransactionWitnessSet witnessSet = TransactionWitnessSet.deserialize(
                    (co.nstant.in.cbor.model.Map) CborSerializationUtil.deserialize(witnessBytes));

            // Create the signed transaction by combining body + witness set + auxiliary data
            Transaction signedTx = new Transaction();
            signedTx.setBody(unsignedTx.getBody());
            signedTx.setWitnessSet(witnessSet);
            if (unsignedTx.getAuxiliaryData() != null) {
                signedTx.setAuxiliaryData(unsignedTx.getAuxiliaryData());
            }
            signedTx.setValid(true);

            String signedTxCbor = signedTx.serializeToHex();

            log.info("Assembled signed transaction successfully");
            return AssembleTxResponse.success(signedTxCbor);

        } catch (Exception e) {
            log.error("Error assembling transaction", e);
            return AssembleTxResponse.error("ASSEMBLE_ERROR", e.getMessage() != null ? e.getMessage() : "Failed to assemble transaction");
        }
    }

    /**
     * Convert TransferAmount list to cardano-client-lib Amount list
     */
    private List<Amount> convertAmounts(List<TransferAmount> transferAmounts) {
        List<Amount> amounts = new ArrayList<>();

        for (TransferAmount ta : transferAmounts) {
            String unit = ta.getUnit();
            BigInteger quantity = new BigInteger(ta.getQuantity());

            if ("lovelace".equalsIgnoreCase(unit)) {
                amounts.add(Amount.lovelace(quantity));
            } else {
                // Native token: unit format is "{policyId}{assetNameHex}" (56 + variable length)
                // Use Amount constructor with full unit (policyId + assetNameHex)
                // This avoids double hex-encoding that Amount.asset() causes
                amounts.add(new Amount(unit, quantity));
            }
        }

        return amounts;
    }

    /**
     * Get the backend service (Yaci Store) for transaction building
     */
    private Optional<BackendService> getBackendService() {
        try {
            String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
            var clusterInfo = clusterService.getClusterInfo(clusterName);
            if (clusterInfo == null) {
                log.error("Cluster {} not found", clusterName);
                return Optional.empty();
            }

            var yaciStorePort = clusterInfo.getYaciStorePort();
            String backendUrl = "http://localhost:" + yaciStorePort + "/api/v1/";
            return Optional.of(new BFBackendService(backendUrl, "dummy_key"));
        } catch (Exception e) {
            log.error("Error getting backend service", e);
            return Optional.empty();
        }
    }
}
