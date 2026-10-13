package com.bloxbean.cardano.yacicli.localcluster;

import com.bloxbean.cardano.yacicli.commands.common.ExecutorHelper;
import com.bloxbean.cardano.yacicli.localcluster.catchup.DevnetCatchUpService;
import com.bloxbean.cardano.yacicli.localcluster.config.CustomGenesisConfig;
import com.bloxbean.cardano.yacicli.localcluster.config.GenesisConfig;
import com.bloxbean.cardano.yacicli.localcluster.model.RunStatus;
import com.bloxbean.cardano.yacicli.localcluster.peer.LocalPeerService;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoBootstrapService;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoCompanionService;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoService;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoTimeTravelBootstrap;
import com.bloxbean.cardano.yacicli.util.ProcessUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** A devnet node that cannot start, e.g. because another cardano-node holds its Prometheus port. */
class NodeStartupFailureTest {
    // What cardano-node prints when its Prometheus port is taken, about a second after it starts
    private static final String PROMETHEUS_BIND_ERROR =
            "cardano-node: ExceptionInLinkedThread (ThreadId 15) Network.Socket.bind: resource busy (Address already in use)";

    @TempDir
    Path tmp;

    private Path clusterFolder;
    private YanoService yanoService;
    private YanoCompanionService yanoCompanionService;
    private ClusterStartService service;
    private final List<String> output = new CopyOnWriteArrayList<>();
    private final Consumer<String> writer = output::add;

    @BeforeEach
    void setUp() throws Exception {
        clusterFolder = tmp.resolve("devnet");
        Files.createDirectories(clusterFolder.resolve(ClusterConfig.NODE_FOLDER_PREFIX));

        ClusterConfig clusterConfig = mock(ClusterConfig.class);
        when(clusterConfig.getYaciCliHome()).thenReturn(Files.createDirectories(tmp.resolve("home")).toString());
        ProcessUtil processUtil = new ProcessUtil(new ExecutorHelper(), clusterConfig);
        yanoService = mock(YanoService.class);
        yanoCompanionService = mock(YanoCompanionService.class);
        service = new ClusterStartService(clusterConfig, mock(ClusterPortInfoHelper.class), processUtil,
                mock(GenesisConfig.class), mock(CustomGenesisConfig.class), mock(LocalPeerService.class),
                yanoCompanionService, yanoService, mock(YanoBootstrapService.class), mock(YanoTimeTravelBootstrap.class),
                mock(DevnetCatchUpService.class));
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static ServerSocket takenPort() throws Exception {
        return new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    }

    private ClusterInfo clusterInfo(NodeMode mode, int prometheusPort) throws Exception {
        return ClusterInfo.builder()
                .nodeMode(mode).masterNode(true).isBlockProducer(true)
                .nodePort(freePort()).submitApiPort(freePort()).prometheusPort(prometheusPort)
                .build();
    }

    /** A node.sh that, like cardano-node with its Prometheus port taken, fails after ProcessUtil's first second. */
    private void nodeScriptThatFailsAfter(double seconds) throws Exception {
        Files.writeString(clusterFolder.resolve(ClusterConfig.NODE_FOLDER_PREFIX).resolve("node.sh"),
                "sleep " + seconds + "\necho 'starting'\necho '" + PROMETHEUS_BIND_ERROR + "' >&2\nexit 1\n");
    }

    @Test
    void aTakenPrometheusPortFailsThePortCheckWithAWayOut() throws Exception {
        try (ServerSocket taken = takenPort()) {
            boolean available = ClusterStartService.portAvailabilityCheck(
                    clusterInfo(NodeMode.COMPANION, taken.getLocalPort()), writer);

            assertThat(available).isFalse();
            String text = String.join("\n", output);
            assertThat(text).contains("Prometheus Port " + taken.getLocalPort() + " is not available");
            assertThat(text).contains("--prometheus-port").contains("prometheus.port=");
        }
    }

    @Test
    void aFreePrometheusPortPassesThePortCheck() throws Exception {
        assertThat(ClusterStartService.portAvailabilityCheck(clusterInfo(NodeMode.COMPANION, freePort()), writer)).isTrue();
        assertThat(output).isEmpty();
    }

    @Test
    void yanoOnlyRunsNoHaskellNodeAndSkipsTheCheck() throws Exception {
        try (ServerSocket taken = takenPort()) {
            assertThat(ClusterStartService.portAvailabilityCheck(
                    clusterInfo(NodeMode.YANO_ONLY, taken.getLocalPort()), writer)).isTrue();
        }
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void aNodeExitingAfterItsFirstSecondFailsTheStartAndShowsWhy() throws Exception {
        Files.createDirectories(clusterFolder.resolve(ClusterConfig.NODE_FOLDER_PREFIX).resolve("db")); // a restart
        nodeScriptThatFailsAfter(1.5);

        long startedAt = System.currentTimeMillis();
        RunStatus status = service.startCluster(clusterInfo(NodeMode.HASKELL_ONLY, freePort()), clusterFolder, writer);

        assertThat(status.stared()).isFalse();
        String text = String.join("\n", output);
        assertThat(text).contains("The node exited during startup (exit code 1)");
        // The reason, printed after ProcessUtil stopped showing the node's errors
        assertThat(text).contains(PROMETHEUS_BIND_ERROR);
        assertThat(text).contains("Node process could not be started");
        // Seen as it exits, not after the 5s socket wait
        assertThat(System.currentTimeMillis() - startedAt).isLessThan(4_500);
        assertThat(service.isClusterRunning()).isFalse();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void companionStopsYanoWhenTheRelayCannotStart() throws Exception {
        // First run: the Yano bootstrap succeeds, then the Haskell relay exits
        when(yanoCompanionService.bootstrap(any(), any(), any())).thenReturn(true);
        nodeScriptThatFailsAfter(1.5);

        RunStatus status = service.startCluster(clusterInfo(NodeMode.COMPANION, freePort()), clusterFolder, writer);

        assertThat(status.stared()).isFalse();
        verify(yanoCompanionService, never()).performHandover(any(), any(), any());
        verify(yanoService).stop();
    }
}
