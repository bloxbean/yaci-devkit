package com.bloxbean.cardano.yacicli.commands.localcluster.api;

import com.bloxbean.cardano.yaci.core.helpers.LocalStateQueryClient;
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

    public LocalStateQueryClient getLocalQueryClient() throws Exception {
        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);
        long protocolMagic = localClusterService.getClusterInfo(clusterName).getProtocolMagic();
        String[] socketPaths = localClusterService.getClusterInfo(clusterName).getSocketPaths();
        LocalStateQueryClient localStateQueryClient = new LocalStateQueryClient(socketPaths[0], protocolMagic);

        return localStateQueryClient;
    }
}
