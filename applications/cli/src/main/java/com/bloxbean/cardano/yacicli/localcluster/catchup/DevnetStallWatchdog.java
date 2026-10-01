package com.bloxbean.cardano.yacicli.localcluster.catchup;

import com.bloxbean.cardano.yacicli.common.AnsiColors;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.NodeMode;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterDeleted;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStarted;
import com.bloxbean.cardano.yacicli.localcluster.events.ClusterStopped;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.jline.reader.LineReader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

/**
 * Catches a running companion devnet up when its block producer has stalled, which is what happens when the laptop
 * lid closes: the processes freeze, and on wake the node's tip is older than its forecast window, so it never forges
 * again. Checks the chain lag every {@value #CHECK_INTERVAL_SECONDS} s while a devnet runs.
 * <p>
 * Yano-only devnets are not checked: Yano forges as soon as the machine wakes, so there is no stall to detect (see
 * ADR-0017, limitations).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DevnetStallWatchdog {
    static final long CHECK_INTERVAL_SECONDS = 30;

    // ObjectProvider: ClusterService -> ClusterStartService -> DevnetCatchUpService, and this listens to both sides
    private final ObjectProvider<ClusterService> clusterServiceProvider;
    private final DevnetCatchUpService devnetCatchUpService;
    // The interactive shell's line reader, absent in non-interactive runs
    private final ObjectProvider<LineReader> lineReaderProvider;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "devnet-stall-watchdog");
        thread.setDaemon(true);
        return thread;
    });
    private ScheduledFuture<?> check;
    private volatile String clusterName;
    private volatile boolean hintShown;

    @EventListener
    public synchronized void handleClusterStarted(ClusterStarted event) {
        stopChecking();
        clusterName = event.getClusterName();
        hintShown = false;
        check = scheduler.scheduleWithFixedDelay(this::checkSafely, CHECK_INTERVAL_SECONDS, CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
    }

    @EventListener
    public void handleClusterStopped(ClusterStopped event) {
        stopChecking();
    }

    @EventListener
    public void handleClusterDeleted(ClusterDeleted event) {
        stopChecking();
    }

    @PreDestroy
    public void shutdown() {
        stopChecking();
        scheduler.shutdownNow();
    }

    private synchronized void stopChecking() {
        if (check != null) {
            check.cancel(false);
            check = null;
        }
    }

    private void checkSafely() {
        try {
            check();
        } catch (Exception e) {
            log.debug("Stall check failed: {}", e.getMessage());
        }
    }

    void check() throws Exception {
        String name = clusterName;
        if (name == null || devnetCatchUpService.isInProgress())
            return;

        ClusterService clusterService = clusterServiceProvider.getObject();
        ClusterInfo clusterInfo = clusterService.getClusterInfo(name);
        if (clusterInfo == null || clusterInfo.getNodeMode() != NodeMode.COMPANION
                || devnetCatchUpService.unsupportedReason(clusterInfo).isPresent())
            return;

        ChainLag lag = clusterService.chainLag(name);
        if (lag == null || !lag.stalled()) {
            hintShown = false;
            return;
        }

        if (!devnetCatchUpService.isAutoCatchUp()) {
            if (!hintShown)
                writeLn(warn("The devnet's chain is %s behind wall clock (the machine slept?) and the node cannot forge. "
                        + "Run 'catch-up' to bring it to wall clock.", lag.idleTimeText()));
            hintShown = true;
            return;
        }

        // The shell prompt is waiting for input on the current line: start below it, redraw it when done
        LineReader reader = lineReaderProvider.getIfAvailable();
        boolean atPrompt = reader != null && reader.isReading();
        if (atPrompt)
            System.out.println();

        writeLn(header(AnsiColors.CYAN_BOLD, "The devnet stopped producing blocks (the machine slept?)"));
        var result = clusterService.catchUp(name, false, console());
        if (!result.success())
            writeLn(error(result.message() + " Run 'catch-up' to retry, or 'reset' to start a new chain."));

        if (atPrompt)
            redrawPrompt(reader);
    }

    private static void redrawPrompt(LineReader reader) {
        try {
            if (reader.isReading()) {
                reader.callWidget(LineReader.REDRAW_LINE);
                reader.callWidget(LineReader.REDISPLAY);
            }
        } catch (RuntimeException e) {
            log.debug("Could not redraw the prompt: {}", e.getMessage());
        }
    }
}
