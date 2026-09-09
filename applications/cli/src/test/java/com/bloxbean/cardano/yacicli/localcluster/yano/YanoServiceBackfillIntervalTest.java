package com.bloxbean.cardano.yacicli.localcluster.yano;

import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class YanoServiceBackfillIntervalTest {

    private static YanoService service(String setting) {
        YanoService service = new YanoService(null, null, null);
        ReflectionTestUtils.setField(service, "backfillBlockIntervalSlots", setting);
        return service;
    }

    private static ClusterInfo cluster(long k, double f) {
        return ClusterInfo.builder().securityParam(k).activeSlotsCoeff(f).build();
    }

    @Test
    void autoDerivesAQuarterOfTheStabilityWindow() {
        // k 300, f 1: 3k/f = 900 slots, a quarter is 225
        assertThat(service("auto").backfillBlockIntervalSlots(cluster(300, 1.0))).isEqualTo(225);
        // k 17280, f 1: 3k/f = 51840, a quarter is 12960
        assertThat(service("auto").backfillBlockIntervalSlots(cluster(17280, 1.0))).isEqualTo(12960);
        // k 20, f 0.2: 3k/f = 300, a quarter is 75
        assertThat(service("auto").backfillBlockIntervalSlots(cluster(20, 0.2))).isEqualTo(75);
    }

    @Test
    void autoFallsBackToOneBlockPerSlotWithoutK() {
        assertThat(service("auto").backfillBlockIntervalSlots(cluster(0, 1.0))).isEqualTo(1);
        assertThat(service(null).backfillBlockIntervalSlots(cluster(1, 1.0))).isEqualTo(1);
    }

    @Test
    void explicitIntegerForcesTheInterval() {
        assertThat(service("500").backfillBlockIntervalSlots(cluster(300, 1.0))).isEqualTo(500);
        assertThat(service("1").backfillBlockIntervalSlots(cluster(300, 1.0))).isEqualTo(1);
        assertThat(service("0").backfillBlockIntervalSlots(cluster(300, 1.0))).isEqualTo(1);
    }

    @Test
    void unparseableSettingMeansOneBlockPerSlot() {
        assertThat(service("dense").backfillBlockIntervalSlots(cluster(300, 1.0))).isEqualTo(1);
    }

    @Test
    void stabilityWindowIsThreeKOverF() {
        assertThat(YanoService.stabilityWindowSlots(cluster(300, 1.0))).isEqualTo(900);
        assertThat(YanoService.stabilityWindowSlots(cluster(20, 0.2))).isEqualTo(300);
        assertThat(YanoService.stabilityWindowSlots(cluster(0, 1.0))).isEqualTo(0);
    }
}
