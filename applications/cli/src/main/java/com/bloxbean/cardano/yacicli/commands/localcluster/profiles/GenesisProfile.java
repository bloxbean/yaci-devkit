package com.bloxbean.cardano.yacicli.commands.localcluster.profiles;

import com.bloxbean.cardano.yacicli.commands.localcluster.config.GenesisConfig;

public enum GenesisProfile {
    zero_fee;

    public static GenesisConfig applyGenesisProfile(GenesisProfile genesisProfile, GenesisConfig defaultGenesisConfig) {
        if (genesisProfile == zero_fee)
            return zeroFeeGenesisProfile(defaultGenesisConfig);

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
}
