package com.bloxbean.cardano.yacicli.commands.localcluster.api;

import com.bloxbean.cardano.client.backend.model.TransactionContent;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yacicli.commands.localcluster.service.ClusterUtilService;
import com.bloxbean.cardano.yacicli.common.Tuple;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RequiredArgsConstructor
@RestController
@RequestMapping(path = "/local-cluster/api")
public class TransactionController {
    private final ClusterUtilService clusterUtilService;

    private RestTemplate restTemplate = new RestTemplate();
    private final String SUBMIT_API_URL = "http://localhost:8090/api/submit/tx";


    @Operation(summary = "Submit Transaction")
    @PostMapping(path = "tx/submit", consumes = "application/cbor")
    ResponseEntity<String> submit(@RequestBody byte[] cborTx) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/cbor");

        HttpEntity<byte[]> entity = new HttpEntity<>(cborTx, headers);
        try {
            ResponseEntity<String> responseEntity = restTemplate
                    .exchange(SUBMIT_API_URL, HttpMethod.POST, entity, String.class);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                clusterUtilService.waitForNextBlocks(1, msg -> {
                });
            }

            return responseEntity;
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @Operation(summary = "This is a dummy endpoint to simulate Blockfrost's txs/{hash} endpoint. It just waits for 1 block and returns")
    @GetMapping(path = "txs/{hash}")
    public ResponseEntity<TransactionContent> getTransaction(@PathVariable String hash) {
        Tuple<Long, Point> tip = clusterUtilService.getTip(msg -> {
        });
        boolean success = clusterUtilService.waitForNextBlocks(1, msg -> {
        });

        if (success) {
            TransactionContent transactionContent = TransactionContent.builder()
                    .block(String.valueOf(tip._1))
                    .build();

            return ResponseEntity.ok(transactionContent);
        } else
            return ResponseEntity.badRequest().body(null);
    }
}
