package com.bloxbean.cardano.yacicli.localcluster.peer;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.Value;
import com.bloxbean.cardano.client.transaction.spec.cert.PoolRegistration;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.api.model.TopupResult;
import com.bloxbean.cardano.yacicli.localcluster.service.AccountService;
import com.bloxbean.cardano.yacicli.util.AdvancedTemplateEngine;
import com.bloxbean.cardano.yacicli.util.TemplateEngine;
import com.google.common.io.Files;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.yacicli.util.AdaConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.yacicli.util.AdaConversionUtil.lovelaceToAda;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PeerService {
    public static final String NODE_FOLDER = "node";
    public static final String TEMPLATE_FOLDER = "templates";
    private final ClusterService clusterService;
    private final ClusterConfig clusterConfig;
    private final PoolKeyGeneratorService poolKeyGeneratorService;
    private final AccountService accountService;
    private final TemplateEngine templateEngine;
    private final AdvancedTemplateEngine advancedTemplateEngine;
    private final int nodePort = 3001;
    private final int submitApiPort = 8090;
    private final BigInteger poolDeposit = adaToLovelace(500);

    @org.springframework.beans.factory.annotation.Value("${is.docker:false}")
    private boolean isDocker;

    @SneakyThrows
    public boolean addPeer(String adminUrl, String nodeName, int portShift, boolean isBlockProducer, boolean overwrite, boolean overwritePoolKeys) {
        String adminNode = getAdminNodeHost(adminUrl).orElse(null);
        if (adminNode == null) {
            writeLn(error("Invalid admin url. Please provide a valid url"));
            return false;
        }

        if (isDocker && (
                adminNode.equals("localhost") || adminNode.equals("127.0.0.1"))) {
            writeLn(error("Admin node host cannot be localhost when running in docker mode. Please provide a valid host which is reachable from the container."));
            return false;
        }

        if (adminUrl != null && adminUrl.endsWith("/"))
            adminUrl = adminUrl.substring(0, adminUrl.length() - 1);

        String peerName = nodeName;
        Path clusterFolder = clusterService.getClusterFolder(peerName);
        //check if clusterFolder exists
        if (clusterFolder.toFile().exists()) {
            if (overwrite) {
                clusterService.deleteCluster(peerName, (msg) -> {
                    writeLn(msg);
                });
            } else {
                writeLn(error("Node folder already exists. Please use --overwrite option to overwrite"));
                return false;
            }
        }

        boolean bootstrapNodeInitialized = ClusterAdminClient.isBootstrapNodeInitialized(adminUrl);
        if (!bootstrapNodeInitialized) {
            writeLn(error("Bootstrap node is not initialized yet." +
                    " Please initialize and run the bootstrap node first"));
            return false;
        }

        int peerNodePort = nodePort + portShift;
        int peerSubmitApiPort = submitApiPort + portShift;

        ClusterInfo bootstrapClusterInfo = ClusterAdminClient.getClusterInfo(adminUrl);
        ClusterInfo clusterInfo = ClusterInfo
                .builder()
                .nodePort(peerNodePort)
                .submitApiPort(peerSubmitApiPort)
                .slotLength(bootstrapClusterInfo.getSlotLength())
                .blockTime(bootstrapClusterInfo.getBlockTime())
                .epochLength(bootstrapClusterInfo.getEpochLength())
                .protocolMagic(bootstrapClusterInfo.getProtocolMagic())
                .startTime(bootstrapClusterInfo.getStartTime())
                .masterNode(false)
                .p2pEnabled(true)
                .isBlockProducer(isBlockProducer)
                .adminNodeUrl(adminUrl)
                .era(bootstrapClusterInfo.getEra())
                .build();

        clusterInfo.setOgmiosPort(clusterInfo.getOgmiosPort() + portShift);
        clusterInfo.setKupoPort(clusterInfo.getKupoPort() + portShift);
        clusterInfo.setYaciStorePort(clusterInfo.getYaciStorePort() + portShift);
        clusterInfo.setSocatPort(clusterInfo.getSocatPort() + portShift);
        clusterInfo.setPrometheusPort(clusterInfo.getPrometheusPort() + portShift);

        boolean created = clusterService.createNodeClusterFolder(peerName, clusterInfo, overwrite, false, (msg) -> {
            writeLn(msg);
        });

        if (!created) {
            writeLn(error("Error creating peer folder"));
            return false;
        }

        String downloadUrl = adminUrl + "/local-cluster/api/admin/devnet/download";
        File downloadFolder = ClusterFileUtil.downloadClusterFiles(downloadUrl);
        if (downloadFolder == null) {
            writeLn(error("Error downloading cluster files from admin node"));
            return false;
        }

        String sourceGenesisFolder = downloadFolder.getAbsolutePath() + File.separator + "node" + File.separator + "genesis";
        String destGenesisFolder = clusterFolder.toAbsolutePath().toString() + File.separator + "node" + File.separator + "genesis";
        File destGenesisFolderFile = new File(destGenesisFolder);
        if (destGenesisFolderFile.exists()) //clean dest genesis folder
            FileUtils.deleteDirectory(destGenesisFolderFile);

        boolean genesisFileCopied = ClusterFileUtil.copyFolder(sourceGenesisFolder, destGenesisFolder);
        if (!genesisFileCopied) {
            writeLn(error("Error copying genesis files"));
            return false;
        }

        updateTopologyFile(clusterFolder, adminNode, nodePort);
        downloadFolder.delete();

        updateRunScripts(clusterFolder, nodeName, peerNodePort);
        poolKeyGeneratorService.updatePoolGenScript(clusterFolder, clusterInfo);

        //Generate pool keys
        if (isBlockProducer) {
            poolKeyGeneratorService.generatePoolKeys(nodeName, overwritePoolKeys, (msg) -> {
                writeLn(msg);
            });

            //Generate KES keys
            poolKeyGeneratorService.generateOperationalCert(adminUrl, nodeName, (msg) -> {
                writeLn(msg);
            });
        }

        return true;
    }

    @SneakyThrows
    public void registerPool(String nodeName, PoolConfig poolConfig, Consumer<String> writer) {
        ClusterInfo clusterInfo = clusterService.getClusterInfo(nodeName);
        if (poolConfig.getRelayPort() == 0)
            poolConfig.setRelayPort(clusterInfo.getNodePort());

        poolKeyGeneratorService.registerPool(nodeName, poolConfig, writer);
        poolKeyGeneratorService.getDefaultPoolOwnerPaymentAddress(nodeName).ifPresent(addr -> {
            List<Utxo> utxoList = ClusterAdminClient.getUtxos(clusterInfo.getAdminNodeUrl(), addr);
            BigInteger amountAvailable = BigInteger.ZERO;
            if (utxoList != null && utxoList.size() > 0) {
                amountAvailable =
                        utxoList.stream()
                                .flatMap(utxo -> utxo.getAmount().stream())
                                .filter(amount -> amount.getUnit().equalsIgnoreCase(LOVELACE))
                                .map(amount -> amount.getQuantity())
                                .reduce(BigInteger::add)
                                .orElse(BigInteger.ZERO);
            }

            if (amountAvailable.compareTo(poolConfig.getPledge()) < 0) {
                writer.accept(info("Owner address does not have enough funds for pledge amount. Let's transfer some funds to owner address."));
                BigInteger topupAmount = poolConfig.getPledge().subtract(amountAvailable).add(adaToLovelace(1000));
                writer.accept(info("Let's topup owner address with " + lovelaceToAda(topupAmount) + " ada"));
                TopupResult topupResult = ClusterAdminClient.topup(clusterInfo.getAdminNodeUrl(), addr, lovelaceToAda(topupAmount).doubleValue());
                if (topupResult.isStatus()) {
                    writer.accept(success("Topup successful. Amount: " + topupResult.getAdaAmount() + " Ada"));
                } else {
                    writer.accept(error("Topup failed. Please topup manually."));
                }
            }

            SecretKey paymentSecretKey = poolKeyGeneratorService.getSecretKey(nodeName, "payment.skey").orElseThrow();
            SecretKey stakeSecretKey = poolKeyGeneratorService.getSecretKey(nodeName, "stake.skey").orElseThrow();
            SecretKey coldSecretKey = poolKeyGeneratorService.getSecretKey(nodeName, "cold.skey").orElseThrow();
//            SecretKey paymentSecretKey = poolKeyGeneratorService.getSecretKey(nodeName, "payment.skey").orElseThrow();
            PoolRegistration poolRegistration = poolKeyGeneratorService.getPoolRegistrationCert(nodeName, writer)
                    .orElseThrow();

            String poolId = Bech32.encode(poolRegistration.getOperator(), "pool");
            writer.accept(successLabel("Pool Id", poolId));

            Address stakeAddress = AddressProvider.getStakeAddress(new Address(addr));
            writer.accept(successLabel("Owner Stake address", stakeAddress.getAddress()));
            writer.accept(successLabel("Owner Payment address", addr));

            BackendService backendService = new BFBackendService(clusterInfo.getAdminNodeUrl()
                    + "/local-cluster/api/", "dummy  id");

            UtxoSupplier utxoSupplier = new DefaultUtxoSupplier(backendService.getUtxoService());
            QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

            writer.accept(info("Registering stake address"));
            Tx stakeKeyRegistrationTx = new Tx()
                    .payToAddress(addr, Amount.ada(1.0))
                    .registerStakeAddress(stakeAddress)
                    .from(addr);
            Result<String> result = quickTxBuilder.compose(stakeKeyRegistrationTx)
                    .withSigner(SignerProviders.signerFrom(paymentSecretKey))
                    .complete();

            if (result.isSuccessful()) {
                writer.accept(success("Stake registration transaction was submitted successfully. Tx hash: " + result.getValue()));
                if (ClusterAdminClient.waitForTxToComplete(utxoSupplier, addr, result.getValue(), writer))
                    writer.accept(success("Stake address registration done." + result));
            } else
                writer.accept(error("Stake address registration failed. Error: " + result));


            //Do pool registration transasction
            writer.accept(info("Registering pool"));
            Tx tx = new Tx()
                    .payToAddress(addr, Amount.ada(1.0))
                    .from(addr);

            result = quickTxBuilder.compose(tx)
                    .withSigner(SignerProviders.signerFrom(paymentSecretKey))
                    .withSigner(SignerProviders.signerFrom(stakeSecretKey))
                    .withSigner(SignerProviders.signerFrom(coldSecretKey))
                    .preBalanceTx((context, txn) -> {
                        Value value = txn.getBody().getOutputs().get(0).getValue();
                        Value valueMinusDeposit = value.minus(Value.builder().coin(poolDeposit).build());
                        txn.getBody().getOutputs().get(0).setValue(valueMinusDeposit);
                        if (txn.getBody().getCerts() == null)
                            txn.getBody().setCerts(new ArrayList<>());
                        txn.getBody().getCerts().add(poolRegistration);
                    })
                    .complete();

            if (result.isSuccessful()) {
                writer.accept(success("Pool registration transaction was submitted successfully. Tx hash: " + result.getValue()));
                if (ClusterAdminClient.waitForTxToComplete(utxoSupplier, addr, result.getValue(), writer)) {
                    writer.accept(success("Pool registration transaction successful. Tx hash: " + result.getValue()));
                }
            } else
                writer.accept(error("Pool registration transaction failed. Error: " + result + ". Please try again"));

            //Do delegation transaction
            writer.accept(info("Delegating stake address to pool"));
            Tx delegationTx = new Tx()
                    .payToAddress(addr, Amount.ada(1.0))
                    .delegateTo(stakeAddress, poolId)
                    .from(addr);

            result = quickTxBuilder.compose(delegationTx)
                    .withSigner(SignerProviders.signerFrom(stakeSecretKey))
                    .withSigner(SignerProviders.signerFrom(paymentSecretKey))
                    .complete();

            if (result.isSuccessful()) {
                writer.accept(success("Delegation tx was submitted successfully. Tx hash: " + result.getValue()));
                if (ClusterAdminClient.waitForTxToComplete(utxoSupplier, addr, result.getValue(), writer))
                    writer.accept(success("Delegation tx successful. Tx hash: " + result.getValue()));
            } else
                writer.accept(error("Delegation Tx failed. Error: " + result));
        });

    }

    private void updateTopologyFile(Path clusterFolder, String adminHost, int adminPort) throws IOException {
        Path topologyPeer = clusterFolder.resolve(TEMPLATE_FOLDER).resolve("topology-peer.json");
        Map<String, String> values = new HashMap<>();
        values.put("adminNodeHost", adminHost);
        values.put("adminNodePort", String.valueOf(adminPort));

        //Update Genesis files
        try {
            templateEngine.replaceValues(topologyPeer, values);
        } catch (Exception e) {
            throw new IOException(e);
        }

        Path topology = clusterFolder.resolve(NODE_FOLDER).resolve("topology.json");
        Files.copy(topologyPeer.toFile(), topology.toFile());
    }

    private void updateRunScripts(Path clusterFolder, String nodeName, int port) throws IOException {
        Path destPath = clusterFolder.resolve(NODE_FOLDER);
        Path templatePath = clusterFolder.resolve(TEMPLATE_FOLDER);

        String relayScript = clusterService.getOSSpecificScriptName(ClusterConfig.NODE_RELAY_SCRIPT);
        String bpScript = clusterService.getOSSpecificScriptName(ClusterConfig.NODE_BP_SCRIPT);

        Path relayRunScriptTemplate = templatePath.resolve(relayScript);
        Path bpRunScriptTemplate = templatePath.resolve(bpScript);

        Path relayRunScript = destPath.resolve(relayScript);
        Path bpRunScript = destPath.resolve(bpScript);
        int nodePort = port; //Node no = 1..3

        Map<String, String> values = new HashMap<>();
        values.put("BIN_FOLDER", clusterConfig.getCLIBinFolder());
        values.put("NODE_NAME", nodeName);
        values.put("port", String.valueOf(nodePort));

        //Update relay run script
        try {
            advancedTemplateEngine.replaceValues(relayRunScriptTemplate, relayRunScript, values);
        } catch (Exception e) {
            throw new IOException(e);
        }
        try {
            advancedTemplateEngine.replaceValues(bpRunScriptTemplate, bpRunScript, values);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private Optional<String> getAdminNodeHost(String adminUrl) {
        try {
            URL url = new URL(adminUrl);
            String hostname = url.getHost();
            if ("localhost".equals(hostname))
                return Optional.of("127.0.0.1");
            else
                return Optional.of(hostname);
        } catch (Exception e) {
            log.error("Invalid admin url. Please provide a valid url");
            return Optional.empty();
        }
    }

}
