package com.bloxbean.cardano.yacicli.localcluster.api;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yacicli.localcluster.api.model.TopupRequest;
import com.bloxbean.cardano.yacicli.localcluster.api.model.TopupResult;
import com.bloxbean.cardano.yacicli.localcluster.service.AccountService;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

import static com.bloxbean.cardano.yacicli.localcluster.ClusterConfig.CLUSTER_NAME;

@RestController
@RequestMapping(path = "/local-cluster/api/addresses")
@RequiredArgsConstructor
@Slf4j
public class AddressController {
    private final AccountService accountService;

    @Operation(summary = "Get utxos by address. Page should be always 1. Returns empty for any page greater than 1.")
    @GetMapping(path = "{address}/utxos")
    List<Utxo> getUtxos(@PathVariable("address") String address, @RequestParam(defaultValue = "1") Integer page) {
        if (page > 1)
            return Collections.EMPTY_LIST;

        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
        Era era = CommandContext.INSTANCE.getEra();
        return accountService.getUtxos(clusterName, era, address, msg -> {});
    }

    @Operation(summary = "Topup address with ada")
    @PostMapping(path = "topup")
    ResponseEntity<TopupResult> topup(@RequestBody TopupRequest topup) {
        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
        Era era = CommandContext.INSTANCE.getEra();
        boolean status = accountService.topup(clusterName, era, topup.getAddress(), topup.getAdaAmount(), msg -> {
        });

        if (status)
            return ResponseEntity.ok(TopupResult.builder()
                    .address(topup.getAddress())
                    .adaAmount(topup.getAdaAmount())
                    .status(true)
                    .message("Topup successful").build());
        else
            return ResponseEntity.internalServerError()
                    .body(TopupResult.builder()
                            .address(topup.getAddress())
                            .adaAmount(topup.getAdaAmount())
                            .status(false)
                            .message("Topup failed").build());
    }
}
