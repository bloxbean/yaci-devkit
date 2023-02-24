package com.bloxbean.cardano.yacicli.commands.localcluster.api;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.core.model.Era;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.UtxoByAddressQuery;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.UtxoByAddressQueryResult;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping(path = "/local-cluster/api/addresses")
public class AddressService {
    private LocalQueryClientUtil localQueryClientUtil;

    public AddressService(LocalQueryClientUtil localQueryClientUtil) {
        this.localQueryClientUtil = localQueryClientUtil;
    }

    @Operation(summary = "Get utxos by address")
    @GetMapping(path = "{address}/utxos")
    Mono<List<Utxo>> getUtxos(@PathVariable("address") String address) {

        try {
            LocalClientProvider localClientProvider = localQueryClientUtil.getLocalQueryClient();
            LocalStateQueryClient localStateQueryClient = localClientProvider.getLocalStateQueryClient();
            localClientProvider.start();
            UtxoByAddressQuery utxoByAddressQuery = new UtxoByAddressQuery(Era.Alonzo, new Address(address));
            Mono<UtxoByAddressQueryResult> mono = localStateQueryClient.executeQuery(utxoByAddressQuery);

            return mono.map(utxoByAddressQueryResult -> utxoByAddressQueryResult.getUtxoList()).doOnTerminate(() -> localClientProvider.shutdown());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
