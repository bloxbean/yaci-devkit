package com.bloxbean.cardano.yacicli.localcluster.service;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.api.DefaultProtocolParamsSupplier;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.backend.model.EpochContent;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.crypto.bip32.key.HdPublicKey;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.common.GenesisUtil;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoBootstrapService;
import com.bloxbean.cardano.yacicli.common.Tuple;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.yacicli.util.AdaConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

/**
 * Provides LocalNodeService-equivalent operations using Yano's Blockfrost-compatible HTTP API.
 * Used in yano-only mode where there is no Haskell node or N2C socket.
 */
@Component
@Slf4j
public class YanoHttpNodeService {
    private final ClusterService clusterService;
    private final YanoBootstrapService yanoBootstrapService;

    public YanoHttpNodeService(ClusterService clusterService, YanoBootstrapService yanoBootstrapService) {
        this.clusterService = clusterService;
        this.yanoBootstrapService = yanoBootstrapService;
    }

    private BackendService getBackendService(String clusterName) throws IOException {
        ClusterInfo info = clusterService.getClusterInfo(clusterName);
        String url = "http://localhost:" + info.getYanoHttpPort() + "/api/v1/";
        return new BFBackendService(url, "dummy_key");
    }

    private int getYanoHttpPort(String clusterName) throws IOException {
        return clusterService.getClusterInfo(clusterName).getYanoHttpPort();
    }

    public List<Utxo> getUtxos(String clusterName, String address) {
        try {
            BackendService backendService = getBackendService(clusterName);
            UtxoSupplier utxoSupplier = new DefaultUtxoSupplier(backendService.getUtxoService());
            return utxoSupplier.getAll(address);
        } catch (Exception e) {
            log.error("Error getting UTXOs via Yano HTTP", e);
            return Collections.emptyList();
        }
    }

    public Map<String, List<Utxo>> getFundsAtGenesisKeys(String clusterName) {
        try {
            Path clusterFolder = clusterService.getClusterFolder(clusterName);
            List<Tuple<VerificationKey, SecretKey>> utxoKeys = GenesisUtil.loadUtxoKeys(clusterFolder);
            BackendService backendService = getBackendService(clusterName);
            UtxoSupplier utxoSupplier = new DefaultUtxoSupplier(backendService.getUtxoService());

            Map<String, List<Utxo>> utxosMap = new LinkedHashMap<>();
            for (Tuple<VerificationKey, SecretKey> tuple : utxoKeys) {
                HdPublicKey hdPublicKey = new HdPublicKey();
                hdPublicKey.setKeyData(tuple._1.getBytes());
                Address address = AddressProvider.getEntAddress(hdPublicKey, Networks.testnet());
                String addr = address.toBech32();
                List<Utxo> utxos = utxoSupplier.getAll(addr);
                utxosMap.put(addr, utxos);
            }
            return utxosMap;
        } catch (Exception e) {
            log.error("Error getting genesis key UTXOs via Yano HTTP", e);
            return Collections.emptyMap();
        }
    }

    public boolean topUp(String clusterName, String receiver, double adaAmount, Consumer<String> writer) {
        try {
            Path clusterFolder = clusterService.getClusterFolder(clusterName);
            List<Tuple<VerificationKey, SecretKey>> utxoKeys = GenesisUtil.loadUtxoKeys(clusterFolder);
            BackendService backendService = getBackendService(clusterName);
            UtxoSupplier utxoSupplier = new DefaultUtxoSupplier(backendService.getUtxoService());

            BigInteger amount = adaToLovelace(adaAmount);
            String senderAddress = null;
            SecretKey senderSkey = null;

            for (int i = 0; i < utxoKeys.size(); i++) {
                Tuple<VerificationKey, SecretKey> tuple = utxoKeys.get(i);
                HdPublicKey hdPublicKey = new HdPublicKey();
                hdPublicKey.setKeyData(tuple._1.getBytes());
                Address address = AddressProvider.getEntAddress(hdPublicKey, Networks.testnet());
                String addr = address.toBech32();

                Optional<Amount> sufficient = utxoSupplier.getAll(addr).stream()
                        .flatMap(utxo -> utxo.getAmount().stream())
                        .filter(amt -> LOVELACE.equals(amt.getUnit()) && amt.getQuantity().compareTo(amount) > 0)
                        .findAny();

                if (sufficient.isPresent()) {
                    senderAddress = addr;
                    senderSkey = tuple._2;
                    break;
                }
            }

            if (senderAddress == null) {
                writer.accept(error("No funded UTXO key found for topup via Yano"));
                return false;
            }

            QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
            Tx tx = new Tx()
                    .payToAddress(receiver, Amount.lovelace(amount))
                    .attachMetadata(MessageMetadata.create().add("Topup Fund"))
                    .from(senderAddress);

            Result<String> result = quickTxBuilder.compose(tx)
                    .withSigner(SignerProviders.signerFrom(senderSkey))
                    .complete();

            if (!result.isSuccessful()) {
                writer.accept(error("Topup TX failed: " + result.getResponse()));
                return false;
            }

            writer.accept(success("Transaction submitted successfully"));
            writer.accept(infoLabel("Txn# : ", result.getValue()));
            return true;
        } catch (Exception e) {
            log.error("Error during topup via Yano HTTP", e);
            writer.accept(error("Topup error: " + e.getMessage()));
            return false;
        }
    }

    public boolean mint(String clusterName, String assetName, BigInteger quantity, String receiver, Consumer<String> writer) {
        try {
            Path clusterFolder = clusterService.getClusterFolder(clusterName);
            List<Tuple<VerificationKey, SecretKey>> utxoKeys = GenesisUtil.loadUtxoKeys(clusterFolder);
            BackendService backendService = getBackendService(clusterName);
            UtxoSupplier utxoSupplier = new DefaultUtxoSupplier(backendService.getUtxoService());

            String senderAddress = null;
            SecretKey senderSkey = null;

            for (int i = 0; i < utxoKeys.size(); i++) {
                Tuple<VerificationKey, SecretKey> tuple = utxoKeys.get(i);
                HdPublicKey hdPublicKey = new HdPublicKey();
                hdPublicKey.setKeyData(tuple._1.getBytes());
                Address address = AddressProvider.getEntAddress(hdPublicKey, Networks.testnet());
                String addr = address.toBech32();

                Optional<Amount> sufficient = utxoSupplier.getAll(addr).stream()
                        .flatMap(utxo -> utxo.getAmount().stream())
                        .filter(amt -> LOVELACE.equals(amt.getUnit()) && amt.getQuantity().compareTo(quantity) > 0)
                        .findAny();

                if (sufficient.isPresent()) {
                    senderAddress = addr;
                    senderSkey = tuple._2;
                    break;
                }
            }

            if (senderAddress == null) {
                writer.accept(error("No funded UTXO key found for mint via Yano"));
                return false;
            }

            var verificationKey = KeyGenUtil.getPublicKeyFromPrivateKey(senderSkey);
            var scriptPubkey = ScriptPubkey.create(verificationKey);

            QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
            Tx tx = new Tx()
                    .mintAssets(scriptPubkey, Asset.builder()
                            .name(assetName)
                            .value(quantity)
                            .build(), receiver)
                    .payToAddress(senderAddress, Amount.ada(1))
                    .from(senderAddress);

            Result<String> result = quickTxBuilder.compose(tx)
                    .withSigner(SignerProviders.signerFrom(senderSkey))
                    .complete();

            if (!result.isSuccessful()) {
                writer.accept(error("Mint TX failed: " + result.getResponse()));
                return false;
            }

            writer.accept(success("Transaction submitted successfully"));
            writer.accept(info("Txn# : " + result.getValue()));
            return true;
        } catch (Exception e) {
            log.error("Error during mint via Yano HTTP", e);
            writer.accept(error("Mint error: " + e.getMessage()));
            return false;
        }
    }

    public Tuple<Long, Point> getTip(String clusterName) {
        try {
            int httpPort = getYanoHttpPort(clusterName);
            JsonNode tip = yanoBootstrapService.getChainTip(httpPort);
            if (tip != null) {
                long height = tip.has("height") ? tip.get("height").asLong(0) : 0;
                long slot = tip.has("slot") ? tip.get("slot").asLong(0) : 0;
                String hash = tip.has("hash") ? tip.get("hash").asText("") : "";
                return new Tuple<>(height, new Point(slot, hash));
            }
        } catch (Exception e) {
            log.error("Error getting tip via Yano HTTP", e);
        }
        return null;
    }

    public EpochContent getLatestEpoch(String clusterName) {
        try {
            BackendService backendService = getBackendService(clusterName);
            Result<EpochContent> result = backendService.getEpochService().getLatestEpoch();
            if (result.isSuccessful()) {
                return result.getValue();
            }
        } catch (Exception e) {
            log.error("Error getting latest epoch via Yano HTTP", e);
        }
        return null;
    }

    public ProtocolParams getProtocolParams(String clusterName) {
        try {
            BackendService backendService = getBackendService(clusterName);
            ProtocolParamsSupplier supplier = new DefaultProtocolParamsSupplier(backendService.getEpochService());
            return supplier.getProtocolParams();
        } catch (Exception e) {
            log.error("Error getting protocol params via Yano HTTP", e);
        }
        return null;
    }
}
