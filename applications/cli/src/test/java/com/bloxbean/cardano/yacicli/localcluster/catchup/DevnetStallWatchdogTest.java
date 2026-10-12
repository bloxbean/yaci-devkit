package com.bloxbean.cardano.yacicli.localcluster.catchup;

import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.NodeMode;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStarted;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStopped;
import org.jline.reader.LineReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DevnetStallWatchdogTest {
    private DevnetStallWatchdog watchdog;

    @AfterEach
    void tearDown() {
        if (watchdog != null)
            watchdog.shutdown();
    }

    @Test
    @SuppressWarnings("unchecked")
    void aStopInterruptsACheckStillReadingTheStoppedNodesTip() throws Exception {
        ClusterService clusterService = mock(ClusterService.class);
        ObjectProvider<ClusterService> clusterServiceProvider = mock(ObjectProvider.class);
        when(clusterServiceProvider.getObject()).thenReturn(clusterService);
        DevnetCatchUpService catchUp = mock(DevnetCatchUpService.class);
        when(catchUp.unsupportedReason(any())).thenReturn(Optional.empty());
        when(clusterService.getClusterInfo("default"))
                .thenReturn(ClusterInfo.builder().nodeMode(NodeMode.COMPANION).build());

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

        watchdog = new DevnetStallWatchdog(clusterServiceProvider, catchUp, mock(ObjectProvider.class));
        watchdog.intervalMillis = 50;
        watchdog.handleClusterStarted(new ClusterStarted("default"));
        assertThat(reading.await(5, TimeUnit.SECONDS)).isTrue();

        watchdog.handleClusterStopped(new ClusterStopped("default"));

        assertThat(interrupted.await(5, TimeUnit.SECONDS)).isTrue();
    }
}
