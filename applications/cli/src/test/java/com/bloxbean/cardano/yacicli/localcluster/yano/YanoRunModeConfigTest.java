package com.bloxbean.cardano.yacicli.localcluster.yano;

import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class YanoRunModeConfigTest {
    private static final Path DIR = Path.of("/tmp/devnet");

    private static Map<String, String> props(YanoRunMode mode) {
        ClusterInfo info = ClusterInfo.builder().nodePort(3001).protocolMagic(42).build();
        return new YanoConfigBuilder(null).properties(info, DIR.resolve("config"), DIR.resolve("data"),
                DIR.resolve("history"), mode, false, 0);
    }

    @Test
    void liveModeSetsNoModeSpecificProperties() {
        Map<String, String> props = props(YanoRunMode.LIVE);

        assertThat(props).doesNotContainKeys("yano.block-producer.past-time-travel-mode",
                "yano.block-producer.block-time-millis", "yano.client.enabled", "yano.dev-mode",
                "yano.block-producer.backfill-block-interval-slots");
        assertThat(props).containsEntry("yano.storage.path", DIR.resolve("data").toAbsolutePath().toString());
    }

    @Test
    void pastTimeTravelModeCarriesTheBackfillInterval() {
        Map<String, String> props = props(YanoRunMode.PAST_TIME_TRAVEL);

        assertThat(props).containsEntry("yano.block-producer.past-time-travel-mode", "true")
                .containsEntry("yano.block-producer.backfill-block-interval-slots", "0");
    }

    @Test
    void catchUpModeForgesNothingOnItsOwnAndBackfillsSparsely() {
        Map<String, String> props = props(YanoRunMode.CATCH_UP);

        assertThat(props).containsEntry("yano.block-producer.block-time-millis", String.valueOf(Integer.MAX_VALUE))
                .containsEntry("yano.block-producer.backfill-block-interval-slots", "0")
                .doesNotContainKeys("yano.block-producer.past-time-travel-mode", "yano.client.enabled");
    }

    @Test
    void followModeSyncsFromTheHaskellNodeWithoutProducing() {
        Map<String, String> props = props(YanoRunMode.FOLLOW);

        assertThat(props).containsEntry("yano.client.enabled", "true")
                .containsEntry("yano.remote.host", "127.0.0.1")
                .containsEntry("yano.remote.port", "3001")
                .containsEntry("yano.block-producer.enabled", "false")
                // Yano refuses dev mode without a block producer
                .containsEntry("yano.dev-mode", "false");
    }

    @Test
    void idleModeServesTheChainWithoutAnyProducerOrClient() {
        Map<String, String> props = props(YanoRunMode.IDLE);

        assertThat(props).containsEntry("yano.block-producer.enabled", "false")
                .containsEntry("yano.client.enabled", "false")
                .containsEntry("yano.dev-mode", "false")
                .doesNotContainKeys("yano.block-producer.block-time-millis", "yano.remote.host");
        assertThat(YanoRunMode.IDLE.producesBlocks()).isFalse();
    }

    @Test
    void envNameFollowsQuarkusMapping() {
        assertThat(YanoService.envName("yano.block-producer.block-time-millis"))
                .isEqualTo("YANO_BLOCK_PRODUCER_BLOCK_TIME_MILLIS");
        assertThat(YanoService.envName("quarkus.http.port")).isEqualTo("QUARKUS_HTTP_PORT");
        assertThat(YanoService.envName("yano.dev-mode")).isEqualTo("YANO_DEV_MODE");
    }

    @Test
    void installedVersionComesFromTheDistributionFile(@TempDir Path yanoHome) throws Exception {
        assertThat(YanoService.installedVersion(yanoHome)).isNull();

        Files.writeString(yanoHome.resolve("yano-distribution-v1.json"),
                "{\"schemaVersion\": 1, \"product\": \"yano\", \"version\": \"0.1.0-pre17\"}");
        assertThat(YanoService.installedVersion(yanoHome)).isEqualTo("0.1.0-pre17");

        Files.writeString(yanoHome.resolve("yano-distribution-v1.json"), "not json");
        assertThat(YanoService.installedVersion(yanoHome)).isNull();
    }
}
