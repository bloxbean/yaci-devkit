package com.bloxbean.cardano.yacicli.commands.localcluster;

import com.bloxbean.cardano.yaci.core.util.OSUtil;
import com.bloxbean.cardano.yacicli.util.ProcessStream;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.EvictingQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.commands.localcluster.ClusterConfig.NODE_FOLDER_PREFIX;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@Slf4j
public class ClusterStartService {

    private ObjectMapper objectMapper = new ObjectMapper();
    private AtomicBoolean showLog = new AtomicBoolean(true);
    private List<Process> processes = new ArrayList<>();

    private Queue<String> logs = EvictingQueue.create(200);

    public void startCluster(Path clusterFolder, Consumer<String> writer) {
        logs.clear();

        if (processes.size() > 0) {
            writer.accept("A cluster is already running. You can only run one cluster at a time.");
            return;
        }

        try {
            if (checkIfFirstRun(clusterFolder))
                setupFirstRun(clusterFolder, writer);

            Process process1 = startNode(clusterFolder, 1, writer);
            Process process2 = startNode(clusterFolder, 2, writer);
            Process process3 = startNode(clusterFolder, 3, writer);

            processes.add(process1);
            processes.add(process2);
            processes.add(process3);
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

    public void stopCluster(Consumer<String> writer) {
        try {
            if (processes != null && processes.size() > 0)
                writer.accept(info("Trying to stop the running cluster ..."));

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

            logs.clear();
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
            while(!logs.isEmpty()) {
                counter++;
                if (counter == 200)
                    return;
                consumer.accept(logs.poll());
            }
        }

    }

    private Process startNode(Path clusterFolder, int nodeNo, Consumer<String> writer) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        String clusterFolderPath = clusterFolder.toAbsolutePath().toString();
        String startScript = NODE_FOLDER_PREFIX + nodeNo;

        ProcessBuilder builder = new ProcessBuilder();
        if (OSUtil.getOperatingSystem() == OSUtil.OS.WINDOWS) {
            builder.command("cmd.exe", startScript + ".bat");
        } else {
            builder.command("sh", startScript + ".sh");
        }

        File nodeStartDir = new File(clusterFolderPath, NODE_FOLDER_PREFIX + nodeNo);
        builder.directory(nodeStartDir);
        Process process = builder.start();

        writer.accept(success("Starting node from directory : " + nodeStartDir.getAbsolutePath()));
        ProcessStream processStream =
                new ProcessStream(process.getInputStream(), line -> {
                    logs.add(String.format("[Node: %s] ", nodeNo) +  line);
                    //writeLn("[Node: %s] %s", nodeNo, line);
                });
        Future<?> future = Executors.newSingleThreadExecutor().submit(processStream);

        return process;
//        int exitCode = process.waitFor();
//        assert exitCode == 0;
//        future.get(10, TimeUnit.SECONDS);
    }

    private boolean setupFirstRun(Path clusterFolder, Consumer<String> writer) throws IOException {
        Path byronGenesis = clusterFolder.resolve("genesis/byron/genesis.json");

        if (!Files.exists(byronGenesis)) {
            writer.accept(error("Byron genesis file is not found"));
            return false;
        }

        ObjectNode jsonNode = (ObjectNode)objectMapper.readTree(byronGenesis.toFile());
        long unixTime = Instant.now().getEpochSecond();

        jsonNode.set("startTime", new LongNode(unixTime));
        objectMapper.writer(new DefaultPrettyPrinter()).writeValue(byronGenesis.toFile(), jsonNode);

        writer.accept(success("Update Start time"));
        return true;
    }

    private boolean checkIfFirstRun(Path clusterFolder) {
        String node1Folder = NODE_FOLDER_PREFIX + 1;
        Path db = clusterFolder.resolve(node1Folder).resolve("db");
        if (Files.exists(db))
            return false;
        else
            return true;
    }
}
