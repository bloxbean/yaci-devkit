package com.bloxbean.cardano.yacicli.commands.localcluster.service;

import com.bloxbean.cardano.client.api.TransactionProcessor;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.EvaluationResult;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.yaci.core.common.TxBodyType;
import com.bloxbean.cardano.yaci.core.protocol.localtx.model.TxSubmissionRequest;
import com.bloxbean.cardano.yaci.helper.LocalTxSubmissionClient;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public class LocalTransactionProcessor implements TransactionProcessor {
    private LocalTxSubmissionClient txSubmissionClient;
    private TxBodyType txBodyType;

    public LocalTransactionProcessor(LocalTxSubmissionClient txSubmissionClient, TxBodyType txBodyType) {
        this.txSubmissionClient = txSubmissionClient;
        this.txBodyType = txBodyType;
    }

    @Override
    public Result<String> submitTransaction(byte[] bytes) throws ApiException {
        TxSubmissionRequest request = new TxSubmissionRequest(txBodyType, bytes);
        var mono = txSubmissionClient.submitTx(request);
        var txResult = mono.block(Duration.ofSeconds(2));

        if (txResult.isAccepted()) {
            return Result.success(txResult.getTxHash()).withValue(txResult.getTxHash());
        } else {
            return Result.error(txResult.getErrorCbor());
        }
    }

    @Override
    public Result<List<EvaluationResult>> evaluateTx(byte[] bytes, Set<Utxo> set) {
        throw new UnsupportedOperationException("Not supported");
    }
}
