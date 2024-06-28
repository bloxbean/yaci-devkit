package com.bloxbean.cardano.yacicli.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.error;
import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.successLabel;

public class ProcessUtil {

    public static boolean executeAndFinish(ProcessBuilder processBuilder, String scriptPurpose, Consumer<String> writer) {
        ExecutorService executor = Executors.newCachedThreadPool();
        try {
            Process process = processBuilder.start();

            ProcessStream processStream =
                    new ProcessStream(process.getInputStream(), line -> {
                        if (line != null && !line.isEmpty())
                            writer.accept(successLabel(scriptPurpose, " %s", line));
                    });

            ProcessStream errorProcessStream =
                    new ProcessStream(process.getErrorStream(), line -> {
                        if (line != null && !line.isEmpty())
                            writer.accept(error(scriptPurpose + " %s", line));
                    });

            Future<?> inputFuture = executor.submit(processStream);
            Future<?> errorFuture = executor.submit(errorProcessStream);

            inputFuture.get();
            errorFuture.get();

            // Wait for process to complete
            int exitCode = process.waitFor();

            if (exitCode == 0)
                return true;
            else
                return false;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }

        return false;
    }

}
