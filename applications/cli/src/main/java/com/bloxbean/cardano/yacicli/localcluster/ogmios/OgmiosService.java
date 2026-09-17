package com.bloxbean.cardano.yacicli.localcluster.ogmios;

import com.bloxbean.cardano.yacicli.localcluster.*;
import com.bloxbean.cardano.yacicli.localcluster.config.ApplicationConfig;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterCreated;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterDeleted;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStarted;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStopped;
import com.bloxbean.cardano.yacicli.util.PortUtil;
import com.bloxbean.cardano.yacicli.util.ProcessUtil;
import com.bloxbean.cardano.yacicli.util.TemplateEngine;
import com.google.common.collect.EvictingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class OgmiosService {
    private final static String OGMIOS_PROCESS_NAME = "ogmios";
    private final static String KUPO_PROCESS_NAME = "kupo";
    private final ApplicationConfig appConfig;
    private final ClusterService clusterService;
    private final ClusterStartService clusterStartService;
    private final ClusterConfig clusterConfig;
    private final ClusterPortInfoHelper clusterPortInfoHelper;
    private final TemplateEngine templateEngine;
    private final ProcessUtil processUtil;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    private List<Process> processes = new ArrayList<>();
    private Process ogmiosProcess;

    private Queue<String> ogmiosLogs = EvictingQueue.create(300);
    private Queue<String> kupoLogs = EvictingQueue.create(300);

    @EventListener
    @Order(0)
    public void handleClusterStarted(ClusterStarted clusterStarted) {
        String clusterName = clusterStarted.getClusterName();

        //Ogmios is optional. This listener runs before YaciStoreService's (@Order(1)), so an exception here
        //would abort the rest of the startup chain and prevent Yaci Store from starting at all.
        try {
            start(clusterName, msg -> writeLn(msg));
        } catch (Exception e) {
            log.error("Error starting Ogmios/Kupo", e);
            writeLn(error("Could not start Ogmios/Kupo : " + e.getMessage()));
        }
    }

    public boolean start(String clusterName, Consumer<String> writer) {
        ogmiosLogs.clear();
        kupoLogs.clear();
        if (!appConfig.isOgmiosEnabled() && !appConfig.isKupoEnabled())
            return false;

        if (!clusterName.equals("default")) {
            writeLn("Ogmios/Kupo is only supported for 'default' cluster");
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

            //Ogmios/Kupo talk to the Haskell node over its n2c socket, which doesn't exist in yano-only mode.
            NodeMode nodeMode = clusterInfo.getNodeMode() != null ? clusterInfo.getNodeMode() : NodeMode.HASKELL_ONLY;
            if (nodeMode == NodeMode.YANO_ONLY) {
                writer.accept(info("Ogmios/Kupo require a Haskell cardano-node and are not supported in yano-only mode. Skipping."));
                return false;
            }

            if (appConfig.isOgmiosEnabled()) {
                if (!ogmiosPortAvailabilityCheck(clusterInfo, writer))
                    return false;

                Process process = startOgmios(clusterName, clusterInfo);
                if (process != null) {
                    ogmiosProcess = process;
                    processes.add(process);
                }
            }

            if (appConfig.isKupoEnabled()) {
                if (!kupoPortAvailabilityCheck(clusterInfo, writer))
                    return false;

                Process kupoProcess = startKupo(clusterName, clusterInfo);
                if (kupoProcess != null)
                    processes.add(kupoProcess);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return true;
    }

    public boolean isOgmiosRunning() {
        return ogmiosProcess != null && ogmiosProcess.isAlive();
    }

    /**
     * Waits until Ogmios actually answers on its HTTP health endpoint.
     * <p>
     * {@link #isOgmiosRunning()} only reports that the process was still alive a second after spawn, which is
     * not enough to hand Yaci Store the "ogmios" tx evaluator : if Ogmios dies or never connects to the node
     * socket shortly after start, the evaluator mode is already baked into the Store process and every Plutus
     * script evaluation would fail for the lifetime of the devnet. Callers use this to fall back to Scalus.
     */
    public boolean waitForOgmiosReady(int ogmiosPort, Consumer<String> writer) {
        String healthUrl = "http://localhost:" + ogmiosPort + "/health";
        int maxAttempts = 15;

        for (int i = 0; i < maxAttempts; i++) {
            if (!isOgmiosRunning()) {
                writer.accept(warn("Ogmios process is no longer running."));
                return false;
            }

            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(healthUrl))
                        .timeout(Duration.ofSeconds(2))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200)
                    return true;
            } catch (Exception e) {
                //Not ready yet
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        writer.accept(warn("Ogmios did not become ready within timeout."));
        return false;
    }

    private static boolean ogmiosPortAvailabilityCheck(ClusterInfo clusterInfo, Consumer<String> writer) {
        boolean ogmiosPortAvailable = PortUtil.isPortAvailable(clusterInfo.getOgmiosPort());
        if (!ogmiosPortAvailable) {
            writer.accept(error("Ogmios Port " + clusterInfo.getOgmiosPort() + " is not available. Please check if the port is already in use."));
        }

        return ogmiosPortAvailable;
    }

    private static boolean kupoPortAvailabilityCheck(ClusterInfo clusterInfo, Consumer<String> writer) {
        boolean kupoPortAvailable = PortUtil.isPortAvailable(clusterInfo.getKupoPort());
        if (!kupoPortAvailable) {
            writer.accept(error("Kupo Port " + clusterInfo.getKupoPort() + " is not available. Please check if the port is already in use."));
        }

        return kupoPortAvailable;
    }

    private Process startOgmios(String cluster, ClusterInfo clusterInfo) throws IOException, InterruptedException {
        Path clusterFolder = clusterService.getClusterFolder(cluster);
        Objects.requireNonNull(clusterFolder, "Cluster folder not found for cluster: " + cluster);

        String clusterFolderPath = clusterFolder.toAbsolutePath().toString();

        //Check if ogmios exists
        String ogmiosBin = "ogmios";
        Path ogmiosBinFolder = Path.of(clusterConfig.getOgmiosHome(), "bin");
        Path ogmiosBinaryPath = ogmiosBinFolder.resolve(ogmiosBin);
        if (!Files.exists(ogmiosBinaryPath)) {
            writeLn(info("ogmios could not be started."));
            writeLn(info("To start ogmios, you need to copy ogmios binary to " + ogmiosBinFolder));
            return null;
        }

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "ogmios.sh");

        File ogmiosStartFolder = new File(clusterFolderPath);
        builder.directory(ogmiosStartFolder);

        Process process = processUtil.startLongRunningProcess("ogmios", builder, ogmiosLogs, s -> writeLn(s));

        if (process == null) {
            writeLn(error("Ogmios process could not be started."));
            return null;
        }

        writeLn(success("Started ogmios : http://localhost:" + clusterPortInfoHelper.getOgmiosPort(clusterInfo)));
        return process;
    }

    private Process startKupo(String cluster, ClusterInfo clusterInfo) throws IOException, InterruptedException {
        Path clusterFolder = clusterService.getClusterFolder(cluster);
        Objects.requireNonNull(clusterFolder, "Cluster folder not found for cluster: " + cluster);

        String clusterFolderPath = clusterFolder.toAbsolutePath().toString();

        //Check if kupo exists
        String kupoBin = "kupo";
        Path kupoBinFolder = Path.of(clusterConfig.getKupoHome(), "bin");
        Path kupoBinaryPath = kupoBinFolder.resolve(kupoBin);
        if (!Files.exists(kupoBinaryPath)) {
            writeLn(info("kupo could not be started."));
            writeLn(info("To start kupo, you need to copy ogmios binary to " + kupoBinFolder));
            return null;
        }

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "kupo.sh");

        File submitApiStartDir = new File(clusterFolderPath);
        builder.directory(submitApiStartDir);

        Process process = processUtil.startLongRunningProcess("kupo", builder, kupoLogs, s -> writeLn(s));

        if (process == null) {
            writeLn(error("Kupo process could not be started."));
            return null;
        }

        writeLn(success("Started kupo : http://localhost:" + clusterPortInfoHelper.getKupoPort(clusterInfo)));
        return process;
    }

    @EventListener
    public void handleClusterStopped(ClusterStopped clusterStopped) {
        stop(msg -> writeLn(msg));
    }

    public boolean stop(Consumer<String> writer) {
        try {
            if (processes != null && processes.size() > 0)
                writer.accept(info("Trying to stop ogmios/kupo ..."));

            boolean error = false;
            for (Process process : processes) {
                if (process != null && process.isAlive()) {
                    process.descendants().forEach(processHandle -> {
                        writer.accept(infoLabel("Process", String.valueOf(processHandle.pid())));
                        processHandle.destroyForcibly();
                    });
                    process.destroyForcibly();
                    killForcibly(process);
                    writer.accept(info("Stopping ogmios/kupo process : " + process));
                    process.waitFor(15, TimeUnit.SECONDS);
                    if (!process.isAlive()) {
                        writer.accept(success("Killed : " + process));
                    } else {
                        writer.accept(error("Process could not be killed : " + process));
                        error = true;
                    }
                }
            }

            if (!error) {
                //clean pid files
                processUtil.deletePidFile(OGMIOS_PROCESS_NAME);
                processUtil.deletePidFile(KUPO_PROCESS_NAME);
                ogmiosProcess = null;
            }

            ogmiosLogs.clear();
        } catch (Exception e) {
            log.error("Error stopping process", e);
            writer.accept(error("Ogmios/Kupo could not be stopped. Please kill the process manually." + e.getMessage()));
            return false;
        } finally {
            processes.clear();
        }
        return true;
    }

    @EventListener
    public void handleClusterDeleted(ClusterDeleted clusterDeleted) {
        Path clusterFolder = clusterService.getClusterFolder(clusterDeleted.getClusterName());
        String dbDir = "kupo_db";
        Path dbPath = clusterFolder.resolve(dbDir);

        if (dbPath.toFile().exists()) {
            try {
                FileUtils.deleteDirectory(dbPath.toFile());
            } catch (IOException e) {
                writeLn(error("kupo db could not be deleted, " + dbPath.toAbsolutePath()));
            }
        }
    }

    @EventListener
    public void handleClusterCreated(ClusterCreated clusterCreated) throws IOException {
        ClusterInfo clusterInfo = clusterService.getClusterInfo(clusterCreated.getClusterName());
        Path clusterFolder = clusterService.getClusterFolder(clusterCreated.getClusterName());
        updateOgmiosStartScript(clusterFolder, clusterInfo);
        updateKupoStartScript(clusterFolder, clusterInfo);
    }

    private void updateOgmiosStartScript(Path clusterFolder, ClusterInfo clusterInfo) throws IOException {
        Map<String, String> values = new HashMap<>();
        values.put("OGMIOS_BIN", clusterConfig.getOgmiosHome() + File.separator + "bin" + File.separator + "ogmios");
        values.put("OGMIOS_PORT", String.valueOf(clusterInfo.getOgmiosPort()));

        Path ogmiosSh = clusterFolder.resolve("ogmios.sh");
        try {
            templateEngine.replaceValues(ogmiosSh, values);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void updateKupoStartScript(Path clusterFolder, ClusterInfo clusterInfo) throws IOException {
        String dbDir = "kupo_db";
        Path dbPath = clusterFolder.resolve(dbDir);

        Map<String, String> values = new HashMap<>();
        values.put("KUPO_BIN", clusterConfig.getKupoHome() + File.separator + "bin" + File.separator + "kupo");
        values.put("KUPO_PORT", String.valueOf(clusterInfo.getKupoPort()));
        values.put("OGMIOS_PORT", String.valueOf(clusterInfo.getOgmiosPort()));
        values.put("KUPO_DB_DIR", dbPath.toAbsolutePath().toString());

        Path kupoSh = clusterFolder.resolve("kupo.sh");
        try {
            templateEngine.replaceValues(kupoSh, values);
        } catch (Exception e) {
            throw new IOException(e);
        }
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
        if (ogmiosLogs.isEmpty()) {
            consumer.accept("No log to show");
        } else {
            int counter = 0;
            while (!ogmiosLogs.isEmpty()) {
                counter++;
                if (counter == 200)
                    return;
                consumer.accept(ogmiosLogs.poll());
            }
        }
    }
}
