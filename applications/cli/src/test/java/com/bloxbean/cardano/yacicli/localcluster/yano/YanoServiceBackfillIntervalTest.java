package com.bloxbean.cardano.yacicli.localcluster.yano;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class YanoServiceBackfillIntervalTest {

    private static YanoService service(String setting) {
        YanoService service = new YanoService(null, null, null);
        ReflectionTestUtils.setField(service, "backfillBlockIntervalSlots", setting);
        return service;
    }

    @Test
    void autoHandsTheDecisionToYanoAsZero() {
        assertThat(service("auto").backfillBlockIntervalSlots()).isZero();
        assertThat(service("AUTO").backfillBlockIntervalSlots()).isZero();
        assertThat(service("  auto  ").backfillBlockIntervalSlots()).isZero();
    }

    @Test
    void denseMeansOneBlockPerSlot() {
        assertThat(service("dense").backfillBlockIntervalSlots()).isEqualTo(1);
    }

    @Test
    void explicitIntegerForcesTheInterval() {
        assertThat(service("500").backfillBlockIntervalSlots()).isEqualTo(500);
        assertThat(service("1").backfillBlockIntervalSlots()).isEqualTo(1);
    }

    @Test
    void unusableValuesFallBackToAutomatic() {
        // automatic, not dense : falling back to the slow default would hide the misconfiguration
        assertThat(service("sparse-ish").backfillBlockIntervalSlots()).isZero();
        assertThat(service("-5").backfillBlockIntervalSlots()).isZero();
        assertThat(service("").backfillBlockIntervalSlots()).isZero();
        assertThat(service(null).backfillBlockIntervalSlots()).isZero();
    }
}
