package com.bloxbean.cardano.yacicli.util;

import com.bloxbean.cardano.yacicli.commands.common.ExecutorHelper;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
public class ProcessUtil {
    private final ExecutorHelper executorHelper;
    private final ClusterConfig clusterConfig;

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

        createProcessId(processName, process);

        //stop consuming error stream
        errorStream.stop();
        return process;
    }

    public String createProcessId(String processName, Process process) {
        try {
            var yaciCliHome = clusterConfig.getYaciCliHome();
            // Validate the yaciCliHome directory
            Path homePath = Paths.get(yaciCliHome);
            if (!Files.exists(homePath)) {
                writeLn(error("yaci-cli home directory does not exist: " + yaciCliHome));
                return null;
            }

            if (!Files.isDirectory(homePath)) {
                writeLn(error("yaci-cli home path is not a directory: " + yaciCliHome));
                return null;
            }

            Path pids = homePath.resolve("pids");
            if (!Files.exists(pids))
                pids.toFile().mkdirs();

            Path pidFilePath = pids.resolve(processName + ".pid");
            long pid = process.pid();
            Files.writeString(pidFilePath, String.valueOf(pid));

            // Return the full path to the created PID file as a string
            return pidFilePath.toString();
        } catch (IOException e) {
            writeLn(error("Failed to create the PID file: " + e.getMessage()));
            return null;
        }
    }

    public void deletePidFile(String processName) {
        var yaciCliHome = clusterConfig.getYaciCliHome();
        // Validate the yaciCliHome directory
        Path homePath = Paths.get(yaciCliHome);
        Path pids = homePath.resolve("pids");

        var pidPath = pids.resolve(processName + ".pid");
        if (Files.exists(pidPath)) {
            pidPath.toFile().delete();
            writeLn(info("Deleted pid file : " + processName + ".pid"));
        }
    }

    public void killRunningProcesses() {
        var yaciCliHome = clusterConfig.getYaciCliHome();

        // Validate the yaciCliHome directory
        Path homePath = Paths.get(yaciCliHome);
        if (!Files.exists(homePath)) {
            writeLn(error("The yaciCliHome directory does not exist: " + yaciCliHome));
            return;
        }
        if (!Files.isDirectory(homePath)) {
            writeLn(error("The yaciCliHome path is not a directory: " + yaciCliHome));
            return;
        }

        // Resolve the pids directory
        Path pidsDir = homePath.resolve("pids");
        if (!Files.exists(pidsDir) || !Files.isDirectory(pidsDir)) {
            writeLn(error("The pids directory does not exist or is not a directory: " + pidsDir));
            return;
        }

        List<Long> pidList = new ArrayList<>();
        try (DirectoryStream<Path> pidFiles = Files.newDirectoryStream(pidsDir, "*.pid")) {
            for (Path pidFile : pidFiles) {
                try {
                    // Read the PID from the file
                    String pidString = Files.readString(pidFile).trim();
                    if (!pidString.isEmpty()) {
                        pidList.add(Long.parseLong(pidString));
                    }
                } catch (IOException | NumberFormatException e) {
                    writeLn(error("Failed to read or parse PID file: " + pidFile + " - " + e.getMessage()));
                }
            }
        } catch (IOException e) {
            writeLn(error("Failed to list PID files in directory: " + pidsDir + " : " + e.getMessage()));
            return;
        }

        var deletedPids = new ArrayList<>();
        for (Long pid : pidList) {
            try {
                ProcessHandle.of(pid)
                                .ifPresent(processHandle -> {
                                    processHandle.descendants().forEach(ph -> {
                                        deletedPids.add(ph.pid());
                                        ph.destroyForcibly();
                                    });
                                    var result = processHandle.destroyForcibly();
                                    if (!result) {
                                        writeLn(error("Failed to kill process with PID : " + pid));
                                    } else {
                                        deletedPids.add(processHandle.pid());
                                    }
                                });
            } catch (Exception e) {
                writeLn(error("Failed to kill process with PID: " + pid + " - " + e.getMessage()));
            }
        }

        if (!deletedPids.isEmpty()) {
            writeLn(info("Found existing processes. Killed processes with pids: " + deletedPids));
        }
    }
}
