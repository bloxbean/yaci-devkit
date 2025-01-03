package com.bloxbean.cardano.yacicli.localcluster.ogmios;

import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterPortInfoHelper;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final ClusterConfig clusterConfig;
    private final ClusterPortInfoHelper clusterPortInfoHelper;
    private final TemplateEngine templateEngine;
    private final ProcessUtil processUtil;

    private List<Process> processes = new ArrayList<>();

    private Queue<String> ogmiosLogs = EvictingQueue.create(300);
    private Queue<String> kupoLogs = EvictingQueue.create(300);

    @EventListener
    public void handleClusterStarted(ClusterStarted clusterStarted) {
        ogmiosLogs.clear();
        kupoLogs.clear();
        if (!appConfig.isOgmiosEnabled() && !appConfig.isKupoEnabled())
            return;

        if (!clusterStarted.getClusterName().equals("default")) {
            writeLn("Ogmios/Kupo is only supported for 'default' cluster");
            return;
        }

        try {
            ClusterInfo clusterInfo = clusterService.getClusterInfo(clusterStarted.getClusterName());
            if (clusterInfo == null)
                throw new IllegalStateException("Cluster info not found for cluster: " + clusterStarted.getClusterName()
                        + ". Please check if the cluster is created.");

            if (appConfig.isOgmiosEnabled()) {
                if (!ogmiosPortAvailabilityCheck(clusterInfo, (msg) -> writeLn(msg)))
                    return;

                Process process = startOgmios(clusterStarted.getClusterName(), clusterInfo);
                if (process != null)
                    processes.add(process);
            }

            if (appConfig.isKupoEnabled()) {
                if (!kupoPortAvailabilityCheck(clusterInfo, (msg) -> writeLn(msg)))
                    return;

                Process kupoProcess = startKupo(clusterStarted.getClusterName(), clusterInfo);
                if (kupoProcess != null)
                    processes.add(kupoProcess);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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
        try {
            if (processes != null && processes.size() > 0)
                writeLn(info("Trying to stop ogmios/kupo ..."));

            boolean error = false;
            for (Process process : processes) {
                if (process != null && process.isAlive()) {
                    process.descendants().forEach(processHandle -> {
                        writeLn(infoLabel("Process", String.valueOf(processHandle.pid())));
                        processHandle.destroyForcibly();
                    });
                    process.destroyForcibly();
                    killForcibly(process);
                    writeLn(info("Stopping ogmios/kupo process : " + process));
                    process.waitFor(15, TimeUnit.SECONDS);
                    if (!process.isAlive()) {
                        writeLn(success("Killed : " + process));
                    } else {
                        writeLn(error("Process could not be killed : " + process));
                        error = true;
                    }
                }
            }

            if (!error) {
                //clean pid files
                processUtil.deletePidFile(OGMIOS_PROCESS_NAME);
                processUtil.deletePidFile(KUPO_PROCESS_NAME);
            }

            ogmiosLogs.clear();
        } catch (Exception e) {
            log.error("Error stopping process", e);
            writeLn(error("Ogmios/Kupo could not be stopped. Please kill the process manually." + e.getMessage()));
        }

        processes.clear();
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
