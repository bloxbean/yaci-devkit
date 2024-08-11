package com.bloxbean.cardano.yacicli.localcluster.profiles;

import com.bloxbean.cardano.yacicli.localcluster.config.GenesisConfig;

import java.math.BigInteger;

public enum GenesisProfile {
    zero_fee,
    zero_min_utxo_value,
    zero_fee_and_min_utxo_value;

    public static GenesisConfig applyGenesisProfile(GenesisProfile genesisProfile, GenesisConfig defaultGenesisConfig) {
        if (genesisProfile == zero_fee)
            return zeroFeeGenesisProfile(defaultGenesisConfig);
        else if (genesisProfile == zero_min_utxo_value)
            return zeroMinimumUtxoValue(defaultGenesisConfig);
        else if (genesisProfile == zero_fee_and_min_utxo_value)
            return zeroFeeAndZeroMinimumUtxoValue(defaultGenesisConfig);

        return defaultGenesisConfig;
    }

    private static GenesisConfig zeroFeeGenesisProfile(GenesisConfig genesisConfig) {
        genesisConfig.setMinFeeA(0);
        genesisConfig.setMinFeeB(0);
        genesisConfig.setPrMem("0");
        genesisConfig.setPrSteps("0");
        genesisConfig.setLovelacePerUTxOWord(0);

        return genesisConfig;
    }

    private static GenesisConfig zeroMinimumUtxoValue(GenesisConfig genesisConfig) {
        genesisConfig.setMinUTxOValue(BigInteger.valueOf(0));

        return genesisConfig;
    }

    private static GenesisConfig zeroFeeAndZeroMinimumUtxoValue(GenesisConfig genesisConfig) {
        zeroFeeGenesisProfile(genesisConfig);
        zeroMinimumUtxoValue(genesisConfig);

        return genesisConfig;
    }
}
