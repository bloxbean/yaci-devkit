package com.bloxbean.cardano.yacicli.cip30.controller;

import com.bloxbean.cardano.yacicli.cip30.dto.*;
import com.bloxbean.cardano.yacicli.cip30.service.Cip30Service;
import com.bloxbean.cardano.yacicli.cip30.service.TransferService;
import com.bloxbean.cardano.yacicli.cip30.service.WalletAccountService;
import com.bloxbean.cardano.yacicli.cip30.service.WalletSigningService;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "/api/v1/wallet")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "CIP-30 Wallet API", description = "CIP-30 compatible wallet API for development")
@Slf4j
public class WalletApiController {
    private final WalletAccountService walletAccountService;
    private final WalletSigningService walletSigningService;
    private final TransferService transferService;
    private final Cip30Service cip30Service;
    private final ClusterService clusterService;

    @Operation(
            summary = "Health check",
            description = "Check if the wallet API is available"
    )
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "service", "yaci-devkit-wallet",
                "version", "1.0.0"
        ));
    }

    @Operation(
            summary = "Get active account",
            description = "Get the currently active CIP-30 wallet account used by the wallet SDK"
    )
    @GetMapping("/active-account")
    public ResponseEntity<Map<String, String>> getActiveAccount() {
        String activeId = walletAccountService.getActiveAccountId();
        var account = walletAccountService.getAccount(activeId);
        // Fall back to "0" if active account was deleted
        if (account.isEmpty()) {
            activeId = "0";
            walletAccountService.setActiveAccountId("0");
            account = walletAccountService.getAccount("0");
        }
        return ResponseEntity.ok(Map.of(
                "accountId", activeId,
                "address", account.map(WalletAccountDto::getAddress).orElse(""),
                "name", account.map(WalletAccountDto::getName).orElse("")
        ));
    }

    @Operation(
            summary = "Set active account",
            description = "Set the active CIP-30 wallet account used by the wallet SDK"
    )
    @PutMapping("/active-account")
    public ResponseEntity<Map<String, Object>> setActiveAccount(@RequestBody Map<String, String> request) {
        String accountId = request.get("accountId");
        if (accountId == null || accountId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "accountId is required"));
        }
        boolean success = walletAccountService.setActiveAccountId(accountId);
        if (success) {
            return ResponseEntity.ok(Map.of("accountId", accountId, "status", "active"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Account not found: " + accountId));
        }
    }

    @Operation(
            summary = "List all accounts",
            description = "Get all wallet accounts including 20 default pre-funded accounts and any custom imported accounts"
    )
    @GetMapping("/accounts")
    public ResponseEntity<WalletAccountsResponse> getAccounts() {
        var accounts = walletAccountService.getAllAccounts();
        return ResponseEntity.ok(WalletAccountsResponse.builder()
                .accounts(accounts)
                .build());
    }

    @Operation(
            summary = "Get account by ID",
            description = "Get a specific wallet account by its ID",
            parameters = {
                    @Parameter(name = "id", description = "Account ID (0-19 for default accounts, or custom-N for imported accounts)", required = true)
            }
    )
    @GetMapping("/accounts/{id}")
    public ResponseEntity<WalletAccountDto> getAccount(@PathVariable("id") String id) {
        Optional<WalletAccountDto> account = walletAccountService.getAccount(id);
        return account
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "Import custom account",
            description = "Import a new wallet account using a mnemonic phrase",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Import account request with mnemonic and name",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ImportAccountRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Account imported successfully"),
                    @ApiResponse(responseCode = "400", description = "Invalid mnemonic")
            }
    )
    @PostMapping("/accounts/import")
    public ResponseEntity<WalletAccountDto> importAccount(@Valid @RequestBody ImportAccountRequest request) {
        try {
            WalletAccountDto account = walletAccountService.importAccount(request.getMnemonic(), request.getName());
            return ResponseEntity.ok(account);
        } catch (Exception e) {
            log.error("Error importing account", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
            summary = "Delete custom account",
            description = "Delete a custom imported account (cannot delete default accounts)",
            parameters = {
                    @Parameter(name = "id", description = "Account ID to delete (must be a custom account)", required = true)
            }
    )
    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<Map<String, Object>> deleteAccount(@PathVariable("id") String id) {
        boolean deleted = walletAccountService.deleteAccount(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("status", "deleted", "id", id));
        } else {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Cannot delete account. Either it doesn't exist or it's a default account."
            ));
        }
    }

    @Operation(
            summary = "Sign transaction",
            description = "Sign a transaction with the specified account. Returns the witness set.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Sign transaction request with CBOR and account ID",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SignTxRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transaction signed successfully"),
                    @ApiResponse(responseCode = "400", description = "Signing failed")
            }
    )
    @PostMapping("/sign-tx")
    public ResponseEntity<?> signTransaction(@Valid @RequestBody SignTxRequest request) {
        Optional<SignTxResponse> response = walletSigningService.signTransaction(
                request.getTxCbor(),
                request.getAccountId()
        );

        return response
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body(Map.of(
                        "code", 1,
                        "info", "Failed to sign transaction. Account not found or signing error."
                )));
    }

    @Operation(
            summary = "Sign data",
            description = "Sign arbitrary data with the specified account (CIP-8 compatible)",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Sign data request with payload and account ID",
                    required = true,
                    content = @Content(schema = @Schema(implementation = SignDataRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Data signed successfully"),
                    @ApiResponse(responseCode = "400", description = "Signing failed")
            }
    )
    @PostMapping("/sign-data")
    public ResponseEntity<?> signData(@Valid @RequestBody SignDataRequest request) {
        Optional<SignDataResponse> response = walletSigningService.signData(
                request.getPayload(),
                request.getAddress(),
                request.getAccountId()
        );

        return response
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.badRequest().body(Map.of(
                        "code", 1,
                        "info", "Failed to sign data. Account not found or signing error."
                )));
    }

    @Operation(
            summary = "Transfer ADA and/or native tokens",
            description = "Transfer ADA and/or native tokens from an account to a receiver address. " +
                    "The backend automatically handles minimum UTXO requirements for native token transfers.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Transfer request with account ID, receiver address, and amounts",
                    required = true,
                    content = @Content(schema = @Schema(implementation = TransferRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transfer completed (check success field)"),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    @PostMapping("/transfer")
    public ResponseEntity<TransferResult> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResult result = transferService.transfer(request);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Build unsigned transaction",
            description = "Build an unsigned transaction for CIP-30 signing flow. " +
                    "Returns the unsigned transaction CBOR which can be signed via walletApi.signTx().",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Build transaction request with account ID, receiver address, and amounts",
                    required = true,
                    content = @Content(schema = @Schema(implementation = BuildTxRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transaction built (check success field)"),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    @PostMapping("/build-tx")
    public ResponseEntity<BuildTxResponse> buildTransaction(@RequestBody BuildTxRequest request) {
        BuildTxResponse result = transferService.buildTransaction(request);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Assemble signed transaction",
            description = "Combine an unsigned transaction with a CIP-30 witness set to produce a signed transaction. " +
                    "The signed transaction can then be submitted via walletApi.submitTx().",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Assemble request with unsigned tx CBOR and witness set CBOR",
                    required = true,
                    content = @Content(schema = @Schema(implementation = AssembleTxRequest.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transaction assembled (check success field)"),
                    @ApiResponse(responseCode = "400", description = "Invalid request")
            }
    )
    @PostMapping("/assemble-tx")
    public ResponseEntity<AssembleTxResponse> assembleTx(@RequestBody AssembleTxRequest request) {
        AssembleTxResponse result = transferService.assembleTx(request);
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Submit signed transaction (CIP-30 proxy)",
            description = "Proxy endpoint that forwards a signed transaction to Yaci Store for submission. " +
                    "Accepts transaction CBOR hex as a JSON body and forwards as application/cbor to the store.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "JSON object with txCbor field containing the signed transaction CBOR hex",
                    required = true
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Transaction submitted, returns tx hash"),
                    @ApiResponse(responseCode = "400", description = "Submission failed")
            }
    )
    @PostMapping("/submit-tx")
    public ResponseEntity<String> submitTransaction(@RequestBody Map<String, String> request) {
        String txCbor = request.get("txCbor");
        if (txCbor == null || txCbor.isBlank()) {
            return ResponseEntity.badRequest().body("txCbor is required");
        }

        try {
            String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
            var clusterInfo = clusterService.getClusterInfo(clusterName);
            if (clusterInfo == null) {
                return ResponseEntity.badRequest().body("Cluster not found");
            }

            var yaciStorePort = clusterInfo.getYaciStorePort();
            String submitUrl = "http://localhost:" + yaciStorePort + "/api/v1/tx/submit";

            byte[] txBytes = HexUtil.decodeHexString(txCbor);

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(submitUrl))
                    .header("Content-Type", "application/cbor")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(txBytes))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() >= 200 && httpResponse.statusCode() < 300) {
                return ResponseEntity.ok(httpResponse.body());
            } else {
                log.error("Transaction submission failed: {} - {}", httpResponse.statusCode(), httpResponse.body());
                return ResponseEntity.badRequest().body(httpResponse.body());
            }

        } catch (Exception e) {
            log.error("Error submitting transaction", e);
            return ResponseEntity.badRequest().body("Submit failed: " + e.getMessage());
        }
    }

    // ==================== CIP-30 CBOR Endpoints ====================

    @Operation(
            summary = "Get network ID (CIP-30)",
            description = "Returns the network ID. Yaci DevKit is always testnet (0)."
    )
    @GetMapping("/cip30/network-id")
    public ResponseEntity<Integer> getNetworkId() {
        return ResponseEntity.ok(cip30Service.getNetworkId());
    }

    @Operation(
            summary = "Get balance as CBOR hex (CIP-30)",
            description = "Returns the wallet balance as CIP-30 compliant CBOR hex of Value type",
            parameters = {
                    @Parameter(name = "accountId", description = "Account ID", required = true)
            }
    )
    @GetMapping(value = "/cip30/{accountId}/balance", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getBalanceCbor(@PathVariable String accountId) {
        String balance = cip30Service.getBalanceCbor(accountId);
        if (balance == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(balance);
    }

    @Operation(
            summary = "Get UTXOs as CBOR hex array (CIP-30)",
            description = "Returns UTXOs as CIP-30 compliant array of CBOR hex strings (TransactionUnspentOutput). " +
                    "If amount is specified and the wallet cannot satisfy it, returns null.",
            parameters = {
                    @Parameter(name = "accountId", description = "Account ID", required = true),
                    @Parameter(name = "amount", description = "Optional CBOR hex of Value type for minimum amount filter")
            }
    )
    @GetMapping("/cip30/{accountId}/utxos")
    public ResponseEntity<List<String>> getUtxosCbor(
            @PathVariable String accountId,
            @RequestParam(required = false) String amount) {
        // Pass amount parameter to service for filtering
        List<String> utxos = cip30Service.getUtxosCbor(accountId, amount);

        // CIP-30: return null if amount was specified but cannot be satisfied
        // (service already handles this and returns null in that case)
        return ResponseEntity.ok(utxos);
    }

    @Operation(
            summary = "Get used addresses as CBOR hex array (CIP-30)",
            description = "Returns used addresses as CIP-30 compliant array of address bytes hex",
            parameters = {
                    @Parameter(name = "accountId", description = "Account ID", required = true)
            }
    )
    @GetMapping("/cip30/{accountId}/used-addresses")
    public ResponseEntity<List<String>> getUsedAddressesCbor(@PathVariable String accountId) {
        List<String> addresses = cip30Service.getUsedAddressesCbor(accountId);
        return ResponseEntity.ok(addresses);
    }

    @Operation(
            summary = "Get change address as CBOR hex (CIP-30)",
            description = "Returns the change address as CIP-30 compliant address bytes hex",
            parameters = {
                    @Parameter(name = "accountId", description = "Account ID", required = true)
            }
    )
    @GetMapping(value = "/cip30/{accountId}/change-address", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getChangeAddressCbor(@PathVariable String accountId) {
        String address = cip30Service.getChangeAddressCbor(accountId);
        if (address == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(address);
    }

    @Operation(
            summary = "Get collateral UTXOs as CBOR hex array (CIP-30)",
            description = "Returns collateral UTXOs suitable for Plutus script execution. " +
                    "Returns a pure-ADA UTXO with at least 5 ADA, or null if none available.",
            parameters = {
                    @Parameter(name = "accountId", description = "Account ID", required = true)
            }
    )
    @GetMapping("/cip30/{accountId}/collateral")
    public ResponseEntity<List<String>> getCollateralCbor(@PathVariable String accountId) {
        List<String> collateral = cip30Service.getCollateralCbor(accountId);
        if (collateral == null) {
            return ResponseEntity.ok(null);
        }
        return ResponseEntity.ok(collateral);
    }

    @Operation(
            summary = "Get reward addresses as CBOR hex array (CIP-30)",
            description = "Returns reward (stake) addresses as CIP-30 compliant array of address bytes hex",
            parameters = {
                    @Parameter(name = "accountId", description = "Account ID", required = true)
            }
    )
    @GetMapping("/cip30/{accountId}/reward-addresses")
    public ResponseEntity<List<String>> getRewardAddressesCbor(@PathVariable String accountId) {
        List<String> addresses = cip30Service.getRewardAddressesCbor(accountId);
        return ResponseEntity.ok(addresses);
    }

    // ==================== Decode Endpoints (for demo/display) ====================

    @Operation(
            summary = "Decode CBOR address to bech32",
            description = "Decode a CIP-30 CBOR hex address back to bech32 format for display purposes",
            parameters = {
                    @Parameter(name = "cborHex", description = "Address bytes as hex string", required = true)
            }
    )
    @GetMapping(value = "/decode/address", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> decodeAddress(@RequestParam String cborHex) {
        String bech32 = cip30Service.decodeAddressFromCbor(cborHex);
        if (bech32 == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(bech32);
    }

    @Operation(
            summary = "Decode CBOR value to JSON",
            description = "Decode a CIP-30 CBOR hex Value to human-readable JSON format for display purposes",
            parameters = {
                    @Parameter(name = "cborHex", description = "Value CBOR as hex string", required = true)
            }
    )
    @GetMapping("/decode/value")
    public ResponseEntity<Map<String, Object>> decodeValue(@RequestParam String cborHex) {
        Map<String, Object> decoded = cip30Service.decodeValueFromCbor(cborHex);
        if (decoded.containsKey("error")) {
            return ResponseEntity.badRequest().body(decoded);
        }
        return ResponseEntity.ok(decoded);
    }
}
