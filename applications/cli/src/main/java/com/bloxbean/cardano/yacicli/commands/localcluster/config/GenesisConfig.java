package com.bloxbean.cardano.yacicli.commands.localcluster.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(ignoreUnknownFields = true)
@Data
public class GenesisConfig {
    private String networkId = "Testnet";
    private long protocolMagic = 42;
    private int maxKESEvolutions = 60;
    private int securityParam = 300;
    private long slotsPerKESPeriod = 129600;
    private int updateQuorum = 1;
    private boolean peerSharing = true;

    //Shelley Genesis
    private String maxLovelaceSupply = "45000000000000000";
    private float poolPledgeInfluence = 0;
    private BigDecimal decentralisationParam = BigDecimal.ZERO;
    private int eMax =18;
    private BigInteger keyDeposit = BigInteger.valueOf(2000000);
    private long maxBlockBodySize = 65536;
    private long maxBlockHeaderSize = 1100;
    private long maxTxSize = 16384;
    private long minFeeA = 44;
    private long minFeeB = 155381;
    private BigInteger minPoolCost = BigInteger.valueOf(340000000);
    private BigInteger minUTxOValue = BigInteger.valueOf(1000000);
    private int nOpt = 100;
    private BigInteger poolDeposit = BigInteger.valueOf(500000000);
    private int protocolMajorVer = 8;
    private int protocolMinorVer = 0;
    private float monetaryExpansionRate = 0.003f;
    private float treasuryGrowthRate = 0.20f;

    private List<Pool> pools = List.of(
            Pool.builder()
                    .poolHash("7301761068762f5900bde9eb7c1c15b09840285130f5b0f53606cc57")
                    .cost(BigInteger.valueOf(340000000))
                    .margin(BigDecimal.ZERO)
                    .rewardAccountHash("11a14edf73b08a0a27cb98b2c57eb37c780df18fcfcf6785ed5df84a")
                    .rewardAccountType("keyHash")
                    .publicKey("7301761068762f5900bde9eb7c1c15b09840285130f5b0f53606cc57")
                    .vrf("c2b62ffa92ad18ffc117ea3abeb161a68885000a466f9c71db5e4731d6630061")
                    .build());
    private List<Delegator> defaultDelegators = List.of(new Delegator("295b987135610616f3c74e11c94d77b6ced5ccc93a7d719cfb135062", "7301761068762f5900bde9eb7c1c15b09840285130f5b0f53606cc57"));

    //Alonzo
    private int collateralPercentage = 150;
    private String prMem = "5.77e-2";
    private String prSteps = "7.21e-5";
    private long lovelacePerUTxOWord = 34482;
    private long maxBlockExUnitsMem = 62000000;
    private long maxBlockExUnitsSteps = 20000000000L;
    private int maxCollateralInputs = 3;
    private long maxTxExUnitsMem = 14000000;
    private long maxTxExUnitsSteps = 10000000000L;
    private int maxValueSize = 5000;

    //conway
    private float pvtcommitteeNormal = 0.51f;
    private float pvtCommitteeNoConfidence = 0.51f;
    private float pvtHardForkInitiation = 0.51f;
    private float pvtMotionNoConfidence = 0.51f;
    private float pvtPPSecurityGroup = 0.51f;

    private float dvtMotionNoConfidence = 0.51f;
    private float dvtCommitteeNormal = 0.51f;
    private float dvtCommitteeNoConfidence = 0.51f;
    private float dvtUpdateToConstitution = 0.51f;
    private float dvtHardForkInitiation = 0.51f;
    private float dvtPPNetworkGroup = 0.51f;
    private float dvtPPEconomicGroup = 0.51f;
    private float dvtPPTechnicalGroup = 0.51f;
    private float dvtPPGovGroup = 0.51f;
    private float dvtTreasuryWithdrawal = 0.51f;

    private int committeeMinSize = 0;
    private int committeeMaxTermLength = 200;
    private int govActionLifetime = 10;
    private BigInteger govActionDeposit = BigInteger.valueOf(1000000000);
    private BigInteger dRepDeposit = BigInteger.valueOf(2000000);
    private int dRepActivity = 20;

    public Map getConfigMap() {
        Map map = new HashMap();
        map.put("networkId", networkId);
        map.put("protocolMagic", protocolMagic);
        map.put("maxKESEvolutions", maxKESEvolutions);
        map.put("securityParam", securityParam);
        map.put("slotsPerKESPeriod", slotsPerKESPeriod);
        map.put("updateQuorum", updateQuorum);
        map.put("peerSharing", peerSharing);
        map.put("maxLovelaceSupply", maxLovelaceSupply);
        map.put("poolPledgeInfluence", poolPledgeInfluence);
        map.put("decentralisationParam", decentralisationParam);
        map.put("eMax", eMax);
        map.put("keyDeposit", keyDeposit);
        map.put("maxBlockBodySize", maxBlockBodySize);
        map.put("maxBlockHeaderSize", maxBlockHeaderSize);
        map.put("maxTxSize", maxTxSize);
        map.put("minFeeA", minFeeA);
        map.put("minFeeB", minFeeB);
        map.put("minPoolCost", minPoolCost);
        map.put("minUTxOValue", minUTxOValue);
        map.put("nOpt", nOpt);
        map.put("poolDeposit", poolDeposit);
        map.put("protocolMajorVer", protocolMajorVer);
        map.put("protocolMinorVer", protocolMinorVer);
        map.put("monetaryExpansionRate", monetaryExpansionRate);
        map.put("treasuryGrowthRate", treasuryGrowthRate);
        map.put("pools", pools);
        map.put("defaultDelegators", defaultDelegators);

        map.put("collateralPercentage", collateralPercentage);
        map.put("prMem", prMem);
        map.put("prSteps", prSteps);
        map.put("lovelacePerUTxOWord", lovelacePerUTxOWord);
        map.put("maxBlockExUnitsMem", maxBlockExUnitsMem);
        map.put("maxBlockExUnitsSteps", maxBlockExUnitsSteps);
        map.put("maxCollateralInputs", maxCollateralInputs);
        map.put("maxTxExUnitsMem", maxTxExUnitsMem);
        map.put("maxTxExUnitsSteps", maxTxExUnitsSteps);
        map.put("maxValueSize", maxValueSize);

        //conway
        map.put("pvtCommitteeNormal", pvtcommitteeNormal);
        map.put("pvtCommitteeNoConfidence", pvtCommitteeNoConfidence);
        map.put("pvtHardForkInitiation", pvtHardForkInitiation);
        map.put("pvtMotionNoConfidence", pvtMotionNoConfidence);
        map.put("pvtPPSecurityGroup", pvtPPSecurityGroup);

        map.put("dvtMotionNoConfidence", dvtMotionNoConfidence);
        map.put("dvtCommitteeNormal", dvtCommitteeNormal);
        map.put("dvtCommitteeNoConfidence", dvtCommitteeNoConfidence);
        map.put("dvtUpdateToConstitution", dvtUpdateToConstitution);
        map.put("dvtHardForkInitiation", dvtHardForkInitiation);
        map.put("dvtPPNetworkGroup", dvtPPNetworkGroup);
        map.put("dvtPPEconomicGroup", dvtPPEconomicGroup);
        map.put("dvtPPTechnicalGroup", dvtPPTechnicalGroup);
        map.put("dvtPPGovGroup", dvtPPGovGroup);
        map.put("dvtTreasuryWithdrawal", dvtTreasuryWithdrawal);

        map.put("committeeMinSize", committeeMinSize);
        map.put("committeeMaxTermLength", committeeMaxTermLength);
        map.put("govActionLifetime", govActionLifetime);
        map.put("govActionDeposit", govActionDeposit);
        map.put("dRepDeposit", dRepDeposit);
        map.put("dRepActivity", dRepActivity);

        return map;
    }

    public GenesisConfig copy() {
        var genesisConfig = new GenesisConfig();
        genesisConfig.setNetworkId(networkId);
        genesisConfig.setProtocolMagic(protocolMagic);
        genesisConfig.setMaxKESEvolutions(maxKESEvolutions);
        genesisConfig.setSecurityParam(securityParam);
        genesisConfig.setSlotsPerKESPeriod(slotsPerKESPeriod);
        genesisConfig.setUpdateQuorum(updateQuorum);
        genesisConfig.setPeerSharing(peerSharing);
        genesisConfig.setMaxLovelaceSupply(maxLovelaceSupply);
        genesisConfig.setPoolPledgeInfluence(poolPledgeInfluence);
        genesisConfig.setDecentralisationParam(decentralisationParam);
        genesisConfig.setEMax(eMax);
        genesisConfig.setKeyDeposit(keyDeposit);
        genesisConfig.setMaxBlockBodySize(maxBlockBodySize);
        genesisConfig.setMaxBlockHeaderSize(maxBlockHeaderSize);
        genesisConfig.setMaxTxSize(maxTxSize);
        genesisConfig.setMinFeeA(minFeeA);
        genesisConfig.setMinFeeB(minFeeB);
        genesisConfig.setMinPoolCost(minPoolCost);
        genesisConfig.setMinUTxOValue(minUTxOValue);
        genesisConfig.setNOpt(nOpt);
        genesisConfig.setPoolDeposit(poolDeposit);
        genesisConfig.setProtocolMajorVer(protocolMajorVer);
        genesisConfig.setProtocolMinorVer(protocolMinorVer);
        genesisConfig.setMonetaryExpansionRate(monetaryExpansionRate);
        genesisConfig.setTreasuryGrowthRate(treasuryGrowthRate);
        genesisConfig.setPools(new ArrayList(pools));
        genesisConfig.setDefaultDelegators(new ArrayList<>(defaultDelegators));

        genesisConfig.setCollateralPercentage(collateralPercentage);
        genesisConfig.setPrMem(prMem);
        genesisConfig.setPrSteps(prSteps);
        genesisConfig.setLovelacePerUTxOWord(lovelacePerUTxOWord);
        genesisConfig.setMaxBlockExUnitsMem(maxBlockExUnitsMem);
        genesisConfig.setMaxBlockExUnitsSteps(maxBlockExUnitsSteps);
        genesisConfig.setMaxCollateralInputs(maxCollateralInputs);
        genesisConfig.setMaxTxExUnitsMem(maxTxExUnitsMem);
        genesisConfig.setMaxTxExUnitsSteps(maxTxExUnitsSteps);
        genesisConfig.setMaxValueSize(maxValueSize);

        genesisConfig.setPvtcommitteeNormal(pvtcommitteeNormal);
        genesisConfig.setPvtCommitteeNoConfidence(pvtCommitteeNoConfidence);
        genesisConfig.setPvtHardForkInitiation(pvtHardForkInitiation);
        genesisConfig.setPvtMotionNoConfidence(pvtMotionNoConfidence);
        genesisConfig.setPvtPPSecurityGroup(pvtPPSecurityGroup);

        genesisConfig.setDvtMotionNoConfidence(dvtMotionNoConfidence);
        genesisConfig.setDvtCommitteeNormal(dvtCommitteeNormal);
        genesisConfig.setDvtCommitteeNoConfidence(dvtCommitteeNoConfidence);
        genesisConfig.setDvtUpdateToConstitution(dvtUpdateToConstitution);
        genesisConfig.setDvtHardForkInitiation(dvtHardForkInitiation);
        genesisConfig.setDvtPPNetworkGroup(dvtPPNetworkGroup);
        genesisConfig.setDvtPPEconomicGroup(dvtPPEconomicGroup);
        genesisConfig.setDvtPPTechnicalGroup(dvtPPTechnicalGroup);
        genesisConfig.setDvtPPGovGroup(dvtPPGovGroup);
        genesisConfig.setDvtTreasuryWithdrawal(dvtTreasuryWithdrawal);

        genesisConfig.setCommitteeMinSize(committeeMinSize);
        genesisConfig.setCommitteeMaxTermLength(committeeMaxTermLength);
        genesisConfig.setGovActionLifetime(govActionLifetime);
        genesisConfig.setGovActionDeposit(govActionDeposit);
        genesisConfig.setDRepDeposit(dRepDeposit);
        genesisConfig.setDRepActivity(dRepActivity);

        return genesisConfig;
    }

}

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class Pool {
    String poolHash;
    BigInteger cost;
    BigDecimal margin;
    String publicKey;
    String rewardAccountHash;
    String rewardAccountType;
    String vrf;
}

record Delegator(String stakeKeyHash, String poolHash) {}
