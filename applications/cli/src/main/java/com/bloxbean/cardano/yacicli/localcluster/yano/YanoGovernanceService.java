package com.bloxbean.cardano.yacicli.localcluster.yano;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.crypto.bip32.key.HdPublicKey;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.api.impl.StaticTransactionEvaluator;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.ScriptTx;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.ProtocolParamUpdate;
import com.bloxbean.cardano.client.transaction.spec.governance.*;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.common.GenesisUtil;
import com.bloxbean.cardano.yacicli.common.Tuple;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

/**
 * Submits governance proposals (Plutus cost model updates) via Yano's Blockfrost-compatible HTTP API
 * during companion mode bootstrap. Uses BFBackendService + QuickTxBuilder, same pattern as PeerService.
 */
@Component
@Slf4j
public class YanoGovernanceService {
    private final YanoBootstrapService yanoBootstrapService;
    private final Path plutusCostModelsBasePath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public YanoGovernanceService(YanoBootstrapService yanoBootstrapService,
                                 @Value("${yaci.cli.plutus-costmodels-path:./config}") String plutusCostModelsBasePath) {
        this.yanoBootstrapService = yanoBootstrapService;
        this.plutusCostModelsBasePath = Paths.get(plutusCostModelsBasePath);
    }

    /**
     * Submit cost model governance proposals to Yano via its Blockfrost-compatible HTTP API.
     * Registers stake + DRep in one TX, submits ParameterChangeAction, and votes YES.
     * All done before Yano catches up to wall clock, so the proposal is enacted by epoch 2.
     */
    public boolean submitCostModelGovernance(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) {
        try {
            int httpPort = clusterInfo.getYanoHttpPort();
            String yanoUrl = "http://localhost:" + httpPort + "/api/v1/";
            BackendService backendService = new BFBackendService(yanoUrl, "dummy_key");
            UtxoSupplier utxoSupplier = new DefaultUtxoSupplier(backendService.getUtxoService());
            QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

            // 1. Load UTXO keys
            List<Tuple<VerificationKey, SecretKey>> utxoKeys = GenesisUtil.loadUtxoKeys(clusterFolder);

            // 2. Find a funded key with sufficient balance (>2000 ADA for deposits + fees)
            BigInteger minBalance = BigInteger.valueOf(2_000_000_000L);
            String senderAddress = null;
            SecretKey senderSkey = null;

            for (int i = 0; i < utxoKeys.size(); i++) {
                Tuple<VerificationKey, SecretKey> tuple = utxoKeys.get(i);
                HdPublicKey hdPublicKey = new HdPublicKey();
                hdPublicKey.setKeyData(tuple._1.getBytes());
                Address address = AddressProvider.getEntAddress(hdPublicKey, Networks.testnet());
                String addr = address.toBech32();

                List<Utxo> utxos = utxoSupplier.getAll(addr);
                Optional<Amount> sufficient = utxos.stream()
                        .flatMap(utxo -> utxo.getAmount().stream())
                        .filter(amt -> LOVELACE.equals(amt.getUnit()) && amt.getQuantity().compareTo(minBalance) > 0)
                        .findAny();

                if (sufficient.isPresent()) {
                    senderAddress = addr;
                    senderSkey = tuple._2;
                    break;
                }
            }

            if (senderAddress == null) {
                writer.accept(error("No funded UTXO key found for governance proposal via Yano"));
                return false;
            }

            // 3. Load cost models
            Path costModelsFile = GenesisUtil.resolveCostModelsFile(plutusCostModelsBasePath, clusterInfo.getProtocolMajorVer());
            Map<String, long[]> costModelMap = GenesisUtil.loadCostModels(costModelsFile);
            writer.accept(info("Using Plutus cost models file: %s", costModelsFile.getFileName()));

            // 4. Build ProtocolParamUpdate with cost models
            CostMdls costMdls = new CostMdls();
            Map<String, Language> languageMap = Map.of(
                    "PlutusV1", Language.PLUTUS_V1,
                    "PlutusV2", Language.PLUTUS_V2,
                    "PlutusV3", Language.PLUTUS_V3
            );
            for (Map.Entry<String, long[]> entry : costModelMap.entrySet()) {
                Language language = languageMap.get(entry.getKey());
                if (language != null) {
                    costMdls.add(new CostModel(language, entry.getValue()));
                }
            }

            ProtocolParamUpdate protocolParamUpdate = ProtocolParamUpdate.builder()
                    .costModels(costMdls)
                    .build();

            // 5. Build always-true PlutusV3 guardrail script
            PlutusV3Script guardrailScript = PlutusV3Script.builder()
                    .type("PlutusScriptV3")
                    .cborHex("46450101002499")
                    .build();

            // 6. Build ParameterChangeAction
            ParameterChangeAction paramChangeAction = ParameterChangeAction.builder()
                    .prevGovActionId(null)
                    .protocolParamUpdate(protocolParamUpdate)
                    .policyHash(guardrailScript.getScriptHash())
                    .build();

            // 7. Build anchor
            Anchor anchor = Anchor.builder()
                    .anchorUrl("https://devkit.yaci.xyz/plutus-costmodel-update.json")
                    .anchorDataHash(new byte[32])
                    .build();

            // 8. Build reward address and DRep credential from sender key
            var senderVkey = KeyGenUtil.getPublicKeyFromPrivateKey(senderSkey);
            HdPublicKey hdPublicKey = new HdPublicKey();
            hdPublicKey.setKeyData(senderVkey.getBytes());
            Address rewardAddr = AddressProvider.getRewardAddress(hdPublicKey, Networks.testnet());
            String rewardAccount = rewardAddr.toBech32();

            byte[] credHash = rewardAddr.getDelegationCredentialHash()
                    .orElseThrow(() -> new RuntimeException("Failed to get delegation credential hash"));
            String credHashHex = HexUtil.encodeHexString(credHash);
            Credential drepCredential = Credential.fromKey(credHashHex);

            // --- TX 1: Register stake + DRep + delegate voting power (all in one TX) ---
            // Combining avoids cross-TX dependency (DRep registration requires registered stake credential,
            // which is satisfied within the same TX since Cardano processes certificates sequentially)
            writer.accept("Registering stake credential, DRep, and delegating voting power...");
            Tx combinedRegTx = new Tx()
                    .registerStakeAddress(rewardAddr)
                    .registerDRep(drepCredential)
                    .delegateVotingPowerTo(rewardAddr, DRep.addrKeyHash(credHashHex))
                    .from(senderAddress);
            Result<String> regResult = quickTxBuilder.compose(combinedRegTx)
                    .withSigner(SignerProviders.signerFrom(senderSkey))
                    .complete();

            if (!regResult.isSuccessful()) {
                writer.accept(error("Stake/DRep registration failed: " + regResult.getResponse()));
                return false;
            }
            writer.accept(success("Stake + DRep registered, voting power delegated. Tx# : " + regResult.getValue()));

            // Wait for TX to be included in a block (not just mempool)
            if (!waitForNextBlock(httpPort, regResult.getValue(), utxoSupplier, senderAddress, writer)) {
                writer.accept(error("Registration TX not confirmed in a block within timeout"));
                return false;
            }

            // --- TX 2: Submit governance proposal (ScriptTx with guardrail script) ---
            writer.accept("Submitting Plutus cost models governance proposal...");
            ScriptTx scriptTx = new ScriptTx()
                    .createProposal(paramChangeAction, rewardAccount, anchor, PlutusData.unit())
                    .attachProposingValidator(guardrailScript);

            var staticEvaluator = new StaticTransactionEvaluator(
                    List.of(ExUnits.builder()
                            .mem(BigInteger.valueOf(500000))
                            .steps(BigInteger.valueOf(200000000))
                            .build()));

            Result<String> proposalResult = quickTxBuilder.compose(scriptTx)
                    .feePayer(senderAddress)
                    .withSigner(SignerProviders.signerFrom(senderSkey))
                    .withTxEvaluator(staticEvaluator)
                    .complete();

            if (!proposalResult.isSuccessful()) {
                writer.accept(error("Plutus cost models governance proposal failed: " + proposalResult.getResponse()));
                return false;
            }
            writer.accept(success("Plutus cost models governance proposal submitted. Tx# : " + proposalResult.getValue()));

            // Wait for proposal TX to be in a block before voting
            if (!waitForNextBlock(httpPort, proposalResult.getValue(), utxoSupplier, senderAddress, writer)) {
                writer.accept(error("Proposal TX not confirmed in a block within timeout"));
                return false;
            }

            // --- TX 3: Vote YES on the proposal ---
            writer.accept("Voting YES on Plutus cost models proposal...");
            Voter voter = Voter.builder()
                    .type(VoterType.DREP_KEY_HASH)
                    .credential(drepCredential)
                    .build();
            GovActionId govActionId = GovActionId.builder()
                    .transactionId(proposalResult.getValue())
                    .govActionIndex(0)
                    .build();

            Tx voteTx = new Tx()
                    .createVote(voter, govActionId, Vote.YES)
                    .from(senderAddress);
            Result<String> voteResult = quickTxBuilder.compose(voteTx)
                    .withSigner(SignerProviders.signerFrom(senderSkey))
                    .complete();

            if (!voteResult.isSuccessful()) {
                writer.accept(error("DRep vote on proposal failed: " + voteResult.getResponse()));
                return false;
            }
            writer.accept(success("DRep voted YES on Plutus cost models proposal. Tx# : " + voteResult.getValue()));

            // Wait for vote TX to be confirmed in a block BEFORE catchUpToWallClock.
            // All governance TXs must be in epoch 0 blocks so they're enacted by epoch 2.
            if (!waitForNextBlock(httpPort, voteResult.getValue(), utxoSupplier, senderAddress, writer)) {
                writer.accept(warn("Vote TX not confirmed in a block within timeout"));
            }

            writer.accept(info("Plutus cost models will be enacted at the next epoch boundary."));
            return true;

        } catch (Exception e) {
            log.error("Failed to submit governance proposals via Yano", e);
            writer.accept(error("Governance proposal via Yano failed: " + e.getMessage()));
            return false;
        }
    }

    /**
     * Wait for a transaction to be confirmed in a block by polling the chain tip.
     * Yano in past-time-travel mode produces blocks rapidly, so this should resolve quickly.
     */
    private boolean waitForNextBlock(int httpPort, String txHash, UtxoSupplier utxoSupplier,
                                     String address, Consumer<String> writer) {
        // Get current tip height
        long startHeight = getBlockHeight(httpPort);

        for (int i = 0; i < 30; i++) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            // Check if tip advanced (a new block was produced)
            long currentHeight = getBlockHeight(httpPort);
            if (currentHeight > startHeight) {
                // Verify the TX is in the UTXO set (confirmed, not just mempool)
                boolean found = utxoSupplier.getAll(address).stream()
                        .anyMatch(utxo -> utxo.getTxHash().equals(txHash));
                if (found) {
                    writer.accept(info("TX confirmed at block height %d", currentHeight));
                    return true;
                }
            }
        }

        writer.accept(warn("TX %s not confirmed within 30s timeout", txHash));
        return false;
    }

    private long getBlockHeight(int httpPort) {
        try {
            JsonNode tip = yanoBootstrapService.getChainTip(httpPort);
            if (tip != null && tip.has("height")) {
                return tip.get("height").asLong(-1);
            }
        } catch (Exception e) {
            log.debug("Error getting chain tip: {}", e.getMessage());
        }
        return -1;
    }

}
