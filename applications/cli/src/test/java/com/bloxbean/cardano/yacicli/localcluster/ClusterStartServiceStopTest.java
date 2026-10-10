package com.bloxbean.cardano.yacicli.localcluster;

import com.bloxbean.cardano.yacicli.localcluster.catchup.ChainLag;
import com.bloxbean.cardano.yacicli.localcluster.catchup.DevnetCatchUpService;
import com.bloxbean.cardano.yacicli.localcluster.config.CustomGenesisConfig;
import com.bloxbean.cardano.yacicli.localcluster.config.GenesisConfig;
import com.bloxbean.cardano.yacicli.localcluster.model.RunStatus;
import com.bloxbean.cardano.yacicli.localcluster.peer.LocalPeerService;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoBootstrapService;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoCompanionService;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoRunMode;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoService;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoTimeTravelBootstrap;
import com.bloxbean.cardano.yacicli.util.ProcessUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** A stop or reset during a start: the start gives up and the stop waits for it (PR #196 review, F1). */
class ClusterStartServiceStopTest {
    @TempDir
    Path tmp;

    private Path clusterFolder;
    private ProcessUtil processUtil;
    private DevnetCatchUpService catchUp;
    private YanoService yano;
    private ClusterStartService service;
    private final List<FakeProcess> started = new CopyOnWriteArrayList<>();
    private final List<String> output = new CopyOnWriteArrayList<>();
    private final Consumer<String> writer = output::add;

    @BeforeEach
    void setUp() throws Exception {
        clusterFolder = tmp.resolve("devnet");
        Path node = clusterFolder.resolve(ClusterConfig.NODE_FOLDER_PREFIX);
        Files.createDirectories(node.resolve("db"));    // an existing devnet: a restart, not a first run
        Files.createFile(node.resolve("node.sock"));     // the node's socket is there at once
        Path bin = Files.createDirectories(tmp.resolve("bin"));
        Files.createFile(bin.resolve("cardano-submit-api"));

        ClusterConfig clusterConfig = mock(ClusterConfig.class);
        when(clusterConfig.getCLIBinFolder()).thenReturn(bin.toString());
        processUtil = mock(ProcessUtil.class);
        when(processUtil.startLongRunningProcess(any(), any(), any(), any())).thenAnswer(invocation -> {
            FakeProcess process = new FakeProcess(invocation.getArgument(0));
            started.add(process);
            return process;
        });
        catchUp = mock(DevnetCatchUpService.class);
        yano = mock(YanoService.class);
        service = new ClusterStartService(clusterConfig, mock(ClusterPortInfoHelper.class), processUtil,
                mock(GenesisConfig.class), mock(CustomGenesisConfig.class), mock(LocalPeerService.class),
                mock(YanoCompanionService.class), yano, mock(YanoBootstrapService.class),
                mock(YanoTimeTravelBootstrap.class), catchUp);

        when(catchUp.unsupportedReason(any())).thenReturn(Optional.empty());
        when(catchUp.isAutoCatchUp()).thenReturn(true);
        // Far behind wall clock: the restart catches up
        when(catchUp.probeLagAfterStart(any(), any())).thenReturn(new ChainLag(100, 1010, 21, 40, 1.0));
    }

    private ClusterInfo clusterInfo(NodeMode mode) throws Exception {
        return ClusterInfo.builder()
                .nodeMode(mode).masterNode(true).nodePort(freePort()).submitApiPort(freePort())
                .startTime(System.currentTimeMillis() / 1000 - 1010).slotLength(1.0).blockTime(1.0).epochLength(40)
                .securityParam(7).activeSlotsCoeff(1.0).yanoHttpPort(6060)
                .build();
    }

    private static int freePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void verifySubmitApiNeverStarted() throws Exception {
        verify(processUtil, never()).startLongRunningProcess(eq(ClusterStartService.SUBMIT_API_PROCESS_NAME),
                any(), any(), any());
    }

    @Test
    void stopDuringTheRestartCatchUpAbortsTheStartAndWaitsForIt() throws Exception {
        AtomicBoolean inProgress = new AtomicBoolean(false);
        CountDownLatch inCatchUp = new CountDownLatch(1);
        CountDownLatch cancel = new CountDownLatch(1);
        when(catchUp.isInProgress()).thenAnswer(invocation -> inProgress.get());
        when(catchUp.catchUpCompanion(any(), any(), any(), any(), any())).thenAnswer(invocation -> {
            inProgress.set(true);
            inCatchUp.countDown();
            cancel.await(10, TimeUnit.SECONDS);
            // A cancelled catch-up clears its interrupt and only reports the cancel
            Thread.sleep(200);
            inProgress.set(false);
            return new DevnetCatchUpService.Result(false, "Catch-up cancelled", -1, -1, 0, 0);
        });
        when(catchUp.cancelAndAwait(any())).thenAnswer(invocation -> {
            cancel.countDown();
            while (inProgress.get())
                Thread.sleep(20);
            return true;
        });
        ClusterInfo info = clusterInfo(NodeMode.COMPANION);

        CompletableFuture<RunStatus> start = CompletableFuture.supplyAsync(() ->
                service.startCluster(info, clusterFolder, writer));
        assertThat(inCatchUp.await(10, TimeUnit.SECONDS)).isTrue();

        service.stopCluster(writer);

        // The node the start had already launched was stopped, and nothing was started or registered after that
        assertThat(started).hasSize(1);
        assertThat(started.get(0).isAlive()).isFalse();
        RunStatus status = start.get(10, TimeUnit.SECONDS);
        assertThat(status.stared()).isFalse();
        verifySubmitApiNeverStarted();
        assertThat(started).hasSize(1);
        assertThat(service.isClusterRunning()).isFalse();
        assertThat(String.join("\n", output)).contains("Start cancelled by a stop");
    }

    @Test
    void aFailedCatchUpWithoutAStopStillStartsTheDevnet() throws Exception {
        // A stop with nothing running must not leave the next start cancelled
        service.stopCluster(writer);
        when(catchUp.catchUpCompanion(any(), any(), any(), any(), any())).thenReturn(
                new DevnetCatchUpService.Result(false, "Yano's catch-up call failed", -1, -1, 0, 0));

        RunStatus status = service.startCluster(clusterInfo(NodeMode.COMPANION), clusterFolder, writer);

        // The catch-up leaves the producer running; the devnet stays usable and 'catch-up' can be retried
        assertThat(status.stared()).isTrue();
        verify(processUtil).startLongRunningProcess(eq(ClusterStartService.SUBMIT_API_PROCESS_NAME), any(), any(), any());
        service.stopCluster(writer);
    }

    @Test
    void catchUpCommandRunsForAnIdleYanoOnlyDevnetInsideTheWindow() throws Exception {
        when(yano.isRunning()).thenReturn(true);
        when(yano.runMode()).thenReturn(YanoRunMode.IDLE);
        // 16 slots behind with a 21-slot window: not stalled, but Yano is not producing
        when(catchUp.probeLag(any())).thenReturn(new ChainLag(994, 1010, 21, 40, 1.0));
        when(catchUp.catchUpYanoOnly(any(), any(), any())).thenReturn(
                new DevnetCatchUpService.Result(true, "resumed", 994, 1010, 2, 1000));

        DevnetCatchUpService.Result result = service.catchUp(clusterInfo(NodeMode.YANO_ONLY), clusterFolder, false, writer);

        assertThat(result.success()).isTrue();
        verify(catchUp).catchUpYanoOnly(any(), any(), any());
    }

    @Test
    void catchUpCommandIsANoOpForALiveYanoOnlyDevnet() throws Exception {
        when(yano.isRunning()).thenReturn(true);
        when(yano.runMode()).thenReturn(YanoRunMode.LIVE);
        when(catchUp.probeLag(any())).thenReturn(new ChainLag(1009, 1010, 21, 40, 1.0));

        DevnetCatchUpService.Result result = service.catchUp(clusterInfo(NodeMode.YANO_ONLY), clusterFolder, false, writer);

        assertThat(result.message()).contains("Nothing to catch up");
        verify(catchUp, never()).catchUpYanoOnly(any(), any(), any());
    }

    /** A process that runs until it is destroyed. */
    private static final class FakeProcess extends Process {
        private final String name;
        private volatile boolean alive = true;

        FakeProcess(String name) {
            this.name = name;
        }

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            return !alive;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
            alive = false;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }

        @Override
        public Stream<ProcessHandle> descendants() {
            return Stream.empty();
        }

        @Override
        public String toString() {
            return "FakeProcess[" + name + "]";
        }
    }
}
