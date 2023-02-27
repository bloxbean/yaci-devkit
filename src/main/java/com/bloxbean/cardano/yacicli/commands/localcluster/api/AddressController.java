package com.bloxbean.cardano.yacicli.commands.localcluster.api;

import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yacicli.commands.localcluster.service.AccountService;
import com.bloxbean.cardano.yacicli.common.CommandContext;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

import static com.bloxbean.cardano.yacicli.commands.localcluster.ClusterCommands.CUSTER_NAME;

@RestController
@RequestMapping(path = "/local-cluster/api/addresses")
@RequiredArgsConstructor
public class AddressController {
    private final AccountService accountService;

    @Operation(summary = "Get utxos by address. Page should be always 1. Returns empty for any page greater than 1.")
    @GetMapping(path = "{address}/utxos")
    List<Utxo> getUtxos(@PathVariable("address") String address, @RequestParam(defaultValue = "1") Integer page) {
        if (page > 1)
            return Collections.EMPTY_LIST;

        String clusterName = CommandContext.INSTANCE.getProperty(CUSTER_NAME);
        return accountService.getUtxos(clusterName, address, msg -> {});
    }
}
