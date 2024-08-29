package com.bloxbean.cardano.yacicli.localcluster.service;

import ch.qos.logback.classic.Level;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yacicli.commands.common.RootLogService;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.common.LocalClientProviderHelper;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.common.Tuple;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClusterUtilService {
    private final ClusterService clusterService;
    private final LocalClientProviderHelper localQueryClientUtil;
    private final RootLogService rootLogService;

    public Tuple<Long, Point> getTip(Consumer<String> writer) {
        String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);
        Era era = CommandContext.INSTANCE.getEra();

        LocalNodeService localNodeService = null;
        Level orgLevel = rootLogService.getLogLevel();
        if (!rootLogService.isDebugLevel())
            rootLogService.setLogLevel(Level.OFF);
        try {
            Path clusterFolder = clusterService.getClusterFolder(clusterName);
            localNodeService = new LocalNodeService(clusterFolder, era, localQueryClientUtil, writer);

            return localNodeService.getTip();
        } catch (Exception e) {
            log.error("Error", e);
            writeLn(error("Find tip error : " + e.getMessage()));
            return null;
        } finally {
            rootLogService.setLogLevel(orgLevel);
            if (localNodeService != null)
                localNodeService.shutdown();
        }
    }

    @SneakyThrows
    public int getKESPeriod() {
        String clusterName = CommandContext.INSTANCE.getProperty(ClusterConfig.CLUSTER_NAME);

        long slotsPerKESPeriod = 129600;
        var clusterInfo = clusterService.getClusterInfo(clusterName);
        if (clusterInfo == null) {
            writeLn(error("Cluster not found. Please create a cluster first."));
        } else {
            if (clusterInfo.getSlotsPerKESPeriod() > 0)
                slotsPerKESPeriod = clusterInfo.getSlotsPerKESPeriod();
        }

        Tuple<Long, Point> tip = getTip((msg) -> {writeLn(msg);});

        if (tip == null)
            return -1;

        return (int) (tip._2.getSlot() /slotsPerKESPeriod);
    }

    public boolean waitForNextBlocks(int noOfBlocks, Consumer<String> writer) {
        int counter = 0;
        Tuple<Long, Point> tip = getTip(writer);
        if (tip == null)
            tip = new Tuple<>(0L, new Point(0, ""));

        Tuple<Long, Point> newTip = new Tuple<>(tip._1, tip._2);
        while (newTip._1 <= tip._1) {
            writer.accept("Waiting for next block...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return false;
            }
            newTip = getTip(writer);
            if (newTip == null)
                newTip = new Tuple<>(0L, new Point(0, ""));

            counter++;
            if (counter == 30) {
                writer.accept(error("Waited too long. Something is wrong. You may want to recreate the cluster or just reset data with 'reset' option ..."));
                return false;
            }
        }

        return true;
    }
}
