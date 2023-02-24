package com.bloxbean.cardano.yacicli.commands.localcluster.api;

import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import org.springframework.stereotype.Component;

import static com.bloxbean.cardano.yacicli.commands.localcluster.ClusterCommands.CUSTER_NAME;

@Component
public class LocalQueryClientUtil {
    private ClusterService localClusterService;

    public LocalQueryClientUtil(ClusterService clusterService) {
        this.localClusterService = clusterService;
    }

    public LocalClientProvider getLocalQueryClient() throws Exception {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);
        long protocolMagic = localClusterService.getClusterInfo(clusterName).getProtocolMagic();
        String[] socketPaths = localClusterService.getClusterInfo(clusterName).getSocketPaths();

        LocalClientProvider localClientProvider = new LocalClientProvider(socketPaths[0], protocolMagic);
        return localClientProvider;
    }
}
