package com.bloxbean.cardano.yacicli.commands.localcluster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClusterInfo {
    private int[] nodePorts;
    private int submitApiPort;
    private String[] socketPaths;
    @Builder.Default
    private long protocolMagic = 42;
}
