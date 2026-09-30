///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//DEPS com.bloxbean.cardano:cardano-client-lib:${ccl.version:0.8.0-preview1}
//DEPS com.bloxbean.cardano:cardano-client-backend-blockfrost:${ccl.version:0.8.0-preview1}
//DEPS org.slf4j:slf4j-nop:2.0.13
//SOURCES Devnet.java

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.plutus.blueprint.PlutusBlueprintUtil;
import com.bloxbean.cardano.client.plutus.blueprint.model.PlutusVersion;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusScript;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.ScriptTx;
import com.bloxbean.cardano.client.quicktx.Tx;

// Lock ADA at an always-succeeds PlutusV3 script and spend it back. Script evaluation goes
// through Yaci Store's Blockfrost-compatible /utils/txs/evaluate endpoint.
public class PlutusV3 {
    // Same always-succeeds validator as lucid-evo/plutus_v3.ts (CBOR-wrapped compiled code)
    static final String COMPILED_CODE = "5857010000323232323225333002323232323253330073370e900118041baa00113232324a26018601a004601600260126ea800458c024c028008c020004c020008c018004c010dd50008a4c26cacae6955ceaab9e5742ae89";

    public static void main(String[] args) throws Exception {
        Account owner = Devnet.account();
        var backend = Devnet.backendService();
        PlutusScript script = PlutusBlueprintUtil.getPlutusScriptFromCompiledCode(COMPILED_CODE, PlutusVersion.v3);
        String scriptAddress = AddressProvider.getEntAddress(script, Networks.testnet()).toBech32();
        System.out.println("Script address: " + scriptAddress);

        var datum = ConstrPlutusData.of(0, BytesPlutusData.of(owner.getBaseAddress().getPaymentCredentialHash().get()));

        // Lock
        Tx lockTx = new Tx()
                .payToContract(scriptAddress, Amount.ada(10), datum)
                .from(owner.baseAddress());
        var lockResult = new QuickTxBuilder(backend)
                .compose(lockTx)
                .withSigner(SignerProviders.signerFrom(owner))
                .completeAndWait(System.out::println);
        Devnet.check("lock", lockResult);

        // Spend the UTxO created by the lock tx
        Utxo scriptUtxo = backend.getUtxoService().getUtxos(scriptAddress, 100, 1).getValue().stream()
                .filter(u -> u.getTxHash().equals(lockResult.getValue()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Locked UTxO not found at " + scriptAddress));

        var redeemer = ConstrPlutusData.of(0, BytesPlutusData.of("Hello, World!"));
        ScriptTx spendTx = new ScriptTx()
                .collectFrom(scriptUtxo, redeemer)
                .payToAddress(owner.baseAddress(), Amount.ada(5))
                .attachSpendingValidator(script);
        var spendResult = new QuickTxBuilder(backend)
                .compose(spendTx)
                .feePayer(owner.baseAddress())
                .collateralPayer(owner.baseAddress())
                .withSigner(SignerProviders.signerFrom(owner))
                .completeAndWait(System.out::println);
        Devnet.check("spend", spendResult);
    }
}
