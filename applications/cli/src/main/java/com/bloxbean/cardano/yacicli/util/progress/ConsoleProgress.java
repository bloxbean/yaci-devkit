package com.bloxbean.cardano.yacicli.util.progress;

import com.bloxbean.cardano.yacicli.util.ConsoleWriter;

import java.io.PrintStream;

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
        TerminalSink() {
            LiveAwareOut.install();
        }

        @Override
        public boolean inPlace() {
            return true;
        }

        @Override
        public void line(String text) {
            LiveAwareOut.line(text);
        }

        @Override
        public void live(String text) {
            LiveAwareOut.live(text);
        }

        @Override
        public void clearLive() {
            LiveAwareOut.clearLive();
        }
    }

    /**
     * Wraps {@code System.out} while progress is rendered in place, so output from anywhere else (process stderr
     * relayed to the console, warnings, other services) never lands on the end of the live step line: the live line
     * is cleared before the write, and the spinner redraws it below on its next tick.
     */
    static final class LiveAwareOut extends PrintStream {
        private static final String CLEAR_LINE = "\r\033[K";
        private static PrintStream terminal;
        // A live step line is on screen
        private static boolean liveShown;
        // Other output left the cursor in the middle of a line
        private static boolean foreignLineOpen;

        private LiveAwareOut(PrintStream target) {
            super(target, true);
        }

        static void install() {
            synchronized (LOCK) {
                if (System.out instanceof LiveAwareOut)
                    return;
                terminal = System.out;
                System.setOut(new LiveAwareOut(terminal));
            }
        }

        @Override
        public void write(int b) {
            synchronized (LOCK) {
                beforeForeignOutput();
                terminal.write(b);
                foreignLineOpen = b != '\n';
            }
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            if (len <= 0)
                return;
            synchronized (LOCK) {
                beforeForeignOutput();
                terminal.write(buf, off, len);
                foreignLineOpen = buf[off + len - 1] != '\n';
            }
        }

        @Override
        public void flush() {
            terminal.flush();
        }

        private static void beforeForeignOutput() {
            if (liveShown) {
                terminal.print(CLEAR_LINE);
                liveShown = false;
            }
        }

        static void line(String text) {
            synchronized (LOCK) {
                clearLiveLocked();
                terminal.println(text);
                terminal.flush();
            }
        }

        static void live(String text) {
            synchronized (LOCK) {
                if (foreignLineOpen) {
                    terminal.println();
                    foreignLineOpen = false;
                }
                terminal.print(CLEAR_LINE + text);
                liveShown = true;
                terminal.flush();
            }
        }

        static void clearLive() {
            synchronized (LOCK) {
                clearLiveLocked();
                terminal.flush();
            }
        }

        private static void clearLiveLocked() {
            if (foreignLineOpen) {
                terminal.println();
                foreignLineOpen = false;
            }
            if (liveShown) {
                terminal.print(CLEAR_LINE);
                liveShown = false;
            }
        }
    }
}
