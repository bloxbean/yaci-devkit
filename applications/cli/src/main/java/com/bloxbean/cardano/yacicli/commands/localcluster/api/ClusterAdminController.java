package com.bloxbean.cardano.yacicli.commands.localcluster.api;

import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterInfo;
import com.bloxbean.cardano.yacicli.commands.localcluster.ClusterService;
import com.bloxbean.cardano.yacicli.commands.localcluster.service.ClusterUtilService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping(path = "/local-cluster/api/admin")
@RequiredArgsConstructor
@Slf4j
public class ClusterAdminController {
    private final ClusterService clusterService;
    private final ClusterUtilService clusterUtilService;

    @GetMapping("/clusters/{clusterName}/download")
    public ResponseEntity<InputStreamResource> downloadFiles(@PathVariable String clusterName) throws IOException {
        Path clusterPath = clusterService.getClusterFolder(clusterName);
        // Specify the path to the folder you want to zip
        String folderPath = clusterPath.toAbsolutePath().toString();
        String zipFileName = "cluster.zip";

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

    @GetMapping("/clusters/{clusterName}")
    public ClusterInfo getClusterInfo(@PathVariable String clusterName) throws IOException {
        return clusterService.getClusterInfo(clusterName);
    }

    @GetMapping("/clusters/{clusterName}/status")
    public String getClusterStatus(@PathVariable String clusterName) {
        return clusterService.isFirstRunt(clusterName)? "not_initialized" : "initialized";
    }

    @GetMapping("/clusters/{clusterName}/kes-period")
    public int getKesPeriod(@PathVariable String clusterName) throws IOException {
        return clusterUtilService.getKESPeriod();
    }
}
