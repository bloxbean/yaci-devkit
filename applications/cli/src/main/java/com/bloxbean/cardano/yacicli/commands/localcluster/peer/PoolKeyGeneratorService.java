package com.bloxbean.cardano.yacicli.commands.localcluster.peer;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.transaction.spec.cert.PoolRegistration;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.util.ProcessStream;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PoolKeyGeneratorService {
    private final ClusterConfig clusterConfig;

    @SneakyThrows
    public void generatePoolKeys(String nodeName, boolean overwrite, Consumer<String> writer) {
        Path clusterPoolKeysFolder = clusterConfig.getPoolKeysFolder(nodeName);

        if (clusterPoolKeysFolder.toFile().exists() && !overwrite) {
            writer.accept(warn("Found existing pool keys for this node. Existing keys will be used." +
                    "Please use --overwrite-pool-keys option to overwrite pool keys"));
            return;
        }

        if (!clusterPoolKeysFolder.toFile().exists()) {
            Files.createDirectories(clusterPoolKeysFolder);
        }

        try {
            String genKeyFile = clusterConfig.getClusterFolder(nodeName).resolve("gen-pool-keys.sh").toFile()
                    .getAbsolutePath();
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("sh", genKeyFile);

            builder.directory(clusterPoolKeysFolder.toFile());
            Process process = builder.start();

            ProcessStream processStream =
                    new ProcessStream(process.getInputStream(), line -> {
                        if (line != null && !line.isEmpty())
                            writer.accept(successLabel("Pool Keys", line));
                    });
            Future<?> future = Executors.newSingleThreadExecutor().submit(processStream);
            future.get();
        } catch (Exception e) {
            log.info(e.getMessage(), e);
        }


        //Generate Cold Keys and a Cold Counter

        //Generate VRF Key pair

        //Generate KES Key pair

        //Generate Operational Certificate
    }

    @SneakyThrows
    public Optional<String> getDefaultPoolOwnerPaymentAddress(String nodeName) {
        Path clusterPoolKeysFolder = clusterConfig.getPoolKeysFolder(nodeName);
        Path paymentAddrFile = clusterPoolKeysFolder.resolve("payment.addr");
        if (!paymentAddrFile.toFile().exists()) {
            return Optional.empty();
        }

        String address = FileUtils.readFileToString(paymentAddrFile.toFile());
        if (address != null && address.length() > 0) {
            return Optional.of(address);
        } else {
            return Optional.empty();
        }
    }

    @SneakyThrows
    public void generateOperationalCert(String adminUrl, String nodeName, Consumer<String> writer) {
        int kesPeriod = ClusterAdminClient.getKesPeriod(adminUrl);
        generateOperationalCert(nodeName, kesPeriod, writer);
    }

    public void generateOperationalCert(String nodeName, int kesPeriod, Consumer<String> writer) throws IOException {
        Path clusterPoolKeysFolder = clusterConfig.getPoolKeysFolder(nodeName);

        if (!clusterPoolKeysFolder.toFile().exists()) {
            Files.createDirectories(clusterPoolKeysFolder);
        }

        try {
            String genCertFile = clusterConfig.getClusterFolder(nodeName).resolve("gen-pool-cert.sh").toFile()
                    .getAbsolutePath();
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("sh", genCertFile, String.valueOf(kesPeriod));

            builder.directory(clusterPoolKeysFolder.toFile());
            Process process = builder.start();

            ProcessStream processStream =
                    new ProcessStream(process.getInputStream(), line -> {
//                    //TODO -- Add logs
                        if (line != null && !line.isEmpty())
                            writer.accept(info(line));
                    });
            Future<?> future = Executors.newSingleThreadExecutor().submit(processStream);
            future.get();
        } catch (Exception e) {
            log.info(e.getMessage(), e);
        }
    }

    @SneakyThrows
    public void registerPool(String nodeName, PoolConfig poolConfig, Consumer<String> writer) {
        Path clusterPoolKeysFolder = clusterConfig.getPoolKeysFolder(nodeName);

        try {
            String genPoolRegistrationScript = clusterConfig.getClusterFolder(nodeName).resolve("pool-registration.sh").toFile()
                    .getAbsolutePath();
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("sh", genPoolRegistrationScript,
                    poolConfig.getPledge().toString(), poolConfig.getCost().toString(), String.valueOf(poolConfig.getMargin()),
                    "http://yacidevkit.node/metadata", poolConfig.getMetadataHash(),
                    poolConfig.getRelayHost(), String.valueOf(poolConfig.getRelayPort()));

            builder.directory(clusterPoolKeysFolder.toFile());
            Process process = builder.start();

            writer.accept("Running script : " + builder.command());

            ProcessStream processStream =
                    new ProcessStream(process.getInputStream(), line -> {
//                    //TODO -- Add logs
                        if (line != null && !line.isEmpty())
                            writer.accept(successLabel("Pool Registration", line));
                    });
            ProcessStream errorProcessStream =
                    new ProcessStream(process.getErrorStream(), line -> {
//                    //TODO -- Add logs
                        writer.accept(error(line));
                    });
            Future<?> future = Executors.newSingleThreadExecutor().submit(processStream, errorProcessStream);
            future.get();
        } catch (Exception e) {
            log.info(e.getMessage(), e);
        }
    }

    @SneakyThrows
    public Optional<SecretKey> getSecretKey(String nodeName, String keyFileName) {
        Path clusterPoolKeysFolder = clusterConfig.getPoolKeysFolder(nodeName);
        Path paymentSkeyFile = clusterPoolKeysFolder.resolve(keyFileName);
        if (!paymentSkeyFile.toFile().exists()) {
            return Optional.empty();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        SecretKey secretKey = objectMapper.readValue(paymentSkeyFile.toFile(), SecretKey.class);

        if (secretKey != null)
            return Optional.of(secretKey);
        else
            return Optional.empty();
    }

    public Optional<PoolRegistration> getPoolRegistrationCert(String nodeName, Consumer<String> writer) {
        Path clusterPoolKeysFolder = clusterConfig.getPoolKeysFolder(nodeName);
        Path poolRegistrationCertFile = clusterPoolKeysFolder.resolve("pool-registration.cert");
        if (!poolRegistrationCertFile.toFile().exists()) {
            writer.accept(error("Pool registration cert file not found"));
            return null;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonNode certNode = objectMapper.readTree(poolRegistrationCertFile.toFile());
            String cborHex = certNode.get("cborHex").asText();
            PoolRegistration poolRegistration =
                    PoolRegistration.deserialize(CborSerializationUtil.deserialize(HexUtil.decodeHexString(cborHex)));
            return Optional.of(poolRegistration);
        } catch (Exception e) {
            log.error("Error reading pool registration cert file", e);
            return Optional.empty();
        }
    }
}
