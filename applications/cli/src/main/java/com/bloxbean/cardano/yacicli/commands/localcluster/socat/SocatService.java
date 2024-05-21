package com.bloxbean.cardano.yacicli.commands.localcluster.socat;

import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterPortInfoHelper;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterCreated;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterStarted;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterStopped;
import com.bloxbean.cardano.yacicli.util.PortUtil;
import com.bloxbean.cardano.yacicli.util.ProcessStream;
import com.bloxbean.cardano.yacicli.util.TemplateEngine;
import com.google.common.collect.EvictingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class SocatService {
    private final static String SOCAT_SCRIPT = "socat.sh";
    private final ClusterService clusterService;
    private final ClusterPortInfoHelper clusterPortInfoHelper;

    private List<Process> processes = new ArrayList<>();

    @Value("${socat.enabled:true}")
    private boolean enableSocat;

    @Autowired
    TemplateEngine templateEngine;

    private Queue<String> socatLogs = EvictingQueue.create(300);

    @EventListener
    public void handleClusterStarted(ClusterStarted clusterStarted) {
        socatLogs.clear();
        if (!enableSocat)
            return;

        if (!clusterStarted.getClusterName().equals("default")) {
            writeLn("Socat is only supported for 'default' cluster");
            return;
        }

        try {
            ClusterInfo clusterInfo = clusterService.getClusterInfo(clusterStarted.getClusterName());
            if (clusterInfo == null)
                throw new IllegalStateException("Cluster info not found for cluster: " + clusterStarted.getClusterName()
                        + ". Please check if the cluster is created.");

            if (!portAvailabilityCheck(clusterInfo, (msg) -> writeLn(msg)))
                return;

            Process process = startSocat(clusterStarted.getClusterName(), clusterInfo);
            if (process != null)
                processes.add(process);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static boolean portAvailabilityCheck(ClusterInfo clusterInfo, Consumer<String> writer) {
        boolean socatPortAvailable = PortUtil.isPortAvailable(clusterInfo.getSocatPort());
        if (!socatPortAvailable) {
            writer.accept(error("Socat Port " + clusterInfo.getSocatPort() + " is not available. Please check if the port is already in use."));
        }

        if (!socatPortAvailable)
            return false;
        else
            return true;
    }

    private Process startSocat(String cluster, ClusterInfo clusterInfo) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        Path clusterFolder = clusterService.getClusterFolder(cluster);
        Objects.requireNonNull(clusterFolder, "Cluster folder not found for cluster: " + cluster);

        String clusterFolderPath = clusterFolder.toAbsolutePath().toString();

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "socat.sh");

        File socatStartDir = new File(clusterFolderPath);
        builder.directory(socatStartDir);
        Process process = builder.start();

        writeLn(success("Started n2c through socat : localhost:" + clusterPortInfoHelper.getN2cSocatPort(clusterInfo)));
        ProcessStream processStream =
                new ProcessStream(process.getInputStream(), line -> {
                    socatLogs.add("[socat] " + line);
                });
        Future<?> future = Executors.newSingleThreadExecutor().submit(processStream);
        return process;
    }

    @EventListener
    public void handleClusterStopped(ClusterStopped clusterStopped) {
        try {
            if (processes != null && processes.size() > 0)
                writeLn(info("Trying to stop socat ..."));

            for (Process process : processes) {
                if (process != null && process.isAlive()) {
                    process.descendants().forEach(processHandle -> {
                        writeLn(infoLabel("Process", String.valueOf(processHandle.pid())));
                        processHandle.destroyForcibly();
                    });
                    process.destroyForcibly();
                    killForcibly(process);
                    writeLn(info("Stopping socat : " + process));
                    process.waitFor(15, TimeUnit.SECONDS);
                    if (!process.isAlive()) {
                        writeLn(success("Killed : " + process));
                    } else {
                        writeLn(error("Process could not be killed : " + process));
                    }
                }
            }

            socatLogs.clear();
        } catch (Exception e) {
            log.error("Error stopping process", e);
            writeLn(error("Socat could not be stopped. Please kill the process manually." + e.getMessage()));
        }

        processes.clear();
    }

    @EventListener
    public void handleClusterCreated(ClusterCreated clusterCreated) throws IOException {
        ClusterInfo clusterInfo = clusterService.getClusterInfo(clusterCreated.getClusterName());
        Path clusterFolder = clusterService.getClusterFolder(clusterCreated.getClusterName());
        updateSocatStartScript(clusterFolder, clusterInfo);
    }

    private void updateSocatStartScript(Path clusterFolder, ClusterInfo clusterInfo) throws IOException {
        Map<String, String> values = new HashMap<>();
        values.put("SOCAT_PORT", String.valueOf(clusterInfo.getSocatPort()));

        Path ogmiosSh = clusterFolder.resolve(SOCAT_SCRIPT);
        try {
            templateEngine.replaceValues(ogmiosSh, values);
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
        if (socatLogs.isEmpty()) {
            consumer.accept("No log to show");
        } else {
            int counter = 0;
            while (!socatLogs.isEmpty()) {
                counter++;
                if (counter == 200)
                    return;
                consumer.accept(socatLogs.poll());
            }
        }
    }
}
