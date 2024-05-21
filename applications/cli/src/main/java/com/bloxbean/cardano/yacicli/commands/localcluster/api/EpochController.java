package com.bloxbean.cardano.yacicli.commands.localcluster.api;

import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.backend.model.EpochContent;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.EpochNoQuery;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.EpochNoQueryResult;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;
import com.bloxbean.cardano.yacicli.commands.localcluster.common.LocalClientProviderHelper;
import com.bloxbean.cardano.yacicli.commands.localcluster.service.LocalProtocolParamSupplier;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static com.bloxbean.cardano.yacicli.commands.localcluster.ClusterCommands.CLUSTER_NAME;

@RestController
@RequestMapping(path = "/local-cluster/api/epochs")
public class EpochController {

    private LocalClientProviderHelper localQueryClientUtil;

    public EpochController(LocalClientProviderHelper localQueryClientUtil) {
        this.localQueryClientUtil = localQueryClientUtil;
    }

    @GetMapping("latest")
    public EpochContent getLatestEpoch() {
        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
        Era era = CommandContext.INSTANCE.getEra();

        LocalClientProvider localClientProvider = null;
        try { //TODO -- replace with try-with-resource
            localClientProvider = localQueryClientUtil.getLocalClientProvider();
            LocalStateQueryClient localStateQueryClient = localClientProvider.getLocalStateQueryClient();
            localClientProvider.start();
            Mono<EpochNoQueryResult> mono = localStateQueryClient.executeQuery(new EpochNoQuery(era));

            long epochNo = mono.block(Duration.ofSeconds(5)).getEpochNo();
            return EpochContent.builder().epoch(Integer.valueOf((int) epochNo)).build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (localClientProvider != null)
                localClientProvider.shutdown();
        }
    }

    @Operation(summary = "Get current protocol parameters.")
    @GetMapping("parameters")
    ProtocolParams getProtocolParameters() {
        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
        Era era = CommandContext.INSTANCE.getEra();

        LocalClientProvider localClientProvider = null;
        try {
            localClientProvider = localQueryClientUtil.getLocalClientProvider();
            LocalStateQueryClient localStateQueryClient = localClientProvider.getLocalStateQueryClient();
            localClientProvider.start();
            LocalProtocolParamSupplier localProtocolSupplier = new LocalProtocolParamSupplier(localStateQueryClient, era);
            return localProtocolSupplier.getProtocolParams();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (localClientProvider != null)
                localClientProvider.shutdown();
        }
    }

    @Operation(summary = "Get protocol parameters. The {number} path variable is ignored. So any value can be passed. It always returns current protocol parameters")
    @GetMapping("/{number}/parameters")
    ProtocolParams getProtocolParameters(@PathVariable int number) {
        return getProtocolParameters();
    }
}
