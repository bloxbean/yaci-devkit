package com.bloxbean.cardano.yacicli.localcluster.catchup;

import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.NodeMode;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStarted;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStopped;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DevnetStallWatchdogTest {
    // Far behind wall clock: stalled
    private static final ChainLag STALLED = new ChainLag(100, 1010, 21, 40, 1.0);

    private ClusterService clusterService;
    private DevnetCatchUpService catchUp;
    private DevnetStallWatchdog watchdog;
    private final AtomicBoolean inProgress = new AtomicBoolean(false);

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        clusterService = mock(ClusterService.class);
        ObjectProvider<ClusterService> clusterServiceProvider = mock(ObjectProvider.class);
        when(clusterServiceProvider.getObject()).thenReturn(clusterService);
        catchUp = mock(DevnetCatchUpService.class);
        when(catchUp.unsupportedReason(any())).thenReturn(Optional.empty());
        when(catchUp.isAutoCatchUp()).thenReturn(true);
        when(catchUp.isInProgress()).thenAnswer(invocation -> inProgress.get());
        when(clusterService.getClusterInfo("default"))
                .thenReturn(ClusterInfo.builder().nodeMode(NodeMode.COMPANION).build());
        watchdog = new DevnetStallWatchdog(clusterServiceProvider, catchUp, mock(ObjectProvider.class));
    }

    @AfterEach
    void tearDown() {
        watchdog.shutdown();
    }

    /** Watch the devnet without scheduled checks: the test runs each check itself. */
    private void watchWithoutScheduledChecks() {
        watchdog.intervalMillis = TimeUnit.HOURS.toMillis(1);
        watchdog.handleClusterStarted(new ClusterStarted("default"));
    }

    private static String consoleOutputOf(ThrowingRunnable runnable) throws Exception {
        PrintStream out = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true));
        try {
            runnable.run();
        } finally {
            System.setOut(out);
        }
        return captured.toString();
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Test
    void aStopInterruptsACheckStillReadingTheStoppedNodesTip() throws Exception {
        // The node client retries a refused connect (a stale socket after the stop) until interrupted
        CountDownLatch reading = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        when(clusterService.chainLag("default")).thenAnswer(invocation -> {
            reading.countDown();
            try {
                Thread.sleep(60_000);
            } catch (InterruptedException e) {
                interrupted.countDown();
                throw e;
            }
            return null;
        });

        watchdog.intervalMillis = 50;
        watchdog.handleClusterStarted(new ClusterStarted("default"));
        assertThat(reading.await(5, TimeUnit.SECONDS)).isTrue();

        watchdog.handleClusterStopped(new ClusterStopped("default"));

        assertThat(interrupted.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void aStalledDevnetIsCaughtUp() throws Exception {
        when(clusterService.chainLag("default")).thenReturn(STALLED);
        when(clusterService.catchUp(eq("default"), eq(false), any()))
                .thenReturn(new DevnetCatchUpService.Result(true, "caught up", 100, 1010, 2, 1000));
        watchWithoutScheduledChecks();

        watchdog.check();

        verify(clusterService).catchUp(eq("default"), eq(false), any());
    }

    @Test
    void aCatchUpStartedWhileTheLagWasReadIsLeftToItself() throws Exception {
        // 'catch-up' typed while the check reads the lag
        when(clusterService.chainLag("default")).thenAnswer(invocation -> {
            inProgress.set(true);
            return STALLED;
        });
        watchWithoutScheduledChecks();

        String output = consoleOutputOf(watchdog::check);

        verify(clusterService, never()).catchUp(any(), anyBoolean(), any());
        assertThat(output).isEmpty();
    }

    @Test
    void losingTheRaceToAnotherCatchUpGivesNoRetryAdvice() throws Exception {
        when(clusterService.chainLag("default")).thenReturn(STALLED);
        // The other catch-up claimed the slot just before this one and is still running
        when(clusterService.catchUp(eq("default"), eq(false), any())).thenAnswer(invocation -> {
            inProgress.set(true);
            return new DevnetCatchUpService.Result(false, "A catch-up is already running", -1, -1, 0, 0);
        });
        watchWithoutScheduledChecks();

        String output = consoleOutputOf(watchdog::check);

        assertThat(output).doesNotContain("Run 'catch-up' to retry");
    }

    @Test
    void aFailedCatchUpGivesRetryAdvice() throws Exception {
        when(clusterService.chainLag("default")).thenReturn(STALLED);
        when(clusterService.catchUp(eq("default"), eq(false), any()))
                .thenReturn(new DevnetCatchUpService.Result(false, "Yano's catch-up call failed.", -1, -1, 0, 0));
        watchWithoutScheduledChecks();

        String output = consoleOutputOf(watchdog::check);

        assertThat(output).contains("Run 'catch-up' to retry");
    }
}
