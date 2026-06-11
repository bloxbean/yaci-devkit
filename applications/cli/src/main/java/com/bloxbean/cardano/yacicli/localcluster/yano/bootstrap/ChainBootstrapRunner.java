package com.bloxbean.cardano.yacicli.localcluster.yano.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

/**
 * Runs all registered {@link ChainBootstrapStep}s in order during the past-bootstrap window.
 * Spring injects the steps already sorted by {@link org.springframework.core.Ordered}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChainBootstrapRunner {

    private final List<ChainBootstrapStep> steps;

    /**
     * Apply every bootstrap step. A failing step does not abort the rest, but the overall
     * result is {@code false} so the caller can warn.
     */
    public boolean runAll(ChainBootstrapContext context, Consumer<String> writer) {
        boolean allOk = true;
        for (ChainBootstrapStep step : steps) {
            writer.accept(info("Running chain bootstrap step: %s", step.name()));
            try {
                boolean ok = step.apply(context, writer);
                if (!ok) {
                    allOk = false;
                    writer.accept(warn("Chain bootstrap step did not complete: " + step.name()));
                }
            } catch (Exception e) {
                allOk = false;
                log.error("Chain bootstrap step {} threw", step.name(), e);
                writer.accept(error("Chain bootstrap step error (" + step.name() + "): " + e.getMessage()));
            }
        }
        return allOk;
    }
}
