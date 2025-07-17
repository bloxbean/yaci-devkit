package com.bloxbean.cardano.yacicli.localcluster.peer;

import com.bloxbean.cardano.yaci.core.util.OSUtil;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfoService;
import com.bloxbean.cardano.yacicli.localcluster.config.GenesisConfig;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStarted;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStopped;
import com.bloxbean.cardano.yacicli.localcluster.events.FirstRunDone;
import com.bloxbean.cardano.yacicli.localcluster.proxy.TcpProxyManager;
import com.bloxbean.cardano.yacicli.util.PortUtil;
import com.bloxbean.cardano.yacicli.util.ProcessUtil;
import com.bloxbean.cardano.yacicli.util.TemplateEngine;
import com.google.common.collect.EvictingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.info;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocalPeerService {
    private final ClusterConfig clusterConfig;
    private final TemplateEngine templateEngine;
    private final ProcessUtil processUtil;
    private final ClusterInfoService clusterInfoService;
    private final TcpProxyManager tcpProxyManager;
    private Queue<String> node2Logs = EvictingQueue.create(200);
    private Queue<String> node3Logs = EvictingQueue.create(200);

    private List<Process> processes = new ArrayList<>();

    public void setupNewPoolInfos(GenesisConfig genesisConfigCopy, Consumer<String> writer) {
        genesisConfigCopy.setPools(genesisConfigCopy.getMultiNodePools());

        //Bootstrap loaders (Byron)
        genesisConfigCopy.setBootStakeHolders(genesisConfigCopy.getMultiNodebootStakeHolders());
        genesisConfigCopy.setHeavyDelegations(genesisConfigCopy.getMultiNodeHeavyDelegations());
        genesisConfigCopy.setGenesisDelegs(genesisConfigCopy.getMultiNodeGenesisDelegs());
        genesisConfigCopy.setNonAvvmBalances(genesisConfigCopy.getMultiNodeNonAvvmBalances());
        genesisConfigCopy.setDefaultDelegators(genesisConfigCopy.getMultiNodeDefaultDelegators());

        // Initial Funds
        //reset last item flag to false
        var initialFundList = genesisConfigCopy.getInitialFundsList();
        var lastItemInFundList = initialFundList.get(initialFundList.size() - 1);
        initialFundList.set(initialFundList.size() - 1, new GenesisConfig.MapItem<>(lastItemInFundList.key(),
                lastItemInFundList.value(), false));
        initialFundList.add(new GenesisConfig.MapItem<>("0013b147b6cc8e23615254c31598bc43159d01b6ceb5984e25771677043a82eb2c3f2729b35c504d305e2f582f8d335d033b4109cccac3c74b", new BigInteger("300000000000"), false));
        initialFundList.add(new GenesisConfig.MapItem<>("00cb2015b6312bbbc11e8c912b54e6187d4d4f196fa5536ea6c64c80f6e95458639f01cbf21e75bc2083d245e55b0773bddf0b06c8b4aff6f0", new BigInteger("300000000000"), true));
    }

    public void adjustAndCopyRequiredFilesForMultiNodeSetup(String clusterName, ClusterInfo clusterInfo, Consumer<String> writer) {
        Path clusterFolder = clusterConfig.getClusterFolder(clusterName);
        Path primaryNodeFolder = clusterFolder.resolve("node");
        Path node2Folder = clusterFolder.resolve("node-2");
        Path node3Folder = clusterFolder.resolve("node-3");

        //Copy configuration file
        try {
            Path primaryConfigFile = primaryNodeFolder.resolve("configuration.json");
            FileUtils.copyFile(primaryConfigFile.toFile(), node2Folder.resolve("configuration.json").toFile());
            FileUtils.copyFile(primaryConfigFile.toFile(), node3Folder.resolve("configuration.json").toFile());
        } catch (Exception e) {
            writer.accept(error("Failed to copy configuration files: " + e.getMessage()));
        }

        //Update node-2 and node-3 run scripts
        Map<String, String> values = new HashMap<>();
        values.put("BIN_FOLDER", clusterConfig.getCLIBinFolder());
        values.put("port", String.valueOf(clusterInfo.getNodePort() + 1));

        //Update Node-2 run script
        Path nodeRunScript = node2Folder.resolve("node.sh");
        try {
            templateEngine.replaceValues(nodeRunScript, values);
        } catch (Exception e) {
            writer.accept(error("Failed to update node run script for node-2 : " + e.getMessage()));
        }

        //Update Node-3 run script
        values.put("port", String.valueOf(clusterInfo.getNodePort() + 2));
        nodeRunScript = node3Folder.resolve("node.sh");
        try {
            templateEngine.replaceValues(nodeRunScript, values);
        } catch (Exception e) {
            writer.accept(error("Failed to update node run script for node-3 : " + e.getMessage()));
        }

        //copy topology-multinode.json to topology.json in all three nodes
        try {
            var topologyFile = primaryNodeFolder.resolve("topology.json").toFile();
            Path multiNodeTopologyFile = primaryNodeFolder.resolve("topology-multinode.json");
            FileUtils.copyFile(topologyFile, primaryNodeFolder.resolve("topology-original.json").toFile());
            FileUtils.copyFile(multiNodeTopologyFile.toFile(), topologyFile);
        } catch (IOException e) {
            writer.accept(error("Failed to copy topology files: " + e.getMessage()));
        }
    }

    @EventListener
    public void handleFirstRun(FirstRunDone firstRunDone) {
        var clusterName = firstRunDone.getCluster();
        Path clusterFolder = clusterConfig.getClusterFolder(clusterName);
        Path primaryNodeFolder = clusterFolder.resolve("node");
        Path node2Folder = clusterFolder.resolve("node-2");
        Path node3Folder = clusterFolder.resolve("node-3");

        //Copy the primary node genesis file to node-2 and node-2's genesis folders
        try {
            //Copy genesis file
            Path primaryGenesisFolder = primaryNodeFolder.resolve("genesis");
            FileUtils.copyDirectory(primaryGenesisFolder.toFile(), node2Folder.resolve("genesis").toFile());
            FileUtils.copyDirectory(primaryGenesisFolder.toFile(), node3Folder.resolve("genesis").toFile());
        } catch (Exception e) {
            writeLn(error("Failed to copy genesis files: " + e.getMessage()));
        }
    }

    @EventListener
    public void handleClusterStarted(ClusterStarted clusterStarted) {
        var clusterName = clusterStarted.getClusterName();
        ClusterInfo clusterInfo = null;
        try {
            clusterInfo = clusterInfoService.getClusterInfo(clusterName);
        } catch (IOException e) {
            writeLn(error("Unable to get cluster info for " + clusterName + ": " + e.getMessage()));
        }
        if (!clusterInfo.isLocalMultiNodeEnabled())
            return;

        Path clusterFolder = clusterConfig.getClusterFolder(clusterName);
        Path node2Folder = clusterFolder.resolve("node-2");
        Path node3Folder = clusterFolder.resolve("node-3");

        //Port checks
        boolean proxyPort1Available = PortUtil.isPortAvailable(4001);
        boolean proxyPort2Available = PortUtil.isPortAvailable(4002);
        boolean proxyPort3Available = PortUtil.isPortAvailable(4003);

        boolean node2PortAvailable = PortUtil.isPortAvailable(3002);
        boolean node3PortAvailable = PortUtil.isPortAvailable(3003);

        if (!proxyPort1Available || !proxyPort2Available || !proxyPort3Available ||
            !node2PortAvailable || !node3PortAvailable) {
            writeLn(error("Not all required ports are not available to start peer nodes and proxies for rollback." +
                    " Please check the following ports: " +
                    "4001, 4002, 4003, 3002, 3003"));
            return;
        }

        //Start tcp proxy
        try {
            tcpProxyManager.startProxy(4001, "127.0.0.1", 3001);
        } catch (IOException e) {
            writeLn(error("Failed to start proxy for main node: " + e.getMessage()));
            return;
        }

        try {
            var node2Process = startNode("node-2", node2Folder, node2Logs, msg -> writeLn(msg));
            if (node2Process == null) {
                writeLn(error("Failed to start node-2. Please check the logs for more details."));
                return;
            }

            processes.add(node2Process);
            tcpProxyManager.startProxy(4002, "127.0.0.1", 3002);
        } catch (Exception e) {
            writeLn(error("Error starting node-2: " + e.getMessage()));
        }

        try {
            var node3Process = startNode("node-3", node3Folder, node3Logs, msg -> writeLn(msg));
            if (node3Process == null) {
                writeLn(error("Failed to start node-3. Please check the logs for more details."));
                return;
            }
            processes.add(node3Process);
            tcpProxyManager.startProxy(4003, "127.0.0.1", 3003);
        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            writeLn(error("Error starting node-3: " + e.getMessage()));
        }
    }

    private Process startNode(String nodeName, Path nodeFolder, Queue<String> logs, Consumer<String> writer) throws IOException, InterruptedException, ExecutionException, TimeoutException {
        String startScript = "node";

        ProcessBuilder builder = new ProcessBuilder();
        if (OSUtil.getOperatingSystem() == OSUtil.OS.WINDOWS) {
            builder.command("cmd.exe", startScript + ".bat");
        } else {
            builder.command("sh", startScript + ".sh");
        }

        File nodeStartDir = nodeFolder.toFile();
        builder.directory(nodeStartDir);

        writer.accept(success("Node %s started", nodeName));
        Process process = processUtil.startLongRunningProcess(nodeName, builder, writer);
        if (process == null) return null;

        Path nodeSocketPath = nodeFolder.resolve("node.sock");
        int counter = 0;
        while (!Files.exists(nodeSocketPath) && counter < 10) { //wait 5 sec max
            Thread.sleep(500);
            if (counter > 4)
                writeLn(info("Waiting for node socket file to be created ..."));
            counter++;
        }

        return process;
    }

    @EventListener
    public void handleClusterStopped(ClusterStopped clusterStopped) {
        var clusterName = clusterStopped.getClusterName();
        if (clusterName == null || clusterName.isEmpty()) {
            return;
        }

        ClusterInfo clusterInfo = null;
        try {
            clusterInfo = clusterInfoService.getClusterInfo(clusterName);
        } catch (Exception e) {
        }
        if (clusterInfo == null || !clusterInfo.isLocalMultiNodeEnabled())
            return;

        stopLocalPeers(msg -> writeLn(msg));
    }

    public void stopLocalPeers(Consumer<String> writer) {
        try {
            tcpProxyManager.stopAll();

            if (processes != null && processes.size() > 0)
                writer.accept(info("Trying to stop the local peers ..."));

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
                processUtil.deletePidFile("node-2");
                processUtil.deletePidFile("node-3");
            }

            node2Logs.clear();
            node3Logs.clear();
        } catch (Exception e) {
            log.error("Error stopping local peers", e);
            writer.accept(error("Peer nodes could not be stopped. Please kill the process manually." + e.getMessage()));
        }

        processes.clear();
    }

}
