package com.bloxbean.cardano.yacicli.localcluster.yano;

import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.NodeMode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class YanoServiceSlotLeaderTimeTravelTest {

    private static YanoService service(String mode) {
        YanoService service = new YanoService(null, null, null);
        ReflectionTestUtils.setField(service, "pastTimeTravelSlotLeaderMode", mode);
        return service;
    }

    private static ClusterInfo cluster(NodeMode nodeMode, double activeSlotsCoeff, boolean multiNode) {
        return ClusterInfo.builder()
                .nodeMode(nodeMode)
                .activeSlotsCoeff(activeSlotsCoeff)
                .localMultiNodeEnabled(multiNode)
                .build();
    }

    @Test
    void autoKeepsDenseBackfillWhenEverySlotIsEligible() {
        assertThat(service("auto").slotLeaderTimeTravelEnabled(cluster(NodeMode.COMPANION, 1.0, false))).isFalse();
    }

    @Test
    void autoTurnsOnSlotLeaderChecksBelowFOneWhenAHaskellNodeValidatesTheChain() {
        assertThat(service("auto").slotLeaderTimeTravelEnabled(cluster(NodeMode.COMPANION, 0.2, false))).isTrue();
        assertThat(service("auto").slotLeaderTimeTravelEnabled(cluster(NodeMode.YANO_PRIMARY, 0.5, false))).isTrue();
    }

    @Test
    void autoLeavesYanoOnlyDenseBecauseNothingChecksVrfThere() {
        assertThat(service("auto").slotLeaderTimeTravelEnabled(cluster(NodeMode.YANO_ONLY, 0.2, false))).isFalse();
    }

    @Test
    void autoKeepsLocalMultiNodeBehaviour() {
        assertThat(service("auto").slotLeaderTimeTravelEnabled(cluster(NodeMode.COMPANION, 1.0, true))).isTrue();
    }

    @Test
    void explicitValuesOverrideAuto() {
        assertThat(service("true").slotLeaderTimeTravelEnabled(cluster(NodeMode.YANO_ONLY, 1.0, false))).isTrue();
        assertThat(service("false").slotLeaderTimeTravelEnabled(cluster(NodeMode.COMPANION, 0.2, true))).isFalse();
        assertThat(service(" TRUE ").slotLeaderTimeTravelEnabled(cluster(NodeMode.YANO_ONLY, 1.0, false))).isTrue();
    }

    @Test
    void unknownValueFallsBackToAuto() {
        assertThat(service("maybe").slotLeaderTimeTravelEnabled(cluster(NodeMode.COMPANION, 0.2, false))).isTrue();
        assertThat(service(null).slotLeaderTimeTravelEnabled(cluster(NodeMode.COMPANION, 1.0, false))).isFalse();
    }
}
