package com.bloxbean.cardano.yacicli.localcluster.yano;

import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.util.ProcessUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A Yano start that is interrupted (a stop or reset cancelling a catch-up) must not leave the spawned process
 * running untracked.
 */
@DisabledOnOs(OS.WINDOWS)
class YanoServiceStartTest {

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Test
    void interruptedStartTerminatesTheSpawnedProcess(@TempDir Path home) throws Exception {
        // A "yano" that never reports it started
        Path pidFile = home.resolve("yano.pid");
        Path yanoBin = home.resolve("yano");
        Files.writeString(yanoBin, "#!/bin/sh\necho $$ > " + pidFile + "\nexec sleep 120\n");
        assertThat(yanoBin.toFile().setExecutable(true)).isTrue();

        ClusterConfig clusterConfig = mock(ClusterConfig.class);
        when(clusterConfig.getYanoHome()).thenReturn(home.toString());
        YanoService service = new YanoService(clusterConfig, mock(ProcessUtil.class), new YanoConfigBuilder(clusterConfig));
        ClusterInfo info = ClusterInfo.builder().yanoServerPort(freePort()).yanoHttpPort(freePort()).nodePort(3001).build();
        Path clusterFolder = Files.createDirectories(home.resolve("cluster"));

        Thread starting = new Thread(() -> service.start(info, clusterFolder, YanoRunMode.CATCH_UP, msg -> {}));
        starting.start();
        for (int i = 0; i < 100 && !Files.exists(pidFile); i++)
            Thread.sleep(100);
        assertThat(pidFile).exists();
        Thread.sleep(200);
        long pid = Long.parseLong(Files.readString(pidFile).trim());
        assertThat(ProcessHandle.of(pid).map(ProcessHandle::isAlive)).contains(true);

        starting.interrupt();
        starting.join(20_000);

        assertThat(starting.isAlive()).isFalse();
        assertThat(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false)).isFalse();
        assertThat(service.isRunning()).isFalse();
    }

    @Test
    void terminateWorksOnAnInterruptedThreadAndKeepsTheFlag() throws Exception {
        Process process = new ProcessBuilder("sleep", "120").start();

        Thread.currentThread().interrupt();
        ProcessUtil.terminate(process);

        assertThat(Thread.interrupted()).isTrue();
        assertThat(process.isAlive()).isFalse();
    }
}
