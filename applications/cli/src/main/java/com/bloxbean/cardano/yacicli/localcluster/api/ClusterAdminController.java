package com.bloxbean.cardano.yacicli.localcluster.api;

import com.bloxbean.cardano.yacicli.localcluster.ClusterCommands;
import com.bloxbean.cardano.yacicli.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.localcluster.config.CustomGenesisConfig;
import com.bloxbean.cardano.yacicli.localcluster.service.ClusterUtilService;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import io.swagger.v3.oas.annotations.Operation;
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
        return clusterService.isFirstRunt(DEFAULT_CLUSTER_NAME)? "not_initialized" : "initialized";
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
    public boolean create(@RequestBody Map<String, String> requestData) {
        CommandContext.INSTANCE.setProperty("cluster_name", DEFAULT_CLUSTER_NAME);

        int epochLength = 0;
        try {
            epochLength = Integer.parseInt(requestData.get("epochLength"));
        } catch (Exception e) {
        }

        if (epochLength == 0)
            epochLength = 600;

        //Check if constitution script is there and it's value is empty
        var constitutionScript = requestData.get("constitutionScript");
        if (constitutionScript == null || constitutionScript.isEmpty())
            requestData.remove(constitutionScript);

        customGenesisConfig.populate(requestData);

        try {
            clusterCommands.createCluster(DEFAULT_CLUSTER_NAME, 3001, 8090, 1, 1, epochLength, true, true, "conway", null, false);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
