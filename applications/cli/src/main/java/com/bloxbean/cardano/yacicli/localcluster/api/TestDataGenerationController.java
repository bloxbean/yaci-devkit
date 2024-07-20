package com.bloxbean.cardano.yacicli.localcluster.api;

import com.bloxbean.cardano.yacicli.localcluster.api.service.TestTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/local-cluster/api/test-data")
@RequiredArgsConstructor
@ConditionalOnBean(TestTransactionService.class)
@Slf4j
public class TestDataGenerationController {
    private final TestTransactionService testTransactionService;

    @Operation(summary = "Generate test data")
    @PostMapping(consumes = "application/json")
    ResponseEntity generate(@RequestBody TestTypeRequest testTypeRequest) {
        switch (testTypeRequest.type) {
            case REFERENCE_INPUT_GEN_TRANSACTION:
                var scriptHashOptional = testTransactionService.generateReferenceInputTransaction(1);
                return scriptHashOptional.map(s -> ResponseEntity.ok(s))
                        .orElseGet(() -> ResponseEntity.badRequest().body("Reference input transaction generation failed"));
            case DATUM_OUTPUT_GEN_TRANSACTION:
                var datumHashOptional = testTransactionService.generateOutputWithDatum(1);
                return datumHashOptional.map(s -> ResponseEntity.ok(s))
                        .orElseGet(() -> ResponseEntity.badRequest().body("Utxo with datum transaction generation failed"));
            default:
                return ResponseEntity.badRequest().body("Invalid test type");
        }
    }

    record TestTypeRequest(TestType type) {
    }

    enum TestType {
        REFERENCE_INPUT_GEN_TRANSACTION,
        DATUM_OUTPUT_GEN_TRANSACTION
    }
}
