package com.bloxbean.cardano.yacicli.localcluster.api;

import com.bloxbean.cardano.yacicli.localcluster.config.ApplicationConfig;
import com.bloxbean.cardano.yacicli.localcluster.ogmios.OgmiosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/local-cluster/api/admin/ogmios")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Ogmios Admin API", description = "API for managing Ogmios and Kupomios processes")
public class OgmiosAdminController {
    private final static String DEFAULT_CLUSTER_NAME = "default";

    private final OgmiosService ogmiosService;
    private final ApplicationConfig appConfig;

    @PostMapping("/start-ogmios")
    @Operation(summary = "Start Ogmios", description = "Start the Ogmios server for the default cluster.")
    public ResponseEntity<String> startOgmios() {
        if (!appConfig.isOgmiosEnabled()) {
            appConfig.setOgmiosEnabled(true);
        }

        var sb = new StringBuilder();
        var status = ogmiosService.start(DEFAULT_CLUSTER_NAME, msg -> {
            log.info(msg);
            sb.append(msg).append("\n");
        });
        if (!status) {
            return ResponseEntity.internalServerError().body("Failed to start Ogmios. " + sb.toString());
        }
        return ResponseEntity.ok("Ogmios started successfully");
    }

    @PostMapping("/start-kupomios")
    @Operation(summary = "Start Kupomios", description = "Start both Ogmios and Kupo servers for the default cluster.")
    public ResponseEntity<String> startKupomios() {
        if (!appConfig.isOgmiosEnabled()) {
            appConfig.setOgmiosEnabled(true);
        }
        if (!appConfig.isKupoEnabled()) {
            appConfig.setKupoEnabled(true);
        }

        var sb = new StringBuilder();
        var status = ogmiosService.start(DEFAULT_CLUSTER_NAME, msg -> {
            log.info(msg);
            sb.append(msg).append("\n");
        });
        if (!status) {
            return ResponseEntity.internalServerError().body("Failed to start Kupomios. " + sb.toString());
        }
        return ResponseEntity.ok("Kupomios started successfully");
    }

    @PostMapping("/stop")
    @Operation(summary = "Stop Ogmios", description = "Stop the Ogmios server for the default cluster.")
    public ResponseEntity<String> stop() {
        var status = ogmiosService.stop(msg -> log.info(msg));
        if (!status) {
            return ResponseEntity.internalServerError().body("Failed to stop Ogmios");
        }
        return ResponseEntity.ok("Ogmios stopped successfully");
    }
}
