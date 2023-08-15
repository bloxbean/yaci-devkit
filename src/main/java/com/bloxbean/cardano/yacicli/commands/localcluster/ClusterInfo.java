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
    private int epochLength;
    private boolean p2pEnabled;
    private long startTime;
    private boolean masterNode;
    private boolean isBlockProducer;
    private String adminNodeUrl; //Only for peer nodes

    @Builder.Default
    private int ogmiosPort = 1337;
    @Builder.Default
    private int kupoPort=1442;
    @Builder.Default
    private int yaciStorePort=8080;
    @Builder.Default
    private int socatPort=3333;
}
