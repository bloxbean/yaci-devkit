package com.bloxbean.cardano.yacicli.commands.localcluster;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressService;
import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.crypto.bip32.key.HdPublicKey;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.helper.AuxDataProviders;
import com.bloxbean.cardano.client.function.helper.InputBuilders;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.yaci.core.common.TxBodyType;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.*;
import com.bloxbean.cardano.yaci.core.protocol.localtx.model.TxSubmissionRequest;
import com.bloxbean.cardano.yacicli.common.Tuple;
import com.bloxbean.cardano.yacicli.txnprovider.LocalNodeClientFactory;
import com.bloxbean.cardano.yacicli.txnprovider.LocalProtocolSupplier;
import com.bloxbean.cardano.yacicli.txnprovider.LocalUtxoSupplier;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
import static com.bloxbean.cardano.yacicli.commands.localcluster.ClusterConfig.NODE_FOLDER_PREFIX;
import static com.bloxbean.cardano.yacicli.util.AdaConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.infoLabel;

//@Component
@Slf4j
public class LocalNodeService {
    private final static String UTXO_KEYS_FOLDER = "utxo-keys";
    private LocalNodeClientFactory localNodeClientFactory;
    private UtxoSupplier utxoSupplier;
    private ProtocolParamsSupplier protocolParamsSupplier;

    private ObjectMapper objectMapper = new ObjectMapper();
    private List<Tuple<VerificationKey, SecretKey>> utxoKeys;

    public LocalNodeService(Path clusterFolder, long protocolMagic, Consumer<String> writer) throws IOException {
        this.utxoKeys = new ArrayList<>();
        String socketFile = clusterFolder.resolve(NODE_FOLDER_PREFIX + 1).resolve("node.sock").toAbsolutePath().toString();
        this.localNodeClientFactory = new LocalNodeClientFactory(socketFile, protocolMagic, writer);
        this.utxoSupplier = new LocalUtxoSupplier(localNodeClientFactory.getLocalStateQueryClient());
        this.protocolParamsSupplier = new LocalProtocolSupplier(localNodeClientFactory.getLocalStateQueryClient());

        loadUtxoKeys(clusterFolder);
    }

    private void loadUtxoKeys(Path clusterFolder) throws IOException {
        utxoKeys.clear();
        Path utxoFolder = clusterFolder.resolve(UTXO_KEYS_FOLDER);

        for (int i = 1; i <= 3; i++) {
            Path skeyPath = utxoFolder.resolve("utxo" + i + ".skey");
            SecretKey skey = objectMapper.readValue(skeyPath.toFile(), SecretKey.class);

            Path vkeyPath = utxoFolder.resolve("utxo" + i + ".vkey");
            VerificationKey vkey = objectMapper.readValue(vkeyPath.toFile(), VerificationKey.class);
            utxoKeys.add(new Tuple<>(vkey, skey));
        }
        ;
    }

    public Map<String, List<Utxo>> getFundsAtGenesisKeys() {

        Map<String, List<Utxo>> utxosMap = new HashMap<>();
        for (Tuple<VerificationKey, SecretKey> tuple : utxoKeys) {
            HdPublicKey hdPublicKey = new HdPublicKey();
            hdPublicKey.setKeyData(tuple._1.getBytes());
            Address address = AddressService.getInstance().getEntAddress(hdPublicKey, Networks.testnet());

            Mono<UtxoByAddressQueryResult> utxosMono = localNodeClientFactory.getLocalStateQueryClient().executeQuery(new UtxoByAddressQuery(Era.Alonzo, address));
            UtxoByAddressQueryResult result = utxosMono.block(Duration.ofSeconds(10));
            utxosMap.put(address.toBech32(), result.getUtxoList());
        }

        return utxosMap;
    }

    public List<Utxo> getUtxos(String address) {
        Mono<UtxoByAddressQueryResult> utxosMono = localNodeClientFactory.getLocalStateQueryClient().executeQuery(new UtxoByAddressQuery(Era.Alonzo, new Address(address)));
        UtxoByAddressQueryResult result = utxosMono.block(Duration.ofSeconds(10));

        return result.getUtxoList();
    }

    public void topUp(String receiver, Double adaAmount, Consumer<String> writer) throws CborSerializationException {
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

        //Submit Tx using LocalStateQuery mini-protocol
        localNodeClientFactory.getTxSubmissionClient().submitTx(new TxSubmissionRequest(TxBodyType.BABBAGE, signedTransaction.serialize()));
        //localNodeClientFactory.shutdown();
    }

    public Tuple<Long, Point> getTip() {
        Mono<BlockHeightQueryResult> blockHeightMono = localNodeClientFactory.getLocalStateQueryClient().executeQuery(new BlockHeightQuery());
        BlockHeightQueryResult result = blockHeightMono.block(Duration.ofSeconds(5));

        Mono<ChainPointQueryResult> chainPointMono = localNodeClientFactory.getLocalStateQueryClient().executeQuery(new ChainPointQuery());
        ChainPointQueryResult chainPointQueryResult = chainPointMono.block(Duration.ofSeconds(5));

        return new Tuple<>(result.getBlockHeight(), chainPointQueryResult.getChainPoint());
    }

    public void shutdown() {
        try {
            localNodeClientFactory.shutdown();
        } catch (Exception e) {
            log.error("Shutdown error", e);
        }
    }

}
