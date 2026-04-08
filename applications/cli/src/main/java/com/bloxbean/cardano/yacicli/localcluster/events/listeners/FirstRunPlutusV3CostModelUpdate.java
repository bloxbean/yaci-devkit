package com.bloxbean.cardano.yacicli.localcluster.events.listeners;

import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.events.FirstRunDone;
import com.bloxbean.cardano.yacicli.localcluster.service.AccountService;
import com.bloxbean.cardano.yacicli.localcluster.service.ClusterUtilService;
import com.bloxbean.cardano.yacicli.common.AnsiColors;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class FirstRunPlutusV3CostModelUpdate {
    private final ClusterService localClusterService;
    private final ClusterUtilService clusterUtilService;
    private final AccountService accountService;

    @EventListener
    @Order(100) // Run after FirstRunTopupAccounts
    public void updatePlutusV3CostModel(FirstRunDone firstRunDone) {
        Consumer<String> writer = msg -> writeLn(msg);
        try {
            String clusterName = firstRunDone.getCluster();
            var clusterInfo = localClusterService.getClusterInfo(clusterName);
            if (clusterInfo != null && !clusterInfo.isMasterNode()) {
                return;
            }

            if (clusterInfo != null && "companion".equals(clusterInfo.getNodeMode())) {
                writeLn(info("Skipping Plutus cost models update - already submitted via Yano companion bootstrap"));
                return;
            }

            Era era = CommandContext.INSTANCE.getEra();
            if (era != Era.Conway) {
                log.debug("Skipping Plutus cost models update - not Conway era");
                return;
            }

            writeLn(header(AnsiColors.CYAN_BOLD, "Submitting Plutus cost models governance proposal..."));
            clusterUtilService.waitForNextBlocks(1, writer);

            boolean success = accountService.updateCostModels(clusterName, era, writer);
            if (success) {
                writeLn(success("Plutus cost models will be enacted from epoch 1 onward"));
            } else {
                writeLn(error("Plutus cost models governance proposal failed. Extended builtins may not be available."));
            }

        } catch (Exception e) {
            log.error("Plutus cost models update failed", e);
            writeLn(error("Plutus cost models update failed: " + e.getMessage()));
        }
    }
}
