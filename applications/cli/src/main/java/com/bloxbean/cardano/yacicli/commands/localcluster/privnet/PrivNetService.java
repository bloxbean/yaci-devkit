package com.bloxbean.cardano.yacicli.commands.localcluster.privnet;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.commands.localcluster.config.GenesisConfig;
import com.bloxbean.cardano.yacicli.commands.localcluster.config.GenesisUtil;
import com.bloxbean.cardano.yacicli.commands.localcluster.peer.PoolConfig;
import com.bloxbean.cardano.yacicli.commands.localcluster.peer.PoolKeyGeneratorService;
import com.bloxbean.cardano.yacicli.commands.localcluster.profiles.GenesisProfile;
import com.bloxbean.cardano.yacicli.util.AdvancedTemplateEngine;
import com.bloxbean.cardano.yacicli.util.ProcessStream;
import com.bloxbean.cardano.yacicli.util.TemplateEngine;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@Slf4j
public class PrivNetService {
    @Autowired
    private ClusterConfig clusterConfig;

    @Autowired
    TemplateEngine templateEngine;

    @Autowired
    AdvancedTemplateEngine templateEngineHelper;

    @Autowired
    private GenesisConfig genesisConfig;

    @Autowired
    private PoolKeyGeneratorService poolKeyGeneratorService;

    public void setupNewKeysAndDefaultPool(Path clusterPath, String clusterName, ClusterInfo clusterInfo, GenesisConfig genesisConfig, Double activeCoeff, Consumer<String> writer) throws IOException {
        updateGenesisScript(clusterPath, clusterName, clusterInfo, clusterInfo.getSlotLength(), activeCoeff,
                clusterInfo.getEpochLength(), writer);
        runGenesisScript(clusterPath, clusterName, true, writer);
        writer.accept(success("New genesis keys generated successfully"));

        updateByronAndShelleyDelegationKeys(clusterPath, clusterName, clusterInfo, writer);

        updateDelegationDetailsInDefaultGenesisFiles(clusterPath, clusterName, clusterInfo, genesisConfig, writer);

        if (!"Mainnet".equals(genesisConfig.getNetworkId())) {
            poolKeyGeneratorService.updatePoolGenScript(clusterPath, clusterInfo);
            generateDefaultPoolKeys(clusterPath, clusterName, genesisConfig, true);
        }

        writer.accept(success("Genesis keys updated in default genesis files"));
    }

    private void updateGenesisScript(Path destPath, String clusterName, ClusterInfo clusterInfo, double slotLength, double activeSlotsCoeff,
                                     int epochLength, Consumer<String> writer) throws IOException {
        Path genCreateScriptTemplate = destPath.resolve("genesis-scripts").resolve("genesis-create-template.sh");
        Path genCreateScript = destPath.resolve("genesis-scripts").resolve("genesis-create.sh");

        GenesisConfig genesisConfigCopy = genesisConfig.copy();
        if (clusterInfo.getGenesisProfile() != null)
            genesisConfigCopy = GenesisProfile.applyGenesisProfile(clusterInfo.getGenesisProfile(), genesisConfigCopy);

        Map values = genesisConfigCopy.getConfigMap();
        values.put("BIN_FOLDER", clusterConfig.getCLIBinFolder());
        values.put("protocolMagic", String.valueOf(clusterInfo.getProtocolMagic()));
        values.put("slotLength", String.valueOf((int) slotLength));
        values.put("activeSlotsCoeff", String.valueOf(activeSlotsCoeff));
        values.put("epochLength", String.valueOf(epochLength));
        values.put("genesisKeysHome", clusterConfig.getGenesisKeysFolder(clusterName));

        try {
            templateEngineHelper.replaceValues(genCreateScriptTemplate, genCreateScript, values);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public void runGenesisScript(Path clusterPah, String clusterName, boolean overwrite, Consumer<String> writer) {
        try {
            Path genCreateScript = clusterPah.resolve("genesis-scripts").resolve("genesis-create.sh");

            if (!genCreateScript.toFile().exists()) {
                writer.accept(error("Genesis script file not found : %s", genCreateScript.toFile().getAbsolutePath()));
                return;
            }

            Path clusterGenesisKeysFolder = clusterConfig.getGenesisKeysFolder(clusterName);

            if (clusterGenesisKeysFolder.toFile().exists() && !overwrite) {
                writer.accept(warn("Found existing genesis keys for this node. Existing keys will be used." +
                        "Please use --overwrite-genesis-keys option to overwrite genesis keys"));
                return;
            }

            FileUtils.deleteDirectory(clusterGenesisKeysFolder.toFile());

            if (!clusterGenesisKeysFolder.toFile().exists()) {
                Files.createDirectories(clusterGenesisKeysFolder);
            }

            String genCreateScriptFile = genCreateScript.toFile().getAbsolutePath();

            ProcessBuilder builder = new ProcessBuilder();
            builder.command("sh", genCreateScriptFile);

            builder.directory(genCreateScript.getParent().toFile());
            Process process = builder.start();

            ProcessStream processStream =
                    new ProcessStream(process.getInputStream(), line -> {
                        if (line != null && !line.isEmpty())
                            writer.accept(successLabel("Genesis Keys", line));
                    });

            ProcessStream errorProcessStream =
                    new ProcessStream(process.getErrorStream(), line -> {
                        if (line != null && !line.isEmpty())
                            writer.accept(error("Genesis Keys", line));
                    });
            Future<?> future = Executors.newSingleThreadExecutor().submit(processStream, errorProcessStream);
            future.get();
        } catch (Exception e) {
            log.info(e.getMessage(), e);
        }
    }

    private void updateByronAndShelleyDelegationKeys(Path destPath, String clusterName, ClusterInfo clusterInfo, Consumer<String> writer) throws IOException {
        Path genesisSetupFilesFolder = clusterConfig.getGenesisKeysFolder(clusterName);
        Path delegateKeysPath = genesisSetupFilesFolder.resolve("delegate-keys");

        //byron.000.cert.json
        Path byronCertFile = delegateKeysPath.resolve("byron.000.cert.json");
        //byron.000.skey
        Path byronKeyFile = delegateKeysPath.resolve("byron.000.key");

        //shelley.000.opcert.json
        Path shelleyOpCertFile = delegateKeysPath.resolve("shelley.000.opcert.json");
        //shelley.000.counter.json
        Path shelleyCounterFile = delegateKeysPath.resolve("shelley.000.counter.json");
        //shelley.000.kes.skey
        Path shelleyKesSkeyFile = delegateKeysPath.resolve("shelley.000.kes.skey");
        //shelley.000.kes.vkey
        Path shelleyKesVkeyFile = delegateKeysPath.resolve("shelley.000.kes.vkey");
        //shelley.000.vrf.skey
        Path shelleyVrfSkeyFile = delegateKeysPath.resolve("shelley.000.vrf.skey");
        //shelley.000.vrf.vkey
        Path shelleyVrfVkeyFile = delegateKeysPath.resolve("shelley.000.vrf.vkey");
        //shelley.000.skey
        Path shelleySkeyFile = delegateKeysPath.resolve("shelley.000.skey");
        //shelley.000.vkey
        Path shelleyVkeyFile = delegateKeysPath.resolve("shelley.000.vkey");

        //Destination file
        Path destByronDelegateKey = destPath.resolve("node").resolve("pool-keys").resolve("byron-delegate.key");
        Path destByronCert = destPath.resolve("node").resolve("pool-keys").resolve("byron-delegation.cert");

        Path destKes = destPath.resolve("node").resolve("pool-keys").resolve("kes.skey");
        Path destOpcert = destPath.resolve("node").resolve("pool-keys").resolve("opcert.cert");
        Path destVrf = destPath.resolve("node").resolve("pool-keys").resolve("vrf.skey");

        FileUtils.copyFile(byronKeyFile.toFile(), destByronDelegateKey.toFile());
        FileUtils.copyFile(byronCertFile.toFile(), destByronCert.toFile());

        if ("Mainnet".equals(genesisConfig.getNetworkId())) { //copy only for mainnet
            FileUtils.copyFile(shelleyKesSkeyFile.toFile(), destKes.toFile());
            FileUtils.copyFile(shelleyOpCertFile.toFile(), destOpcert.toFile());
            FileUtils.copyFile(shelleyVrfSkeyFile.toFile(), destVrf.toFile());
        }

        writer.accept(success("Byron and Shelley delegation keys copied"));
    }

    public void updateDelegationDetailsInDefaultGenesisFiles(Path destPath, String clusterName, ClusterInfo clusterInfo, GenesisConfig genesisConfig, Consumer<String> writer) throws IOException {
        Path genesisSetupFilesFolder = clusterConfig.getGenesisKeysFolder(clusterName);
        Path newByronGenesisFile = genesisSetupFilesFolder.resolve("byron-genesis.json");
        Path newShelleyGenesisFile = genesisSetupFilesFolder.resolve("shelley-genesis.json");

        var byronHeavyDelegations = GenesisUtil.getHeavyDelegations(newByronGenesisFile);
        var nonAvvmBalances = GenesisUtil.getNonAvvmBalances(newByronGenesisFile);
        var shelleyGenesisDelegs = GenesisUtil.getGenesisDelegs(newShelleyGenesisFile);

        genesisConfig.setGenesisDelegs(shelleyGenesisDelegs);
        genesisConfig.setHeavyDelegations(byronHeavyDelegations);
        genesisConfig.setNonAvvmBalances(nonAvvmBalances);
        genesisConfig.setBootStakeHolders(byronHeavyDelegations.stream().map(GenesisConfig.HeavyDelegation::bootStakeDelegator).collect(Collectors.toList()));
    }

    @SneakyThrows
    public void generateDefaultPoolKeys(Path clusterPath, String clusterName, GenesisConfig genesisConfig, boolean overwritePoolKeys) {
        poolKeyGeneratorService.generatePoolKeys("default", overwritePoolKeys, (msg) -> {
            writeLn(msg);
        });

        //Generate KES keys
        poolKeyGeneratorService.generateOperationalCert("default", 0, (msg) -> {
            writeLn(msg);
        });

        //dummy values
        PoolConfig poolConfig = PoolConfig.builder()
                .ticker("DEFAULT_POOL")
                .metadataHash("6bf124f217d0e5a0a8adb1dbd8540e1334280d49ab861127868339f43b3948af") //dummy
                .pledge(adaToLovelace(10000000))
                .cost(adaToLovelace(340))
                .margin(0.02)
                .relayHost("127.0.0.1")
                .relayPort(3001)
                .build();
        poolKeyGeneratorService.registerPool(clusterName, poolConfig, (msg) -> {
            writeLn(msg);
        });

        var poolRegCert = poolKeyGeneratorService.getPoolRegistrationCert("default", (msg) -> {
            writeLn(msg);
        }).orElseThrow(() -> new RuntimeException("Pool registration certificate not found"));

        if (poolRegCert == null) {
            writeLn(error("Pool registration certificate not found"));
            return;
        }

        Path destKes = clusterPath.resolve("node").resolve("pool-keys").resolve("kes.skey");
        Path destOpcert = clusterPath.resolve("node").resolve("pool-keys").resolve("opcert.cert");
        Path destVrf = clusterPath.resolve("node").resolve("pool-keys").resolve("vrf.skey");

        var clusterPoolKeyFolder = clusterConfig.getPoolKeysFolder(clusterName);
        Path srcKes = clusterPoolKeyFolder.resolve("kes.skey");
        Path srcOpcert = clusterPoolKeyFolder.resolve("opcert.cert");
        Path srcVrf = clusterPoolKeyFolder.resolve("vrf.skey");

        FileUtils.copyFile(srcKes.toFile(), destKes.toFile());
        FileUtils.copyFile(srcOpcert.toFile(), destOpcert.toFile());
        FileUtils.copyFile(srcVrf.toFile(), destVrf.toFile());

        writeLn(success("KES, VRF and Operational certificate copied to node folder"));

        var address = new Address(HexUtil.decodeHexString(poolRegCert.getRewardAccount()));
        var rewardAccCred = address.getDelegationCredential()
                .map(cred -> HexUtil.encodeHexString(cred.getBytes())).orElse(null);

        var poolHash = HexUtil.encodeHexString(poolRegCert.getOperator());

        genesisConfig.setPools(List.of(
                GenesisConfig.Pool.builder()
                        .poolHash(poolHash)
                        .cost(genesisConfig.getMinPoolCost())
                        .margin(BigDecimal.ZERO)
                        .rewardAccountHash(rewardAccCred)
                        .rewardAccountType("keyHash")
                        .publicKey(poolHash)
                        .vrf(HexUtil.encodeHexString(poolRegCert.getVrfKeyHash()))
                        .build()
        ));

        //Filter delegators with same pool hash or pool hash null
        List<GenesisConfig.Delegator> delegators = new ArrayList<>();

        var additionalDelegators = genesisConfig.getDefaultDelegators()
                .stream().filter(delegator -> delegator.getPoolHash() == null)
                .map(delegator -> new GenesisConfig.Delegator(delegator.getStakeKeyHash(), poolHash, false))
                .toList();

        delegators.addAll(additionalDelegators);
        //Add reward acc to delegators list
        delegators.add(new GenesisConfig.Delegator(rewardAccCred, poolHash, true));

        genesisConfig.setDefaultDelegators(delegators);
    }

    public void copyKeys(Path destPath, String clusterName, ClusterInfo clusterInfo, Consumer<String> writer) {
        Path genesisSetupFilesFolder = clusterConfig.getGenesisKeysFolder(clusterName);

        Path utxoKeysPath = genesisSetupFilesFolder.resolve("utxo-keys");
        Path delegateKeysPath = genesisSetupFilesFolder.resolve("delegate-keys");
        Path genesisKeysPath = genesisSetupFilesFolder.resolve("genesis-keys");

        Path destUtxoKeysPath = destPath.resolve("utxo-keys");
        Path destGenesisKeys = destPath.resolve("genesis-keys");
        Path destDelegateKeys = destPath.resolve("delegate-keys");

        try {
            FileUtils.forceMkdir(destUtxoKeysPath.toFile());

            for (int i=0; i < genesisConfig.getNGenesisUtxoKeys(); i++) {
                Path srcUtxoSkey = utxoKeysPath.resolve("shelley.00" + i +".skey");
                Path srcUtxoVkey = utxoKeysPath.resolve("shelley.00" + i + ".vkey");

                Path destUtxoSkey = destUtxoKeysPath.resolve("utxo" + (i + 1) + ".skey");
                Path destUtxoVkey = destUtxoKeysPath.resolve("utxo" + (i + 1) + ".vkey");

                FileUtils.copyFile(srcUtxoSkey.toFile(), destUtxoSkey.toFile());
                FileUtils.copyFile(srcUtxoVkey.toFile(), destUtxoVkey.toFile());
            }

            writer.accept(success("Faucet UTXO keys copied"));

            FileUtils.copyDirectory(delegateKeysPath.toFile(), destDelegateKeys.toFile());
            writer.accept(success("Delegate keys copied"));

            FileUtils.copyDirectory(genesisKeysPath.toFile(), destGenesisKeys.toFile());
            writer.accept(success("Genesis keys copied"));

        } catch (IOException e) {
            e.printStackTrace();
            writer.accept(error("Error copying utxo keys"));
        }
    }
}
