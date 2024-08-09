package com.bloxbean.cardano.yacicli.localcluster.events.listeners;

import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.events.FirstRunDone;
import com.bloxbean.cardano.yacicli.localcluster.service.AccountService;
import com.bloxbean.cardano.yacicli.localcluster.service.ClusterUtilService;
import com.bloxbean.cardano.yacicli.localcluster.service.DefaultAddressService;
import com.bloxbean.cardano.yacicli.common.CommandContext;
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
    private final DefaultAddressService defaultAddressService;

    @Value("${topup_addresses:#{null}}")
    private String[] topupAddresses;

    @EventListener
    public void topupInitialAccounts(FirstRunDone firstRunDone) {
        Consumer<String> writer = msg -> writeLn(msg);
        try {
            String clusterName = firstRunDone.getCluster();
            var clusterInfo = localClusterService.getClusterInfo(clusterName);
            if (clusterInfo != null && !clusterInfo.isMasterNode()) {
                //Return if it's a peer node, not the master node
                return;
            }

            defaultAddressService.printDefaultAddresses(true);
            if (topupAddresses != null) {
                if (topupAddresses.length > 0) {
                    writeLn("First run. Let's topup configured addresses");
                    Thread.sleep(1000);
                    clusterUtilService.waitForNextBlocks(1, writer);

                    Era era = CommandContext.INSTANCE.getEra();

                    for (String topupAddress : topupAddresses) {
                        String[] tokens = topupAddress.split(":");
                        String address = tokens[0].trim();
                        String value = tokens[1].trim();
                        writeLn("Topup address: " + address + ", value: " + value + " Ada" + "\n");
                        accountService.topup(clusterName, era, address, Double.parseDouble(value), msg -> writeLn(msg));
                        boolean flag = clusterUtilService.waitForNextBlocks(1, writer);
                        if (flag)
                            writeLn(infoLabel("OK", "Topup done"));
                    }
                }
            } else {
                Thread.sleep(1000);
                clusterUtilService.waitForNextBlocks(1, writer);
            }

        } catch (Exception e) {
            e.printStackTrace();
            writeLn("Ada topup failed. Please manually topup additional account");
        }
    }
}
