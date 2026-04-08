package com.bloxbean.cardano.yacicli.localcluster.service;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.crypto.bip32.key.HdPublicKey;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.helper.AuxDataProviders;
import com.bloxbean.cardano.client.function.helper.InputBuilders;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.api.impl.StaticTransactionEvaluator;
import com.bloxbean.cardano.client.plutus.spec.CostMdls;
import com.bloxbean.cardano.client.plutus.spec.CostModel;
import com.bloxbean.cardano.client.plutus.spec.ExUnits;
import com.bloxbean.cardano.client.plutus.spec.Language;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusV3Script;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.ScriptTx;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.ProtocolParamUpdate;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.governance.Anchor;
import com.bloxbean.cardano.client.transaction.spec.governance.DRep;
import com.bloxbean.cardano.client.transaction.spec.governance.Vote;
import com.bloxbean.cardano.client.transaction.spec.governance.Voter;
import com.bloxbean.cardano.client.transaction.spec.governance.VoterType;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.GovActionId;
import com.bloxbean.cardano.client.transaction.spec.governance.actions.ParameterChangeAction;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import com.bloxbean.cardano.client.transaction.util.TransactionUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.bloxbean.cardano.yaci.core.common.TxBodyType;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.*;
import com.bloxbean.cardano.yaci.core.protocol.localtx.LocalTxSubmissionListener;
import com.bloxbean.cardano.yaci.core.protocol.localtx.messages.MsgAcceptTx;
import com.bloxbean.cardano.yaci.core.protocol.localtx.messages.MsgRejectTx;
import com.bloxbean.cardano.yaci.core.protocol.localtx.model.TxSubmissionRequest;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yacicli.localcluster.common.GenesisUtil;
import com.bloxbean.cardano.yacicli.localcluster.common.LocalClientProviderHelper;
import com.bloxbean.cardano.yacicli.common.Tuple;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.function.helper.BalanceTxBuilders.balanceTx;
import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;
import static com.bloxbean.cardano.yacicli.util.AdaConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Slf4j
public class LocalNodeService {
    private final static String UTXO_KEYS_FOLDER = "utxo-keys";
    private LocalClientProvider localClientProvider;
    private UtxoSupplier utxoSupplier;
    private ProtocolParamsSupplier protocolParamsSupplier;

    private ObjectMapper objectMapper = new ObjectMapper();
    private List<Tuple<VerificationKey, SecretKey>> utxoKeys;

    private Era era;

    public LocalNodeService(Path clusterFolder, Era era, LocalClientProviderHelper localQueryClientUtil, Consumer<String> writer) throws Exception {
        this(clusterFolder, era, localQueryClientUtil, null, writer);
    }

    public LocalNodeService(Path clusterFolder, Era era, LocalClientProviderHelper localQueryClientUtil, String nodeName, Consumer<String> writer) throws Exception {
        this.utxoKeys = new ArrayList<>();
        this.localClientProvider = localQueryClientUtil.getLocalClientProvider(nodeName);
        this.era = era;
        this.localClientProvider.addTxSubmissionListener(new LocalTxSubmissionListener() {
            @Override
            public void txAccepted(TxSubmissionRequest txSubmissionRequest, MsgAcceptTx msgAcceptTx) {
            }

            @Override
            public void txRejected(TxSubmissionRequest txSubmissionRequest, MsgRejectTx msgRejectTx) {
            }
        });
        this.localClientProvider.start();

        this.utxoSupplier = new LocalUtxoSupplier(localClientProvider.getLocalStateQueryClient(), era);
        this.protocolParamsSupplier = new LocalProtocolParamSupplier(localClientProvider.getLocalStateQueryClient(), era);

        loadUtxoKeys(clusterFolder);
    }

    private void loadUtxoKeys(Path clusterFolder) throws IOException {
        utxoKeys.clear();
        utxoKeys.addAll(GenesisUtil.loadUtxoKeys(clusterFolder));
    }

    public Map<String, List<Utxo>> getFundsAtGenesisKeys() {

        Map<String, List<Utxo>> utxosMap = new HashMap<>();
        for (Tuple<VerificationKey, SecretKey> tuple : utxoKeys) {
            HdPublicKey hdPublicKey = new HdPublicKey();
            hdPublicKey.setKeyData(tuple._1.getBytes());
            Address address = AddressProvider.getEntAddress(hdPublicKey, Networks.testnet());

            Mono<UtxoByAddressQueryResult> utxosMono = localClientProvider.getLocalStateQueryClient().executeQuery(new UtxoByAddressQuery(era, address));
            UtxoByAddressQueryResult result = utxosMono.block(Duration.ofSeconds(10));
            utxosMap.put(address.toBech32(), result.getUtxoList());
        }

        return utxosMap;
    }

    public List<Utxo> getUtxos(String address) {
        Mono<UtxoByAddressQueryResult> utxosMono = localClientProvider.getLocalStateQueryClient().executeQuery(new UtxoByAddressQuery(era, new Address(address)));
        UtxoByAddressQueryResult result = utxosMono.block(Duration.ofSeconds(10));

        //TODO -- Replace "." in the unit name. As yaci sends "." in unit name, but cardano-client-lib's Amount needs without "."
        for (Utxo utxo: result.getUtxoList()) {
            for (Amount amount: utxo.getAmount()) {
                if (amount.getUnit() != null && amount.getUnit().contains("."))
                    amount.setUnit(amount.getUnit().replace(".", ""));
            }

            //Update dataHash if dataHash is empty but inline datum has value
            String dataHash = utxo.getDataHash();
            try {
                if (!StringUtils.hasText(dataHash) && StringUtils.hasText(utxo.getInlineDatum())) {
                    byte[] inlineDatumBytes = HexUtil.decodeHexString(utxo.getInlineDatum());
                    dataHash = PlutusData.deserialize(inlineDatumBytes).getDatumHash();
                    utxo.setDataHash(dataHash);
                }
            } catch (Exception e) {
                log.error("Invalid inline datum found in utxo tx : {}, index: {}, inline_datum: {}", utxo.getTxHash(), utxo.getOutputIndex(), utxo.getInlineDatum());
            }
        }

        return result.getUtxoList();
    }

    public boolean topUp(String receiver, Double adaAmount, Consumer<String> writer) throws CborSerializationException {
        //Find address and utxos with required balance
        BigInteger amount = adaToLovelace(adaAmount);
        String senderAddress = null;
        SecretKey senderSkey = null;

        int i = 0;
        for (Map.Entry<String, List<Utxo>> entry : getFundsAtGenesisKeys().entrySet()) {
            String address = entry.getKey();
            Optional<Amount> amountOptional = entry.getValue().stream()
                    .flatMap(utxo -> utxo.getAmount().stream())
                    .filter(amt -> LOVELACE.equals(amt.getUnit()) && amt.getQuantity().compareTo(amount) == 1)
                    .findAny();
            if (amountOptional.isPresent()) {
                senderAddress = address;
                senderSkey = utxoKeys.get(i)._2;
                break;
            }

            i++;
        }

        Output output = Output.builder()
                .address(receiver)
                .assetName(LOVELACE)
                .qty(amount)
                .build();

        TxBuilder txBuilder = output.outputBuilder()
                .buildInputs(InputBuilders.createFromSender(senderAddress, senderAddress))
                .andThen(AuxDataProviders.metadataProvider(MessageMetadata.create().add("Topup Fund")))
                .andThen(balanceTx(senderAddress, 1));

        Transaction signedTransaction = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
                .buildAndSign(txBuilder, signerFrom(senderSkey));

        writer.accept(infoLabel("Txn Cbor", signedTransaction.serializeToHex()));
        String txHash = TransactionUtil.getTxHash(signedTransaction);

        //Submit Tx using LocalStateQuery mini-protocol
        TxSubmissionRequest txSubmissionRequest = new TxSubmissionRequest(TxBodyType.CONWAY, signedTransaction.serialize());
        var mono = localClientProvider.getTxSubmissionClient().submitTx(txSubmissionRequest);
        var txResult = mono.block(Duration.ofSeconds(2));
        if (!txResult.isAccepted()) {
            writer.accept(error("Transaction submission failed : " + txResult.getErrorCbor()));
            return false;
        } else {
            writer.accept(success("Transaction submitted successfully"));
        }

        waitForTx(receiver, txHash, writer);

        return true;
    }

    public Tuple<Long, Point> getTip() {
        Mono<BlockHeightQueryResult> blockHeightMono = localClientProvider.getLocalStateQueryClient().executeQuery(new BlockHeightQuery());
        BlockHeightQueryResult result = blockHeightMono.block(Duration.ofSeconds(5));

        Mono<ChainPointQueryResult> chainPointMono = localClientProvider.getLocalStateQueryClient().executeQuery(new ChainPointQuery());
        ChainPointQueryResult chainPointQueryResult = chainPointMono.block(Duration.ofSeconds(5));

        return new Tuple<>(result.getBlockHeight(), chainPointQueryResult.getChainPoint());
    }

    public boolean mint(String assetName, BigInteger quntity, String receiver, Consumer<String> writer) throws CborSerializationException {
        String senderAddress = null;
        SecretKey senderSkey = null;

        int i = 0;
        for (Map.Entry<String, List<Utxo>> entry : getFundsAtGenesisKeys().entrySet()) {
            String address = entry.getKey();
            Optional<Amount> amountOptional = entry.getValue().stream()
                    .flatMap(utxo -> utxo.getAmount().stream())
                    .filter(amt -> LOVELACE.equals(amt.getUnit()) && amt.getQuantity().compareTo(quntity) == 1)
                    .findAny();
            if (amountOptional.isPresent()) {
                senderAddress = address;
                senderSkey = utxoKeys.get(i)._2;
                break;
            }

            i++;
        }

        var verificationKey = KeyGenUtil.getPublicKeyFromPrivateKey(senderSkey);
        var scriptPubkey = ScriptPubkey.create(verificationKey);

        var transactionProcessor = new LocalTransactionProcessor(localClientProvider.getTxSubmissionClient(), TxBodyType.CONWAY);

        Tx tx = new Tx()
                .mintAssets(scriptPubkey, Asset.builder()
                        .name(assetName)
                        .value(quntity)
                        .build(), receiver)
                .payToAddress(senderAddress, Amount.ada(1))
                .from(senderAddress);

        var result = new QuickTxBuilder(utxoSupplier, protocolParamsSupplier, transactionProcessor)
                .compose(tx)
                .withSigner(SignerProviders.signerFrom(senderSkey))
                .complete();

        if (!result.isSuccessful()) {
            writer.accept(error("Transaction submission failed : " + result.getResponse()));
            return false;
        } else {
            writer.accept(success("Transaction submitted successfully"));
            writer.accept(info("Txn# : " + result.getValue()));
            return waitForTx(receiver, result.getValue(), writer);
        }
    }

    public boolean updateCostModels(Path costModelsFile, Consumer<String> writer) {
        try {
            // 1. Find a funded UTXO key (same pattern as topUp)
            BigInteger minBalance = BigInteger.valueOf(2_000_000_000L); // 2000 ADA to cover deposit + fees
            String senderAddress = null;
            SecretKey senderSkey = null;

            int i = 0;
            for (Map.Entry<String, List<Utxo>> entry : getFundsAtGenesisKeys().entrySet()) {
                String address = entry.getKey();
                Optional<Amount> amountOptional = entry.getValue().stream()
                        .flatMap(utxo -> utxo.getAmount().stream())
                        .filter(amt -> LOVELACE.equals(amt.getUnit()) && amt.getQuantity().compareTo(minBalance) > 0)
                        .findAny();
                if (amountOptional.isPresent()) {
                    senderAddress = address;
                    senderSkey = utxoKeys.get(i)._2;
                    break;
                }
                i++;
            }

            if (senderAddress == null) {
                writer.accept(error("No funded UTXO key found for governance proposal"));
                return false;
            }

            // 2. Load cost models from external config file
            Map<String, long[]> costModelMap = loadCostModels(costModelsFile);

            // 3. Build the ProtocolParamUpdate with all available cost models
            CostMdls costMdls = new CostMdls();
            Map<String, Language> languageMap = Map.of(
                    "PlutusV1", Language.PLUTUS_V1,
                    "PlutusV2", Language.PLUTUS_V2,
                    "PlutusV3", Language.PLUTUS_V3
            );
            for (Map.Entry<String, long[]> entry2 : costModelMap.entrySet()) {
                Language language = languageMap.get(entry2.getKey());
                if (language != null) {
                    costMdls.add(new CostModel(language, entry2.getValue()));
                } else {
                    writer.accept(error("Unknown Plutus version in cost models file: " + entry2.getKey()));
                }
            }

            ProtocolParamUpdate protocolParamUpdate = ProtocolParamUpdate.builder()
                    .costModels(costMdls)
                    .build();

            // 4. Build the always-true PlutusV3 guardrail script
            // CBOR hex: 46450101002499 -> script hash: 186e32faa80a26810392fda6d559c7ed4721a65ce1c9d4ef3e1c87b4
            PlutusV3Script guardrailScript = PlutusV3Script.builder()
                    .type("PlutusScriptV3")
                    .cborHex("46450101002499")
                    .build();

            // 5. Build the ParameterChangeAction
            ParameterChangeAction paramChangeAction = ParameterChangeAction.builder()
                    .prevGovActionId(null)
                    .protocolParamUpdate(protocolParamUpdate)
                    .policyHash(guardrailScript.getScriptHash())
                    .build();

            // 6. Build dummy anchor
            Anchor anchor = Anchor.builder()
                    .anchorUrl("https://devkit.yaci.xyz/plutus-costmodel-update.json")
                    .anchorDataHash(new byte[32]) // zeroed hash for devnet
                    .build();

            // 7. Build a reward address from the sender's verification key for deposit return
            var senderVkey = KeyGenUtil.getPublicKeyFromPrivateKey(senderSkey);
            HdPublicKey hdPublicKey = new HdPublicKey();
            hdPublicKey.setKeyData(senderVkey.getBytes());
            Address rewardAddr = AddressProvider.getRewardAddress(hdPublicKey, Networks.testnet());
            String rewardAccount = rewardAddr.toBech32();

            // 7a. Register stake credential for the reward address
            writer.accept("Registering stake credential for governance proposal...");
            var regProcessor = new LocalTransactionProcessor(localClientProvider.getTxSubmissionClient(), TxBodyType.CONWAY);
            Tx regTx = new Tx()
                    .registerStakeAddress(rewardAddr)
                    .from(senderAddress);
            Result<String> regResult = new QuickTxBuilder(utxoSupplier, protocolParamsSupplier, regProcessor)
                    .compose(regTx)
                    .withSigner(SignerProviders.signerFrom(senderSkey))
                    .complete();

            if (!regResult.isSuccessful()) {
                writer.accept(error("Stake credential registration failed: " + regResult.getResponse()));
                return false;
            }
            writer.accept(success("Stake credential registered. Tx# : " + regResult.getValue()));
            waitForTx(senderAddress, regResult.getValue(), writer);

            // 7b. Register DRep and delegate voting power for governance voting
            writer.accept("Registering DRep and delegating voting power...");
            byte[] credHash = rewardAddr.getDelegationCredentialHash()
                    .orElseThrow(() -> new RuntimeException("Failed to get delegation credential hash"));
            String credHashHex = HexUtil.encodeHexString(credHash);
            Credential drepCredential = Credential.fromKey(credHashHex);

            var drepProcessor = new LocalTransactionProcessor(localClientProvider.getTxSubmissionClient(), TxBodyType.CONWAY);
            Tx drepTx = new Tx()
                    .registerDRep(drepCredential)
                    .delegateVotingPowerTo(rewardAddr, DRep.addrKeyHash(credHashHex))
                    .from(senderAddress);
            Result<String> drepResult = new QuickTxBuilder(utxoSupplier, protocolParamsSupplier, drepProcessor)
                    .compose(drepTx)
                    .withSigner(SignerProviders.signerFrom(senderSkey))
                    .complete();

            if (!drepResult.isSuccessful()) {
                writer.accept(error("DRep registration failed: " + drepResult.getResponse()));
                return false;
            }
            writer.accept(success("DRep registered and voting power delegated. Tx# : " + drepResult.getValue()));
            waitForTx(senderAddress, drepResult.getValue(), writer);

            // 8. Build and submit the governance proposal using ScriptTx
            var transactionProcessor = new LocalTransactionProcessor(localClientProvider.getTxSubmissionClient(), TxBodyType.CONWAY);

            ScriptTx scriptTx = new ScriptTx()
                    .createProposal(paramChangeAction, rewardAccount, anchor, PlutusData.unit())
                    .attachProposingValidator(guardrailScript);

            // Use StaticTransactionEvaluator with fixed ex units for the always-true guardrail script
            // since LocalTransactionProcessor doesn't support evaluateTx
            var staticEvaluator = new StaticTransactionEvaluator(
                    List.of(ExUnits.builder()
                            .mem(BigInteger.valueOf(500000))
                            .steps(BigInteger.valueOf(200000000))
                            .build()));

            Result<String> result = new QuickTxBuilder(utxoSupplier, protocolParamsSupplier, transactionProcessor)
                    .compose(scriptTx)
                    .feePayer(senderAddress)
                    .withSigner(SignerProviders.signerFrom(senderSkey))
                    .withTxEvaluator(staticEvaluator)
                    .complete();

            if (!result.isSuccessful()) {
                writer.accept(error("Plutus cost models governance proposal failed: " + result.getResponse()));
                return false;
            }

            writer.accept(success("Plutus cost models governance proposal submitted. Tx# : " + result.getValue()));

            // 9. Vote YES on the proposal as DRep
            writer.accept("Voting YES on Plutus cost models proposal...");
            waitForTx(senderAddress, result.getValue(), writer);

            Voter voter = Voter.builder()
                    .type(VoterType.DREP_KEY_HASH)
                    .credential(drepCredential)
                    .build();
            GovActionId govActionId = GovActionId.builder()
                    .transactionId(result.getValue())
                    .govActionIndex(0)
                    .build();

            var voteProcessor = new LocalTransactionProcessor(localClientProvider.getTxSubmissionClient(), TxBodyType.CONWAY);
            Tx voteTx = new Tx()
                    .createVote(voter, govActionId, Vote.YES)
                    .from(senderAddress);
            Result<String> voteResult = new QuickTxBuilder(utxoSupplier, protocolParamsSupplier, voteProcessor)
                    .compose(voteTx)
                    .withSigner(SignerProviders.signerFrom(senderSkey))
                    .complete();

            if (!voteResult.isSuccessful()) {
                writer.accept(error("DRep vote on proposal failed: " + voteResult.getResponse()));
                return false;
            }
            writer.accept(success("DRep voted YES on Plutus cost models proposal. Tx# : " + voteResult.getValue()));
            writer.accept(info("Plutus cost models will be enacted at the next epoch boundary."));
            return true;

        } catch (Exception e) {
            log.error("Failed to submit Plutus cost models governance proposal", e);
            writer.accept(error("Plutus cost models update failed: " + e.getMessage()));
            return false;
        }
    }

    private Map<String, long[]> loadCostModels(Path costModelsFile) throws IOException {
        return GenesisUtil.loadCostModels(costModelsFile);
    }

    private boolean waitForTx(String receiver, String txHash, Consumer<String> writer) {
        int count = 0;
        while (true) {
            writer.accept("Waiting for tx to be included in block...");
            count++;
            if (count > 10)
                break;
            boolean found = utxoSupplier.getAll(receiver)
                    .stream()
                    .filter(utxo -> utxo.getTxHash().equals(txHash))
                    .findAny()
                    .isPresent();
            if (found) {
                writer.accept(infoLabel("Txn# : ", txHash));
                return true;
            } else {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        return false;
    }

    public void shutdown() {
        try {
            localClientProvider.shutdown();
        } catch (Exception e) {
            log.error("Shutdown error", e);
        }
    }

}
