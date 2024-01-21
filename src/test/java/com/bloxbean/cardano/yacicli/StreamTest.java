package com.bloxbean.cardano.yacicli;

import com.bloxbean.cardano.yaci.core.model.Amount;
import com.bloxbean.cardano.yaci.core.model.TransactionBody;
import com.bloxbean.cardano.yaci.core.model.TransactionOutput;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public class StreamTest {

    @Test
    public void flat() {
        long t1 = System.currentTimeMillis();
        TransactionBody txBody1 = TransactionBody.builder()
                .outputs(List.of(
                        TransactionOutput.builder()
                                .address("ab")
                                .amounts(List.of(new Amount("a1", "p1", "n1", "n1".getBytes(), new BigInteger("1")), new Amount("a1", "p1", "n1", "n1".getBytes(), new BigInteger("133"))))
                                .build(),
                        TransactionOutput.builder()
                                .address("cd")
                                .amounts(List.of(new Amount("b1", "pb1", "cn1", "cn1".getBytes(), new BigInteger("2")), new Amount("ca1", "cp1", "cn1", "cn1".getBytes(), new BigInteger("3"))))
                                .build(),
                        TransactionOutput.builder()
                                .address("ab")
                                .amounts(List.of(new Amount("da1", "dp1", "dn1", "dn1".getBytes(), new BigInteger("4")), new Amount("ea1", "ep1", "en1", "en1".getBytes(), new BigInteger("5"))))
                                .build()
                )).build();

        TransactionBody txBody2 = TransactionBody.builder()
                .outputs(List.of(
                        TransactionOutput.builder()
                                .address("ab")
                                .amounts(List.of(new Amount("fa1", "fp1", "fn1", "fn1".getBytes(), new BigInteger("16")), new Amount("ga1", "gp1", "gn1", "gn1".getBytes(), new BigInteger("133"))))
                                .build(),
                        TransactionOutput.builder()
                                .address("cd")
                                .amounts(List.of(new Amount("hb1", "hpb1", "hnb1", "hnb1".getBytes(), new BigInteger("2")), new Amount("kca1", "kcp1", "kcn1", "kcn1".getBytes(), new BigInteger("3"))))
                                .build()
//                        TransactionOutput.builder()
//                                .address("ab")
//                                .amounts(List.of(new Amount("da1", "dp1", "dn1", new BigInteger("4")), new Amount("ea1", "ep1", "en1", new BigInteger("5"))))
//                                .build()
                )).build();

        List<Amount> amounts = List.of(txBody1, txBody2).stream()
                .flatMap(transactionBody ->
                        transactionBody.getOutputs().stream()
                                .flatMap(txOutput -> txOutput.getAmounts().stream()))
                .collect(Collectors.toList());

        amounts.forEach(amount -> {
        });


//        Map assetAmountsMap = amounts.stream()
//                .collect(Collectors.groupingBy(amount -> amount.getAssetName(),
//                        Collectors.summingLong(value -> value.getQuantity().longValue())));

        Map<String, BigInteger> assetAmountsMap = amounts.stream()
                .collect(toMap(
                        amount -> amount.getAssetName(),
                        amount -> amount.getQuantity(),
                        (qty1, qty2) -> qty1.add(qty2)
                ));

        System.out.println(assetAmountsMap);
        long t2 = System.currentTimeMillis();
        System.out.printf("Time >> " + (t2 - t1));
    }
}
