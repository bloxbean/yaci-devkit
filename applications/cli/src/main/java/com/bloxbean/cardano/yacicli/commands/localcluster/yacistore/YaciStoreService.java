package com.bloxbean.cardano.yacicli.commands.localcluster.yacistore;

import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.util.OSUtil;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterDeleted;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterStarted;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.ClusterStopped;
import com.bloxbean.cardano.yacicli.util.PortUtil;
import com.bloxbean.cardano.yacicli.util.ProcessStream;
import com.google.common.collect.EvictingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
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
    private final ClusterService clusterService;
    private final ClusterConfig clusterConfig;

    private List<Process> processes = new ArrayList<>();

    @Value("${yaci.store.enabled:false}")
    private boolean enableYaciStore;

    private Queue<String> logs = EvictingQueue.create(300);

    @EventListener
    public void handleClusterStarted(ClusterStarted clusterStarted) {
        logs.clear();
        if (!enableYaciStore)
            return;

        if (!clusterStarted.getClusterName().equals("default")) {
            writeLn("Yaci Store is only supported for 'default' cluster");
            return;
        }

        try {
            ClusterInfo clusterInfo = clusterService.getClusterInfo(clusterStarted.getClusterName());
            if (clusterInfo == null)
                throw new IllegalStateException("Cluster info not found for cluster: " + clusterStarted.getClusterName()
                        + ". Please check if the cluster is created.");

            if (!portAvailabilityCheck(clusterInfo, (msg) -> writeLn(msg)))
                return;

            Era era = clusterInfo.getEra();
            Process process = startStoreApp(clusterStarted.getClusterName(), era);
            if (process != null)
                processes.add(process);
//            Process viewerProcess = startViewerApp(clusterStarted.getClusterName());
//                processes.add(viewerProcess);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

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

    private Process startStoreApp(String cluster, Era era) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(new File(clusterConfig.getYaciStoreBinPath()));

        Path yaciStoreJar = Path.of(clusterConfig.getYaciStoreBinPath(), "yaci-store.jar");
        if (!yaciStoreJar.toFile().exists()) {
            writeLn(error("yaci-store.jar is not found at " + clusterConfig.getYaciStoreBinPath()));
            return null;
        }

        if (OSUtil.getOperatingSystem() == OSUtil.OS.WINDOWS) {
            builder.command("java", "-Dstore.cardano.n2c-era=" + era.name(), "-jar", clusterConfig.getYaciStoreBinPath() + File.separator + "yaci-store.jar");
        } else {
            builder.command("java", "-Dstore.cardano.n2c-era=" + era.name(), "-jar", clusterConfig.getYaciStoreBinPath() + File.separator + "yaci-store.jar");
        }

        Process process = builder.start();

        writeLn(success("Yaci store starting ..."));
        AtomicBoolean started = new AtomicBoolean(false);
        ProcessStream processStream =
                new ProcessStream(process.getInputStream(), line -> {
                    logs.add(line);
                    if (line != null && line.contains("Started YaciStoreApplication")) {
                        writeLn(infoLabel("OK", "Yaci Store Started"));
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
            writeLn("Waiting for Yaci Store to start ...");
        }

        if (counter == 20) {
            writeLn(error("Waited too long. Could not start Yaci Store. Something is wrong.."));
            writeLn(error("Use \"yaci-store-logs\" to see the logs"));
            writeLn(error("Please verify if another yaci-store in running in the same port. " +
                    "If so, please check the process and kill it manually. e.g. kill -9 <pid>"));
        }

        return process;
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
        try {
            if (processes != null && processes.size() > 0)
                writeLn(info("Trying to stop yaci-store ..."));

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

            logs.clear();
        } catch (Exception e) {
            log.error("Error stopping process", e);
            writeLn(error("Yaci Store could not be stopped. Please kill the process manually." + e.getMessage()));
        }

        processes.clear();
    }

    @EventListener
    public void handleClusterDeleted(ClusterDeleted clusterDeleted) {
        Path clusterFolder = clusterService.getClusterFolder(clusterDeleted.getClusterName());
        String dbDir = "yaci_store";
        Path dbPath = clusterFolder.resolve(dbDir);

        if (dbPath.toFile().exists()) {
            try {
                FileUtils.deleteDirectory(dbPath.toFile());
            } catch (IOException e) {
                writeLn(error("Yaci store db could not be deleted, " + dbPath.toAbsolutePath()));
            }
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

    public boolean isEnableYaciStore() {
        return enableYaciStore;
    }

    public void setEnableYaciStore(boolean enableYaciStore) {
        this.enableYaciStore = enableYaciStore;
    }
}
