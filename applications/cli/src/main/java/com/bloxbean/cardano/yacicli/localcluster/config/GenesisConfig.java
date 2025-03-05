package com.bloxbean.cardano.yacicli.localcluster.config;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.util.HexUtil;
import jakarta.annotation.PostConstruct;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@ConfigurationProperties(ignoreUnknownFields = true)
@Data
public class GenesisConfig {
    private String networkId = "Testnet";
    private long protocolMagic = 42;
    private int maxKESEvolutions = 60;
    private double stabilityWindowFactor = 0.5; //This is used to automatically derive the security parameter from epoch length
    private int securityParam = 0;
    private long slotsPerKESPeriod = 129600;
    private int updateQuorum = 1;
    private boolean peerSharing = true;

    private String genesisUtxoSupply = "30000000000000000"; //In byron genesis
    private int nGenesisKeys = 3; //For new priv network
    private int nGenesisUtxoKeys = 3; //For new priv network


    //Shelley Genesis
    private String maxLovelaceSupply = "45000000000000000";
    private float poolPledgeInfluence = 0;
    private BigDecimal decentralisationParam = BigDecimal.ZERO;
    private int eMax =18;
    private BigInteger keyDeposit = BigInteger.valueOf(2000000);
    private long maxBlockBodySize = 90112;
    private long maxBlockHeaderSize = 1100;
    private long maxTxSize = 16384;
    private long minFeeA = 44;
    private long minFeeB = 155381;
    private BigInteger minPoolCost = BigInteger.valueOf(170000000);
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
    private List<Delegator> defaultDelegators = List.of(new Delegator("295b987135610616f3c74e11c94d77b6ced5ccc93a7d719cfb135062", "7301761068762f5900bde9eb7c1c15b09840285130f5b0f53606cc57", true));

    //Alonzo
    private int collateralPercentage = 150;
    private String prMemNumerator = "577";
    private String prMemDenominator ="10000";
    private String prStepsNumerator = "721";
    private String prStepsDenominator ="10000000";
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

    private float dvtMotionNoConfidence = 0.67f;
    private float dvtCommitteeNormal = 0.67f;
    private float dvtCommitteeNoConfidence = 0.6f;
    private float dvtUpdateToConstitution = 0.75f;
    private float dvtHardForkInitiation = 0.6f;
    private float dvtPPNetworkGroup = 0.67f;
    private float dvtPPEconomicGroup = 0.67f;
    private float dvtPPTechnicalGroup = 0.67f;
    private float dvtPPGovGroup = 0.75f;
    private float dvtTreasuryWithdrawal = 0.67f;

    private int committeeMinSize = 0;
    private int committeeMaxTermLength = 146;
    private int govActionLifetime = 6;
    private BigInteger govActionDeposit = BigInteger.valueOf(1000000000);
    private BigInteger dRepDeposit = BigInteger.valueOf(500000000);
    private int dRepActivity = 20;
    private BigDecimal minFeeRefScriptCostPerByte = BigDecimal.valueOf(15);

    private String constitutionUrl = "https://devkit.yaci.xyz/constitution.json";
    private String constitutionDataHash = "f89cc2469ce31c3dfda2f3e0b56c5c8b4ee4f0e5f66c30a3f12a95298b01179e";

    //Always true script V3. Cbor hex: 46450101002499
    private String constitutionScript = "186e32faa80a26810392fda6d559c7ed4721a65ce1c9d4ef3e1c87b4";

    private List<CCMember> ccMembers = new ArrayList<>();
    private int ccThresholdNumerator = 2;
    private int ccThresholdDenominator = 3;

    private boolean disableFaucet = false;
    private boolean disableShelleyInitialFunds = false;

    //Default faucet address
    private Map<String, BigInteger> faucets= new LinkedHashMap<>();
    private Map<String, BigInteger> initialFunds = new LinkedHashMap<>();
    private List<MapItem<String, BigInteger>> initialFundsList = new ArrayList<>();

    private List<InitialAddress> initialAddresses = new ArrayList<>();

    //Byron Genesis
    private List<String> bootStakeHolders = List.of(
            "4c23c4a699c4245f41c79d444e0a3322edbf66daa7efe001c6c8657c"
//            "ca456dbf716c78fd4069ca352e3c56501ed37983534de76f7987cd33",
//            "fe60e90aa8237e2fc643d38655aa5ee69c69e03db80a0f63eb8d42b4"
    );

    private List<HeavyDelegation> heavyDelegations = List.of(
            new HeavyDelegation("4c23c4a699c4245f41c79d444e0a3322edbf66daa7efe001c6c8657c",
                    0,
                    "+AkxDu8deptOlFXf1QMC0ys/w0y7mjqHRCqybUequeotqUVCz1h1HSOCNK5eBPE5svg2tHyQJKQzToAfCiSDOg==",
                    "OBjvlmcUFmFHcRV27X2eRjBjAexq/Q0KiYDwkeEYbtJwT0xPnjn1+NE8oI4ePOA/M4mtHbtuYf40wLdvJVRCnw==",
                    "44327b2748c561d6bfed3d3f62dde3745d89b768a24dfa5977908433afb53611bdd841f70b33554ad57aef804448f9a09132c55ab08add5aa3b7dab150c2ae0b", true)
//            new HeavyDelegation("ca456dbf716c78fd4069ca352e3c56501ed37983534de76f7987cd33",
//                    0,
//                    "EQEJb8IW6YTdsBnksNkbRi086JA0K2ek20CZgxsMDaSUkatLQY7+guZsyX9/xv1Rx/dy2mrnZSBAxo53wZecOA==",
//                    "QeBvAkkrR0T5YoeqGa0u7wCio5D9dRIT0BoPRXfDVO0skNlO4TmJHTVJnCs2hWDtwaznWygJAx1AFk4tBvhu1A==",
//                    "7525bb530324039c1c5faa2088a95272c7195ff49a97f014a60d944b1c2212b11e1867c1fb69895609f8b976926bb8309489258b3be0b819b36f9c0a0c403d02", false),
//            new HeavyDelegation("fe60e90aa8237e2fc643d38655aa5ee69c69e03db80a0f63eb8d42b4",
//                    0,
//                    "h745CvlXG8yPF2y8MU/bSbMekN61YN+V+htnswdPvAIhIwi3XH9ZIxsdR6DCYq+maOfwzUvys5Z9WEholH3/EQ==",
//                    "5p4ntkX2uDoCsi/s136jaswzPLJr7qcaTaguGokToHuwdxRV+6LGuwvmWHs5kdQg518v+0ZCji1QLo8X4ApezA==",
//                    "abf663bac9650ee8547a98c33a4dca528493025f130556e9037c56885e2edbbe2d445b3714363b05f48d0bb6892cdae0ca27568f2aa7f1295cf11c5316da7b05", true)
    );

    private List<GenesisDeleg> genesisDelegs = List.of(
            new GenesisDeleg("337bc5ef0f1abf205624555c13a37258c42b46b1259a6b1a6d82574e",
                    "41fd6bb31f34469320aa47cf6ccc3918e58a06d60ea1f2361efe2458",
                    "7053e3ecd2b19db13e5338aa75fb518fc08b6c218f56ad65760d3eb074be95d4", true)
//            new GenesisDeleg("b5c3fed76c54bfdcda7993e4171eff2fb853f8ab920ab80047278a91",
//                    "fcb677a90948d1d5b358912f54f6baaf762ecf5cd6579c93bcb49cef",
//                    "c7715f726e8e4f7745ccc646f4350758460de71de5694b244a47761fb106ec6e", false),
//            new GenesisDeleg("e34a75849b978bb20c366f531f978b3111b91aae0e469108dca8e433",
//                    "81babf3c139646f0f0268abed36d2a54757492a3a68cda2438a37f7e",
//                    "ca336185cd781a6543b6c1e62ee1eee53e237d5d1fb065f08412d40d61b6ca06", true)
    );

    private List<NonAvvmBalances> nonAvvmBalances = List.of(
            new NonAvvmBalances("2657WMsDfac6EtPTiPEptLHDYUVYD5DtRpTmVWb6X95beFrKXqPULmyvCwmCxZEGN", "3340000000", true)
//            new NonAvvmBalances("2657WMsDfac6PDUZWRH4fh4j6ARZaH3x7SaaQ48d8SkGcNxmpoxPthQSiEahDTzAB", "3340000000", false),
//            new NonAvvmBalances("2657WMsDfac6if177KSAP7hosuDveRHN3ZsyP2EQNgTaQ5tqFTnmw1EMZcGreMHva", "3340000000", true)
    );


    //Introduced for the issue https://github.com/bloxbean/yaci-devkit/issues/65
    private int conwayHardForkAtEpoch = 0;
    private boolean shiftStartTimeBehind = false;

    @PostConstruct
    public void postInit() {
        if (faucets.size() == 0 && !disableFaucet) {
            faucets = Map.of(
                    "007290ea8fa9433c1045a4c8473959ad608e6c03a58c7de33bdbd3ce6f295b987135610616f3c74e11c94d77b6ced5ccc93a7d719cfb135062", BigInteger.valueOf(300000000000L),
                    "605276322ac7882434173dcc6441905f6737689bd309b68ad8b3614fd8", BigInteger.valueOf(3000000000000000L),
                    "60a0f1aa7dca95017c11e7e373aebcf0c4568cf47ec12b94f8eb5bba8b", BigInteger.valueOf(3000000000000000L),
                    "60ba957a0fff6816021b2afa7900beea68fd10f2d78fb5b64de0d2379c", BigInteger.valueOf(3000000000000000L)
            );
        }

        if (initialFunds.size() == 0 && initialAddresses.size() == 0 && !disableShelleyInitialFunds) {
            initialFunds.put( "00c8c47610a36034aac6fc58848bdae5c278d994ff502c05455e3b3ee8f8ed3a0eea0ef835ffa7bbfcde55f7fe9d2cc5d55ea62cecb42bab3c", BigInteger.valueOf(10000000000L));
            initialFunds.put( "004048ff89ca4f88e66598e620aa0c7128c2145d9a181ae9a4a81ca8e3e849af38840c5562dd382be37c9e76545c8191f9d8f6df1d20cfcee0", BigInteger.valueOf(10000000000L));
            initialFunds.put( "00ca6e1b1f320d543a24adeabc0aa4627635c7349b639f86f74bdfdd78d31b28c9619a58b3792a7394ab85deb36889c4d7b0632c8167b855d2", BigInteger.valueOf(10000000000L));
            initialFunds.put( "0007d781fe8e33883e371f9550c2f1087321fc32e06e80b65e349ccb027702d6880e86e77a0520efa37ede45002a1de43b68692e175b742e67", BigInteger.valueOf(10000000000L));
            initialFunds.put( "00627b2598dd71129167825160c564067d1d245e79cc237094815c5cb2b125e30ec2f4ce4059a069e08c3cd82cdfc9451bfb22487f8a25ceef", BigInteger.valueOf(10000000000L));
            initialFunds.put( "00c6cf7bd50f37f7e4cc161fc00f07e9b2226ba5552ccaf30d315fa0135bbc8cbd9ab5379f368fc8d3500c37a9d14074cc6ddad89e3686f0e0", BigInteger.valueOf(10000000000L));
            initialFunds.put( "005164ab186715c86378020956d892cf72f67636b78967d67cfe7360479130dc89cf7a9bc89109f939956b66f93293ade4c3920b72fd40beea", BigInteger.valueOf(10000000000L));
            initialFunds.put( "003dd38742e9848c6f12c13ddb1f9464fc0ce0bb92102768087975317e5a9f869fcd913562c9b0e0f01f77e5359ea780d37f9355f9702eff8b", BigInteger.valueOf(10000000000L));
            initialFunds.put( "0088e7e670b45cab2322b518ef7b6f66d30aec0d923dc463e467091a790f67796b9fa71224f2846cebbcf4950c11e040ee124d30f6e164bcd5", BigInteger.valueOf(10000000000L));
            initialFunds.put( "00c70b8421617802d3f23956cab1957e1d306cd4808589b41760e97927ebfd6053ba12b38288b2b6d5d4c4618d6a8ce59d50580e9c6f704af5", BigInteger.valueOf(10000000000L));
            initialFunds.put( "00c0933b8238f6f3332e48c34cf1a8e0555943b33cd4abc53aefb7d6124b7ce40dd496bdc02b34602f3a773ff7cccee873991e4c8866f3a70b", BigInteger.valueOf(10000000000L));
            initialFunds.put( "0069f7d7289de2f01cd1e0265ac5be943b41775abae0ce6b3eac0edee0ce9cadb7cdec2bded3ef8a7bbe3352869bfc1387754c9ee6b1782d9c", BigInteger.valueOf(10000000000L));
            initialFunds.put( "00709a7070005c652c27df73dbbde3319a90b127bea96aded1c5fb87a59c51dbcf90fa890174497f3f66a0dad06eb7f131e06567995e9c50a5", BigInteger.valueOf(10000000000L));
            initialFunds.put( "00fc576df3a279885a7a4d0fc500372daa1d96f26c6763542ecd2ad8551753024adea37c134edebb68dc0cfaed5a7009e8305fe1fed8d0ccd1", BigInteger.valueOf(10000000000L));
            initialFunds.put( "003346a630e6972bf38cce87219db1d63061e7cd324cad88c18e504f2990cac68e973f51256ca938683fa4ea12173d7d047d940fbb883bd0e8", BigInteger.valueOf(10000000000L));
            initialFunds.put( "0028b862d001e6a64a02b3560cbc532eab4557593477c39cc523e0b9fc527100898c11e731194171b908aad463770d6cbf7ec8871c4cb1e518", BigInteger.valueOf(10000000000L));
            initialFunds.put( "005e0e57040b06e9d71e0f28f126262838a68db0b52b4fd1b3877dda2203d5d7d4f19c5ee3a1ed51bb670779de19d40aaff2e5e9468cc05c5e", BigInteger.valueOf(10000000000L));
            initialFunds.put( "00367f65ab69b1e6715c8d5a14964214c9505ed17032266b3209a2c40dcbae9a2a881e603ff39d36e987bacfb87ee98051f222c5fe3efd350c", BigInteger.valueOf(10000000000L));
            initialFunds.put( "00c5c4ca287f3b53948b5468e5e23b1c87fe61ce52c0d9afd65d070795038751a619d463e91eaed0a774ebdb2f8e12a01a378a153bc3627323", BigInteger.valueOf(10000000000L));
            initialFunds.put( "00ef198fb7c35e1968308a0b75cfee54a46e13e86dd3354283300831d624165c357b5a0413906a0bfea8ba57587331f0836a186d632ed041b8", BigInteger.valueOf(10000000000L));
        }

        if (faucets.size() > 0 && !disableFaucet)
            initialFunds.putAll(faucets);

//        if (initialFunds.size() > 0) {
//            initialFunds = initialFunds.entrySet()
//                    .stream()
//                    .collect(Collectors.toMap(
//                            entry -> transformAddrToHex(entry.getKey()), // Transform the key
//                            Map.Entry::getValue // Keep the value as is
//                    ));
//        }

        initialAddresses.stream()
                .forEach(initialAddress -> {
                    var address = new Address(initialAddress.address);
                    initialFunds.put(HexUtil.encodeHexString(address.getBytes()), initialAddress.balance());
                });

        initialFundsList = createListWithLastFlag(initialFunds);

        if (initialAddresses == null || initialAddresses.size() == 0) {
            if (defaultDelegators != null && defaultDelegators.size() > 0) {
                defaultDelegators.get(defaultDelegators.size() - 1).setLast(true);
            }
        } else {
            defaultDelegators = new ArrayList<>();
            initialAddresses.forEach(initialAddress -> {
                if (!initialAddress.staked)
                    return;

                Address address = new Address(initialAddress.address);
                var stakeKeyHash = address.getDelegationCredentialHash()
                        .map(HexUtil::encodeHexString)
                                .orElse(null);
                defaultDelegators.add(new Delegator(stakeKeyHash, null, false));
            });

            defaultDelegators.getLast().setLast(true);
        }
    }

    private String transformAddrToHex(String key) {
        if (key == null)
            return null;
        if (key.startsWith("addr")) {
            var address = new Address(key);
            return HexUtil.encodeHexString(address.getBytes());
        } else
            return key;
    }

    public Map getConfigMap() {
        Map map = new HashMap();
        map.put("networkId", networkId);
        if ("Mainnet".equals(networkId)) {
            map.put("mainnet", true);
        }
        map.put("protocolMagic", protocolMagic);
        map.put("maxKESEvolutions", maxKESEvolutions);
        map.put("stabilityWindowFactor", stabilityWindowFactor);
        map.put("securityParam", securityParam);
        map.put("slotsPerKESPeriod", slotsPerKESPeriod);
        map.put("updateQuorum", updateQuorum);
        map.put("peerSharing", peerSharing);
        map.put("genesisUtxoSupply", genesisUtxoSupply);
        map.put("nGenesisKeys", nGenesisKeys);
        map.put("nGenesisUtxoKeys", nGenesisUtxoKeys);
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

        if (pools != null && pools.size() > 0)
            pools.getLast().setLast(true);
        map.put("pools", pools);

        if (defaultDelegators != null && defaultDelegators.size() >0)
            defaultDelegators.getLast().setLast(true);
        map.put("defaultDelegators", defaultDelegators);

        map.put("collateralPercentage", collateralPercentage);
        map.put("prMemNumerator", prMemNumerator);
        map.put("prMemDenominator", prMemDenominator);
        map.put("prStepsNumerator", prStepsNumerator);
        map.put("prStepsDenominator", prStepsDenominator);
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
        map.put("minFeeRefScriptCostPerByte", minFeeRefScriptCostPerByte);
        map.put("constitutionUrl", constitutionUrl);
        map.put("constitutionDataHash", constitutionDataHash);
        if (constitutionScript != null && !constitutionScript.trim().isEmpty())
            map.put("constitutionScript", constitutionScript);

        if (ccMembers != null && ccMembers.size() > 0)
            ccMembers.getLast().setLast(true);
        map.put("ccMembers", ccMembers);
        map.put("ccThresholdNumerator", ccThresholdNumerator);
        map.put("ccThresholdDenominator", ccThresholdDenominator);

        map.put("initialFunds", initialFundsList);

        map.put("heavyDelegations", heavyDelegations);
        map.put("genesisDelegs", genesisDelegs);
        map.put("nonAvvmBalances", nonAvvmBalances);

        map.put("conwayHardForkAtEpoch", conwayHardForkAtEpoch);
        map.put("shiftStartTimeBehind", shiftStartTimeBehind);

        return map;
    }

    public GenesisConfig copy() {
        var genesisConfig = new GenesisConfig();
        genesisConfig.setNetworkId(networkId);
        genesisConfig.setProtocolMagic(protocolMagic);
        genesisConfig.setMaxKESEvolutions(maxKESEvolutions);
        genesisConfig.setStabilityWindowFactor(stabilityWindowFactor);
        genesisConfig.setSecurityParam(securityParam);
        genesisConfig.setSlotsPerKESPeriod(slotsPerKESPeriod);
        genesisConfig.setUpdateQuorum(updateQuorum);
        genesisConfig.setPeerSharing(peerSharing);
        genesisConfig.setGenesisUtxoSupply(genesisUtxoSupply);
        genesisConfig.setNGenesisKeys(nGenesisKeys);
        genesisConfig.setNGenesisUtxoKeys(nGenesisUtxoKeys);
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
        genesisConfig.setPrMemNumerator(prMemNumerator);
        genesisConfig.setPrMemDenominator(prMemDenominator);
        genesisConfig.setPrStepsNumerator(prStepsNumerator);
        genesisConfig.setPrStepsDenominator(prStepsDenominator);
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
        genesisConfig.setMinFeeRefScriptCostPerByte(minFeeRefScriptCostPerByte);
        genesisConfig.setConstitutionUrl(constitutionUrl);
        genesisConfig.setConstitutionDataHash(constitutionDataHash);
        genesisConfig.setConstitutionScript(constitutionScript);
        genesisConfig.setCcMembers(ccMembers);
        genesisConfig.setCcThresholdNumerator(ccThresholdNumerator);
        genesisConfig.setCcThresholdDenominator(ccThresholdDenominator);

        genesisConfig.setFaucets(faucets);
        genesisConfig.setInitialFunds(initialFunds);
        genesisConfig.setInitialFundsList(initialFundsList);

        genesisConfig.setHeavyDelegations(new ArrayList<>(heavyDelegations));
        genesisConfig.setGenesisDelegs(new ArrayList<>(genesisDelegs));
        genesisConfig.setNonAvvmBalances(new ArrayList<>(nonAvvmBalances));

        genesisConfig.setConwayHardForkAtEpoch(conwayHardForkAtEpoch);
        genesisConfig.setShiftStartTimeBehind(shiftStartTimeBehind);

        return genesisConfig;
    }

    public void merge(Map<String, String> updatedValues) {
        if (updatedValues != null && !updatedValues.isEmpty()) {
            if (updatedValues.get("protocolMagic") != null && !updatedValues.get("protocolMagic").isEmpty())
                protocolMagic = Long.parseLong(updatedValues.get("protocolMagic"));
            if (updatedValues.get("maxKESEvolutions") != null && !updatedValues.get("maxKESEvolutions").isEmpty())
                maxKESEvolutions = Integer.parseInt(updatedValues.get("maxKESEvolutions"));
            if (updatedValues.get("stabilityWindowFactor") != null && !updatedValues.get("stabilityWindowFactor").isEmpty())
                stabilityWindowFactor = Integer.parseInt(updatedValues.get("stabilityWindowFactor"));
            if (updatedValues.get("securityParam") != null && !updatedValues.get("securityParam").isEmpty())
                securityParam = Integer.parseInt(updatedValues.get("securityParam"));
            if (updatedValues.get("slotsPerKESPeriod") != null && !updatedValues.get("slotsPerKESPeriod").isEmpty())
                slotsPerKESPeriod = Integer.parseInt(updatedValues.get("slotsPerKESPeriod"));
            if (updatedValues.get("updateQuorum") != null && !updatedValues.get("updateQuorum").isEmpty())
                updateQuorum = Integer.parseInt(updatedValues.get("updateQuorum"));
            if (updatedValues.get("peerSharing") != null && !updatedValues.get("peerSharing").isEmpty())
                peerSharing = Boolean.parseBoolean(updatedValues.get("peerSharing"));
            if (updatedValues.get("genesisUtxoSupply") != null && !updatedValues.get("genesisUtxoSupply").isEmpty())
                genesisUtxoSupply = updatedValues.get("genesisUtxoSupply");
            if (updatedValues.get("nGenesisKeys") != null && !updatedValues.get("nGenesisKeys").isEmpty())
                nGenesisKeys = Integer.parseInt(updatedValues.get("nGenesisKeys"));
            if (updatedValues.get("nGenesisUtxoKeys") != null && !updatedValues.get("nGenesisUtxoKeys").isEmpty())
                nGenesisUtxoKeys = Integer.parseInt(updatedValues.get("nGenesisUtxoKeys"));
            if (updatedValues.get("maxLovelaceSupply") != null && !updatedValues.get("maxLovelaceSupply").isEmpty())
                maxLovelaceSupply = updatedValues.get("maxLovelaceSupply");
            if (updatedValues.get("poolPledgeInfluence") != null && !updatedValues.get("poolPledgeInfluence").isEmpty())
                poolPledgeInfluence = Float.parseFloat(updatedValues.get("poolPledgeInfluence"));
            if (updatedValues.get("decentralisationParam") != null && !updatedValues.get("decentralisationParam").isEmpty())
                decentralisationParam = new BigDecimal(updatedValues.get("decentralisationParam"));
            if (updatedValues.get("eMax") != null && !updatedValues.get("eMax").isEmpty())
                eMax = Integer.parseInt(updatedValues.get("eMax"));
            if (updatedValues.get("keyDeposit") != null && !updatedValues.get("keyDeposit").isEmpty())
                keyDeposit = new BigInteger(updatedValues.get("keyDeposit"));
            if (updatedValues.get("maxBlockBodySize") != null && !updatedValues.get("maxBlockBodySize").isEmpty())
                maxBlockBodySize = Long.parseLong(updatedValues.get("maxBlockBodySize"));
            if (updatedValues.get("maxBlockHeaderSize") != null && !updatedValues.get("maxBlockHeaderSize").isEmpty())
                maxBlockHeaderSize = Long.parseLong(updatedValues.get("maxBlockHeaderSize"));
            if (updatedValues.get("maxTxSize") != null && !updatedValues.get("maxTxSize").isEmpty())
                maxTxSize = Long.parseLong(updatedValues.get("maxTxSize"));
            if (updatedValues.get("minFeeA") != null && !updatedValues.get("minFeeA").isEmpty())
                minFeeA = Long.parseLong(updatedValues.get("minFeeA"));
            if (updatedValues.get("minFeeB") != null && !updatedValues.get("minFeeB").isEmpty())
                minFeeB = Long.parseLong(updatedValues.get("minFeeB"));
            if (updatedValues.get("minPoolCost") != null && !updatedValues.get("minPoolCost").isEmpty())
                minPoolCost = new BigInteger(updatedValues.get("minPoolCost"));
            if (updatedValues.get("minUTxOValue") != null && !updatedValues.get("minUTxOValue").isEmpty())
                minUTxOValue = new BigInteger(updatedValues.get("minUTxOValue"));
            if (updatedValues.get("nOpt") != null && !updatedValues.get("nOpt").isEmpty())
                nOpt = Integer.parseInt(updatedValues.get("nOpt"));
            if (updatedValues.get("poolDeposit") != null && !updatedValues.get("poolDeposit").isEmpty())
                poolDeposit = new BigInteger(updatedValues.get("poolDeposit"));
            if (updatedValues.get("protocolMajorVer") != null && !updatedValues.get("protocolMajorVer").isEmpty())
                protocolMajorVer = Integer.parseInt(updatedValues.get("protocolMajorVer"));
            if (updatedValues.get("protocolMinorVer") != null && !updatedValues.get("protocolMinorVer").isEmpty())
                protocolMinorVer = Integer.parseInt(updatedValues.get("protocolMinorVer"));
            if (updatedValues.get("monetaryExpansionRate") != null && !updatedValues.get("monetaryExpansionRate").isEmpty())
                monetaryExpansionRate = Float.parseFloat(updatedValues.get("monetaryExpansionRate"));
            if (updatedValues.get("treasuryGrowthRate") != null && !updatedValues.get("treasuryGrowthRate").isEmpty())
                treasuryGrowthRate = Float.parseFloat(updatedValues.get("treasuryGrowthRate"));
            if (updatedValues.get("collateralPercentage") != null && !updatedValues.get("collateralPercentage").isEmpty())
                collateralPercentage = Integer.parseInt(updatedValues.get("collateralPercentage"));
            if (updatedValues.get("prMemNumerator") != null && !updatedValues.get("prMemNumerator").isEmpty())
                prMemNumerator = updatedValues.get("prMemNumerator");
            if (updatedValues.get("prMemDenominator") != null && !updatedValues.get("prMemDenominator").isEmpty())
                prMemDenominator = updatedValues.get("prMemDenominator");
            if (updatedValues.get("prStepsNumerator") != null && !updatedValues.get("prStepsNumerator").isEmpty())
                prStepsNumerator = updatedValues.get("prStepsNumerator");
            if (updatedValues.get("prStepsDenominator") != null && !updatedValues.get("prStepsDenominator").isEmpty())
                prStepsDenominator = updatedValues.get("prStepsDenominator");
            if (updatedValues.get("lovelacePerUTxOWord") != null && !updatedValues.get("lovelacePerUTxOWord").isEmpty())
                lovelacePerUTxOWord = Long.parseLong(updatedValues.get("lovelacePerUTxOWord"));
            if (updatedValues.get("maxBlockExUnitsMem") != null && !updatedValues.get("maxBlockExUnitsMem").isEmpty())
                maxBlockExUnitsMem = Long.parseLong(updatedValues.get("maxBlockExUnitsMem"));
            if (updatedValues.get("maxBlockExUnitsSteps") != null && !updatedValues.get("maxBlockExUnitsSteps").isEmpty())
                maxBlockExUnitsSteps = Long.parseLong(updatedValues.get("maxBlockExUnitsSteps"));
            if (updatedValues.get("maxCollateralInputs") != null && !updatedValues.get("maxCollateralInputs").isEmpty())
                maxCollateralInputs = Integer.parseInt(updatedValues.get("maxCollateralInputs"));
            if (updatedValues.get("maxTxExUnitsMem") != null && !updatedValues.get("maxTxExUnitsMem").isEmpty())
                maxTxExUnitsMem = Long.parseLong(updatedValues.get("maxTxExUnitsMem"));
            if (updatedValues.get("maxTxExUnitsSteps") != null && !updatedValues.get("maxTxExUnitsSteps").isEmpty())
                maxTxExUnitsSteps = Long.parseLong(updatedValues.get("maxTxExUnitsSteps"));
            if (updatedValues.get("maxValueSize") != null && !updatedValues.get("maxValueSize").isEmpty())
                maxValueSize = Integer.parseInt(updatedValues.get("maxValueSize"));
            if (updatedValues.get("committeeMinSize") != null && !updatedValues.get("committeeMinSize").isEmpty())
                committeeMinSize = Integer.parseInt(updatedValues.get("committeeMinSize"));
            if (updatedValues.get("committeeMaxTermLength") != null && !updatedValues.get("committeeMaxTermLength").isEmpty())
                committeeMaxTermLength = Integer.parseInt(updatedValues.get("committeeMaxTermLength"));
            if (updatedValues.get("govActionLifetime") != null && !updatedValues.get("govActionLifetime").isEmpty())
                govActionLifetime = Integer.parseInt(updatedValues.get("govActionLifetime"));
            if (updatedValues.get("govActionDeposit") != null && !updatedValues.get("govActionDeposit").isEmpty())
                govActionDeposit = new BigInteger(updatedValues.get("govActionDeposit"));
            if (updatedValues.get("dRepDeposit") != null && !updatedValues.get("dRepDeposit").isEmpty())
                dRepDeposit = new BigInteger(updatedValues.get("dRepDeposit"));
            if (updatedValues.get("dRepActivity") != null && !updatedValues.get("dRepActivity").isEmpty())
                dRepActivity = Integer.parseInt(updatedValues.get("dRepActivity"));
            if (updatedValues.get("minFeeRefScriptCostPerByte") != null && !updatedValues.get("minFeeRefScriptCostPerByte").isEmpty())
                minFeeRefScriptCostPerByte = new BigDecimal(updatedValues.get("minFeeRefScriptCostPerByte"));
            if (updatedValues.get("constitutionUrl") != null && !updatedValues.get("constitutionUrl").isEmpty())
                constitutionUrl = updatedValues.get("constitutionUrl");
            if (updatedValues.get("constitutionDataHash") != null && !updatedValues.get("constitutionDataHash").isEmpty())
                constitutionDataHash = updatedValues.get("constitutionDataHash");
            if (updatedValues.get("constitutionScript") != null && !updatedValues.get("constitutionScript").trim().isEmpty())
                constitutionScript = updatedValues.get("constitutionScript");

            if (updatedValues.get("shiftStartTimeBehind") != null && !updatedValues.get("shiftStartTimeBehind").isEmpty())
                shiftStartTimeBehind = Boolean.parseBoolean(updatedValues.get("shiftStartTimeBehind"));
            if (updatedValues.get("conwayHardForkAtEpoch") != null && !updatedValues.get("conwayHardForkAtEpoch").isEmpty())
                conwayHardForkAtEpoch = Integer.parseInt(updatedValues.get("conwayHardForkAtEpoch"));
        }
    }

    private <K, V> List<MapItem<K, V>> createListWithLastFlag(Map<K, V> faucets) {
        List<MapItem<K, V>> faucetList = new ArrayList<>();
        int i = 0;
        int size = faucets.size();
        for (Map.Entry<K, V> entry : faucets.entrySet()) {
            var item = new MapItem<K, V>(entry.getKey(), entry.getValue(), i == size - 1);
            faucetList.add(item);
            i++;
        }

        return faucetList;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Pool {
        String poolHash;
        BigInteger cost;
        BigDecimal margin;
        String publicKey;
        String rewardAccountHash;
        String rewardAccountType;
        String vrf;
        boolean last;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Delegator {
        String stakeKeyHash;
        String poolHash;
        boolean last;
    }

    public record MapItem<K, V>(K key, V value, boolean last) {}
    public record HeavyDelegation(String bootStakeDelegator, int omega, String issuerPk, String delegatePk, String cert, boolean last) {}
    public record GenesisDeleg(String delegator, String delegate, String vrf, boolean last) {}
    public record NonAvvmBalances(String address, String balance, boolean last) {}

    public record InitialAddress(String address, BigInteger balance, boolean staked, boolean last) {}

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CCMember {
        String hash;
        int term;
        boolean last;
    }
}
