import { Lucid, Blockfrost, Kupmios, SpendingValidator, validatorToAddress, Data, Constr, getAddressDetails, OutRef} from "@lucid-evolution/lucid";

const network = "Custom";

const lucid = await Lucid(
    new Blockfrost("http://localhost:8080/api/v1", "Dummy Key"),
    "Custom"
    );


const seedPhrase = "test test test test test test test test test test test test test test test test test test test test test test test sauce";
lucid.selectWallet.fromSeed(seedPhrase);

const address = await lucid.wallet().address();
const publicKeyHash = await getAddressDetails(address).paymentCredential?.hash;

if (!publicKeyHash) throw new Error("Could not get public key hash");

console.log(address);

const spend_val: SpendingValidator = {
  type: "PlutusV3",
  script: "5857010000323232323225333002323232323253330073370e900118041baa00113232324a26018601a004601600260126ea800458c024c028008c020004c020008c018004c010dd50008a4c26cacae6955ceaab9e5742ae89"
};

const scriptAddress = validatorToAddress(network, spend_val);
console.log(scriptAddress);

const datum = Data.to(new Constr(0, [publicKeyHash]));

const tx = await lucid
  .newTx()
  .pay.ToContract(scriptAddress, { kind: "inline", value: datum }, { lovelace: 10_000_000n })
  .complete();

const signedTx = await tx.sign.withWallet().complete();

const txHash = await signedTx.submit();

console.log(txHash);

await new Promise(resolve => setTimeout(resolve, 3000));

console.log("Spend script UTxO ----");

const DatumSchema = Data.Object({
  owner: Data.Bytes(),
});
type DatumType = Data.Static<typeof DatumSchema>;
const DatumType = DatumSchema as unknown as DatumType;

// Find the UTxO we want to spend
const allUTxOs = await lucid.utxosAt(scriptAddress);
const ownerUTxO = allUTxOs.find((utxo) => {
  if (utxo.datum) {
    const datum = Data.from(utxo.datum, DatumType);
    return datum.owner === publicKeyHash;
  }
});

if (!ownerUTxO) throw new Error("Could not get utxos");
const redeemer = Data.to(new Constr(0, [Buffer.from("Hello, World!").toString("hex")]));

// Spend script UTxO
const tx2 = await lucid
  .newTx()
  .collectFrom([ownerUTxO], redeemer) // Provide the redeemer argument
  .attach.SpendingValidator(spend_val) // Attach validator
  .complete({
    localUPLCEval: false,
  });

// const evalRes = await provider.evaluateTx(tx2.toCBOR());

console.log(tx2.toCBOR());

const signedTx2 = await tx2.sign.withWallet().complete();

console.log("Signed tx2 ---->>>>>>>>>>>>>>>>>>> " + signedTx2.toCBOR());

const txHash2 = await signedTx2.submit();

console.log(txHash2);
