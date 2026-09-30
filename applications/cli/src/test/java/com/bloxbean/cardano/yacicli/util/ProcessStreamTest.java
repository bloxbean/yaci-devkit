package com.bloxbean.cardano.yacicli.util;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProcessStreamTest {

    private static ByteArrayInputStream input(String... lines) {
        return new ByteArrayInputStream((String.join("\n", lines) + "\n").getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void keepsReadingAfterAConsumerFailure() {
        // A consumer that throws used to end the read loop, after which nothing drained the process
        // pipe (seen with cardano-node's "Progress: 95.65%" lines when they were used as a format string).
        List<String> seen = new ArrayList<>();
        ProcessStream stream = new ProcessStream(input("first", "bad", "last"), line -> {
            if (line.equals("bad"))
                throw new IllegalStateException("boom");
            seen.add(line);
        });

        stream.run();

        assertThat(seen).containsExactly("first", "last");
    }
}
