package com.bloxbean.cardano.yacicli.localcluster.yacistore;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.util.OSUtil;
import com.bloxbean.cardano.yacicli.commands.common.JreResolver;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.common.Tuple;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.ClusterStartService;
import com.bloxbean.cardano.yacicli.localcluster.NodeMode;
import com.bloxbean.cardano.yacicli.localcluster.config.ApplicationConfig;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterDeleted;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStarted;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStopped;
import com.bloxbean.cardano.yacicli.localcluster.events.RollbackDone;
import com.bloxbean.cardano.yacicli.localcluster.ogmios.OgmiosService;
import com.bloxbean.cardano.yacicli.localcluster.service.ClusterUtilService;
import com.bloxbean.cardano.yacicli.util.PortUtil;
import com.bloxbean.cardano.yacicli.util.ProcessStream;
import com.bloxbean.cardano.yacicli.util.ProcessUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.EvictingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class YaciStoreService {
    private static final String STORE_PROCESS_NAME = "yaci-store";
    private static final String TX_EVALUATOR_MODE_OGMIOS = "ogmios";
    private static final String TX_EVALUATOR_MODE_SCALUS = "scalus";
    private static final String PLUTUS_V3 = "PlutusV3";

    private final ApplicationConfig appConfig;
    private final ClusterService clusterService;
    private final ClusterStartService clusterStartService;
    private final ClusterConfig clusterConfig;
    private final ClusterUtilService clusterUtilService;
    private final JreResolver jreResolver;
    private final YaciStoreConfigBuilder yaciStoreConfigBuilder;
    private final YaciStoreCustomDbHelper customDBHelper;
    private final ProcessUtil processUtil;
    private final OgmiosService ogmiosService;

    private List<Process> processes = new ArrayList<>();

    @Value("${yaci.store.mode:java}")
    private String yaciStoreMode;

    private Queue<String> logs = EvictingQueue.create(300);

    @EventListener
    @Order(1)
    public void handleClusterStarted(ClusterStarted clusterStarted) {
        String clusterName = clusterStarted.getClusterName();

        start(clusterName, msg -> writeLn(msg));
    }

    public boolean start(String clusterName, Consumer<String> writer) {
        logs.clear();
        if (!appConfig.isYaciStoreEnabled())
            return false;

        if (!clusterName.equals("default")) {
            writer.accept(warn("Yaci Store is only supported for 'default' cluster"));
            return false;
        }

        if (!clusterStartService.isClusterRunning()) {
            writer.accept(warn("Node is not running. Please start the cluster first."));
            return false;
        }

        try {
            ClusterInfo clusterInfo = clusterService.getClusterInfo(clusterName);
            if (clusterInfo == null)
                throw new IllegalStateException("Cluster info not found for cluster: " + clusterName
                        + ". Please check if the cluster is created.");

            if (!portAvailabilityCheck(clusterInfo, (msg) -> writeLn(msg)))
                return false;

            Era era = clusterInfo.getEra();
            StoreStartResult result = startStoreApp(clusterInfo, era, writer);
            // No process means the Store could not be launched (e.g. the required native variant
            // was missing or config generation failed). Surface that as a start failure.
            if (result.process() == null) {
                writer.accept(error("Yaci Store failed to start."));
                return false;
            }
            // Track the live process so it can be cleaned up by stop(), even when the boot log
            // wasn't observed (the process may still recover).
            processes.add(result.process());

            // Wait for indexer to catch up to chain tip — only if boot was actually
            // observed, otherwise we'd burn the 60s deadline on a stuck process.
            if (result.bootObserved() && result.process().isAlive()) {
                waitForSyncToTip(clusterInfo, writer);
                if (NodeMode.YANO_ONLY == clusterInfo.getNodeMode() && !waitForYanoOnlyProtocolParamsReady(clusterInfo, writer)) {
                    throw new IllegalStateException("Yaci Store protocol parameters did not become ready.");
                }
            }

//            Process viewerProcess = startViewerApp(clusterStarted.getClusterName());
//                processes.add(viewerProcess);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    /** Result of an attempt to spawn Yaci Store: the process (may be null) and
     *  whether the "Started YaciStoreApplication" boot log line was observed. */
    private record StoreStartResult(Process process, boolean bootObserved) {}

    private static boolean portAvailabilityCheck(ClusterInfo clusterInfo, Consumer<String> writer) {
        boolean yaciPortAvailable = PortUtil.isPortAvailable(clusterInfo.getYaciStorePort());
        if (!yaciPortAvailable) {
            writer.accept(error("Yaci Store Port " + clusterInfo.getYaciStorePort() + " is not available. Please check if the port is already in use."));
        }

        if (!yaciPortAvailable)
            return false;
        else
            return true;
    }

    private static final long SYNC_WAIT_DEADLINE_MS = 60_000L;

    /**
     * Block until Yaci Store's latest indexed block height is within {@code slackBlocks}
     * of the cardano-node tip. Used to keep callers (notably {@code /devnet/reset}) from
     * returning before integration-test workloads can rely on the indexer.
     *
     * Slack is derived from the cluster's block-time so fast devnets (e.g. 0.2-0.3s)
     * don't fail the check on a single in-flight block:
     *   1s  -> 1 block
     *   0.3s -> 4 blocks
     *   0.2s -> 5 blocks (cap)
     *
     * Returns silently after a soft 60s deadline; matches existing "warn + continue" pattern.
     */
    private void waitForSyncToTip(ClusterInfo clusterInfo, Consumer<String> writer) {
        // ClusterUtilService.getTip reads CommandContext.INSTANCE.getEra(); the primary
        // fix lives in ClusterCommands.startLocalCluster() but set here too as defense
        // against alternate call paths.
        if (clusterInfo.getEra() != null) {
            CommandContext.INSTANCE.setEra(clusterInfo.getEra());
        }

        double blockTime = Math.max(clusterInfo.getBlockTime(), 0.1);
        final long maxLagBlocks = Math.min(5L, Math.max(1L, (long) Math.ceil(1.0 / blockTime)));

        String url = "http://localhost:" + clusterInfo.getYaciStorePort() + "/api/v1/blocks/latest";
        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();

        long deadlineMs = System.currentTimeMillis() + SYNC_WAIT_DEADLINE_MS;
        writer.accept(info("Waiting for Yaci Store to sync to chain tip (lag tolerance " + maxLagBlocks + " blocks)..."));

        while (true) {
            long remaining = deadlineMs - System.currentTimeMillis();
            if (remaining <= 0) {
                writer.accept(warn("Yaci Store did not reach chain tip within 60s. Proceeding anyway."));
                return;
            }

            Tuple<Long, Point> nodeTip = clusterUtilService.getTip(msg -> {}); // re-queried each iteration
            Long indexerHeight = fetchIndexerHeight(restTemplate, mapper, url);

            if (nodeTip != null && nodeTip._1 != null && indexerHeight != null) {
                long nodeHeight = nodeTip._1;
                long lag = nodeHeight - indexerHeight;
                // Non-negative guard: if indexer is ahead of node (stale DB / aborted reset),
                // don't false-pass — keep waiting until the indexer's height makes sense.
                if (lag >= 0 && lag <= maxLagBlocks) {
                    writer.accept(success("Yaci Store synced to chain tip (indexer height "
                            + indexerHeight + ", node height " + nodeHeight + ", lag " + lag + ")"));
                    return;
                }
            }
            // null/negative-lag cases fall through to the sleep + retry within the deadline.

            remaining = deadlineMs - System.currentTimeMillis();
            if (remaining <= 0) continue; // loop top will emit the timeout warn
            try {
                Thread.sleep(Math.min(1000L, remaining));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private Long fetchIndexerHeight(RestTemplate restTemplate, ObjectMapper mapper, String url) {
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return null;
            JsonNode body = mapper.readTree(resp.getBody());
            JsonNode height = body.get("height");
            return (height == null || height.isNull()) ? null : height.asLong();
        } catch (RestClientException e) {
            // indexer HTTP not ready, or 404 because no blocks indexed yet — retry
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Yano-only Store derives epoch protocol params from indexed epoch/adapot state.
     * Blocks may be caught up before /epochs/latest/parameters has advanced to the
     * post-bootstrap params, so wait for the exact data CCL uses for script hashes.
     */
    private boolean waitForYanoOnlyProtocolParamsReady(ClusterInfo clusterInfo, Consumer<String> writer) {
        String adapotUrl = "http://localhost:" + clusterInfo.getYaciStorePort() + "/api/v1/adapot";
        BFBackendService storeBackendService = new BFBackendService(blockfrostBaseUrl(clusterInfo.getYaciStorePort()), "dummy_key");
        BFBackendService yanoBackendService = new BFBackendService(blockfrostBaseUrl(clusterInfo.getYanoHttpPort()), "dummy_key");
        int targetAdapotEpoch = ClusterConfig.YANO_BOOTSTRAP_EPOCH_SHIFT;

        RestTemplate restTemplate = new RestTemplate();
        ObjectMapper mapper = new ObjectMapper();
        long deadlineMs = System.currentTimeMillis() + SYNC_WAIT_DEADLINE_MS;
        String lastStatus = null;

        writer.accept(info("Waiting for Yaci Store protocol parameters to match Yano..."));

        while (true) {
            long remaining = deadlineMs - System.currentTimeMillis();
            if (remaining <= 0) {
                writer.accept(error("Yaci Store protocol parameters did not match Yano within 60s."));
                return false;
            }

            Long adapotEpoch = fetchLatestAdapotEpoch(restTemplate, mapper, adapotUrl);
            ProtocolParamsProbe yanoParams = fetchProtocolParamsProbe(yanoBackendService, targetAdapotEpoch);
            ProtocolParamsProbe storeParams = fetchProtocolParamsProbe(storeBackendService);

            boolean adapotReady = adapotEpoch != null && adapotEpoch >= targetAdapotEpoch;
            boolean paramsReady = protocolParamsMatch(yanoParams, storeParams);
            if (adapotReady && paramsReady) {
                writer.accept(success("Yaci Store protocol parameters are ready "
                        + "(adapot epoch " + adapotEpoch + ", PlutusV3 cost model size "
                        + storeParams.plutusV3CostModelSize() + ")"));
                return true;
            }

            String status = "Waiting for Yaci Store protocol params: adapot epoch "
                    + (adapotEpoch == null ? "?" : adapotEpoch)
                    + "/" + targetAdapotEpoch
                    + ", Store V3 size " + storeParams.plutusV3CostModelSize()
                    + ", Yano epoch " + targetAdapotEpoch + " V3 size " + yanoParams.plutusV3CostModelSize();
            if (!status.equals(lastStatus)) {
                writer.accept(info(status));
                lastStatus = status;
            }

            remaining = deadlineMs - System.currentTimeMillis();
            if (remaining <= 0) continue;
            try {
                Thread.sleep(Math.min(1000L, remaining));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }

    private String blockfrostBaseUrl(int port) {
        return "http://localhost:" + port + "/api/v1/";
    }

    private boolean protocolParamsMatch(ProtocolParamsProbe yanoParams, ProtocolParamsProbe storeParams) {
        return yanoParams.isComplete()
                && storeParams.isComplete()
                && yanoParams.protocolMajorVer().equals(storeParams.protocolMajorVer())
                && Arrays.equals(yanoParams.plutusV3CostModel(), storeParams.plutusV3CostModel());
    }

    private Long fetchLatestAdapotEpoch(RestTemplate restTemplate, ObjectMapper mapper, String url) {
        try {
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return null;
            JsonNode body = mapper.readTree(resp.getBody());
            JsonNode epoch = body.get("epoch");
            return epoch == null || epoch.isNull() ? null : epoch.asLong();
        } catch (Exception e) {
            return null;
        }
    }

    private ProtocolParamsProbe fetchProtocolParamsProbe(BFBackendService backendService) {
        try {
            Result<ProtocolParams> result = backendService.getEpochService().getProtocolParameters();
            return protocolParamsProbe(result);
        } catch (Exception e) {
            return ProtocolParamsProbe.empty();
        }
    }

    private ProtocolParamsProbe fetchProtocolParamsProbe(BFBackendService backendService, int epoch) {
        try {
            Result<ProtocolParams> result = backendService.getEpochService().getProtocolParameters(epoch);
            return protocolParamsProbe(result);
        } catch (Exception e) {
            return ProtocolParamsProbe.empty();
        }
    }

    private ProtocolParamsProbe protocolParamsProbe(Result<ProtocolParams> result) {
        if (!result.isSuccessful() || result.getValue() == null)
            return ProtocolParamsProbe.empty();

        ProtocolParams protocolParams = result.getValue();
        return new ProtocolParamsProbe(
                protocolParams.getProtocolMajorVer(),
                plutusV3RawCostModel(protocolParams)
        );
    }

    private long[] plutusV3RawCostModel(ProtocolParams protocolParams) {
        if (protocolParams.getCostModelsRaw() == null) return new long[0];

        List<Long> plutusV3 = protocolParams.getCostModelsRaw().get(PLUTUS_V3);
        if (plutusV3 == null || plutusV3.isEmpty()) return new long[0];

        long[] values = new long[plutusV3.size()];
        for (int i = 0; i < plutusV3.size(); i++) {
            values[i] = plutusV3.get(i);
        }
        return values;
    }

    private record ProtocolParamsProbe(Integer protocolMajorVer, long[] plutusV3CostModel) {
        static ProtocolParamsProbe empty() {
            return new ProtocolParamsProbe(null, new long[0]);
        }

        boolean isComplete() {
            return protocolMajorVer != null && plutusV3CostModel.length > 0;
        }

        int plutusV3CostModelSize() {
            return plutusV3CostModel.length;
        }
    }

    private StoreStartResult startStoreApp(ClusterInfo clusterInfo, Era era, Consumer<String> writer) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(new File(clusterConfig.getYaciStoreBinPath()));

        boolean yanoOnly = NodeMode.YANO_ONLY == clusterInfo.getNodeMode();
        String txEvaluatorMode = resolveTxEvaluatorMode(writer);
        builder.environment().put("STORE_SUBMIT_TX_EVALUATOR_MODE", txEvaluatorMode);
        writer.accept(info("Yaci Store tx evaluator mode: " + txEvaluatorMode));

        // Select the native Store binary by node mode (no legacy/JAR fallback):
        //   yano-only  -> yaci-store-all (no N2C; derives protocol params from epoch_param)
        //   all others -> yaci-store-n2c (N2C local-state-query against the Haskell node)
        String storeVariant = yanoOnly ? "all" : "n2c";
        String nativeBinName = "yaci-store-" + storeVariant;

        String effectiveMode = yaciStoreMode;

        if (effectiveMode == null || effectiveMode.equals("java")) {
            // Explicit Java mode is an advanced/external-DB opt-in. No JAR is bundled in Docker.
            Path yaciStoreJar = Path.of(clusterConfig.getYaciStoreBinPath(), "yaci-store.jar");
            if (!yaciStoreJar.toFile().exists()) {
                if (appConfig.isDocker()) {
                    writeLn(error("yaci.store.mode=java is unsupported in Docker: no yaci-store.jar is bundled. "
                            + "Mount a jar at " + clusterConfig.getYaciStoreBinPath() + "/yaci-store.jar, or use native mode."));
                } else {
                    writeLn(error("yaci-store.jar not found at " + clusterConfig.getYaciStoreBinPath()
                            + ". Run: download --component yaci-store-jar --overwrite, or use native mode (yaci.store.mode=native)."));
                }
                return new StoreStartResult(null, false);
            }
        } else if (effectiveMode.equals("native")) {
            Path yaciStoreBin = Path.of(clusterConfig.getYaciStoreBinPath(), nativeBinName);
            if (!yaciStoreBin.toFile().exists()) {
                writeLn(error("Yaci Store binary for " + clusterInfo.getNodeMode().getValue()
                        + " mode was not found: " + nativeBinName + "."));
                writeLn(error("Please run: download --component yaci-store --overwrite"));
                return new StoreStartResult(null, false);
            }
        }

        // Generate Store config for BOTH Docker and non-Docker so the correct node endpoints
        // (Yano vs Haskell), submit URL and epoch settings are applied per mode. Abort before
        // launching the process if config generation fails (otherwise Store starts with stale config).
        if (!yaciStoreConfigBuilder.build(clusterInfo, txEvaluatorMode)) {
            writeLn(error("Failed to generate Yaci Store configuration."));
            return new StoreStartResult(null, false);
        }

        if (effectiveMode != null && effectiveMode.equals("native")) {
            // N2C era only applies to the n2c variant; the yano-only (-all) binary has no N2C.
            if (!yanoOnly) {
                builder.environment().put("STORE_CARDANO_N2C_ERA", era.name());
            }
            builder.environment().put("STORE_CARDANO_PROTOCOL_MAGIC", String.valueOf(clusterInfo.getProtocolMagic()));
            if (OSUtil.getOperatingSystem() == OSUtil.OS.WINDOWS) {
                builder.command(clusterConfig.getYaciStoreBinPath() + File.separator + nativeBinName + ".exe");
            } else {
                builder.command(clusterConfig.getYaciStoreBinPath() + File.separator + nativeBinName);
            }
        } else {
            String javaExecPath = jreResolver.getJavaCommand();

            List<String> cmd = new ArrayList<>();
            cmd.add(javaExecPath);
            // N2C era only for the n2c path; yano-only Java mode must omit it.
            if (!yanoOnly) {
                cmd.add("-Dstore.cardano.n2c-era=" + era.name());
            }
            cmd.add("-Dstore.cardano.protocol-magic=" + clusterInfo.getProtocolMagic());
            if (yanoOnly) {
                cmd.add("-Dstore.epoch.endpoints.epoch.local.enabled=false");
            }
            cmd.add("-jar");
            cmd.add(clusterConfig.getYaciStoreBinPath() + File.separator + "yaci-store.jar");
            builder.command(cmd);

            writeLn(info("Java Path: " + javaExecPath));
        }

        //Set custom db info if provided through env file (docker) or application.properties
        if (customDBHelper.getStoreDbUrl() != null) {
            builder.environment().put("SPRING_DATASOURCE_URL", customDBHelper.getStoreDbUrl());

            writeLn(info("Yaci Store DB Url: " + customDBHelper.getStoreDbUrl()));
            writeLn(info("Yaci Store DB User: " + customDBHelper.getStoreDbUsername()));
        }
        if (customDBHelper.getStoreDbUsername() != null)
            builder.environment().put("SPRING_DATASOURCE_USERNAME", customDBHelper.getStoreDbUsername());
        if (customDBHelper.getStoreDbPassword() != null)
            builder.environment().put("SPRING_DATASOURCE_PASSWORD", customDBHelper.getStoreDbPassword());

        Process process = builder.start();

        writeLn(success("Yaci store starting ..."));
        AtomicBoolean started = new AtomicBoolean(false);
        AtomicBoolean intersectNotFoundAlreadyShown = new AtomicBoolean(false);
        ProcessStream processStream =
                new ProcessStream(process.getInputStream(), line -> {
                    logs.add(line);
                    if (line != null && line.contains("Started YaciStoreApplication")) {
                        writeLn(infoLabel("OK", "Yaci Store Started"));
                        started.set(true);
                    }

                    if (line != null && customDBHelper.getStoreDbUrl() != null && !customDBHelper.getStoreDbUrl().isEmpty()) {
                        if (!intersectNotFoundAlreadyShown.get() && line.contains("IntersactNotFound")) {
                            writeLn(warn("Looks like some issue while starting yaci store."));
                            writeLn(warn("Please check the logs for more details. Command: yaci-store-logs"));
                            writeLn(warn("Please verify if you are using an empty schema while creating a new devnet."));
                            intersectNotFoundAlreadyShown.set(true);
                        }
                    }
                });
        Future<?> future = Executors.newSingleThreadExecutor().submit(processStream);

        int counter = 0;
        while (counter < 40) {
            counter++;
            if (started.get())
                break;
            Thread.sleep(1000);
            writeLn("Waiting for Yaci Store to start ...");
        }

        if (!started.get()) {
            writeLn(error("Waited too long. Could not start Yaci Store. Something is wrong.."));
            writeLn(error("Use \"yaci-store-logs\" to see the logs"));
            writeLn(error("Please verify if another yaci-store in running in the same port. " +
                    "If so, please check the process and kill it manually. e.g. kill -9 <pid>"));
        }

        if (customDBHelper.getStoreDbUrl() != null && !customDBHelper.getStoreDbUrl().isEmpty()) {
            writeLn("");
            writeLn(info("######################### Important ########################################################################################"));
            writeLn("!!!! Yaci Store is connecting to an external database: " + customDBHelper.getStoreDbUrl());
            writeLn("Automatic management of an external database may not be possible during 'reset' or when creating a new devnet with 'create-node'.");
            writeLn("You can use the 'yaci-store-drop-db' command to drop the schema. If that doesn’t work, " +
                    "please drop the schema manually before performing a reset or creating a new devnet.");
            writeLn("Use the 'yaci-store-logs' command to verify if Yaci Store has started successfully.");
            writeLn("###########################################################################################################################");
        }

        processUtil.createProcessId(STORE_PROCESS_NAME, process);

        return new StoreStartResult(process, started.get());
    }

    private String resolveTxEvaluatorMode(Consumer<String> writer) {
        if (appConfig.isOgmiosEnabled() && ogmiosService.isOgmiosRunning())
            return TX_EVALUATOR_MODE_OGMIOS;

        if (appConfig.isOgmiosEnabled()) {
            writer.accept(warn("Ogmios is enabled but not running. Using Scalus tx evaluator for Yaci Store."));
        }

        return TX_EVALUATOR_MODE_SCALUS;
    }

    private Process startViewerApp(String cluster) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ProcessBuilder builder = new ProcessBuilder();

        Path yaciViewerFolder = Path.of(clusterConfig.getYaciStoreBinPath(), "yaci-viewer");
        if (!yaciViewerFolder.toFile().exists()) {
            writeLn(error("yaci-viewer folder is not found at " + clusterConfig.getYaciStoreBinPath()));
            return null;
        }

        builder.directory(yaciViewerFolder.toFile());

        builder.command("npm", "run", "dev");

        Process process = builder.start();

        writeLn(success("Yaci Viewer starting ..."));
        AtomicBoolean started = new AtomicBoolean(false);
        ProcessStream processStream =
                new ProcessStream(process.getInputStream(), line -> {
                    logs.add(line);
                    if (line != null && line.contains("ready in")) {
                        writeLn(infoLabel("OK", "Yaci Viwer was started successfully"));
                        started.set(true);
                    }
                });
        Future<?> future = Executors.newSingleThreadExecutor().submit(processStream);

        int counter = 0;
        while (counter < 20) {
            counter++;
            if (started.get())
                break;
            Thread.sleep(1000);
            writeLn("Waiting for Yaci Viewer to start ...");
        }

        if (counter == 20) {
            writeLn("Waited too long. Could not start Yaci Viewer. Something is wrong..");
        }

        return process;
    }

    @EventListener
    public void handleClusterStopped(ClusterStopped clusterStopped) {
        stop();
    }

    public boolean stop() {
        try {
            if (processes != null && processes.size() > 0)
                writeLn(info("Trying to stop yaci-store ..."));

            boolean error = false;
            for (Process process : processes) {
                if (process != null && process.isAlive()) {
                    process.descendants().forEach(processHandle -> {
                        writeLn(infoLabel("Process", String.valueOf(processHandle.pid())));
                        processHandle.destroyForcibly();
                    });
                    process.destroyForcibly();
                    killForcibly(process);
                    writeLn(info("Stopping yaci-store process : " + process));
                    process.waitFor(15, TimeUnit.SECONDS);
                    if (!process.isAlive()) {
                        writeLn(success("Killed : " + process));
                    } else {
                        writeLn(error("Process could not be killed : " + process));
                    }
                }
            }

            if (!error) {
                //clean pid files
                processUtil.deletePidFile(STORE_PROCESS_NAME);
            }

            logs.clear();
        } catch (Exception e) {
            log.error("Error stopping process", e);
            writeLn(error("Yaci Store could not be stopped. Please kill the process manually." + e.getMessage()));
            return false;
        } finally {
            processes.clear();
        }

        return true;
    }

    @EventListener
    public void handleClusterDeleted(ClusterDeleted clusterDeleted) {
        customDBHelper.dropDatabase(clusterDeleted.getClusterName());
    }

    @EventListener
    public void handleRollbackDone(RollbackDone rollbackDone) {
        String clusterName = rollbackDone.getClusterName();

        stopAndSyncFromBeginning(clusterName, msg -> writeLn(msg));
    }

    public boolean stopAndSyncFromBeginning(String clusterName, Consumer<String> writer) {
        if (!clusterStartService.isClusterRunning()) {
            writer.accept(warn("Node is not running. Please start the cluster first."));
            return false;
        }

        stop();

        //delete yaci-store db folder
        try {
            customDBHelper.dropDatabase(clusterName);
        } catch (Exception e) {
        }

        start(clusterName, writer);
        return true;
    }

    private void killForcibly(Process process) {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("kill", "-9", String.valueOf(process.pid()));

            Process killProcess = builder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void showLogs(Consumer<String> consumer) {
        if (logs.isEmpty()) {
            consumer.accept("No log to show");
        } else {
            int counter = 0;
            while (!logs.isEmpty()) {
                counter++;
                if (counter == 200)
                    return;
                consumer.accept(logs.poll());
            }
        }
    }
}
