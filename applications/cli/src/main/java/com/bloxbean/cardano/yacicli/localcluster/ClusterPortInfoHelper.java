package com.bloxbean.cardano.yacicli.localcluster;

import com.bloxbean.cardano.yacicli.localcluster.config.ApplicationConfig;
import com.bloxbean.cardano.yacicli.common.AnsiColors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

@Component
@RequiredArgsConstructor
public class ClusterPortInfoHelper {
    private static final String DEFAULT_CLUSTER_NAME = "default";

    private final ApplicationConfig applicationConfig;

    @Value("${is.docker:false}")
    private boolean isDocker;

    public void printUrls(String clusterName, ClusterInfo clusterInfo) {
        NodeMode nodeMode = clusterInfo.getNodeMode() != null ? clusterInfo.getNodeMode() : NodeMode.HASKELL_ONLY;
        boolean hasHaskellNode = hasHaskellNode(nodeMode);

        writeLn("");
        if (isDocker) {
            writeLn(header(AnsiColors.CYAN_BOLD, "###### Node Details (Container) ######"));
        } else {
            writeLn(infoLabel("Admin Port", String.valueOf(getAdminPort())));
            writeLn(header(AnsiColors.CYAN_BOLD, "###### Node Details ######"));
        }
        if (hasHaskellNode) {
            writeLn(infoLabel("Node port", String.valueOf(clusterInfo.getNodePort())));
            writeLn(infoLabel("Node Socket Paths", ""));
            writeLn(clusterInfo.getSocketPath());
            writeLn(infoLabel("Submit Api Port", String.valueOf(clusterInfo.getSubmitApiPort())));
        }
        writeLn(infoLabel("Protocol Magic", String.valueOf(clusterInfo.getProtocolMagic())));
        writeLn(infoLabel("Block Time", String.valueOf(clusterInfo.getBlockTime())) + " sec");
        writeLn(infoLabel("Slot Length", String.valueOf(clusterInfo.getSlotLength())) + " sec");
        writeLn(infoLabel("Start Time", String.valueOf(clusterInfo.getStartTime())));
        writeLn(infoLabel("Epoch Length", String.valueOf(clusterInfo.getEpochLength())));
        writeLn(infoLabel("Security Param", String.valueOf(clusterInfo.getSecurityParam())));
        writeLn(infoLabel("SlotsPerKESPeriod", String.valueOf(clusterInfo.getSlotsPerKESPeriod())));
        writeLn(infoLabel("Node Mode", nodeMode.getValue()));
        writeLn(infoLabel("Node Mode Detail", getNodeModeDescription(nodeMode)));

        if (NodeMode.YANO_ONLY == nodeMode) {
            printYanoOnlyInfo(clusterInfo);
        }

        if (clusterName == null || !DEFAULT_CLUSTER_NAME.equals(clusterName))
            return;

        if (isDocker) {
            writeLn("\n");
            writeLn(header(AnsiColors.CYAN_BOLD, "#################### URLS (Host) ####################"));
            writeLn(infoLabel("Yaci Viewer", String.format("http://localhost:%s", getViewerPort())));
            writeLn(infoLabel("Pool Id", "pool1wvqhvyrgwch4jq9aa84hc8q4kzvyq2z3xr6mpafkqmx9wce39zy"));

            if (hasHaskellNode) {
                writeLn("\n");
                writeLn(header(AnsiColors.CYAN_BOLD, "#################### Node Ports ####################"));
                writeLn(infoLabel("n2n port", "localhost:" + getN2NPort(clusterInfo)));
                writeLn(infoLabel("n2c port (socat)", "localhost:" + getN2cSocatPort(clusterInfo)));
            }
        }

        if (applicationConfig.isYaciStoreEnabled()) {
            printYaciStoreInfo(clusterInfo);
        }

        if (hasHaskellNode && applicationConfig.isOgmiosEnabled()) {
            printOgmiosInfo(clusterInfo);
        }

        if (hasHaskellNode && applicationConfig.isKupoEnabled()) {
            printKupoInfo(clusterInfo);
        }

        printWalletSdkInfo();
        printMcpInfo();
    }

    private boolean hasHaskellNode(NodeMode nodeMode) {
        return NodeMode.YANO_ONLY != nodeMode;
    }

    private void printYaciStoreInfo(ClusterInfo clusterInfo) {
        int storePort = getStorePort(clusterInfo);
        writeLn("\n");
        writeLn(header(AnsiColors.CYAN_BOLD, "#################### Yaci Store ####################"));
        writeLn(infoLabel("Yaci Store HTTP Port", "localhost:" + storePort));
        writeLn(infoLabel("Yaci Store Swagger UI", String.format("http://localhost:%s/swagger-ui.html", storePort)));
        writeLn(infoLabel("Yaci Store Base API URL (BF compatible)", String.format("http://localhost:%s/api/v1/", storePort)));
    }

    private void printOgmiosInfo(ClusterInfo clusterInfo) {
        int ogmiosPort = getOgmiosPort(clusterInfo);
        writeLn("\n");
        writeLn(header(AnsiColors.CYAN_BOLD, "#################### Ogmios ####################"));
        writeLn(infoLabel("Ogmios Port", "localhost:" + ogmiosPort));
        writeLn(infoLabel("Ogmios WebSocket URL", "ws://localhost:" + ogmiosPort));
    }

    private void printKupoInfo(ClusterInfo clusterInfo) {
        int kupoPort = getKupoPort(clusterInfo);
        writeLn("\n");
        writeLn(header(AnsiColors.CYAN_BOLD, "#################### Kupo ####################"));
        writeLn(infoLabel("Kupo HTTP Port", "localhost:" + kupoPort));
        writeLn(infoLabel("Kupo URL", "http://localhost:" + kupoPort));
    }

    private void printYanoOnlyInfo(ClusterInfo clusterInfo) {
        int yanoHttpPort = getYanoHttpPort(clusterInfo);
        writeLn("\n");
        writeLn(header(AnsiColors.CYAN_BOLD, "#################### Yano (Yano Only Mode) ####################"));
        writeLn(infoLabel("Yano HTTP Port", "localhost:" + yanoHttpPort));
        writeLn(infoLabel("Yano n2n Port", "localhost:" + getYanoN2NPort(clusterInfo)));
        writeLn(infoLabel("Yano Swagger UI", String.format("http://localhost:%s/q/swagger-ui", yanoHttpPort)));
        writeLn(infoLabel("Yano Base API URL (BF compatible)", String.format("http://localhost:%s/api/v1/", yanoHttpPort)));
    }

    private void printWalletSdkInfo() {
        int adminPort = getAdminPort();
        writeLn("\n");
        writeLn(header(AnsiColors.CYAN_BOLD, "#################### Wallet SDK / CIP-30 ####################"));
        writeLn(infoLabel("Wallet Demo", String.format("http://localhost:%s/wallet", adminPort)));
        writeLn(infoLabel("Wallet SDK JS", String.format("http://localhost:%s/wallet-sdk.js", adminPort)));
        writeLn(infoLabel("Injected Wallet", "window.cardano.yacidevkit"));
    }

    private void printMcpInfo() {
        int adminPort = getAdminPort();
        writeLn("\n");
        writeLn(header(AnsiColors.CYAN_BOLD, "#################### MCP Server ####################"));
        writeLn(infoLabel("MCP URL", String.format("http://localhost:%s/mcp", adminPort)));
        writeLn(infoLabel("MCP Tools", "devnet_status, devnet_reset, devnet_topup, devnet_utxos, devnet_submit_tx"));
        writeLn(infoLabel("MCP config (.mcp.json)", ""));
        writeLn("{");
        writeLn("  \"mcpServers\": {");
        writeLn("    \"yaci-devkit\": {");
        writeLn("      \"url\": \"http://localhost:" + adminPort + "/mcp\"");
        writeLn("    }");
        writeLn("  }");
        writeLn("}");
    }

    private String getNodeModeDescription(NodeMode nodeMode) {
        return switch (nodeMode) {
            case COMPANION -> "Yano bootstraps the chain, then Haskell node takes over";
            case YANO_ONLY -> "Yano runs as the only node";
            case YANO_PRIMARY -> "Yano runs as block producer with Haskell relay";
            case HASKELL_ONLY -> "Haskell node only";
        };
    }

    public int getStorePort(ClusterInfo clusterInfo) {
        if (isDocker)
            return getPort("HOST_STORE_API_PORT", 8080);
        else
            return clusterInfo.getYaciStorePort();
    }

    public int getSubmitApiPort(ClusterInfo clusterInfo) {
        if (isDocker)
            return getPort("HOST_SUBMIT_API_PORT", 8090);
        else
            return clusterInfo.getSubmitApiPort();
    }

    public int getViewerPort() {
        if (isDocker)
            return getPort("HOST_VIEWER_PORT", 5173);
        else
            return 0;
    }

    public int getN2NPort(ClusterInfo clusterInfo) {
        if (isDocker)
            return getPort("HOST_N2N_PORT", 3001);
        else
            return clusterInfo.getNodePort();
    }

    public int getN2cSocatPort(ClusterInfo clusterInfo) {
        if (isDocker)
            return getPort("HOST_N2C_SOCAT_PORT", 3333);
        else
            return clusterInfo.getSocatPort();
    }

    public int getOgmiosPort(ClusterInfo clusterInfo) {
        if (isDocker)
            return getPort("HOST_OGMIOS_PORT", 1337);
        else
            return clusterInfo.getOgmiosPort();
    }

    public int getKupoPort(ClusterInfo clusterInfo) {
        if (isDocker)
            return getPort("HOST_KUPO_PORT", 1442);
        else
            return clusterInfo.getKupoPort();
    }

    public int getYanoHttpPort(ClusterInfo clusterInfo) {
        if (isDocker)
            return getPort("HOST_YANO_HTTP_PORT", 6060);
        else
            return clusterInfo.getYanoHttpPort();
    }

    public int getYanoN2NPort(ClusterInfo clusterInfo) {
        if (isDocker)
            return getPort("HOST_YANO_N2N_PORT", 14447);
        else
            return clusterInfo.getYanoServerPort();
    }

    public int getAdminPort() {
        if (isDocker)
            return getPort("HOST_CLUSTER_API_PORT", 10000);
        else
            return applicationConfig.getAdminPort();
    }

    private static int getPort(String propName, int port) {
        String envPort = System.getenv(propName);
        if (envPort != null && !envPort.isEmpty())
            return Integer.parseInt(envPort.trim());
        else
            return port;
    }
}
