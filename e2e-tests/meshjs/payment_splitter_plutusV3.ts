import {
    MeshWallet, serializePlutusScript, resolvePaymentKeyHash, Transaction, largestFirst,
    Asset, YaciProvider
} from '@meshsdk/core';
import { applyParamsToScript, deserializeAddress } from '@meshsdk/core-cst';
import { builtinByteString, list, PlutusScript } from "@meshsdk/common";

const seedPhrase = ["test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "sauce"];

//Payment splitter
const compiledCode ="5903ac0100003232323232323223225333004323232323253323300a3001300b37540042646464a66601a66e1d2000300e375400c26464a666024602a0042646464646464646464646464a66603666ebcdd3999918008009112999810800880109998018019919198008008021129998120008a5eb804c8c94ccc08ccdd7801002880089981380119802002000981400118130009811800981200099918008009129998100008a5eb804c8cc088004cc00c00ccc01cc0900088ccc07ccdd78008012504a26044002646600200201044a666040002297ae01330213007301f3754600e603e6ea8c088004cc008008c08c00402d3001018000100114a0646600200200444a66603e00229444c94ccc074cdc39bad302200233005533302000414c0103d87a80001300e3302130220044bd70240002660060060022940c088004c8cc004004028894ccc07800452f5c026603e6ea0c8c8c8c8c8c94ccc084cdc424000002266e04008cdc0800806080119980119804807119baf300b30233754601660466ea8c014c08cdd5000803240004466e00004c014dd5980318121baa30063024375400466600266010016466ebcc028c088dd5180518111baa0010054800088cdc000098021bab300530233754004444646600200200844a66604c0022008266006605000266004004605200246600c64a66603e602c60406ea80045300103d87a8000132330010013756604a60446ea8008894ccc090004530103d87a800013232323253330253372291100002153330253371e9101000021301633029375000297ae014c0103d87a8000133006006003375a604c0066eb8c090008c0a0008c098004c8cc004004008894ccc08c0045300103d87a800013232323253330243372291100002153330243371e9101000021301533028374c00297ae014c0103d87a80001330060060033756604a0066eb8c08c008c09c008c0940052000230223023001302000133002002302100122533301a3011301b3754004200226eb4c07cc070dd500111191980080080191299980f0008a5eb804c8c94ccc074c0140084cc084008cc0100100044cc010010004c088008c0800048c070004dd6980d180d8011bac3019001301930190023758602e00260266ea8030c8cc00400403c894ccc05400452f5c026602c60066602c602e00297ae0330020023018001374a90000b1bae3013001300f375400c2c60226024004602000260186ea8008dc3a40042c601a601c004601800260180046014002600c6ea8004526136563758002ae6955ceaab9e5573eae815d0aba21";

async function setup () {
    const yaciProvider = new YaciProvider("http://localhost:8080/api/v1/");
    const mnemonic = seedPhrase;

    const wallet = new MeshWallet({
        networkId: 0,
        fetcher: yaciProvider,
        submitter: yaciProvider,
        key: {
            type: 'mnemonic',
            words: mnemonic,
        },
    });

    const payees = ["addr_test1qrzufj3g0ua489yt235wtc3mrjrlucww2tqdnt7kt5rs09grsag6vxw5v053atks5a6whke03cf2qx3h3g2nhsmzwv3sgml3ed",
            "addr_test1qrh3nrahcd0pj6ps3g9htnlw2jjxuylgdhfn2s5rxqyrr43yzewr2766qsfeq6stl65t546cwvclpqm2rpkkxtksgxuq90xn5f",
            "addr_test1qq5tscksq8n2vjszkdtqe0zn9645246ex3mu88x9y0stnlzjwyqgnrq3uuc3jst3hyy244rrwuxke0m7ezr3cn93u5vq0rfv8t",
            "addr_test1qp5l04egnh30q8x3uqn943d7jsa5za66htsvu6e74s8dacxwnjkm0n0v900d8mu20wlrx55xn07p8pm4fj0wdvtc9kwq7pztl7",
            "addr_test1qryvgass5dsrf2kxl3vgfz76uhp83kv5lagzcp29tcana68ca5aqa6swlq6llfamln09tal7n5kvt4275ckwedpt4v7q48uhex"
    ];

    const plutusData = list(
        payees.map((payee) => {
            const paymentCredential = deserializeAddress(payee).asBase()?.getPaymentCredential().hash
            if (paymentCredential) {
                return builtinByteString(paymentCredential)
            }
        }));

    const parameterizedScript = applyParamsToScript(
        compiledCode,
        [plutusData],
        "JSON",
    );

    const script: PlutusScript = {
        code: parameterizedScript,
        version: "V3",
    };
    const scriptAddress = serializePlutusScript(script, undefined, 0);

    return {
        yaciProvider,
        wallet,
        scriptAddress,
        script,
        payees
    }
}

async function lockAda(lovelaceAmount: string) {
    const { wallet, scriptAddress } = await setup();
    const paymentAddress = await wallet.getChangeAddress();

    const hash = resolvePaymentKeyHash(paymentAddress);
    const datum = {
        alternative: 0,
        fields: [hash],
    };

    const tx = new Transaction({ initiator: wallet }).sendLovelace(
        {
            address: scriptAddress.address,
            datum: { value: datum }
        },
        lovelaceAmount
    );

    const unsignedTx = await tx.build();
    const signedTx = await wallet.signTx(unsignedTx);
    const txHash = await wallet.submitTx(signedTx);
    console.log(`Successfully locked ${lovelaceAmount} lovelace to the script address ${scriptAddress.address}`);
};

async function unlockAda () {
    const { yaciProvider, wallet, scriptAddress, script, payees } = await setup();
    const utxos = await yaciProvider.fetchAddressUTxOs(scriptAddress.address);
    const paymentAddress = await wallet.getChangeAddress();

    const lovelaceForCollateral = "5000000";
    const collateralUtxos = largestFirst(lovelaceForCollateral, await yaciProvider.fetchAddressUTxOs(paymentAddress));
    const pubKeyHash = deserializeAddress(await wallet.getChangeAddress()).asBase()?.getPaymentCredential().hash || '';
    const datum = {
        alternative: 0,
        fields: [pubKeyHash],
    };

    const redeemerData = "Hello, World!";
    const redeemer = { data: { alternative: 0, fields: [redeemerData] } };

    let tx = new Transaction({ initiator: wallet, fetcher: yaciProvider });
    tx.setNetwork('testnet')
    let split = 0;
    for (const utxo of utxos) {
        const amount: Asset[] = utxo.output?.amount;
        if (amount) {
            const lovelace = amount.find((asset) => asset.unit === 'lovelace');
            if (lovelace) {
                split += Math.floor(Number(lovelace.quantity) / payees.length);
            }

            tx = tx.redeemValue({
                value: utxo,
                script: script,
                datum: datum,
                redeemer: redeemer,
            })
        }
    }

    console.log("split: " + split);
    for (const payee of payees) {
        tx = tx.sendLovelace(
            payee,
            split.toString()
        )
    }

    tx = tx.setRequiredSigners([paymentAddress]);
    const unsignedTx = await tx.build();
    try {
        const signedTx = await wallet.signTx(unsignedTx, true);
        const txHash = await wallet.submitTx(signedTx);
        console.log("Unlock Tx hash: " + txHash);
        console.log(`Successfully unlocked the lovelace from the script address ${scriptAddress.address} and split it equally (${split} Lovelace) to all payees.`);
    } catch (error) {
        console.warn(error);
    }
};

//First lock
lockAda("20000000");

await new Promise(resolve => setTimeout(resolve, 2000));

//Unlock
unlockAda();
