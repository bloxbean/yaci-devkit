package com.bloxbean.cardano.yacicli.util.progress;

import com.bloxbean.cardano.yacicli.util.ConsoleWriter;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * Progress output for long-running CLI work: numbered steps ({@code [2/4] Yano following the chain}), a spinner
 * while a step runs, a bar with a percentage when the total is known, and the step's duration when it is done.
 * <p>
 * In a terminal the current step is redrawn in place. Anywhere else (Docker logs, pipes, the admin API's writer)
 * the same information is written as plain lines, with progress updates throttled, so logs stay readable.
 * <p>
 * In-place rendering needs the console: pass {@link ConsoleWriter#console()} as the writer. Any other writer gets
 * plain lines.
 */
public final class ConsoleProgress {
    static final long PLAIN_PROGRESS_INTERVAL_MS = 5_000;
    static final long SPINNER_INTERVAL_MS = 120;

    private static final Object LOCK = new Object();
    private static volatile ScheduledExecutorService spinner;

    private ConsoleProgress() {
    }

    /**
     * A task made of a known number of steps. The title is written first.
     */
    public static ProgressTask task(String title, int totalSteps, Consumer<String> writer) {
        ProgressTask task = new ProgressTask(totalSteps, sinkFor(writer));
        if (title != null)
            task.log(title);
        return task;
    }

    /** A single step without a {@code [i/n]} prefix, for one long wait. */
    public static Step step(String label, Consumer<String> writer) {
        return new ProgressTask(0, sinkFor(writer)).step(label);
    }

    static Sink sinkFor(Consumer<String> writer) {
        if (writer instanceof ConsoleWriter.ConsoleSink && interactiveTerminal())
            return new TerminalSink();
        return new PlainSink(writer);
    }

    /**
     * Whether stdout is an interactive terminal that can redraw a line. {@code YACI_PROGRESS=plain} (env or system
     * property) forces plain lines, e.g. for a terminal that does not handle carriage returns.
     */
    static boolean interactiveTerminal() {
        String mode = System.getProperty("yaci.progress", System.getenv("YACI_PROGRESS"));
        if ("plain".equalsIgnoreCase(mode))
            return false;
        if ("dumb".equalsIgnoreCase(System.getenv("TERM")))
            return false;
        return System.console() != null;
    }

    static Object lock() {
        return LOCK;
    }

    static ScheduledExecutorService spinner() {
        if (spinner == null) {
            synchronized (LOCK) {
                if (spinner == null)
                    spinner = Executors.newSingleThreadScheduledExecutor(runnable -> {
                        Thread thread = new Thread(runnable, "console-progress");
                        thread.setDaemon(true);
                        return thread;
                    });
            }
        }
        return spinner;
    }

    /** Where progress goes: the terminal (line redrawn in place) or a plain line writer. */
    interface Sink {
        boolean inPlace();

        /** Write a complete line (above the live step line, if any). */
        void line(String text);

        /** Replace the live step line. Only used when {@link #inPlace()}. */
        default void live(String text) {
        }

        /** Remove the live step line. Only used when {@link #inPlace()}. */
        default void clearLive() {
        }
    }

    static final class PlainSink implements Sink {
        private final Consumer<String> writer;

        PlainSink(Consumer<String> writer) {
            this.writer = writer;
        }

        @Override
        public boolean inPlace() {
            return false;
        }

        @Override
        public void line(String text) {
            writer.accept(text);
        }
    }

    static final class TerminalSink implements Sink {
        private static final String CLEAR_LINE = "\r\033[K";
        private boolean liveShown;

        @Override
        public boolean inPlace() {
            return true;
        }

        @Override
        public void line(String text) {
            synchronized (LOCK) {
                if (liveShown)
                    System.out.print(CLEAR_LINE);
                System.out.println(text);
                liveShown = false;
                System.out.flush();
            }
        }

        @Override
        public void live(String text) {
            synchronized (LOCK) {
                System.out.print(CLEAR_LINE + text);
                liveShown = true;
                System.out.flush();
            }
        }

        @Override
        public void clearLive() {
            synchronized (LOCK) {
                if (liveShown)
                    System.out.print(CLEAR_LINE);
                liveShown = false;
                System.out.flush();
            }
        }
    }
}
