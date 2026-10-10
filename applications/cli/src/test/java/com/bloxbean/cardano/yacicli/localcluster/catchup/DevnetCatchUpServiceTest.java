package com.bloxbean.cardano.yacicli.localcluster.catchup;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yacicli.common.Tuple;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.NodeMode;
import com.bloxbean.cardano.yacicli.localcluster.service.ClusterUtilService;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoBootstrapService;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoCompanionService;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoRunMode;
import com.bloxbean.cardano.yacicli.localcluster.yano.YanoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DevnetCatchUpServiceTest {
    private static final Path FOLDER = Path.of("/tmp/devnet");
    private static final int HTTP_PORT = 6060;

    private YanoService yano;
    private YanoBootstrapService bootstrap;
    private YanoCompanionService companion;
    private ClusterUtilService clusterUtil;
    private DevnetCatchUpService service;
    private final List<String> output = new ArrayList<>();
    private final Consumer<String> writer = output::add;

    // Wall clock is about slot 1,010 of a chain with 40-slot epochs (window 21 slots)
    private final long startTime = System.currentTimeMillis() / 1000 - 1010;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        yano = mock(YanoService.class);
        bootstrap = mock(YanoBootstrapService.class);
        companion = mock(YanoCompanionService.class);
        clusterUtil = mock(ClusterUtilService.class);
        ObjectProvider<ClusterUtilService> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(clusterUtil);
        service = new DevnetCatchUpService(yano, bootstrap, companion, provider);

        when(yano.start(any(), any(), any(YanoRunMode.class), any())).thenReturn(true);
        when(bootstrap.waitForNodeTip(eq(HTTP_PORT), any())).thenReturn(true);
    }

    private ClusterInfo clusterInfo(NodeMode mode) {
        return ClusterInfo.builder()
                .nodeMode(mode).startTime(startTime).slotLength(1.0).blockTime(1.0).epochLength(40)
                .securityParam(7).activeSlotsCoeff(1.0).yanoHttpPort(HTTP_PORT)
                .build();
    }

    private long wallSlot() {
        return ChainLag.wallClockSlot(startTime, 1.0, System.currentTimeMillis());
    }

    private static Tuple<Long, Point> tip(long slot) {
        return new Tuple<>(slot, new Point(slot, "hash-" + slot));
    }

    private static com.fasterxml.jackson.databind.JsonNode caughtUp(long newSlot, long blocks) {
        return new ObjectMapper().createObjectNode().put("new_slot", newSlot).put("blocks_produced", blocks);
    }

    private void verifyNoLiveStart() {
        verify(yano, never()).start(any(), any(), eq(YanoRunMode.LIVE), any());
    }

    // --- Yano-only ------------------------------------------------------------------------------------------

    @Test
    void yanoOnlyFailedBackfillAcrossEpochsDoesNotStartLiveProduction() {
        when(bootstrap.getNodeTip(HTTP_PORT)).thenReturn(tip(100));   // epoch 2, wall clock is epoch 25
        when(bootstrap.catchUp(HTTP_PORT)).thenReturn(null);

        boolean started = service.startYanoOnly(clusterInfo(NodeMode.YANO_ONLY), FOLDER, writer);

        assertThat(started).isFalse();
        verifyNoLiveStart();
        assertThat(String.join("\n", output)).contains("would skip 23 epochs");
    }

    @Test
    void yanoOnlyFailedBackfillInsideOneEpochStillStartsLive() {
        when(bootstrap.getNodeTip(HTTP_PORT)).thenReturn(tip(1001));  // same epoch as wall clock (1,000-1,039)
        when(bootstrap.catchUp(HTTP_PORT)).thenReturn(null);

        boolean started = service.startYanoOnly(clusterInfo(NodeMode.YANO_ONLY), FOLDER, writer);

        assertThat(started).isTrue();
        verify(yano).start(any(), any(), eq(YanoRunMode.LIVE), any());
    }

    @Test
    void yanoOnlyStartBackfillsThenStartsLive() {
        when(bootstrap.getNodeTip(HTTP_PORT)).thenReturn(tip(100));
        when(bootstrap.catchUp(HTTP_PORT)).thenReturn(caughtUp(1010, 30));

        boolean started = service.startYanoOnly(clusterInfo(NodeMode.YANO_ONLY), FOLDER, writer);

        assertThat(started).isTrue();
        var order = inOrder(yano, bootstrap);
        order.verify(yano).start(any(), any(), eq(YanoRunMode.CATCH_UP), any());
        order.verify(bootstrap).catchUp(HTTP_PORT);
        order.verify(yano).stopQuietly();
        order.verify(yano).start(any(), any(), eq(YanoRunMode.LIVE), any());
    }

    @Test
    void explicitYanoOnlyCatchUpBackfillsEvenWithAutoCatchUpOff() {
        ReflectionTestUtils.setField(service, "autoCatchUp", false);
        when(bootstrap.getNodeTip(HTTP_PORT)).thenReturn(tip(100));
        when(bootstrap.catchUp(HTTP_PORT)).thenReturn(caughtUp(1010, 30));

        DevnetCatchUpService.Result result = service.catchUpYanoOnly(clusterInfo(NodeMode.YANO_ONLY), FOLDER, writer);

        assertThat(result.success()).isTrue();
        assertThat(result.blocksProduced()).isEqualTo(30);
        verify(bootstrap).catchUp(HTTP_PORT);
        verify(yano).start(any(), any(), eq(YanoRunMode.LIVE), any());
    }

    @Test
    void explicitYanoOnlyCatchUpReportsAFailedBackfill() {
        when(bootstrap.getNodeTip(HTTP_PORT)).thenReturn(tip(100));
        when(bootstrap.catchUp(HTTP_PORT)).thenReturn(null);

        DevnetCatchUpService.Result result = service.catchUpYanoOnly(clusterInfo(NodeMode.YANO_ONLY), FOLDER, writer);

        assertThat(result.success()).isFalse();
        verifyNoLiveStart();
    }

    @Test
    void yanoOnlyStartWithAutoCatchUpOffLeavesYanoIdleWhenBehind() {
        ReflectionTestUtils.setField(service, "autoCatchUp", false);
        when(bootstrap.getNodeTip(HTTP_PORT)).thenReturn(tip(100));

        boolean started = service.startYanoOnly(clusterInfo(NodeMode.YANO_ONLY), FOLDER, writer);

        assertThat(started).isTrue();
        // Waiting for catch-up in a mode that cannot forge at all, not merely one with a long block time
        verify(yano).start(any(), any(), eq(YanoRunMode.IDLE), any());
        verify(yano, never()).start(any(), any(), eq(YanoRunMode.CATCH_UP), any());
        verify(yano, never()).stopQuietly();
        verify(bootstrap, never()).catchUp(anyInt());
        verifyNoLiveStart();
        assertThat(String.join("\n", output)).contains("run 'catch-up'");
    }

    @Test
    void yanoOnlyFailedBackfillRechecksTheLagBeforeAllowingLiveProduction() {
        // At the start the gap is inside one epoch (tip 1,000, wall clock 1,001) ...
        long[] now = {(startTime + 1001) * 1000};
        service.clock = () -> now[0];
        when(bootstrap.getNodeTip(HTTP_PORT)).thenReturn(tip(1000));
        // ... but the failing call takes long enough for five epochs to pass
        when(bootstrap.catchUp(HTTP_PORT)).thenAnswer(invocation -> {
            now[0] += 200_000;
            return null;
        });

        boolean started = service.startYanoOnly(clusterInfo(NodeMode.YANO_ONLY), FOLDER, writer);

        assertThat(started).isFalse();
        verifyNoLiveStart();
        assertThat(String.join("\n", output)).contains("would skip 5 epochs");
    }

    @Test
    void yanoOnlyStartWithAutoCatchUpOffGoesLiveAtTheTip() {
        ReflectionTestUtils.setField(service, "autoCatchUp", false);
        when(bootstrap.getNodeTip(HTTP_PORT)).thenAnswer(invocation -> tip(wallSlot()));

        boolean started = service.startYanoOnly(clusterInfo(NodeMode.YANO_ONLY), FOLDER, writer);

        assertThat(started).isTrue();
        verify(yano).start(any(), any(), eq(YanoRunMode.LIVE), any());
    }

    @Test
    void yanoOnlyStartWithAutoCatchUpOffFailsWhenYanoNeverAnswers() {
        ReflectionTestUtils.setField(service, "autoCatchUp", false);
        when(bootstrap.waitForNodeTip(eq(HTTP_PORT), any())).thenReturn(false);

        boolean started = service.startYanoOnly(clusterInfo(NodeMode.YANO_ONLY), FOLDER, writer);

        // Not knowing the lag is no permission to forge
        assertThat(started).isFalse();
        verifyNoLiveStart();
        verify(yano).stopQuietly();
        assertThat(String.join("\n", output)).contains("Retry with 'start'");
    }

    @Test
    void yanoOnlyStartWithAutoCatchUpOffFailsWhenTheTipCannotBeRead() {
        ReflectionTestUtils.setField(service, "autoCatchUp", false);
        when(bootstrap.getNodeTip(HTTP_PORT)).thenReturn(null);

        boolean started = service.startYanoOnly(clusterInfo(NodeMode.YANO_ONLY), FOLDER, writer);

        assertThat(started).isFalse();
        verifyNoLiveStart();
        verify(yano).stopQuietly();
        assertThat(String.join("\n", output)).contains("Could not read the chain tip");
    }

    @Test
    void yanoOnlyStartWithAutoCatchUpOffStaysIdleWhenAShortLagCrossesSeveralEpochs() {
        ReflectionTestUtils.setField(service, "autoCatchUp", false);
        // securityParam 70: a 210-slot window, longer than the 40-slot epoch. 100 slots behind is inside the
        // threshold, yet a live block would jump from epoch 22 to 25.
        ClusterInfo info = clusterInfo(NodeMode.YANO_ONLY);
        info.setSecurityParam(70);
        when(bootstrap.getNodeTip(HTTP_PORT)).thenAnswer(invocation -> tip(wallSlot() - 100));

        boolean started = service.startYanoOnly(info, FOLDER, writer);

        assertThat(started).isTrue();
        verify(yano).start(any(), any(), eq(YanoRunMode.IDLE), any());
        verifyNoLiveStart();
        assertThat(String.join("\n", output)).contains("run 'catch-up'");
    }

    @Test
    void yanoOnlySlowBackfillIsRepeatedUntilItReachesTheCurrentWallClock() {
        long[] now = {(startTime + 1010) * 1000};
        service.clock = () -> now[0];
        when(bootstrap.getNodeTip(HTTP_PORT)).thenReturn(tip(100));
        // The first call reaches the wall clock it saw when it came in (slot 1,010), but takes 200 s: five epochs
        // pass meanwhile. The second reaches the wall clock of its own start.
        when(bootstrap.catchUp(HTTP_PORT)).thenAnswer(invocation -> {
            now[0] += 200_000;
            return caughtUp(1010, 30);
        }).thenAnswer(invocation -> caughtUp(1210, 6));

        boolean started = service.startYanoOnly(clusterInfo(NodeMode.YANO_ONLY), FOLDER, writer);

        assertThat(started).isTrue();
        var order = inOrder(yano, bootstrap);
        order.verify(bootstrap, times(2)).catchUp(HTTP_PORT);
        order.verify(yano).start(any(), any(), eq(YanoRunMode.LIVE), any());
        assertThat(String.join("\n", output)).contains("36 blocks to slot 1,210");
    }

    @Test
    void yanoOnlyBackfillThatNeverCatchesUpDoesNotStartLiveProduction() {
        long[] now = {(startTime + 1010) * 1000};
        service.clock = () -> now[0];
        when(bootstrap.getNodeTip(HTTP_PORT)).thenReturn(tip(100));
        // Every call ends 200 s behind the wall clock
        when(bootstrap.catchUp(HTTP_PORT)).thenAnswer(invocation -> {
            long target = ChainLag.wallClockSlot(startTime, 1.0, now[0]);
            now[0] += 200_000;
            return caughtUp(target, 6);
        });

        boolean started = service.startYanoOnly(clusterInfo(NodeMode.YANO_ONLY), FOLDER, writer);

        assertThat(started).isFalse();
        verifyNoLiveStart();
        assertThat(String.join("\n", output)).contains("could not keep up with wall clock").contains("would skip 5 epochs");
    }

    // --- Companion ------------------------------------------------------------------------------------------

    @Test
    void companionFailedProducerRestartGoesThroughRecovery() {
        NodeControl node = mock(NodeControl.class);
        when(node.stopNode(any())).thenReturn(true);
        when(node.startNode(eq(true), any())).thenReturn(true);
        when(node.startNode(eq(false), any())).thenReturn(false);
        // Both Haskell node and Yano are at wall clock, so every sync is immediate
        when(clusterUtil.getTip(any())).thenAnswer(invocation -> tip(wallSlot()));
        when(bootstrap.getNodeTip(HTTP_PORT)).thenAnswer(invocation -> tip(wallSlot()));
        when(bootstrap.catchUp(HTTP_PORT)).thenAnswer(invocation -> caughtUp(wallSlot(), 3));
        ClusterInfo info = clusterInfo(NodeMode.COMPANION);

        DevnetCatchUpService.Result result = service.catchUpCompanion(info, FOLDER, node,
                ChainLag.of(info, wallSlot() - 100, System.currentTimeMillis()), writer);

        assertThat(result.success()).isFalse();
        // Once in the hand-back step, once more by the recovery that a failed hand-back must still run
        verify(node, times(2)).startNode(eq(false), any());
        assertThat(String.join("\n", output)).contains("Run 'stop' and 'start' to recover");
    }

    @Test
    void cancelledCompanionCatchUpStartsNothingFurtherAndDoesNotRestartTheNode() throws Exception {
        NodeControl node = mock(NodeControl.class);
        when(node.stopNode(any())).thenReturn(true);
        when(node.startNode(eq(true), any())).thenReturn(true);
        when(clusterUtil.getTip(any())).thenAnswer(invocation -> tip(wallSlot() - 100));
        CountDownLatch yanoFollowing = new CountDownLatch(1);
        // Yano's follow start blocks until the catch-up is interrupted
        when(yano.start(any(), any(), eq(YanoRunMode.FOLLOW), any())).thenAnswer(invocation -> {
            yanoFollowing.countDown();
            try {
                Thread.sleep(30_000);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        });
        ClusterInfo info = clusterInfo(NodeMode.COMPANION);

        CompletableFuture<DevnetCatchUpService.Result> running = CompletableFuture.supplyAsync(() ->
                service.catchUpCompanion(info, FOLDER, node, ChainLag.of(info, wallSlot() - 100,
                        System.currentTimeMillis()), writer));
        assertThat(yanoFollowing.await(10, TimeUnit.SECONDS)).isTrue();

        assertThat(service.cancelAndAwait(Duration.ofSeconds(10))).isTrue();
        DevnetCatchUpService.Result result = running.get(10, TimeUnit.SECONDS);

        assertThat(result.success()).isFalse();
        assertThat(result.message()).isEqualTo("Catch-up cancelled");
        assertThat(service.isInProgress()).isFalse();
        verify(yano, never()).start(any(), any(), eq(YanoRunMode.CATCH_UP), any());
        verify(node, never()).startNode(eq(false), any());
    }

    @Test
    void cancelWithNothingRunningReturnsImmediately() throws Exception {
        assertThat(service.cancelAndAwait(Duration.ofSeconds(1))).isTrue();
    }
}
