///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS com.bloxbean.cardano:cardano-client-lib:${ccl.version:0.8.0-preview1}
//DEPS com.bloxbean.cardano:cardano-client-backend-blockfrost:${ccl.version:0.8.0-preview1}
//DEPS org.slf4j:slf4j-nop:2.0.13
//SOURCES Devnet.java

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;

// Simple ADA payment using Yaci Store as the Blockfrost backend
public class Payment {
    public static void main(String[] args) {
        Account sender = Devnet.account();
        System.out.println("Sender: " + sender.baseAddress());

        Tx tx = new Tx()
                .payToAddress(Devnet.RECEIVER, Amount.ada(5))
                .from(sender.baseAddress());

        var result = new QuickTxBuilder(Devnet.backendService())
                .compose(tx)
                .withSigner(SignerProviders.signerFrom(sender))
                .completeAndWait(System.out::println);

        Devnet.check("payment", result);
    }
}
