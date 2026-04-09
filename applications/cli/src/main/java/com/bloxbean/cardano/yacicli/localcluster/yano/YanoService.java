package com.bloxbean.cardano.yacicli.localcluster.yano;

import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStopped;
import com.bloxbean.cardano.yacicli.util.PortUtil;
import com.bloxbean.cardano.yacicli.util.ProcessStream;
import com.bloxbean.cardano.yacicli.util.ProcessUtil;
import com.google.common.collect.EvictingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class YanoService {
    private static final String YANO_PROCESS_NAME = "yano";

    private final ClusterConfig clusterConfig;
    private final ProcessUtil processUtil;
    private final YanoConfigBuilder yanoConfigBuilder;

    private List<Process> processes = new ArrayList<>();
    private List<ExecutorService> executors = new ArrayList<>();
    private Queue<String> logs = EvictingQueue.create(300);

    public boolean start(ClusterInfo clusterInfo, Path clusterFolder, boolean pastTimeTravelMode, Consumer<String> writer) {
        logs.clear();

        Path yanoBin = Path.of(clusterConfig.getYanoHome(), "yano");
        if (!yanoBin.toFile().exists()) {
            writer.accept(error("yano binary not found at " + yanoBin));
            writer.accept(error("Please run 'download --component yano' first"));
            return false;
        }

        if (!PortUtil.isPortAvailable(clusterInfo.getYanoServerPort())) {
            writer.accept(error("Yano n2n port " + clusterInfo.getYanoServerPort() + " is not available"));
            return false;
        }
        if (!PortUtil.isPortAvailable(clusterInfo.getYanoHttpPort())) {
            writer.accept(error("Yano HTTP port " + clusterInfo.getYanoHttpPort() + " is not available"));
            return false;
        }

        try {
            // Prepare Yano config directory with genesis files and keys
            Path yanoConfigDir = prepareYanoConfig(clusterInfo, clusterFolder, writer);
            if (yanoConfigDir == null) return false;

            Process process = startYanoProcess(clusterInfo, clusterFolder, yanoConfigDir, pastTimeTravelMode, writer);
            if (process != null) {
                processes.add(process);
                return true;
            }
        } catch (Exception e) {
            log.error("Error starting Yano", e);
            writer.accept(error("Failed to start Yano: " + e.getMessage()));
        }
        return false;
    }

    private Path prepareYanoConfig(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) throws IOException {
        Path yanoConfigDir = clusterFolder.resolve("yano-config").resolve("network").resolve("devnet");
        Files.createDirectories(yanoConfigDir);

        // 1. Start with Yano's bundled devnet config as base (protocol-param.json, keys, genesis defaults)
        Path yanoBundledDevnetConfig = Path.of(clusterConfig.getYanoHome(), "config", "network", "devnet");
        if (yanoBundledDevnetConfig.toFile().exists()) {
            FileUtils.copyDirectory(yanoBundledDevnetConfig.toFile(), yanoConfigDir.toFile());
            writer.accept(info("Copied Yano bundled devnet config as base"));
        } else {
            writer.accept(warn("Yano bundled config not found at " + yanoBundledDevnetConfig));
        }

        // 2. Overwrite genesis files from DevKit's cluster (these have DevKit's specific params)
        Path genesisDir = clusterFolder.resolve("node").resolve("genesis");
        String[] genesisFiles = {"shelley-genesis.json", "byron-genesis.json", "alonzo-genesis.json", "conway-genesis.json"};
        for (String gf : genesisFiles) {
            Path src = genesisDir.resolve(gf);
            if (src.toFile().exists()) {
                Files.copy(src, yanoConfigDir.resolve(gf), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        // 3. Align protocol-param.json with DevKit's genesis params so Yano-produced blocks
        //    are valid from the Haskell node's perspective (deposits, fees, etc. must match)
        alignProtocolParams(yanoConfigDir, genesisDir, writer);

        // 4. Overwrite VRF/KES keys from DevKit's pool-keys (must match genesis genDelegs)
        Path poolKeysDir = clusterFolder.resolve("node").resolve("pool-keys");
        String[] keyFiles = {"vrf.skey", "kes.skey", "opcert.cert"};
        for (String kf : keyFiles) {
            Path src = poolKeysDir.resolve(kf);
            if (src.toFile().exists()) {
                Files.copy(src, yanoConfigDir.resolve(kf), StandardCopyOption.REPLACE_EXISTING);
            } else {
                writer.accept(warn("Key file not found: " + src + ". Yano block production may fail."));
            }
        }

        writer.accept(info("Yano config prepared at " + yanoConfigDir));
        return yanoConfigDir;
    }

    /**
     * Align Yano's protocol-param.json with DevKit's genesis protocol parameters.
     * Yano's bundled defaults may differ from DevKit's genesis (e.g., govActionDeposit),
     * which would cause Yano-produced blocks to be invalid from the Haskell node's perspective.
     */
    private void alignProtocolParams(Path yanoConfigDir, Path genesisDir, Consumer<String> writer) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Path ppPath = yanoConfigDir.resolve("protocol-param.json");
            if (!ppPath.toFile().exists()) return;

            ObjectNode pp = (ObjectNode) mapper.readTree(ppPath.toFile());

            // Read conway-genesis for governance params and PlutusV3 cost model
            Path conwayPath = genesisDir.resolve("conway-genesis.json");
            if (conwayPath.toFile().exists()) {
                JsonNode conway = mapper.readTree(conwayPath.toFile());
                if (conway.has("govActionDeposit"))
                    pp.put("gov_action_deposit", conway.get("govActionDeposit").asLong());
                if (conway.has("dRepDeposit"))
                    pp.put("drep_deposit", conway.get("dRepDeposit").asLong());
                if (conway.has("committeeMinSize"))
                    pp.put("committee_min_size", conway.get("committeeMinSize").asInt());
                if (conway.has("committeeMaxTermLength"))
                    pp.put("committee_max_term_length", conway.get("committeeMaxTermLength").asInt());
                if (conway.has("govActionLifetime"))
                    pp.put("gov_action_lifetime", conway.get("govActionLifetime").asInt());
                if (conway.has("dRepActivity"))
                    pp.put("drep_activity", conway.get("dRepActivity").asInt());

                // Align PlutusV3 cost model from conway-genesis.
                // Genesis has an array of values; Yano needs a dict with named keys (sorted alphabetically).
                // Use existing keys from Yano's bundled config, map genesis values to the first N keys.
                JsonNode genesisV3 = conway.get("plutusV3CostModel");
                if (genesisV3 != null && genesisV3.isArray() && pp.has("cost_models")) {
                    ObjectNode costModels = (ObjectNode) pp.get("cost_models");
                    alignCostModelFromArray(mapper, costModels, "PlutusV3", genesisV3);
                }
            }

            // Align PlutusV1/V2 cost models from alonzo-genesis
            Path alonzoPath = genesisDir.resolve("alonzo-genesis.json");
            if (alonzoPath.toFile().exists() && pp.has("cost_models")) {
                JsonNode alonzo = mapper.readTree(alonzoPath.toFile());
                JsonNode genesisCostModels = alonzo.get("costModels");
                if (genesisCostModels != null) {
                    ObjectNode costModels = (ObjectNode) pp.get("cost_models");
                    for (String lang : new String[]{"PlutusV1", "PlutusV2"}) {
                        if (genesisCostModels.has(lang) && genesisCostModels.get(lang).isArray()) {
                            alignCostModelFromArray(mapper, costModels, lang, genesisCostModels.get(lang));
                        }
                    }
                }
            }

            // Read shelley-genesis for base protocol params
            Path shelleyPath = genesisDir.resolve("shelley-genesis.json");
            if (shelleyPath.toFile().exists()) {
                JsonNode shelley = mapper.readTree(shelleyPath.toFile());
                JsonNode sp = shelley.get("protocolParams");
                if (sp != null) {
                    if (sp.has("keyDeposit"))
                        pp.put("key_deposit", sp.get("keyDeposit").asLong());
                    if (sp.has("poolDeposit"))
                        pp.put("pool_deposit", sp.get("poolDeposit").asLong());
                    if (sp.has("minFeeA"))
                        pp.put("min_fee_a", sp.get("minFeeA").asInt());
                    if (sp.has("minFeeB"))
                        pp.put("min_fee_b", sp.get("minFeeB").asInt());
                    if (sp.has("maxTxSize"))
                        pp.put("max_tx_size", sp.get("maxTxSize").asInt());
                    if (sp.has("maxBlockBodySize"))
                        pp.put("max_block_size", sp.get("maxBlockBodySize").asInt());
                    if (sp.has("minPoolCost"))
                        pp.put("min_pool_cost", sp.get("minPoolCost").asLong());
                }
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(ppPath.toFile(), pp);
            writer.accept(info("Aligned protocol-param.json with DevKit genesis"));
        } catch (Exception e) {
            log.warn("Failed to align protocol params: {}", e.getMessage());
            writer.accept(warn("Could not align protocol-param.json: " + e.getMessage()));
        }
    }

    /**
     * Align a cost model in protocol-param.json from a genesis array.
     * Takes the existing dict keys (sorted), maps genesis array values to the first N keys.
     * If genesis has fewer values than keys, only the first N keys are kept.
     */
    private void alignCostModelFromArray(ObjectMapper mapper, ObjectNode costModels, String language, JsonNode genesisArray) {
        if (!costModels.has(language) || !costModels.get(language).isObject()) return;

        JsonNode existingDict = costModels.get(language);
        List<String> sortedKeys = new ArrayList<>();
        existingDict.fieldNames().forEachRemaining(sortedKeys::add);
        Collections.sort(sortedKeys);

        ObjectNode aligned = mapper.createObjectNode();
        int genesisSize = genesisArray.size();
        for (int i = 0; i < Math.min(genesisSize, sortedKeys.size()); i++) {
            aligned.put(sortedKeys.get(i), genesisArray.get(i).asLong());
        }
        costModels.set(language, aligned);
    }

    private Process startYanoProcess(ClusterInfo clusterInfo, Path clusterFolder, Path yanoConfigDir,
                                     boolean pastTimeTravelMode, Consumer<String> writer)
            throws IOException, InterruptedException {

        Path yanoBin = Path.of(clusterConfig.getYanoHome(), "yano");

        // Store Yano data inside node folder so it gets cleaned up with create-node -o
        Path yanoDataDir = clusterFolder.resolve("node").resolve("yano");
        Files.createDirectories(yanoDataDir);

        // Write application.properties for Yano (persists config on disk for debugging)
        yanoConfigBuilder.build(clusterInfo, yanoConfigDir, yanoDataDir, pastTimeTravelMode);

        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(new File(clusterConfig.getYanoHome()));

        // Env vars override the properties file (Quarkus priority: env > config file)
        // Keep them for runtime guarantee with native binaries
        var env = builder.environment();
        env.put("QUARKUS_PROFILE", "devnet");
        env.put("YACI_NODE_REMOTE_PROTOCOL_MAGIC", String.valueOf(clusterInfo.getProtocolMagic()));
        env.put("YACI_NODE_SERVER_PORT", String.valueOf(clusterInfo.getYanoServerPort()));
        env.put("QUARKUS_HTTP_PORT", String.valueOf(clusterInfo.getYanoHttpPort()));
        env.put("YACI_NODE_GENESIS_SHELLEY_GENESIS_FILE", yanoConfigDir.resolve("shelley-genesis.json").toAbsolutePath().toString());
        env.put("YACI_NODE_GENESIS_BYRON_GENESIS_FILE", yanoConfigDir.resolve("byron-genesis.json").toAbsolutePath().toString());
        env.put("YACI_NODE_GENESIS_ALONZO_GENESIS_FILE", yanoConfigDir.resolve("alonzo-genesis.json").toAbsolutePath().toString());
        env.put("YACI_NODE_GENESIS_CONWAY_GENESIS_FILE", yanoConfigDir.resolve("conway-genesis.json").toAbsolutePath().toString());
        env.put("YACI_NODE_GENESIS_PROTOCOL_PARAMETERS_FILE", yanoConfigDir.resolve("protocol-param.json").toAbsolutePath().toString());
        env.put("YACI_NODE_BLOCK_PRODUCER_VRF_SKEY_FILE", yanoConfigDir.resolve("vrf.skey").toAbsolutePath().toString());
        env.put("YACI_NODE_BLOCK_PRODUCER_KES_SKEY_FILE", yanoConfigDir.resolve("kes.skey").toAbsolutePath().toString());
        env.put("YACI_NODE_BLOCK_PRODUCER_OPCERT_FILE", yanoConfigDir.resolve("opcert.cert").toAbsolutePath().toString());
        env.put("YACI_NODE_STORAGE_PATH", yanoDataDir.toAbsolutePath().toString());

        if (pastTimeTravelMode) {
            env.put("YACI_NODE_BLOCK_PRODUCER_PAST_TIME_TRAVEL_MODE", "true");
        }

        builder.command(yanoBin.toAbsolutePath().toString());

        Process process = builder.start();
        writer.accept(info("Starting Yano (n2n port: %d, HTTP port: %d) ...",
                clusterInfo.getYanoServerPort(), clusterInfo.getYanoHttpPort()));

        AtomicBoolean started = new AtomicBoolean(false);
        ProcessStream processStream = new ProcessStream(process.getInputStream(), line -> {
            logs.add(line);
            if (line != null && (line.contains("Listening on") || line.contains("started in"))) {
                started.set(true);
            }
        });
        ExecutorService stdoutExecutor = Executors.newSingleThreadExecutor();
        stdoutExecutor.submit(processStream);
        executors.add(stdoutExecutor);

        // Also capture stderr
        ProcessStream errorStream = new ProcessStream(process.getErrorStream(), line -> logs.add(line));
        ExecutorService stderrExecutor = Executors.newSingleThreadExecutor();
        stderrExecutor.submit(errorStream);
        executors.add(stderrExecutor);

        // Wait for startup
        int counter = 0;
        while (counter < 30) {
            counter++;
            if (started.get()) break;
            if (!process.isAlive()) {
                writer.accept(error("Yano process exited unexpectedly. Check logs with 'yano-logs'"));
                return null;
            }
            Thread.sleep(1000);
            writer.accept("Waiting for Yano to start ...");
        }

        if (!started.get()) {
            writer.accept(error("Yano did not start within timeout. Check logs with 'yano-logs'"));
            return null;
        }

        writer.accept(success("Yano started successfully"));
        processUtil.createProcessId(YANO_PROCESS_NAME, process);
        return process;
    }

    @EventListener
    public void handleClusterStopped(ClusterStopped clusterStopped) {
        stop();
    }

    public boolean stop() {
        try {
            if (processes != null && !processes.isEmpty())
                writeLn(info("Trying to stop Yano ..."));

            for (Process process : processes) {
                if (process != null && process.isAlive()) {
                    process.descendants().forEach(ph -> {
                        ph.destroyForcibly();
                    });
                    process.destroyForcibly();
                    process.waitFor(15, TimeUnit.SECONDS);
                    if (!process.isAlive()) {
                        writeLn(success("Yano stopped"));
                    } else {
                        writeLn(error("Yano process could not be killed"));
                    }
                }
            }
            processUtil.deletePidFile(YANO_PROCESS_NAME);
            executors.forEach(ExecutorService::shutdownNow);
            executors.clear();
            logs.clear();
        } catch (Exception e) {
            log.error("Error stopping Yano", e);
            writeLn(error("Yano could not be stopped: " + e.getMessage()));
            return false;
        } finally {
            processes.clear();
        }
        return true;
    }

    public boolean isRunning() {
        return processes.stream().anyMatch(Process::isAlive);
    }

    public void showLogs(Consumer<String> consumer) {
        if (logs.isEmpty()) {
            consumer.accept("No Yano logs to show");
        } else {
            int counter = 0;
            while (!logs.isEmpty()) {
                counter++;
                if (counter == 200) return;
                consumer.accept(logs.poll());
            }
        }
    }
}
