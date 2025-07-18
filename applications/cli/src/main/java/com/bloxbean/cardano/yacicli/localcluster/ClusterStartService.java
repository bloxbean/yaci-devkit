package com.bloxbean.cardano.yacicli.localcluster;

import com.bloxbean.cardano.yaci.core.util.OSUtil;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.localcluster.config.CustomGenesisConfig;
import com.bloxbean.cardano.yacicli.localcluster.config.GenesisConfig;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStarted;
import com.bloxbean.cardano.yacicli.localcluster.events.FirstRunDone;
import com.bloxbean.cardano.yacicli.localcluster.model.RunStatus;
import com.bloxbean.cardano.yacicli.localcluster.peer.LocalPeerService;
import com.bloxbean.cardano.yacicli.util.PortUtil;
import com.bloxbean.cardano.yacicli.util.ProcessUtil;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.EvictingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClusterStartService {
    public static final String SUBMIT_API_PROCESS_NAME = "submit-api";
    public static final String NODE_PROCESS_NAME = "node";
    private final ClusterConfig clusterConfig;
    private final ClusterPortInfoHelper clusterPortInfoHelper;
    private final ProcessUtil processUtil;
    private final GenesisConfig genesisConfig;
    private final CustomGenesisConfig customGenesisConfig;
    private final LocalPeerService localPeerService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private List<Process> processes = new ArrayList<>();

    private Queue<String> logs = EvictingQueue.create(200);
    private Queue<String> submitApiLogs = EvictingQueue.create(100);

    public RunStatus startCluster(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) {
        logs.clear();
        submitApiLogs.clear();

        if (processes.size() > 0) {
            writer.accept("A cluster is already running. You can only run one cluster at a time.");
            return new RunStatus(false, false);
        }

        //check if all ports are available
        if (!portAvailabilityCheck(clusterInfo, writer))
            return new RunStatus(false, false);

        try {
            boolean firstRun = checkIfFirstRun(clusterFolder);
            if (clusterInfo.isMasterNode() && firstRun)
                setupFirstRun(clusterInfo, clusterFolder, writer);

            if (clusterInfo.isLocalMultiNodeEnabled()) {
                String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
                if (firstRun) {
                    localPeerService.handleFirstRun(new FirstRunDone(clusterName));
                }

                localPeerService.handleClusterStarted(new ClusterStarted(clusterName));
            }

            Process nodeProcess = startNode(clusterFolder, clusterInfo, writer);
            if (nodeProcess == null) {
                writer.accept(error("Node process could not be started."));
                return new RunStatus(false, firstRun);
            }

            Process submitApiProcess = startSubmitApi(clusterInfo, clusterFolder, writer);
            if (submitApiProcess == null) {
                writer.accept(error("Submit API process could not be started."));
                return new RunStatus(false, firstRun);
            }

            processes.add(nodeProcess);
            if (submitApiProcess != null)
                processes.add(submitApiProcess);

            return new RunStatus(true, firstRun);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean portAvailabilityCheck(ClusterInfo clusterInfo, Consumer<String> writer) {
        boolean nodePortAvailable = PortUtil.isPortAvailable(clusterInfo.getNodePort());
        if (!nodePortAvailable) {
            writer.accept(info("Node Port %s is not available. Waiting for 2 seconds and try again", clusterInfo.getNodePort()));
            //Wait for 2 seconds and try
            try {
                Thread.sleep(1000);
                nodePortAvailable = PortUtil.isPortAvailable(clusterInfo.getNodePort());
            } catch (InterruptedException e) {
                log.error("Error waiting for port availability", e);
            }
        }

        if (!nodePortAvailable) {
            writer.accept(error("Node Port " + clusterInfo.getNodePort() + " is not available. Please check if the port is already in use."));
        }

        boolean submitPortAvaiable = PortUtil.isPortAvailable(clusterInfo.getSubmitApiPort());
        if (!submitPortAvaiable) {
            writer.accept(error("Submit Api Port " + clusterInfo.getSubmitApiPort() + " is not available. Please check if the port is already in use."));
        }

        if (!nodePortAvailable || !submitPortAvaiable)
            return false;
        else
            return true;
    }

    public void stopCluster(Consumer<String> writer) {
        try {
            if (processes != null && processes.size() > 0)
                writer.accept(info("Trying to stop the running cluster ..."));

            boolean error = false;
            for (Process process : processes) {
                if (process != null && process.isAlive()) {
                    process.descendants().forEach(processHandle -> {
                        writer.accept(infoLabel("Process", String.valueOf(processHandle.pid())));
                        processHandle.destroyForcibly();
                    });
                    process.destroy();
                    writer.accept(info("Stopping node process : " + process));
                    process.waitFor(15, TimeUnit.SECONDS);
                    if (!process.isAlive())
                        writer.accept(success("Killed : " + process));
                    else
                        writer.accept(error("Process could not be killed : " + process));
                }
            }

            if (!error) {
                //clean pid files
                processUtil.deletePidFile(NODE_PROCESS_NAME);
                processUtil.deletePidFile(SUBMIT_API_PROCESS_NAME);
            }

            logs.clear();
            submitApiLogs.clear();
        } catch (Exception e) {
            log.error("Error stopping process", e);
            writer.accept(error("Cluster could not be stopped. Please kill the process manually." + e.getMessage()));
        }

        processes.clear();
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

    public void showSubmitApiLogs(Consumer<String> consumer) {
        if (submitApiLogs.isEmpty()) {
            consumer.accept("No log to show");
        } else {
            int counter = 0;
            while (!submitApiLogs.isEmpty()) {
                counter++;
                if (counter == 100)
                    return;
                consumer.accept(submitApiLogs.poll());
            }
        }
    }

    private Process startNode(Path clusterFolder, ClusterInfo clusterInfo, Consumer<String> writer) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        String clusterFolderPath = clusterFolder.toAbsolutePath().toString();
        String startScript = null;
        if (clusterInfo.isMasterNode()) {
            startScript = ClusterConfig.NODE_FOLDER_PREFIX;
        } else if (clusterInfo.isBlockProducer()) {
            startScript = ClusterConfig.NODE_BP_SCRIPT;
        } else {
            startScript = ClusterConfig.NODE_RELAY_SCRIPT;
        }

        ProcessBuilder builder = new ProcessBuilder();
        if (OSUtil.getOperatingSystem() == OSUtil.OS.WINDOWS) {
            builder.command("cmd.exe", startScript + ".bat");
        } else {
            builder.command("sh", startScript + ".sh");
        }

        File nodeStartDir = new File(clusterFolderPath, ClusterConfig.NODE_FOLDER_PREFIX);
        builder.directory(nodeStartDir);

        writer.accept(success("Starting node from directory : " + nodeStartDir.getAbsolutePath()));
        Process process = processUtil.startLongRunningProcess(NODE_PROCESS_NAME, builder, logs, writer);
        if (process == null) return null;

        Path nodeSocketPath = clusterFolder.resolve(ClusterConfig.NODE_FOLDER_PREFIX).resolve("node.sock");
        int counter = 0;
        while (!Files.exists(nodeSocketPath) && counter < 10) { //wait 5 sec max
            Thread.sleep(500);
            if (counter > 4)
                writeLn(info("Waiting for node socket file to be created ..."));
            counter++;
        }

        return process;
    }

    private Process startSubmitApi(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        String clusterFolderPath = clusterFolder.toAbsolutePath().toString();

        //Check if cardano-submit-api exists
        String cardanoSubmitCli = "cardano-submit-api";
        Path binFolder = Path.of(clusterConfig.getCLIBinFolder());
        Path cardanoSubmitCliPath = binFolder.resolve(cardanoSubmitCli);
        if (!Files.exists(cardanoSubmitCliPath)) {
            writer.accept(info("Cardano submit api could not be started."));
            writer.accept(info("To start submit api, you need to copy cardano-submit-api binary to " + clusterConfig.getCLIBinFolder()));
            return null;
        }

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("sh", "submit-api.sh");

        File submitApiStartDir = new File(clusterFolderPath);
        builder.directory(submitApiStartDir);

        Process process = processUtil.startLongRunningProcess(SUBMIT_API_PROCESS_NAME, builder, submitApiLogs, writer);
        if (process == null) return null;

        writer.accept(success("Started submit api : http://localhost:" + clusterPortInfoHelper.getSubmitApiPort(clusterInfo)));
        return process;
    }

    private boolean setupFirstRun(ClusterInfo clusterInfo, Path clusterFolder, Consumer<String> writer) throws IOException {
        Path byronGenesis = clusterFolder.resolve("node").resolve("genesis").resolve("byron-genesis.json");
        Path shelleyGenesis = clusterFolder.resolve("node").resolve("genesis").resolve("shelley-genesis.json");

        if (!Files.exists(byronGenesis)) {
            writer.accept(error("Byron genesis file is not found"));
            return false;
        }

        //Update Byron Genesis file
        ObjectNode jsonNode = (ObjectNode) objectMapper.readTree(byronGenesis.toFile());
        long byronStartTime = Instant.now().getEpochSecond();

        var genesisConfigCopy = genesisConfig.copy();
        genesisConfigCopy.merge(customGenesisConfig.getMap());

        if (genesisConfigCopy.getConwayHardForkAtEpoch() > 0 && genesisConfigCopy.isShiftStartTimeBehind()) {
            long stabilityWindow = (long) Math.floor((3 * clusterInfo.getSecurityParam()) / clusterInfo.getActiveSlotsCoeff());

            long maxBehindSlots = stabilityWindow - 5;
            if (stabilityWindow > clusterInfo.getEpochLength()) {
                maxBehindSlots = clusterInfo.getEpochLength();
            }

            long maxBehindSeconds = Math.round(maxBehindSlots * clusterInfo.getSlotLength());

            byronStartTime = byronStartTime - maxBehindSeconds;
            writer.accept(success("Updating Start time to current time - " + maxBehindSeconds + " in byron-genesis.json"));
        }
        jsonNode.set("startTime", new LongNode(byronStartTime));
        objectMapper.writer(new DefaultPrettyPrinter()).writeValue(byronGenesis.toFile(), jsonNode);

        //Update Shelley Genesis file
        ObjectNode shelleyJsonNode = (ObjectNode) objectMapper.readTree(shelleyGenesis.toFile());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String shelleyStart = formatter.format(Instant.now().atZone(ZoneOffset.UTC));
        shelleyJsonNode.set("systemStart", new TextNode(shelleyStart));
        objectMapper.writer(new DefaultPrettyPrinter()).writeValue(shelleyGenesis.toFile(), shelleyJsonNode);

        clusterInfo.setStartTime(byronStartTime);
        saveClusterInfo(clusterFolder, clusterInfo);

        writer.accept(success("Updated Start time"));
        return true;
    }

    public void saveClusterInfo(Path clusterFolder, ClusterInfo clusterInfo) throws IOException {
        if (!Files.exists(clusterFolder)) {
            throw new IllegalStateException("Cluster folder not found - " + clusterFolder);
        }

        String clusterInfoPath = clusterFolder.resolve(ClusterConfig.CLUSTER_INFO_FILE).toAbsolutePath().toString();
        objectMapper.writer(new DefaultPrettyPrinter()).writeValue(new File(clusterInfoPath), clusterInfo);
    }

    public boolean checkIfFirstRun(Path clusterFolder) {
        String node1Folder = ClusterConfig.NODE_FOLDER_PREFIX;
        Path db = clusterFolder.resolve(node1Folder).resolve("db");
        if (Files.exists(db))
            return false;
        else
            return true;
    }

    public boolean isClusterRunning() {
        if (processes != null && processes.size() > 0) {
            for (Process process : processes) {
                if (process != null && process.isAlive()) {
                    return true;
                }
            }
        }
        return false;
    }
}
