package com.bloxbean.cardano.yacicli.localcluster.yacistore;

import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.util.OSUtil;
import com.bloxbean.cardano.yacicli.commands.common.JreResolver;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.config.ApplicationConfig;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterDeleted;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStarted;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStopped;
import com.bloxbean.cardano.yacicli.util.PortUtil;
import com.bloxbean.cardano.yacicli.util.ProcessStream;
import com.google.common.collect.EvictingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final ApplicationConfig appConfig;
    private final ClusterService clusterService;
    private final ClusterConfig clusterConfig;
    private final JreResolver jreResolver;
    private final YaciStoreConfigBuilder yaciStoreConfigBuilder;
    private final YaciStoreCustomDbHelper customDBHelper;

    private List<Process> processes = new ArrayList<>();

    @Value("${yaci.store.mode:java}")
    private String yaciStoreMode;

    private Queue<String> logs = EvictingQueue.create(300);

    @EventListener
    public void handleClusterStarted(ClusterStarted clusterStarted) {
        logs.clear();
        if (!appConfig.isYaciStoreEnabled())
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
            Process process = startStoreApp(clusterInfo, era);
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

    private Process startStoreApp(ClusterInfo clusterInfo, Era era) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(new File(clusterConfig.getYaciStoreBinPath()));

        if (yaciStoreMode == null || yaciStoreMode.equals("java")) {
            Path yaciStoreJar = Path.of(clusterConfig.getYaciStoreBinPath(), "yaci-store.jar");
            if (!yaciStoreJar.toFile().exists()) {
                writeLn(error("yaci-store.jar is not found at " + clusterConfig.getYaciStoreBinPath()));
                return null;
            }
        } else if (yaciStoreMode != null && yaciStoreMode.equals("native")) {
            Path yaciStoreBin = Path.of(clusterConfig.getYaciStoreBinPath(), "yaci-store");
            if (!yaciStoreBin.toFile().exists()) {
                writeLn(error("yaci-store binary is not found at " + clusterConfig.getYaciStoreBinPath()));
                return null;
            }
        }

        if (!appConfig.isDocker()) {
            yaciStoreConfigBuilder.build(clusterInfo);
        }

        if (yaciStoreMode != null && yaciStoreMode.equals("native")) {
            builder.environment().put("STORE_CARDANO_N2C_ERA", era.name());
            builder.environment().put("STORE_CARDANO_PROTOCOL_MAGIC", String.valueOf(clusterInfo.getProtocolMagic()));
            if (OSUtil.getOperatingSystem() == OSUtil.OS.WINDOWS) {
                builder.command(clusterConfig.getYaciStoreBinPath() + File.separator + "yaci-store.exe");
            } else {
                builder.command(clusterConfig.getYaciStoreBinPath() + File.separator + "yaci-store");
            }
        } else {
            String javaExecPath = jreResolver.getJavaCommand();

            if (OSUtil.getOperatingSystem() == OSUtil.OS.WINDOWS) {
                builder.command(javaExecPath, "-Dstore.cardano.n2c-era=" + era.name(), "-Dstore.cardano.protocol-magic=" + clusterInfo.getProtocolMagic(), "-jar", clusterConfig.getYaciStoreBinPath() + File.separator + "yaci-store.jar");
            } else {
                builder.command(javaExecPath, "-Dstore.cardano.n2c-era=" + era.name(), "-Dstore.cardano.protocol-magic=" + clusterInfo.getProtocolMagic(), "-jar", clusterConfig.getYaciStoreBinPath() + File.separator + "yaci-store.jar");
            }

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

        if (counter == 40) {
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
        customDBHelper.dropDatabase(clusterDeleted.getClusterName());
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
