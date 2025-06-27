package com.bloxbean.cardano.yacicli.localcluster.api;

import com.bloxbean.cardano.yacicli.localcluster.config.ApplicationConfig;
import com.bloxbean.cardano.yacicli.localcluster.yacistore.YaciStoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/local-cluster/api/admin/yaci-store")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Yaci Store Admin API", description = "Endpoints for managing the administration of the Yaci Store")
public class YaciStoreAdminController {
    private final static String DEFAULT_CLUSTER_NAME = "default";

    private final YaciStoreService yaciStoreService;
    private final ApplicationConfig appConfig;

    @PostMapping("/start")
    @Operation(summary = "Start Yaci Store", description = "Starts the Yaci Store process for the default cluster.")
    public ResponseEntity<String> startYaciStore() {
        if (!appConfig.isYaciStoreEnabled()) {
            appConfig.setYaciStoreEnabled(true);
        }

        var sb = new StringBuilder();
        var status = yaciStoreService.start(DEFAULT_CLUSTER_NAME, msg -> {
            log.info(msg);
            sb.append(msg).append("\n");
        });
        if (!status) {
            return ResponseEntity.internalServerError().body("Failed to start Yaci Store. " + sb.toString());
        }
        return ResponseEntity.ok("Yaci Store started successfully");
    }

    @PostMapping("/stop")
    @Operation(summary = "Stop Yaci Store", description = "Stops the running Yaci Store process.")
    public ResponseEntity<String> stopYaciStore() {
        var status = yaciStoreService.stop();
        if (!status) {
            return ResponseEntity.internalServerError().body("Failed to stop Yaci Store");
        }
        return ResponseEntity.ok("Yaci Store stopped successfully");
    }

    @PostMapping("/resync")
    @Operation(summary = "Resync Yaci Store", description = "Stops the Yaci Store process and synchronizes it from the beginning.")
    public ResponseEntity<String> resyncYaciStore() {
        var status = yaciStoreService.stopAndSyncFromBeginning(DEFAULT_CLUSTER_NAME, msg -> log.info(msg));
        if (!status) {
            return ResponseEntity.internalServerError().body("Failed to resync Yaci Store");
        }
        return ResponseEntity.ok("Yaci Store resynchronized successfully");
    }
}
