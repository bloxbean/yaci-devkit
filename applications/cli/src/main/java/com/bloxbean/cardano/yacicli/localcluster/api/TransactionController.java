package com.bloxbean.cardano.yacicli.localcluster.api;

import com.bloxbean.cardano.client.backend.model.TransactionContent;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yacicli.localcluster.service.ClusterUtilService;
import com.bloxbean.cardano.yacicli.common.Tuple;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Transaction API", description = "Handles submission of transactions and simulate transaction lookups.")
public class TransactionController {
    private final ClusterUtilService clusterUtilService;

    private RestTemplate restTemplate = new RestTemplate();
    private final String SUBMIT_API_URL = "http://localhost:8090/api/submit/tx";


    @Operation(summary = "Submit Transaction", description = "Submit a transaction in CBOR format to the cluster.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Transaction in CBOR format"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction successfully submitted"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad request, invalid transaction CBOR format")
            })
    @PostMapping(path = "tx/submit", consumes = "application/cbor")
    ResponseEntity<String> submit(@RequestBody byte[] cborTx) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/cbor");

        HttpEntity<byte[]> entity = new HttpEntity<>(cborTx, headers);
        try {
            ResponseEntity<String> responseEntity = restTemplate
                    .exchange(SUBMIT_API_URL, HttpMethod.POST, entity, String.class);

            return responseEntity;
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(e.getMessage());
        }
    }

    @Operation(summary = "Get Transaction by Hash", description = "Simulates fetching a transaction by hash. Waits for one block and returns transaction details.",
            parameters = @io.swagger.v3.oas.annotations.Parameter(name = "hash", description = "Transaction hash to search for"),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Transaction content found"),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Unable to fetch transaction details")
            })
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
