package com.bloxbean.cardano.yacicli.localcluster;

import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yacicli.localcluster.profiles.GenesisProfile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private long slotsPerKESPeriod;
    private long securityParam;
    private double activeSlotsCoeff;
    private boolean p2pEnabled;
    private long startTime;
    private boolean masterNode;
    private boolean isBlockProducer;
    private String adminNodeUrl; //Only for peer nodes
    private Era era;
    private GenesisProfile genesisProfile;

    @Builder.Default
    private int ogmiosPort = 1337;
    @Builder.Default
    private int kupoPort=1442;
    @Builder.Default
    private int yaciStorePort=8080;
    @Builder.Default
    private int socatPort=3333;
    @Builder.Default
    private int prometheusPort=12798;

    private boolean localMultiNodeEnabled;
}
