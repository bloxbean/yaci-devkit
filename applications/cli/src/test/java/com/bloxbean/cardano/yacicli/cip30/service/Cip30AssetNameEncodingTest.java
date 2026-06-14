package com.bloxbean.cardano.yacicli.cip30.service;

import co.nstant.in.cbor.model.Array;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.MultiAsset;
import com.bloxbean.cardano.client.transaction.spec.TransactionInput;
import com.bloxbean.cardano.client.transaction.spec.TransactionOutput;
import com.bloxbean.cardano.client.transaction.spec.Value;
import com.bloxbean.cardano.client.util.HexUtil;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Cip30AssetNameEncodingTest {
    private static final String POLICY_ID = "11111111111111111111111111111111111111111111111111111111";
    private static final String ASSET_NAME = "xNFT";
    private static final String ASSET_NAME_HEX = "784e4654";

    @Test
    void cip30UtxoDeserializationUsesHexAssetNameInAmountUnit() throws Exception {
        String cborHex = cip30UtxoCborHex();

        TransferService transferService = new TransferService(null, null);
        List<Utxo> utxos = ReflectionTestUtils.invokeMethod(transferService, "deserializeCip30Utxos", List.of(cborHex));

        Amount tokenAmount = utxos.getFirst().getAmount().stream()
                .filter(amount -> !"lovelace".equals(amount.getUnit()))
                .findFirst()
                .orElseThrow();

        assertEquals(POLICY_ID + ASSET_NAME_HEX, tokenAmount.getUnit());
        assertEquals(BigInteger.ONE, tokenAmount.getQuantity());
    }

    @Test
    void cip30ValueBuilderTreatsBackendAssetUnitAsHexAssetName() {
        Cip30Service cip30Service = new Cip30Service(null, null);
        List<Amount> amounts = List.of(
                new Amount("lovelace", BigInteger.valueOf(5_000_000)),
                new Amount(POLICY_ID + ASSET_NAME_HEX, BigInteger.ONE)
        );

        Value value = ReflectionTestUtils.invokeMethod(cip30Service, "buildValueFromAmounts", amounts);

        Asset asset = value.getMultiAssets().getFirst().getAssets().getFirst();
        assertEquals("0x" + ASSET_NAME_HEX, asset.getNameAsHex());
        assertEquals(ASSET_NAME, new String(asset.getNameAsBytes(), StandardCharsets.UTF_8));
    }

    private String cip30UtxoCborHex() throws Exception {
        TransactionInput input = TransactionInput.builder()
                .transactionId("2222222222222222222222222222222222222222222222222222222222222222")
                .index(0)
                .build();

        Value value = Value.builder()
                .coin(BigInteger.valueOf(5_000_000))
                .multiAssets(List.of(MultiAsset.builder()
                        .policyId(POLICY_ID)
                        .assets(List.of(new Asset(ASSET_NAME, BigInteger.ONE)))
                        .build()))
                .build();

        TransactionOutput output = TransactionOutput.builder()
                .address("addr_test1qzx9hu8j4ah3auytk0mwcupd69hpc52t0cw39a65ndrah86djs784u92a3m5w475w3w35tyd6v3qumkze80j8a6h5tuqq5xe8y")
                .value(value)
                .build();

        Array utxoArray = new Array();
        utxoArray.add(input.serialize());
        utxoArray.add(output.serialize());

        byte[] cborBytes = CborSerializationUtil.serialize(utxoArray);
        return HexUtil.encodeHexString(cborBytes);
    }
}
