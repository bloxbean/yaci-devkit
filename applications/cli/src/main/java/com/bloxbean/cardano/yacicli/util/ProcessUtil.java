package com.bloxbean.cardano.yacicli.util;

import com.bloxbean.cardano.yacicli.commands.common.ExecutorHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
public class ProcessUtil {
    private final ExecutorHelper executorHelper;

    public boolean executeAndFinish(ProcessBuilder processBuilder, String scriptPurpose, Consumer<String> writer) {
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

            Future<?> inputFuture = executorHelper.getExecutor().submit(processStream);
            Future<?> errorFuture = executorHelper.getExecutor().submit(errorProcessStream);

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
        }

        return false;
    }

    public Process startLongRunningProcess(String processName, ProcessBuilder builder, Queue<String> logs, Consumer<String> writer)
            throws IOException, InterruptedException {
        Process process = builder.start();
        ProcessStream processStream =
                new ProcessStream(process.getInputStream(), line -> {
                    logs.add(String.format("[%s] " + line, processName));
                });

        ProcessStream errorStream =
                new ProcessStream(process.getErrorStream(), line -> {
                    writeLn(error(line));
                });

        executorHelper.getExecutor().submit(processStream);
        executorHelper.getExecutor().submit(errorStream);

        process.waitFor(1, TimeUnit.SECONDS);
        if (!process.isAlive()) {
            writer.accept(error("%s process could not be started.", processName));
            return null;
        }

        //stop consuming error stream
        errorStream.stop();
        return process;
    }

    public String executeAndReturnOutput(ProcessBuilder processBuilder) {
        try {
            StringBuilder sb = new StringBuilder();
            Process process = processBuilder.start();

            ProcessStream processStream =
                    new ProcessStream(process.getInputStream(), line -> {
                        if (line != null && !line.isEmpty())
                            sb.append(line).append(System.lineSeparator());
                    });

            Future<?> inputFuture = executorHelper.getExecutor().submit(processStream);
            inputFuture.get();

            // Wait for process to complete
            process.waitFor();

            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
