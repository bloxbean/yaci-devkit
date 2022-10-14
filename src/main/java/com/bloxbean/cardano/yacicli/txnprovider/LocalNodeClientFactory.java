package com.bloxbean.cardano.yacicli.txnprovider;

import com.bloxbean.cardano.yaci.core.helpers.LocalStateQueryClient;
import com.bloxbean.cardano.yaci.core.helpers.LocalTxSubmissionClient;
import com.bloxbean.cardano.yaci.core.protocol.localtx.LocalTxSubmissionListener;
import com.bloxbean.cardano.yaci.core.protocol.localtx.messages.MsgAcceptTx;
import com.bloxbean.cardano.yaci.core.protocol.localtx.messages.MsgRejectTx;
import com.bloxbean.cardano.yaci.core.protocol.localtx.model.TxSubmissionRequest;

import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

public class LocalNodeClientFactory {
    private LocalStateQueryClient localStateQueryClient;
    private LocalTxSubmissionClient txSubmissionClient;

    public LocalNodeClientFactory(String socketFile, long protocolMagic, Consumer<String> writer) {
        this.localStateQueryClient = new LocalStateQueryClient(socketFile, protocolMagic);
        this.localStateQueryClient.start(result -> {});

        this.txSubmissionClient = new LocalTxSubmissionClient(socketFile, protocolMagic);
        this.txSubmissionClient.addTxSubmissionListener(new LocalTxSubmissionListener() {
            @Override
            public void txAccepted(TxSubmissionRequest txSubmissionRequest, MsgAcceptTx msgAcceptTx) {
                writer.accept(success("Transaction submitted successfully"));
                writer.accept(infoLabel("Tx Hash", txSubmissionRequest.getTxHash()));
            }

            @Override
            public void txRejected(TxSubmissionRequest txSubmissionRequest, MsgRejectTx msgRejectTx) {
                writer.accept(error("Transaction submission failed"));
            }
        });
        this.txSubmissionClient.start(txResult -> {});
    }

    public LocalStateQueryClient getLocalStateQueryClient() {
        return localStateQueryClient;
    }

    public LocalTxSubmissionClient getTxSubmissionClient() {
        return txSubmissionClient;
    }

    public void shutdown() {
        try {
            localStateQueryClient.shutdown();
        } catch (Exception e) {
            throw e;
        } finally {
            txSubmissionClient.shutdown();
        }
    }
}
