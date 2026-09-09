package com.bloxbean.cardano.yacicli.localcluster.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GenesisConfigMergeTest {

    @Test
    void mergeAcceptsFractionalStabilityWindowFactor() {
        GenesisConfig config = new GenesisConfig();

        config.merge(Map.of("stabilityWindowFactor", "0.7"));

        assertThat(config.getStabilityWindowFactor()).isEqualTo(0.7);
    }

    @Test
    void mergeLeavesStabilityWindowFactorAloneWhenAbsentOrBlank() {
        GenesisConfig config = new GenesisConfig();

        config.merge(Map.of("securityParam", "80"));
        assertThat(config.getStabilityWindowFactor()).isEqualTo(0.5);

        config.merge(Map.of("stabilityWindowFactor", ""));
        assertThat(config.getStabilityWindowFactor()).isEqualTo(0.5);
    }
}
