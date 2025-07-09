package com.bloxbean.cardano.yacicli.localcluster.api;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yacicli.localcluster.api.model.TopupRequest;
import com.bloxbean.cardano.yacicli.localcluster.api.model.TopupResult;
import com.bloxbean.cardano.yacicli.localcluster.service.AccountService;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Address API", description = "API for Address UTXO and Topup operations")
@Slf4j
public class AddressController {
    private final AccountService accountService;

    @Operation(
        summary = "Get UTXOs by address",
        description = "Retrieve the UTXOs for a given address. Page number should always be 1, and results will be empty for any page greater than 1.",
        parameters = {
            @Parameter(name = "address", description = "Cardano address to fetch UTXOs", required = true),
            @Parameter(name = "page", description = "Page number (default is 1)", required = false)
        }
    )
    @GetMapping(path = "{address}/utxos")
    List<Utxo> getUtxos(@PathVariable("address") String address, @RequestParam(defaultValue = "1") Integer page) {
        if (page > 1)
            return Collections.EMPTY_LIST;

        String clusterName = CommandContext.INSTANCE.getProperty(CLUSTER_NAME);
        Era era = CommandContext.INSTANCE.getEra();
        return accountService.getUtxos(clusterName, era, address, msg -> {});
    }

    @Operation(
        summary = "Top up an address with ADA",
        description = "Allows topping up a specified Cardano address with the desired amount of ADA.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "TopupRequest containing the address and ADA amount",
            required = true
        ),
        responses = {
            @ApiResponse(responseCode = "200", description = "Topup successful",
                         content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "500", description = "Topup failed",
                         content = @Content(mediaType = "application/json"))
        }
    )
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
