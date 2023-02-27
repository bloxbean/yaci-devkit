package com.bloxbean.cardano.yacicli.commands.localcluster.common;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.common.OrderEnum;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.UtxoByAddressQuery;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.UtxoByAddressQueryResult;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;

import java.util.Collections;
import java.util.List;

public class LocalUtxoSupplier implements UtxoSupplier {
    private LocalStateQueryClient localStateQueryClient;

    public LocalUtxoSupplier(LocalStateQueryClient localStateQueryClient) {
        this.localStateQueryClient = localStateQueryClient;
    }

    @Override
    public List<Utxo> getPage(String address, Integer nrOfItems, Integer page, OrderEnum order) {
        if (page != null)
            page = page + 1;
        else
            page = 1;

        if (page != 1)
            return Collections.EMPTY_LIST;

        UtxoByAddressQueryResult queryResult = (UtxoByAddressQueryResult) localStateQueryClient
                .executeQuery(new UtxoByAddressQuery(new Address(address))).block();

        List<Utxo> utxos = queryResult.getUtxoList();

        return utxos;
    }

}
