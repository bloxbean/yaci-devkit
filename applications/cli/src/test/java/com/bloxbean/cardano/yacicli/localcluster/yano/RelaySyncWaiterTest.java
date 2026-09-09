package com.bloxbean.cardano.yacicli.localcluster.yano;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yacicli.common.Tuple;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RelaySyncWaiterTest {
    private static final long EPOCH_LENGTH = 600;
    private static final long TARGET_EPOCH = 3;

    /** Fake clock: sleeping advances time, and each tip read costs a little time too. */
    private static final class FakeTime {
        long now = 1_000_000L;
        final List<Long> sleeps = new ArrayList<>();

        long read() {
            return now;
        }

        void sleep(long millis) {
            sleeps.add(millis);
            now += millis;
        }
    }

    private static Tuple<Long, Point> tip(long height, long slot) {
        return new Tuple<>(height, new Point(slot, "00"));
    }

    /** Replays scripted tips in order; the last one repeats forever. */
    private static RelaySyncWaiter.TipReader script(List<Tuple<Long, Point>> tips) {
        Iterator<Tuple<Long, Point>> it = tips.iterator();
        Tuple<Long, Point>[] last = new Tuple[1];
        return () -> {
            if (it.hasNext()) {
                last[0] = it.next();
            }
            return last[0];
        };
    }

    @Test
    void returnsSyncedAsSoonAsTipReachesTargetEpoch() throws Exception {
        FakeTime time = new FakeTime();
        RelaySyncWaiter waiter = new RelaySyncWaiter(30_000, 0, time::read, time::sleep);
        List<String> progress = new ArrayList<>();

        RelaySyncWaiter.Outcome outcome = waiter.await(TARGET_EPOCH, EPOCH_LENGTH,
                script(List.of(tip(10, 10), tip(700, 700), tip(1900, 1900))), progress::add);

        assertThat(outcome.synced()).isTrue();
        assertThat(outcome.epoch()).isEqualTo(3);
        assertThat(outcome.slot()).isEqualTo(1900);
        assertThat(outcome.height()).isEqualTo(1900);
        assertThat(outcome.reason()).isNull();
        assertThat(time.sleeps).hasSize(2);
    }

    @Test
    void keepsWaitingBeyondStallTimeoutWhileTipAdvances() throws Exception {
        FakeTime time = new FakeTime();
        // 30s stall timeout, but the relay needs 120 polls (2 minutes) to reach the target.
        RelaySyncWaiter waiter = new RelaySyncWaiter(30_000, 0, time::read, time::sleep);
        List<Tuple<Long, Point>> tips = new ArrayList<>();
        for (long i = 1; i <= 120; i++) {
            tips.add(tip(i * 15, i * 15));
        }
        List<String> progress = new ArrayList<>();

        RelaySyncWaiter.Outcome outcome = waiter.await(TARGET_EPOCH, EPOCH_LENGTH, script(tips), progress::add);

        assertThat(outcome.synced()).isTrue();
        assertThat(outcome.slot()).isEqualTo(1800);
        assertThat(time.now - 1_000_000L).isGreaterThan(100_000L);
        assertThat(progress).isNotEmpty();
        assertThat(progress.get(0)).startsWith("Relay sync in progress: epoch 0 of 3");
        assertThat(progress.get(0)).contains("blocks/s");
    }

    @Test
    void givesUpWhenTipStopsAdvancing() throws Exception {
        FakeTime time = new FakeTime();
        RelaySyncWaiter waiter = new RelaySyncWaiter(30_000, 0, time::read, time::sleep);

        RelaySyncWaiter.Outcome outcome = waiter.await(TARGET_EPOCH, EPOCH_LENGTH,
                script(List.of(tip(100, 100), tip(200, 200))), msg -> {});

        assertThat(outcome.synced()).isFalse();
        assertThat(outcome.slot()).isEqualTo(200);
        assertThat(outcome.reason()).contains("did not advance for 30s").contains("slot 200");
        // one poll with progress, then 30s of stalled polls
        assertThat(time.now - 1_000_000L).isEqualTo(31_000L);
    }

    @Test
    void givesUpWhenNoTipIsEverRead() throws Exception {
        FakeTime time = new FakeTime();
        RelaySyncWaiter waiter = new RelaySyncWaiter(5_000, 0, time::read, time::sleep);

        RelaySyncWaiter.Outcome outcome = waiter.await(TARGET_EPOCH, EPOCH_LENGTH, () -> null, msg -> {});

        assertThat(outcome.synced()).isFalse();
        assertThat(outcome.slot()).isEqualTo(-1);
        assertThat(outcome.reason()).contains("before any tip was read");
    }

    @Test
    void ceilingEndsTheWaitEvenWhileTipAdvances() throws Exception {
        FakeTime time = new FakeTime();
        RelaySyncWaiter waiter = new RelaySyncWaiter(30_000, 10_000, time::read, time::sleep);
        List<Tuple<Long, Point>> tips = new ArrayList<>();
        for (long i = 1; i <= 100; i++) {
            tips.add(tip(i, i));
        }

        RelaySyncWaiter.Outcome outcome = waiter.await(TARGET_EPOCH, EPOCH_LENGTH, script(tips), msg -> {});

        assertThat(outcome.synced()).isFalse();
        assertThat(outcome.reason()).contains("ceiling of 10s");
        assertThat(time.now - 1_000_000L).isEqualTo(10_000L);
    }

    @Test
    void unknownEpochLengthCannotBeWaitedFor() throws Exception {
        FakeTime time = new FakeTime();
        RelaySyncWaiter waiter = new RelaySyncWaiter(30_000, 0, time::read, time::sleep);

        RelaySyncWaiter.Outcome outcome = waiter.await(TARGET_EPOCH, 0, () -> tip(1, 1), msg -> {});

        assertThat(outcome.synced()).isFalse();
        assertThat(outcome.reason()).contains("epoch length");
        assertThat(time.sleeps).isEmpty();
    }
}
