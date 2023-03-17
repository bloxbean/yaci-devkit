package com.bloxbean.cardano.yacicli.commands.localcluster.events.listeners;

import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.commands.localcluster.events.FirstRunDone;
import com.bloxbean.cardano.yacicli.commands.localcluster.service.AccountService;
import com.bloxbean.cardano.yacicli.commands.localcluster.service.ClusterUtilService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
public class FirstRunTopupAccounts {
    private final ClusterService localClusterService;
    private final ClusterUtilService clusterUtilService;
    private final AccountService accountService;

    @Value("${topup_addresses:#{null}}")
    private String[] topupAddresses;

    @EventListener
    public void topupInitialAccounts(FirstRunDone firstRunDone) {
        Consumer<String> writer = msg -> writeLn(msg);
        try {
            String clusterName = firstRunDone.getCluster();
            if (localClusterService.isFirstRunt(clusterName)) {
                if (topupAddresses != null) {
                    if (topupAddresses.length > 0) {
                        writeLn("First run. Let's topup configured addresses");
                        Thread.sleep(1000);
                        clusterUtilService.waitForNextBlocks(1, writer);

                        for (String topupAddress : topupAddresses) {
                            String[] tokens = topupAddress.split(":");
                            String address = tokens[0].trim();
                            String value = tokens[1].trim();
                            writeLn("Topup address: " + address + ", value: " + value + " Ada" + "\n");
                            accountService.topup(clusterName, address, Double.parseDouble(value), msg -> writeLn(msg));
                            boolean flag = clusterUtilService.waitForNextBlocks(1, writer);
                            if (flag)
                                writeLn(infoLabel("OK", "Topup done"));
                        }
                    }
                } else {
                    Thread.sleep(1000);
                    clusterUtilService.waitForNextBlocks(1, writer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            writeLn("Ada topup failed. Please manually topup additional account");
        }
    }
}
