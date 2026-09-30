///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS com.bloxbean.cardano:cardano-client-lib:${ccl.version:0.8.0-preview1}
//DEPS com.bloxbean.cardano:cardano-client-backend-blockfrost:${ccl.version:0.8.0-preview1}
//DEPS org.slf4j:slf4j-nop:2.0.13
//SOURCES Devnet.java

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.util.PolicyUtil;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.Asset;

import java.math.BigInteger;

// Mint a native token with a native-script policy, then query the holder's UTxOs for it
public class MintToken {
    public static void main(String[] args) throws Exception {
        Account minter = Devnet.account();
        var policy = PolicyUtil.createMultiSigScriptAllPolicy("e2e-policy", 1);
        var asset = new Asset("E2EToken", BigInteger.valueOf(1000));

        Tx tx = new Tx()
                .mintAssets(policy.getPolicyScript(), asset, minter.baseAddress())
                .from(minter.baseAddress());

        var result = new QuickTxBuilder(Devnet.backendService())
                .compose(tx)
                .withSigner(SignerProviders.signerFrom(minter))
                .withSigner(SignerProviders.signerFrom(policy))
                .completeAndWait(System.out::println);
        Devnet.check("mint", result);

        String unit = policy.getPolicyId() + asset.getNameAsHex().substring(2);
        var utxos = Devnet.backendService().getUtxoService().getUtxos(minter.baseAddress(), unit, 10, 1);
        if (!utxos.isSuccessful() || utxos.getValue().isEmpty()) {
            System.out.println("FAIL query minted asset " + unit + ": " + utxos.getResponse());
            System.exit(1);
        }
        System.out.println("OK   query minted asset: " + unit);
    }
}
