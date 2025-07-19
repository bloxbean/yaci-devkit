package com.bloxbean.cardano.yacicli.localcluster.api;

import com.bloxbean.cardano.yacicli.localcluster.ClusterCommands;
import com.bloxbean.cardano.yacicli.localcluster.ClusterConfig;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.config.ApplicationConfig;
import com.bloxbean.cardano.yacicli.localcluster.config.CustomGenesisConfig;
import com.bloxbean.cardano.yacicli.localcluster.service.ClusterUtilService;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import com.bloxbean.cardano.yacicli.util.PortUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yacicli.common.Tuple;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

@RestController
@RequestMapping(path = "/local-cluster/api/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Devnet Admin API", description = "Controller for managing local Cardano devnet")
public class ClusterAdminController {
    private final static String DEFAULT_CLUSTER_NAME = "default";

    private final ClusterService clusterService;
    private final ClusterUtilService clusterUtilService;
    private final ClusterCommands clusterCommands;
    private final CustomGenesisConfig customGenesisConfig;
    private final ApplicationConfig applicationConfig;

    @Operation(summary = "Download all devnet files as a zipped archive")
    @GetMapping("/devnet/download")
    public ResponseEntity<InputStreamResource> downloadFiles() throws IOException {
        Path clusterPath = clusterService.getClusterFolder(DEFAULT_CLUSTER_NAME);
        // Specify the path to the folder you want to zip
        String folderPath = clusterPath.toAbsolutePath().toString();
        String zipFileName = "devnet.zip";

        // Create a temporary zip file
        File zipFile = File.createTempFile(zipFileName, ".tmp");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // Get all files in the folder and its subdirectories
            Files.walk(Paths.get(folderPath))
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String filePath = file.toAbsolutePath().toString();
                            String entryName = filePath.substring(folderPath.length() + 1); // Remove the folder path from the entry name
                            zipOutputStream.putNextEntry(new ZipEntry(entryName));

                            FileInputStream fileInputStream = new FileInputStream(filePath);
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = fileInputStream.read(buffer)) > 0) {
                                zipOutputStream.write(buffer, 0, bytesRead);
                            }
                            fileInputStream.close();
                            zipOutputStream.closeEntry();
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    });
        }

        // Prepare the response with the zip file
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", zipFileName);
        headers.setContentLength(zipFile.length());

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(new FileInputStream(zipFile)));
    }

    @Operation(summary = "Download genesis files as a zipped archive")
    @GetMapping("/devnet/genesis/download")
    public ResponseEntity<InputStreamResource> downloadGenesisFiles() throws IOException {
        Path clusterPath = clusterService.getClusterFolder(DEFAULT_CLUSTER_NAME);
        Path genesisPath = clusterPath.resolve("node").resolve("genesis");
        // Specify the path to the folder you want to zip
        String folderPath = genesisPath.toAbsolutePath().toString();
        String zipFileName = "genesis.zip";

        // Create a temporary zip file
        File zipFile = File.createTempFile(zipFileName, ".tmp");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // Get all files in the folder and its subdirectories
            Files.walk(Paths.get(folderPath))
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String filePath = file.toAbsolutePath().toString();
                            String entryName = filePath.substring(folderPath.length() + 1); // Remove the folder path from the entry name
                            zipOutputStream.putNextEntry(new ZipEntry(entryName));

                            FileInputStream fileInputStream = new FileInputStream(filePath);
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = fileInputStream.read(buffer)) > 0) {
                                zipOutputStream.write(buffer, 0, bytesRead);
                            }
                            fileInputStream.close();
                            zipOutputStream.closeEntry();
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    });
        }

        // Prepare the response with the zip file
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", zipFileName);
        headers.setContentLength(zipFile.length());

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(new FileInputStream(zipFile)));
    }

    @Operation(summary = "Download a specific genesis file by era")
    @GetMapping("/devnet/genesis/{era}")
    public ResponseEntity<InputStreamResource> getGenesisFile(@PathVariable String era) throws IOException {
        Path clusterPath = clusterService.getClusterFolder(DEFAULT_CLUSTER_NAME);
        Path genesisPath = clusterPath.resolve("node").resolve("genesis");
        Path genesisFile = genesisPath.resolve(era + "-genesis.json");

        if (!genesisFile.toFile().exists()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(new InputStreamResource(FileUtils.openInputStream(genesisFile.toFile())));
    }

    @Operation(summary = "Retrieve devnet information for the default cluster")
    @GetMapping("/devnet")
    public ClusterInfo getClusterInfo() throws IOException {
        return clusterService.getClusterInfo(DEFAULT_CLUSTER_NAME);
    }

    @Operation(summary = "Check if the devnet is initialized or not")
    @GetMapping("/devnet/status")
    public String getClusterStatus() {
        return clusterService.isFirstRunt(DEFAULT_CLUSTER_NAME) ? "not_initialized" : "initialized";
    }

    @Operation(summary = "Reset the local devnet to its initial state")
    @PostMapping("/devnet/reset")
    public String reset() {
        CommandContext.INSTANCE.setProperty("cluster_name", DEFAULT_CLUSTER_NAME);
        clusterCommands.resetLocalCluster();

        return "done";
    }

    @Operation(summary = "Retrieve the current KES period")
    @GetMapping("/devnet/kes-period")
    public int getKesPeriod() throws IOException {
        return clusterUtilService.getKESPeriod();
    }

    @Operation(summary = "Retrieve the genesis hash")
    @GetMapping("/devnet/genesis/hash")
    public ResponseEntity<String> getGenesisHash() {
        var genesisHash = clusterService.getGenesisHash(DEFAULT_CLUSTER_NAME);
        if (genesisHash != null)
            return ResponseEntity.ok(genesisHash);
        else
            return ResponseEntity.notFound().build();
    }

    @PostMapping("/devnet/create")
    @Operation(summary = """
            Create and start the devnet by overriding the provided genesis properties. This method accepts a JSON object as input to 
            override the default genesis property values. All properties defined in `node.properties`, except for nested properties, are supported. 
            This is useful for dynamically changing the node configuration without modifying the `node.properties` file.
            A value should always be set as string even if it's a number or boolean. 
            """)
    public boolean create(@RequestBody DevNetCreateRequest request) {
        CommandContext.INSTANCE.setProperty("cluster_name", DEFAULT_CLUSTER_NAME);

        int epochLength = 0;
        try {
            epochLength = Integer.parseInt(request.genesisProperties().get("epochLength"));
        } catch (Exception e) {
        }

        if (epochLength == 0)
            epochLength = 600;

        long blockTime = 1;
        try {
            blockTime = Long.parseLong(request.genesisProperties().get("blockTime"));
        } catch (Exception e) {
        }

        if (blockTime == 0) blockTime = 1;

        double slotLength = 1;
        try {
            slotLength = Double.parseDouble(request.genesisProperties().get("slotLength"));
        } catch (Exception e) {

        }
        if (slotLength == 0) slotLength = 1;

        //Check if constitution script is there and it's value is empty
        var constitutionScript = request.genesisProperties().get("constitutionScript");
        if (constitutionScript == null || constitutionScript.isEmpty())
            request.genesisProperties().remove(constitutionScript);

        customGenesisConfig.populate(request.genesisProperties());

        boolean originalEnableYaciStore = applicationConfig.isYaciStoreEnabled();
        boolean originalEnableOgmios = applicationConfig.isOgmiosEnabled();
        boolean originalEnableKupo = applicationConfig.isKupoEnabled();

        applicationConfig.setYaciStoreEnabled(request.enableYaciStore());
        applicationConfig.setOgmiosEnabled(request.enableOgmios());

        if (request.enableKupomios()) {
            applicationConfig.setOgmiosEnabled(true);
            applicationConfig.setKupoEnabled(true);
        } else {
            applicationConfig.setKupoEnabled(false);
        }

        try {
            clusterCommands.createCluster(DEFAULT_CLUSTER_NAME, 3001, 8090, slotLength, blockTime, epochLength, true,
                    true, "conway", null, false, request.enableMultiNode(), request.multiNodeStakeRatioFactor);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            //Reset the application config to original values
            applicationConfig.setYaciStoreEnabled(originalEnableYaciStore);
            applicationConfig.setOgmiosEnabled(originalEnableOgmios);
            applicationConfig.setKupoEnabled(originalEnableKupo);
        }
    }

    @Operation(summary = "Retrieve the current tip for all nodes (multi-node aware)")
    @GetMapping("/devnet/tip")
    public MultiNodeTipResponse getTip() {
        CommandContext.INSTANCE.setProperty(ClusterConfig.CLUSTER_NAME, DEFAULT_CLUSTER_NAME);
        
        boolean isMultiNodeEnabled = false;
        try {
            var clusterInfo = clusterService.getClusterInfo(DEFAULT_CLUSTER_NAME);
            isMultiNodeEnabled = clusterInfo.isLocalMultiNodeEnabled();
        } catch (Exception e) {
            log.error("Error getting cluster info", e);
        }

        List<NodeTip> tips = new ArrayList<>();
        
        // Get tip for primary node
        Tuple<Long, Point> tip1 = clusterUtilService.getTip(msg -> log.debug(msg));
        if (tip1 != null) {
            tips.add(new NodeTip("node-1", tip1._1, tip1._2.getSlot(), tip1._2.getHash()));
        }

        // Get tips for additional nodes if multi-node is enabled
        if (isMultiNodeEnabled) {
            Tuple<Long, Point> tip2 = clusterUtilService.getTip(msg -> log.debug(msg), "node-2");
            if (tip2 != null) {
                tips.add(new NodeTip("node-2", tip2._1, tip2._2.getSlot(), tip2._2.getHash()));
            }

            Tuple<Long, Point> tip3 = clusterUtilService.getTip(msg -> log.debug(msg), "node-3");
            if (tip3 != null) {
                tips.add(new NodeTip("node-3", tip3._1, tip3._2.getSlot(), tip3._2.getHash()));
            }
        }

        return new MultiNodeTipResponse(isMultiNodeEnabled, tips);
    }

    record DevNetCreateRequest(Map<String, String> genesisProperties,
                               @Schema(description = "Create multiple local block producing nodes", defaultValue = "false")
                               boolean enableMultiNode,
                               @Schema(description = "The stake ratio between the primary node and two peers is only used when multi-node is enabled for rollback testing", defaultValue = "5")
                               int multiNodeStakeRatioFactor,
                               @Schema(description = "Enable Yaci Store", defaultValue = "false")
                               boolean enableYaciStore,
                               @Schema(description = "Enable Ogmios", defaultValue = "false")
                               boolean enableOgmios,
                               @Schema(description = "Enable Ogmios and Kupo", defaultValue = "false")
                               boolean enableKupomios) {
    }

    record NodeTip(
            @Schema(description = "Node name")
            String nodeName,
            @Schema(description = "Current block number")
            long blockNumber,
            @Schema(description = "Current slot number")
            long slot,
            @Schema(description = "Current block hash")
            String blockHash) {
    }

    record MultiNodeTipResponse(
            @Schema(description = "Whether multi-node mode is enabled")
            boolean multiNodeEnabled,
            @Schema(description = "List of tips for all nodes")
            List<NodeTip> tips) {
    }

    @Operation(summary = "Get service status for all services")
    @GetMapping("/services/status")
    public Map<String, ServiceStatus> getServiceStatus() {
        Map<String, ServiceStatus> serviceStatuses = new HashMap<>();
        
        // Check Yaci Store (port 8080)
        boolean yaciStoreRunning = !PortUtil.isPortAvailable(8080);
        serviceStatuses.put("yaciStore", new ServiceStatus(yaciStoreRunning, 8080));
        
        // Check Ogmios (port 1337)
        boolean ogmiosRunning = !PortUtil.isPortAvailable(1337);
        serviceStatuses.put("ogmios", new ServiceStatus(ogmiosRunning, 1337));
        
        // Check Kupo (port 1442)
        boolean kupoRunning = !PortUtil.isPortAvailable(1442);
        serviceStatuses.put("kupo", new ServiceStatus(kupoRunning, 1442));
        
        // Check Submit API (port 8090)
        boolean submitApiRunning = !PortUtil.isPortAvailable(8090);
        serviceStatuses.put("submitApi", new ServiceStatus(submitApiRunning, 8090));
        
        return serviceStatuses;
    }

    record ServiceStatus(
            @Schema(description = "Whether the service is running")
            boolean running,
            @Schema(description = "Port number the service runs on")
            int port) {
    }
}
