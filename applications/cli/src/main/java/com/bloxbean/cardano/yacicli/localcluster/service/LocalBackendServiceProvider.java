package com.bloxbean.cardano.yacicli.localcluster.service;

import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.NodeMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Provides a cardano-client-lib {@link BackendService} pointing at the running devnet's
 * Blockfrost-compatible indexer (yaci-store), or the Yano HTTP API in yano-only mode.
 *
 * <p>Shared by features that build/submit transactions via QuickTx / TxPlan / TxFlow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LocalBackendServiceProvider {
    private final ClusterService clusterService;

    public Optional<BackendService> getBackendService(String clusterName) {
        try {
            ClusterInfo clusterInfo = clusterService.getClusterInfo(clusterName);
            if (clusterInfo == null) {
                log.error("Cluster {} not found", clusterName);
                return Optional.empty();
            }

            String backendUrl;
            if (NodeMode.YANO_ONLY == clusterInfo.getNodeMode()) {
                backendUrl = "http://localhost:" + clusterInfo.getYanoHttpPort() + "/api/v1/";
            } else {
                backendUrl = "http://localhost:" + clusterInfo.getYaciStorePort() + "/api/v1/";
            }
            return Optional.of(new BFBackendService(backendUrl, "dummy_key"));
        } catch (Exception e) {
            log.error("Error creating backend service for cluster {}", clusterName, e);
            return Optional.empty();
        }
    }
}
