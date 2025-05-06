import {MeshWallet, Transaction, YaciProvider} from "@meshsdk/core";

const provider = new YaciProvider('http://localhost:8080/api/v1/');

const seedPhrase = ["test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "test", "sauce"];

const wallet = new MeshWallet({
    networkId: 0,
    fetcher: provider,
    submitter: provider,
    key: {
        type: "mnemonic",
        words: seedPhrase,
    },
});

const tx = new Transaction({ initiator: wallet });
tx.sendLovelace('addr_test1qrzufj3g0ua489yt235wtc3mrjrlucww2tqdnt7kt5rs09grsag6vxw5v053atks5a6whke03cf2qx3h3g2nhsmzwv3sgml3ed', "2000000");

const unsignedTx = await tx.build();
const signedTx = await wallet.signTx(unsignedTx);
const txHash = await wallet.submitTx(signedTx);

console.log("Tx hash: " + txHash)
