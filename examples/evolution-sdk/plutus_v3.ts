import * as Evolution from "@evolution-sdk/evolution";
import { fetchDevnetChain, installYaciStoreFetchShim } from "./devnet";

installYaciStoreFetchShim();
const devnet = await fetchDevnetChain();

const seedPhrase =
  "test test test test test test test test test test test test test test test test test test test test test test test sauce";

const client = Evolution.Client.make(devnet)
  .withBlockfrost({ baseUrl: "http://localhost:8080/api/v1", projectId: "Dummy Key" })
  .withSeed({ mnemonic: seedPhrase });

const ownerAddress = await client.address();
const ownerBech32 = Evolution.Address.toBech32(ownerAddress);
console.log(ownerBech32);

const ownerDetails = Evolution.Address.getAddressDetails(ownerBech32);
if (!ownerDetails || ownerDetails.paymentCredential._tag !== "KeyHash") {
  throw new Error("Owner address must have a key-hash payment credential");
}
const ownerKeyHash = ownerDetails.paymentCredential;
const publicKeyHash = Evolution.KeyHash.toHex(ownerKeyHash);

// Compiled Plutus V3 spending validator from the Lucid Evolution example.
const spendScript = new Evolution.PlutusV3.PlutusV3({
  bytes: new Uint8Array(
    Buffer.from(
      "5857010000323232323225333002323232323253330073370e900118041baa00113232324a26018601a004601600260126ea800458c024c028008c020004c020008c018004c010dd50008a4c26cacae6955ceaab9e5742ae89",
      "hex",
    ),
  ),
});

const scriptHash = Evolution.ScriptHash.fromScript(spendScript);
const scriptAddress = new Evolution.Address.Address({
  networkId: devnet.id,
  paymentCredential: scriptHash,
});
console.log(Evolution.Address.toBech32(scriptAddress));

const datumData = Evolution.Data.constr(0n, [
  new Uint8Array(Buffer.from(publicKeyHash, "hex")),
]);
const datum = new Evolution.InlineDatum.InlineDatum({ data: datumData });

const lockTx = await client
  .newTx()
  .payToAddress({
    address: scriptAddress,
    assets: Evolution.Assets.fromLovelace(10_000_000n),
    datum,
  })
  .build();

const lockHash = await lockTx.signAndSubmit();
console.log(Evolution.TransactionHash.toHex(lockHash));

await new Promise((resolve) => setTimeout(resolve, 3000));

console.log("Spend script UTxO ----");

const scriptUtxos = await client.getUtxos(scriptAddress);
const ownerUtxo = scriptUtxos.find((utxo) => {
  if (utxo.datumOption && utxo.datumOption._tag === "InlineDatum") {
    const data = utxo.datumOption.data;
    if (Evolution.Data.isConstr(data) && data.index === 0n) {
      const owner = data.fields[0];
      if (owner instanceof Uint8Array) {
        return Buffer.from(owner).toString("hex") === publicKeyHash;
      }
    }
  }
  return false;
});

if (!ownerUtxo) throw new Error("Could not find locked UTxO at script address");

const redeemerData = Evolution.Data.constr(0n, [
  new Uint8Array(Buffer.from("Hello, World!", "utf8")),
]);

const spendTx = await client
  .newTx()
  .collectFrom({ inputs: [ownerUtxo], redeemer: redeemerData })
  .attachScript({ script: spendScript })
  .addSigner({ keyHash: ownerKeyHash })
  .build();

const spendHash = await spendTx.signAndSubmit();
console.log(Evolution.TransactionHash.toHex(spendHash));
