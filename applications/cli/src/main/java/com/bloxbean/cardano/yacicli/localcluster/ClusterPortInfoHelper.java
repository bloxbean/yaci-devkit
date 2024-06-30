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
    private final ApplicationConfig applicationConfig;

    @Value("${ogmios.enabled:false}")
    private boolean enableOgmios;

    @Value("${is.docker:false}")
    private boolean isDocker;

    public void printUrls(String clusteName, ClusterInfo clusterInfo) {
        writeLn("");
        if (isDocker) {
            writeLn(header(AnsiColors.CYAN_BOLD, "###### Node Details (Container) ######"));
        } else {
            writeLn(successLabel("Admin Port", String.valueOf(applicationConfig.getAdminPort())));
            writeLn(header(AnsiColors.CYAN_BOLD, "###### Node Details ######"));
        }
        writeLn(successLabel("Node port", String.valueOf(clusterInfo.getNodePort())));
        writeLn(successLabel("Node Socket Paths", ""));
        writeLn(clusterInfo.getSocketPath());
        writeLn(successLabel("Submit Api Port", String.valueOf(clusterInfo.getSubmitApiPort())));
        writeLn(successLabel("Protocol Magic", String.valueOf(clusterInfo.getProtocolMagic())));
        writeLn(successLabel("Block Time", String.valueOf(clusterInfo.getBlockTime())) + " sec");
        writeLn(successLabel("Slot Length", String.valueOf(clusterInfo.getSlotLength())) + " sec");
        writeLn(successLabel("Start Time", String.valueOf(clusterInfo.getStartTime())));

        if (clusteName == null || !"default".equals(clusteName))
            return;

        if (isDocker) {
            writeLn("\n");
            writeLn(header(AnsiColors.CYAN_BOLD, "#################### URLS (Host) ####################"));
            writeLn(successLabel("Yaci Viewer", String.format("http://localhost:%s", getViewerPort())));
            writeLn(successLabel("Yaci Store Swagger UI", String.format("http://localhost:%s/swagger-ui.html", getStorePort(clusterInfo))));
            writeLn(successLabel("Yaci Store Api URL", String.format("http://localhost:%s/api/v1/", getStorePort(clusterInfo))));
            writeLn(successLabel("Pool Id", "pool1wvqhvyrgwch4jq9aa84hc8q4kzvyq2z3xr6mpafkqmx9wce39zy"));

            if (enableOgmios) {
                writeLn("\n");
                writeLn(header(AnsiColors.CYAN_BOLD, "#################### Other URLS ####################"));
                writeLn(successLabel("Ogmios Url (Optional)", "ws://localhost:" + getOgmiosPort(clusterInfo)));
                writeLn(successLabel("Kupo Url   (Optional)", "http://localhost:" + getKupoPort(clusterInfo)));
            }

            writeLn("\n");
            writeLn(header(AnsiColors.CYAN_BOLD, "#################### Node Ports ####################"));
            writeLn(successLabel("n2n port", "localhost:" + getN2NPort(clusterInfo)));
            writeLn(successLabel("n2c port (socat)", "localhost:" + getN2cSocatPort(clusterInfo)));
        }
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


    private static int getPort(String propName, int port) {
        String envPort = System.getenv(propName);
        if (envPort != null && !envPort.isEmpty())
            return Integer.parseInt(envPort.trim());
        else
            return port;
    }
}
