import * as Evolution from "@evolution-sdk/evolution";
import { fetchDevnetChain, installYaciStoreFetchShim } from "./devnet";

installYaciStoreFetchShim();
const devnet = await fetchDevnetChain();

const seedPhrase =
  "test test test test test test test test test test test test test test test test test test test test test test test sauce";

const client = Evolution.Client.make(devnet)
  .withBlockfrost({ baseUrl: "http://localhost:8080/api/v1", projectId: "Dummy Key" })
  .withSeed({ mnemonic: seedPhrase });

const address = await client.address();
console.log(Evolution.Address.toBech32(address));

const receiver = Evolution.Address.fromBech32(
  "addr_test1qqm87edtdxc7vu2u34dpf9jzzny4qhk3wqezv6ejpx3vgrwt46dz4zq7vqll88fkaxrm4nac0m5cq50jytzlu0hax5xqwlraql",
);

const built = await client
  .newTx()
  .payToAddress({
    address: receiver,
    assets: Evolution.Assets.fromLovelace(5_000_000n),
  })
  .build();

const txHash = await built.signAndSubmit();
console.log(Evolution.TransactionHash.toHex(txHash));
