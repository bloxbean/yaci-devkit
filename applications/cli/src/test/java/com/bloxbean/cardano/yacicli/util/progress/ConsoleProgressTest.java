package com.bloxbean.cardano.yacicli.util.progress;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConsoleProgressTest {

    @Test
    void anyWriterOtherThanTheConsoleGetsPlainLines() {
        List<String> lines = new ArrayList<>();

        ProgressTask task = ConsoleProgress.task("Catching up ...", 2, lines::add);
        Step first = task.step("Haskell node → relay");
        first.done("tip slot 1,832");
        Step second = task.step("Backfill to wall clock");
        second.fail("Yano's catch-up call failed");

        assertThat(lines).containsExactly(
                "Catching up ...",
                "  [1/2] Haskell node → relay ...",
                "  [1/2] Haskell node → relay: tip slot 1,832 (0s)",
                "  [2/2] Backfill to wall clock ...",
                "  [2/2] Backfill to wall clock: FAILED: Yano's catch-up call failed");
    }

    @Test
    void plainProgressIsThrottled() {
        List<String> lines = new ArrayList<>();
        Step step = ConsoleProgress.step("Downloading yano", lines::add);

        // Within the first five seconds updates are not written as lines
        step.progress(10, 100, "1 MB / 10 MB");
        step.progress(50, 100, "5 MB / 10 MB");
        step.done("10.0 MB");

        assertThat(lines).containsExactly("  Downloading yano ...", "  Downloading yano: 10.0 MB (0s)");
    }

    @Test
    void percentClampsToTheTotal() {
        Step step = ConsoleProgress.step("Sync", line -> {});
        step.progress(50, 200, "");
        assertThat(step.percent()).isEqualTo(25);
        step.progress(300, 200, "");
        assertThat(step.percent()).isEqualTo(100);
        step.progress(5, 0, "no total");
        assertThat(step.percent()).isZero();
    }

    @Test
    void finishingTwiceWritesOnce() {
        List<String> lines = new ArrayList<>();
        Step step = ConsoleProgress.step("Starting Yano", lines::add);
        step.done("started");
        step.fail("late failure");
        step.progress(1, 1, "ignored");

        assertThat(lines).hasSize(2);
    }

    @Test
    void startingANewStepFinishesTheRunningOne() {
        List<String> lines = new ArrayList<>();
        ProgressTask task = ConsoleProgress.task(null, 2, lines::add);
        task.step("first");
        task.step("second");

        assertThat(lines).contains("  [1/2] first: done (0s)");
    }

    @Test
    void durationsUseTheLargestUnits() {
        assertThat(Step.format(Duration.ofSeconds(9))).isEqualTo("9s");
        assertThat(Step.format(Duration.ofSeconds(125))).isEqualTo("2m 5s");
        assertThat(Step.format(Duration.ofMinutes(75))).isEqualTo("1h 15m");
    }
}
