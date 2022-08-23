package com.bloxbean.cardano.yacicli.util;

import java.math.BigDecimal;
import java.math.BigInteger;

public class AdaConversionUtil {
    private static final int ADA_DECIMAL = 6;

    public AdaConversionUtil() {
    }

    public static BigInteger adaToLovelace(BigDecimal amount) {
        return assetFromDecimal(amount, 6L);
    }

    public static BigDecimal lovelaceToAda(BigInteger amount) {
        return assetToDecimal(amount, 6L);
    }

    public static BigDecimal assetToDecimal(BigInteger amount, long decimals) {
        if (decimals == 0L) {
            return new BigDecimal(amount);
        } else {
            double oneUnit = Math.pow(10.0, (double)decimals);
            BigDecimal bigDecimalAmt = new BigDecimal(amount);
            BigDecimal decimalAmt = bigDecimalAmt.divide(new BigDecimal(oneUnit));
            return decimalAmt;
        }
    }

    public static BigInteger assetFromDecimal(BigDecimal doubleAmout, long decimals) {
        if (decimals == 0L) {
            return doubleAmout.toBigInteger();
        } else {
            double oneUnit = Math.pow(10.0, (double)decimals);
            BigDecimal amount = (new BigDecimal(oneUnit)).multiply(doubleAmout);
            return amount.toBigInteger();
        }
    }

    public static BigInteger adaToLovelace(double amount) {
        return adaToLovelace(BigDecimal.valueOf(amount));
    }
}
