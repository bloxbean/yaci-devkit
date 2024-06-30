package com.bloxbean.cardano.yacicli.commands.common;

import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ExecutorHelper {
    private ExecutorService executor;

    public ExecutorService getExecutor() {
        if (executor == null) {
            synchronized (ExecutorHelper.class) {
                if (executor == null)
                    executor = Executors.newCachedThreadPool();
            }
        }

        return executor;
    }
}
