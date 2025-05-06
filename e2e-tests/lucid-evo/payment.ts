import { Lucid, Blockfrost, getAddressDetails } from "@lucid-evolution/lucid";

const lucid = await Lucid(
    new Blockfrost("http://localhost:8080/api/v1", "Dummy Key"),
    "Custom"
    );

const seedPhrase = "test test test test test test test test test test test test test test test test test test test test test test test sauce";
lucid.selectWallet.fromSeed(seedPhrase);

const address = await lucid.wallet().address(); 
const publicKeyHash = await getAddressDetails(address).paymentCredential.hash;

console.log(address);

const tx = await lucid
  .newTx()
  .pay.ToAddress("addr_test1qqm87edtdxc7vu2u34dpf9jzzny4qhk3wqezv6ejpx3vgrwt46dz4zq7vqll88fkaxrm4nac0m5cq50jytzlu0hax5xqwlraql", 
    { lovelace: 5000000n })
  .complete();

const signedTx = await tx.sign.withWallet().complete();  

const txHash = await signedTx.submit();

console.log(txHash);