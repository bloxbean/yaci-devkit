package com.bloxbean.cardano.yacicli.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessStream implements Runnable {
    private InputStream inputStream;
    private Consumer<String> consumer;
    private boolean stop;

    public ProcessStream(InputStream inputStream, Consumer<String> consumer) {
        this.inputStream = inputStream;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        new BufferedReader(new InputStreamReader(inputStream)).lines()
                .forEach(line -> {
                    if(stop)
                        return;

                    //A failing consumer must not end the read loop : nothing else drains the pipe, so the
                    //process would then block or drop its output once the pipe buffer is full
                    try {
                        consumer.accept(line);
                    } catch (RuntimeException e) {
                        log.warn("Could not process output line: {}", e.getMessage());
                    }
                });
    }

    public void stop() {
        stop = true;
    }
}
