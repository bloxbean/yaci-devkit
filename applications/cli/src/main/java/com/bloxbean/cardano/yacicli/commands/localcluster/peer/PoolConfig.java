package com.bloxbean.cardano.yacicli.commands.localcluster.peer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

import static com.bloxbean.cardano.yacicli.util.AdaConversionUtil.adaToLovelace;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolConfig {
    private String ticker;
    private String metadataHash;
    private BigInteger pledge;
    private BigInteger cost;
    double margin = 0.02;
    private String relayHost;
    private int relayPort;
}
