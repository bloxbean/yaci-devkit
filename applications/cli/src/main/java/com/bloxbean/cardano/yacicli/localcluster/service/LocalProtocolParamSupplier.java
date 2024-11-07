package com.bloxbean.cardano.yacicli.localcluster.service;

import co.nstant.in.cbor.model.Array;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Number;
import co.nstant.in.cbor.model.Special;
import com.bloxbean.cardano.client.api.ProtocolParamsSupplier;
import com.bloxbean.cardano.client.api.model.ProtocolParams;
import com.bloxbean.cardano.client.plutus.util.PlutusOps;
import com.bloxbean.cardano.yaci.core.model.ProtocolParamUpdate;
import com.bloxbean.cardano.yaci.core.protocol.localstate.api.Era;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.CurrentProtocolParamQueryResult;
import com.bloxbean.cardano.yaci.core.protocol.localstate.queries.CurrentProtocolParamsQuery;
import com.bloxbean.cardano.yaci.core.util.CborSerializationUtil;
import com.bloxbean.cardano.yaci.core.util.HexUtil;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;

public class LocalProtocolParamSupplier implements ProtocolParamsSupplier {
    public static final String PLUTUS_V_1 = "PlutusV1";
    public static final String PLUTUS_V_2 = "PlutusV2";
    public static final String PLUTUS_V_3 = "PlutusV3";
    private LocalStateQueryClient localStateQueryClient;

    private Era era = Era.Babbage;

    /**
     * Constructor
     * @param localStateQueryClient LocalStateQueryClient
     */
    public LocalProtocolParamSupplier(LocalStateQueryClient localStateQueryClient, Era era) {
        this.localStateQueryClient = localStateQueryClient;
        this.era = era;
    }

    @Override
    public ProtocolParams getProtocolParams() {
        //Try to release first before a new query to avoid stale data
        try {
            localStateQueryClient.release().block(Duration.ofSeconds(5));
        } catch (Exception e) {
            //Ignore the error
        }

        CurrentProtocolParamQueryResult currentProtocolParameters =
                (CurrentProtocolParamQueryResult) localStateQueryClient.executeQuery(new CurrentProtocolParamsQuery(era)).block(Duration.ofSeconds(8));

        ProtocolParamUpdate protocolParamUpdate = currentProtocolParameters.getProtocolParams();

        ProtocolParams protocolParams = new ProtocolParams();
        protocolParams.setMinFeeA(protocolParamUpdate.getMinFeeA());
        protocolParams.setMinFeeB(protocolParamUpdate.getMinFeeB());
        protocolParams.setMaxBlockSize(protocolParamUpdate.getMaxBlockSize());
        protocolParams.setMaxTxSize(protocolParamUpdate.getMaxTxSize());
        protocolParams.setMaxBlockHeaderSize(protocolParamUpdate.getMaxBlockHeaderSize());
        protocolParams.setKeyDeposit(String.valueOf(protocolParamUpdate.getKeyDeposit()));
        protocolParams.setPoolDeposit(String.valueOf(protocolParamUpdate.getPoolDeposit()));
        protocolParams.setEMax(protocolParamUpdate.getMaxEpoch());
        protocolParams.setNOpt(protocolParamUpdate.getNOpt());
        protocolParams.setA0(protocolParamUpdate.getPoolPledgeInfluence());
        protocolParams.setRho(protocolParamUpdate.getExpansionRate());
        protocolParams.setTau(protocolParamUpdate.getTreasuryGrowthRate());
        protocolParams.setDecentralisationParam(protocolParamUpdate.getDecentralisationParam()); //Deprecated. Not there
        //protocolParams.setExtraEntropy(protocolParamUpdate.getExtraEntropy()); //TODO
        protocolParams.setProtocolMajorVer(protocolParamUpdate.getProtocolMajorVer());
        protocolParams.setProtocolMinorVer(protocolParamUpdate.getProtocolMinorVer());
        protocolParams.setMinUtxo(String.valueOf(protocolParamUpdate.getMinUtxo()));
        protocolParams.setMinPoolCost(String.valueOf(protocolParamUpdate.getMinPoolCost()));
//        protocolParams.setNonce(currentProtocolParameters.getProtocolParameters().getNonce()); //TODO

        LinkedHashMap<String, Long> plutusV1CostModel
                = cborToCostModel(protocolParamUpdate.getCostModels().get(0), PlutusOps.getOperations(1));
        LinkedHashMap<String, Long> plutusV2CostModel
                = cborToCostModel(protocolParamUpdate.getCostModels().get(1), PlutusOps.getOperations(2));
        LinkedHashMap<String, Long> plutusV3CostModel = null;
        if (era == Era.Conway) {
            plutusV3CostModel = cborToCostModel(protocolParamUpdate.getCostModels().get(2), PlutusOps.getOperations(3));
        }

        LinkedHashMap<String, LinkedHashMap<String, Long>> costModels = new LinkedHashMap<>();
        costModels.put("PlutusV1", plutusV1CostModel);
        costModels.put("PlutusV2", plutusV2CostModel);
        if (plutusV3CostModel != null)
            costModels.put("PlutusV3", plutusV3CostModel);

        protocolParams.setCostModels(costModels);

        protocolParams.setPriceMem(protocolParamUpdate.getPriceMem());
        protocolParams.setPriceStep(protocolParamUpdate.getPriceStep());
        protocolParams.setMaxTxExMem(String.valueOf(protocolParamUpdate.getMaxTxExMem()));
        protocolParams.setMaxTxExSteps(String.valueOf(protocolParamUpdate.getMaxTxExSteps()));
        protocolParams.setMaxBlockExMem(String.valueOf(protocolParamUpdate.getMaxBlockExMem()));
        protocolParams.setMaxBlockExSteps(String.valueOf(protocolParamUpdate.getMaxBlockExSteps()));
        protocolParams.setMaxValSize(String.valueOf(protocolParamUpdate.getMaxValSize()));
        protocolParams.setCollateralPercent(BigDecimal.valueOf(protocolParamUpdate.getCollateralPercent()));
        protocolParams.setMaxCollateralInputs(protocolParamUpdate.getMaxCollateralInputs());
        protocolParams.setCoinsPerUtxoSize(String.valueOf(protocolParamUpdate.getAdaPerUtxoByte()));
        return protocolParams;
    }

    private LinkedHashMap<String, Long> cborToCostModel(String costModelCbor, List<String> ops) {
        Array array = (Array) CborSerializationUtil.deserializeOne(HexUtil.decodeHexString(costModelCbor));
        LinkedHashMap<String, Long> costModel = new LinkedHashMap<>();

        if (ops.size() == array.getDataItems().size()) {
            int index = 0;
            for (DataItem di : array.getDataItems()) {
                if (di == Special.BREAK)
                    continue;
                BigInteger val = ((Number) di).getValue();
                costModel.put(ops.get(index++), val.longValue());
            }
        } else {
            int index = 0;
            for (DataItem di : array.getDataItems()) {
                if (di == Special.BREAK)
                    continue;
                BigInteger val = ((Number) di).getValue();
                costModel.put(String.format("%03d", index++), val.longValue());
            }
        }

        return costModel;
    }
}

