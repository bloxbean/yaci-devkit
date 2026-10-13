package com.bloxbean.cardano.yacicli.util.progress;

import java.util.function.Consumer;

/**
 * A task of numbered steps, rendered as {@code [i/n] label}. Create one with
 * {@link ConsoleProgress#task(String, int, Consumer)}.
 */
public final class ProgressTask {
    private final int totalSteps;
    private final ConsoleProgress.Sink sink;
    private int stepNumber;
    private Step current;

    ProgressTask(int totalSteps, ConsoleProgress.Sink sink) {
        this.totalSteps = totalSteps;
        this.sink = sink;
    }

    /** Start the next step. A step still running is marked done first. */
    public synchronized Step step(String label) {
        if (current != null && current.isRunning())
            current.done("done");
        stepNumber++;
        String prefix = totalSteps > 0 ? String.format("  [%d/%d] ", stepNumber, totalSteps) : "  ";
        current = new Step(prefix, label, sink);
        current.start();
        return current;
    }

    /** Write a line above the running step (a warning, the summary), keeping the step line intact. */
    public void log(String line) {
        Step step = current;
        if (step != null && step.isRunning())
            step.log(line);
        else
            sink.line(line);
    }

    /**
     * A writer that prints above the running step, for code that reports through a {@code Consumer<String>}.
     */
    public Consumer<String> logWriter() {
        return this::log;
    }
}
