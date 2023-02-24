package com.bloxbean.cardano.yacicli.txnprovider;

import com.bloxbean.cardano.yaci.core.protocol.localtx.LocalTxSubmissionListener;
import com.bloxbean.cardano.yaci.core.protocol.localtx.messages.MsgAcceptTx;
import com.bloxbean.cardano.yaci.core.protocol.localtx.messages.MsgRejectTx;
import com.bloxbean.cardano.yaci.core.protocol.localtx.model.TxSubmissionRequest;
import com.bloxbean.cardano.yaci.helper.LocalClientProvider;
import com.bloxbean.cardano.yaci.helper.LocalStateQueryClient;
import com.bloxbean.cardano.yaci.helper.LocalTxSubmissionClient;

import java.util.function.Consumer;

import static com.bloxbean.cardano.yacicli.util.ConsoleWriter.*;

public class LocalNodeClientFactory {
    private LocalClientProvider localClientProvider;
    private LocalStateQueryClient localStateQueryClient;
    private LocalTxSubmissionClient txSubmissionClient;

    public LocalNodeClientFactory(String socketFile, long protocolMagic, Consumer<String> writer) {
        this.localClientProvider = new LocalClientProvider(socketFile, protocolMagic);
        this.localStateQueryClient = localClientProvider.getLocalStateQueryClient();

        this.txSubmissionClient = localClientProvider.getTxSubmissionClient();
        this.localClientProvider.addTxSubmissionListener(new LocalTxSubmissionListener() {
            @Override
            public void txAccepted(TxSubmissionRequest txSubmissionRequest, MsgAcceptTx msgAcceptTx) {
                writer.accept(success("Transaction submitted successfully"));
                writer.accept(infoLabel("Tx Hash", txSubmissionRequest.getTxHash()));
            }

            @Override
            public void txRejected(TxSubmissionRequest txSubmissionRequest, MsgRejectTx msgRejectTx) {
                writer.accept(error("Transaction submission failed. " + msgRejectTx.getReasonCbor()));
            }
        });
        this.localClientProvider.start();
    }

    public LocalStateQueryClient getLocalStateQueryClient() {
        return localStateQueryClient;
    }

    public LocalTxSubmissionClient getTxSubmissionClient() {
        return txSubmissionClient;
    }

    public void shutdown() {
        try {
            localClientProvider.shutdown();
        } catch (Exception e) {
            throw e;
        } finally {
            localClientProvider.shutdown();
        }
    }
}
