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
    private int nodePort;
    private int submitApiPort;
    private String socketPath;
    private long protocolMagic;
    private double slotLength;
    private double blockTime;
    private long startTime;
}
