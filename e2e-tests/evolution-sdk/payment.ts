// Simple ADA payment with Evolution SDK using Yaci Store as the Blockfrost provider.
// Transaction building needs protocol parameters, so this also fails while
// https://github.com/bloxbean/yaci-store/issues/1187 is present.
import { Address, Assets, Client, TransactionHash } from "@evolution-sdk/evolution";
import { RECEIVER, SEED_PHRASE, YACI_STORE_URL, devnetChain } from "./devnet";

const client = Client.make(await devnetChain())
  .withBlockfrost({ baseUrl: YACI_STORE_URL, projectId: "Dummy Key" })
  .withSeed({ mnemonic: SEED_PHRASE, accountIndex: 0 });

console.log("Sender:", Address.toBech32(await client.address()));

const signBuilder = await client
  .newTx()
  .payToAddress({ address: Address.fromBech32(RECEIVER), assets: Assets.fromLovelace(5_000_000n) })
  .build();

const txHash = TransactionHash.toHex(await signBuilder.signAndSubmit());
console.log("Submitted:", txHash);

const confirmed = await client.awaitTx(TransactionHash.fromHex(txHash), 1000, 60_000);
console.log("Confirmed:", confirmed);
process.exit(confirmed ? 0 : 1);
