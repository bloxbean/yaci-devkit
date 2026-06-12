package com.bloxbean.cardano.yacicli.localcluster;

import com.bloxbean.cardano.yacicli.localcluster.config.ApplicationConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClusterPortInfoHelperTest {

    @Test
    void printsMcpInfoForDefaultClusterWithoutYanoUrlsInCompanionMode() {
        ClusterPortInfoHelper helper = newHelper(10000, false, false, false);
        ClusterInfo clusterInfo = baseClusterInfo()
                .nodeMode(NodeMode.COMPANION)
                .build();

        String output = captureOutput(() -> helper.printUrls("default", clusterInfo));

        assertTrue(output.contains("companion"));
        assertTrue(output.contains("Node port"));
        assertTrue(output.contains("Node Socket Paths"));
        assertTrue(output.contains("Submit Api Port"));
        assertTrue(output.contains("Yano bootstraps the chain, then Haskell node takes over"));
        assertTrue(output.contains("http://localhost:10000/mcp"));
        assertTrue(output.contains("MCP config (.mcp.json)"));
        assertFalse(output.contains("Yano Swagger UI"));
        assertFalse(output.contains("Yano Base API URL"));
        assertFalse(output.contains("Yaci Store Swagger UI"));
        assertFalse(output.contains("Ogmios WebSocket URL"));
        assertFalse(output.contains("Kupo URL"));
    }

    @Test
    void printsYanoOnlyHttpN2nSwaggerAndBaseApiUrl() {
        ClusterPortInfoHelper helper = newHelper(10000, false, false, false);
        ClusterInfo clusterInfo = baseClusterInfo()
                .nodeMode(NodeMode.YANO_ONLY)
                .yanoHttpPort(7070)
                .yanoServerPort(15447)
                .build();

        String output = captureOutput(() -> helper.printUrls("default", clusterInfo));

        assertTrue(output.contains("yano-only"));
        assertTrue(output.contains("Yano HTTP Port"));
        assertTrue(output.contains("localhost:7070"));
        assertTrue(output.contains("Yano n2n Port"));
        assertTrue(output.contains("localhost:15447"));
        assertTrue(output.contains("http://localhost:7070/q/swagger-ui"));
        assertTrue(output.contains("http://localhost:7070/api/v1/"));
        assertTrue(output.contains("BF compatible"));
        assertFalse(output.contains("Node port"));
        assertFalse(output.contains("Node Socket Paths"));
        assertFalse(output.contains("/tmp/node.sock"));
        assertFalse(output.contains("Submit Api Port"));
    }

    @Test
    void doesNotPrintHaskellNodePortsInDockerYanoOnlyMode() {
        ClusterPortInfoHelper helper = newHelper(10000, false, false, false, true);
        ClusterInfo clusterInfo = baseClusterInfo()
                .nodeMode(NodeMode.YANO_ONLY)
                .yanoHttpPort(7070)
                .yanoServerPort(15447)
                .build();

        String output = captureOutput(() -> helper.printUrls("default", clusterInfo));

        assertTrue(output.contains("Yano n2n Port"));
        assertFalse(output.contains("Node Ports"));
        assertFalse(output.contains("n2n port"));
        assertFalse(output.contains("n2c port (socat)"));
    }

    @Test
    void doesNotPrintOgmiosOrKupoInYanoOnlyMode() {
        ClusterPortInfoHelper helper = newHelper(10000, false, true, true);
        ClusterInfo clusterInfo = baseClusterInfo()
                .nodeMode(NodeMode.YANO_ONLY)
                .ogmiosPort(1338)
                .kupoPort(1443)
                .build();

        String output = captureOutput(() -> helper.printUrls("default", clusterInfo));

        assertFalse(output.contains("Ogmios WebSocket URL"));
        assertFalse(output.contains("Kupo URL"));
    }

    @Test
    void printsYaciStoreInfoWhenEnabledForDefaultCluster() {
        ClusterPortInfoHelper helper = newHelper(10000, true, false, false);
        ClusterInfo clusterInfo = baseClusterInfo()
                .nodeMode(NodeMode.COMPANION)
                .yaciStorePort(8181)
                .build();

        String output = captureOutput(() -> helper.printUrls("default", clusterInfo));

        assertTrue(output.contains("Yaci Store HTTP Port"));
        assertTrue(output.contains("localhost:8181"));
        assertTrue(output.contains("http://localhost:8181/swagger-ui.html"));
        assertTrue(output.contains("http://localhost:8181/api/v1/"));
        assertTrue(output.contains("BF compatible"));
    }

    @Test
    void doesNotPrintYaciStoreInfoForNonDefaultCluster() {
        ClusterPortInfoHelper helper = newHelper(10000, true, true, true);
        ClusterInfo clusterInfo = baseClusterInfo()
                .nodeMode(NodeMode.COMPANION)
                .yaciStorePort(8181)
                .build();

        String output = captureOutput(() -> helper.printUrls("peer1", clusterInfo));

        assertFalse(output.contains("Yaci Store Swagger UI"));
        assertFalse(output.contains("Yaci Store Base API URL"));
        assertFalse(output.contains("Ogmios WebSocket URL"));
        assertFalse(output.contains("Kupo URL"));
    }

    @Test
    void printsOgmiosInfoWhenEnabledForDefaultCluster() {
        ClusterPortInfoHelper helper = newHelper(10000, false, true, false);
        ClusterInfo clusterInfo = baseClusterInfo()
                .nodeMode(NodeMode.COMPANION)
                .ogmiosPort(1338)
                .build();

        String output = captureOutput(() -> helper.printUrls("default", clusterInfo));

        assertTrue(output.contains("Ogmios Port"));
        assertTrue(output.contains("localhost:1338"));
        assertTrue(output.contains("ws://localhost:1338"));
        assertFalse(output.contains("Kupo URL"));
    }

    @Test
    void printsKupoInfoWhenEnabledForDefaultCluster() {
        ClusterPortInfoHelper helper = newHelper(10000, false, false, true);
        ClusterInfo clusterInfo = baseClusterInfo()
                .nodeMode(NodeMode.COMPANION)
                .kupoPort(1443)
                .build();

        String output = captureOutput(() -> helper.printUrls("default", clusterInfo));

        assertTrue(output.contains("Kupo HTTP Port"));
        assertTrue(output.contains("localhost:1443"));
        assertTrue(output.contains("http://localhost:1443"));
        assertFalse(output.contains("Ogmios WebSocket URL"));
    }

    private static ClusterInfo.ClusterInfoBuilder baseClusterInfo() {
        return ClusterInfo.builder()
                .nodePort(3001)
                .submitApiPort(8090)
                .socketPath("/tmp/node.sock")
                .protocolMagic(42)
                .slotLength(1)
                .blockTime(1)
                .epochLength(600)
                .securityParam(300)
                .slotsPerKESPeriod(129600);
    }

    private static ClusterPortInfoHelper newHelper(int adminPort,
                                                   boolean yaciStoreEnabled,
                                                   boolean ogmiosEnabled,
                                                   boolean kupoEnabled) {
        return newHelper(adminPort, yaciStoreEnabled, ogmiosEnabled, kupoEnabled, false);
    }

    private static ClusterPortInfoHelper newHelper(int adminPort,
                                                   boolean yaciStoreEnabled,
                                                   boolean ogmiosEnabled,
                                                   boolean kupoEnabled,
                                                   boolean isDocker) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("yaci.store.enabled", String.valueOf(yaciStoreEnabled))
                .withProperty("ogmios.enabled", String.valueOf(ogmiosEnabled))
                .withProperty("kupo.enabled", String.valueOf(kupoEnabled));
        ApplicationConfig applicationConfig = new ApplicationConfig(environment);
        ReflectionTestUtils.setField(applicationConfig, "adminPort", adminPort);
        ClusterPortInfoHelper helper = new ClusterPortInfoHelper(applicationConfig);
        ReflectionTestUtils.setField(helper, "isDocker", isDocker);
        return helper;
    }

    private static String captureOutput(Runnable action) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            action.run();
        } finally {
            System.setOut(originalOut);
        }

        return out.toString(StandardCharsets.UTF_8);
    }
}
