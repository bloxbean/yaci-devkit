package com.bloxbean.cardano.yacicli.localcluster.catchup;

import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ChainLagTest {
    // Default devnet: 600-slot epochs, k = 100, f = 1, 1 s slots -> forecast window 300 slots
    private static ChainLag lag(long tipSlot, long wallClockSlot) {
        return new ChainLag(tipSlot, wallClockSlot, 300, 600, 1.0);
    }

    @Test
    void forecastWindowIsThreeKOverF() {
        assertThat(ChainLag.forecastWindowSlots(100, 1.0)).isEqualTo(300);
        assertThat(ChainLag.forecastWindowSlots(7, 1.0)).isEqualTo(21);
        assertThat(ChainLag.forecastWindowSlots(100, 0.5)).isEqualTo(600);
        assertThat(ChainLag.forecastWindowSlots(2160, 0.05)).isEqualTo(129600);
        assertThat(ChainLag.forecastWindowSlots(0, 1.0)).isZero();
        assertThat(ChainLag.forecastWindowSlots(100, 0)).isZero();
    }

    @Test
    void wallClockSlotCountsSlotsSinceTheStartTime() {
        long start = 1_790_000_000L;
        assertThat(ChainLag.wallClockSlot(start, 1.0, start * 1000 + 42_500)).isEqualTo(42);
        assertThat(ChainLag.wallClockSlot(start, 0.2, start * 1000 + 1_000)).isEqualTo(5);
        assertThat(ChainLag.wallClockSlot(start, 1.0, start * 1000 - 5_000)).isZero();
        assertThat(ChainLag.wallClockSlot(start, 0, start * 1000 + 5_000)).isZero();
    }

    @Test
    void ofReadsTheClusterInfo() {
        ClusterInfo info = ClusterInfo.builder()
                .startTime(1_790_000_000L).slotLength(1.0).epochLength(600).securityParam(100).activeSlotsCoeff(1.0)
                .build();

        ChainLag lag = ChainLag.of(info, 1000, 1_790_000_000_000L + 2_000_000);

        assertThat(lag.wallClockSlot()).isEqualTo(2000);
        assertThat(lag.lagSlots()).isEqualTo(1000);
        assertThat(lag.forecastWindowSlots()).isEqualTo(300);
    }

    @Test
    void stalledOnceTheLagReachesTheForecastWindow() {
        assertThat(lag(1000, 1299).stalled()).isFalse();
        assertThat(lag(1000, 1300).stalled()).isTrue();
        assertThat(lag(1000, 900).stalled()).isFalse();
        assertThat(lag(1000, 900).lagSlots()).isZero();
    }

    @Test
    void startCatchesUpPastThreeQuartersOfTheWindow() {
        assertThat(lag(1000, 1225).needsCatchUpOnStart()).isFalse();
        assertThat(lag(1000, 1226).needsCatchUpOnStart()).isTrue();
        assertThat(new ChainLag(0, 10_000, 0, 600, 1.0).needsCatchUpOnStart()).isFalse();
    }

    @Test
    void countsEpochBoundariesBetweenTipAndWallClock() {
        assertThat(lag(1000, 1100).epochsToCross()).isZero();
        assertThat(lag(1100, 1300).epochsToCross()).isEqualTo(1);
        assertThat(lag(1973, 2608).epochsToCross()).isEqualTo(1);
        assertThat(lag(0, 6000).epochsToCross()).isEqualTo(10);
    }

    @Test
    void estimatesOneBlockPerWindowPlusEpochBoundariesPlusTheTarget() {
        // The 2026-09-30 run: 635 slots from 1973 produced 3 blocks (2272, 2400, 2608)
        assertThat(lag(1973, 2608).estimatedBackfillBlocks()).isEqualTo(635 / 299 + 1 + 1);
        assertThat(lag(1000, 1000).estimatedBackfillBlocks()).isZero();
        // Three days on the default devnet
        assertThat(lag(0, 259_200).estimatedBackfillBlocks()).isEqualTo(259_200 / 299 + 432 + 1);
    }

    @Test
    void idleTimeUsesTheSlotLength() {
        assertThat(lag(0, 90).idleTime()).isEqualTo(Duration.ofSeconds(90));
        assertThat(new ChainLag(0, 90, 21, 40, 0.2).idleTime()).isEqualTo(Duration.ofMillis(18_000));
    }

    @Test
    void idleTimeTextShowsTheTwoLargestUnits() {
        assertThat(ChainLag.durationText(Duration.ofSeconds(40))).isEqualTo("40s");
        assertThat(ChainLag.durationText(Duration.ofSeconds(432))).isEqualTo("7m 12s");
        assertThat(ChainLag.durationText(Duration.ofMinutes(125))).isEqualTo("2h 5m");
        assertThat(ChainLag.durationText(Duration.ofHours(76).plusMinutes(12))).isEqualTo("3d 4h");
    }
}
