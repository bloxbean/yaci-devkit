package com.bloxbean.cardano.yacicli.util.progress;

import com.bloxbean.cardano.yacicli.common.AnsiColors;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * One step of a {@link ProgressTask}: a spinner while it runs, a bar with a percentage once
 * {@link #progress(long, long, String)} reports a total, and a final line with the result and duration.
 */
public final class Step {
    private static final String[] SPINNER = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    private static final int BAR_WIDTH = 20;
    private static final int LABEL_WIDTH = 38;
    private static final int MAX_DETAIL = 48;

    private final String prefix;
    private final String label;
    private final ConsoleProgress.Sink sink;
    private final long startedAt = System.currentTimeMillis();

    private volatile boolean running;
    private volatile long done;
    private volatile long total;
    private volatile String detail = "";
    private int frame;
    private long lastPlainReportAt;
    private ScheduledFuture<?> animation;

    Step(String prefix, String label, ConsoleProgress.Sink sink) {
        this.prefix = prefix;
        this.label = label;
        this.sink = sink;
    }

    void start() {
        running = true;
        lastPlainReportAt = startedAt;
        if (sink.inPlace()) {
            redraw();
            animation = ConsoleProgress.spinner().scheduleAtFixedRate(this::redraw,
                    ConsoleProgress.SPINNER_INTERVAL_MS, ConsoleProgress.SPINNER_INTERVAL_MS, TimeUnit.MILLISECONDS);
        } else {
            sink.line(prefix + label + " ...");
        }
    }

    boolean isRunning() {
        return running;
    }

    /**
     * Report progress. With {@code total > 0} a bar and percentage are shown; otherwise only the detail text.
     *
     * @param done   units done so far (relative to the start of this step)
     * @param total  units in total, or 0 when unknown
     * @param detail short text after the bar, e.g. {@code slot 92,310 / 150,004}
     */
    public void progress(long done, long total, String detail) {
        if (!running)
            return;
        this.done = Math.max(0, Math.min(done, total > 0 ? total : done));
        this.total = total;
        this.detail = detail == null ? "" : detail;
        if (!sink.inPlace()) {
            long now = System.currentTimeMillis();
            if (now - lastPlainReportAt >= ConsoleProgress.PLAIN_PROGRESS_INTERVAL_MS) {
                lastPlainReportAt = now;
                String pct = total > 0 ? percent() + "% " : "";
                sink.line(prefix + label + ": " + pct + (this.detail.isEmpty() ? "" : "(" + this.detail + ")"));
            }
        }
    }

    /** Update the text shown after the label without a percentage. */
    public void detail(String detail) {
        progress(0, 0, detail);
    }

    /** Write a line above the step line (warnings, errors) without breaking the live rendering. */
    public void log(String line) {
        sink.line(line);
        if (running && sink.inPlace())
            redraw();
    }

    /** Finish the step, e.g. {@code done("1,372 blocks")}. */
    public void done(String result) {
        finish(AnsiColors.BLUE + "✅ " + AnsiColors.ANSI_RESET + dotted(label) + " " + result + " (" + elapsed() + ")",
                prefix + label + ": " + result + " (" + elapsed() + ")");
    }

    /** Finish the step as failed. */
    public void fail(String reason) {
        finish(AnsiColors.RED + "🔴 " + AnsiColors.ANSI_RESET + label + ": " + reason,
                prefix + label + ": FAILED: " + reason);
    }

    private void finish(String terminalLine, String plainLine) {
        if (!running)
            return;
        running = false;
        if (animation != null)
            animation.cancel(false);
        synchronized (ConsoleProgress.lock()) {
            sink.line(sink.inPlace() ? prefix + terminalLine : plainLine);
        }
    }

    private void redraw() {
        if (!running)
            return;
        synchronized (ConsoleProgress.lock()) {
            if (!running)
                return;
            String spin = AnsiColors.CYAN_BOLD + SPINNER[frame++ % SPINNER.length] + AnsiColors.ANSI_RESET;
            StringBuilder line = new StringBuilder(prefix).append(spin).append(' ').append(pad(label));
            if (total > 0)
                line.append(' ').append(bar()).append(String.format(" %3d%%", percent()));
            if (!detail.isEmpty())
                line.append("  ").append(truncate(detail));
            line.append(" (").append(elapsed()).append(')');
            sink.live(line.toString());
        }
    }

    int percent() {
        if (total <= 0)
            return 0;
        return (int) Math.min(100, done * 100 / total);
    }

    private String bar() {
        int filled = total > 0 ? (int) Math.min(BAR_WIDTH, done * BAR_WIDTH / total) : 0;
        return "▕" + "█".repeat(filled) + "░".repeat(BAR_WIDTH - filled) + "▏";
    }

    private String elapsed() {
        return format(Duration.ofMillis(System.currentTimeMillis() - startedAt));
    }

    static String format(Duration d) {
        long seconds = d.getSeconds();
        if (seconds < 60)
            return seconds + "s";
        if (seconds < 3600)
            return d.toMinutesPart() + "m " + d.toSecondsPart() + "s";
        return d.toHours() + "h " + d.toMinutesPart() + "m";
    }

    private static String pad(String text) {
        return text.length() >= LABEL_WIDTH ? text : text + " ".repeat(LABEL_WIDTH - text.length());
    }

    private static String dotted(String text) {
        return text.length() >= LABEL_WIDTH ? text : text + " " + ".".repeat(LABEL_WIDTH - text.length() - 1);
    }

    private static String truncate(String text) {
        return text.length() <= MAX_DETAIL ? text : text.substring(0, MAX_DETAIL - 1) + "…";
    }
}
