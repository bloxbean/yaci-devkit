// Reproduces https://github.com/bloxbean/yaci-store/issues/1187
//
// Yaci Store 2.0.x returns drep_deposit and gov_action_deposit as JSON numbers in
// GET /epochs/latest/parameters. Blockfrost's OpenAPI defines them as strings, and the
// Evolution SDK Blockfrost provider rejects the response with a ParseError.
import { Client } from "@evolution-sdk/evolution";
import { YACI_STORE_URL, devnetChain } from "./devnet";

let failed = false;

// 1. Check the raw JSON types returned by Yaci Store
const raw = await (await fetch(`${YACI_STORE_URL}/epochs/latest/parameters`)).json();
for (const field of ["key_deposit", "pool_deposit", "drep_deposit", "gov_action_deposit"]) {
  const value = raw[field];
  const ok = typeof value === "string";
  if (!ok) failed = true;
  console.log(`${ok ? "OK  " : "FAIL"} ${field} = ${JSON.stringify(value)} (${typeof value}, expected string)`);
}

// 2. Fetch protocol parameters through the Evolution SDK Blockfrost provider
const client = Client.make(await devnetChain()).withBlockfrost({ baseUrl: YACI_STORE_URL, projectId: "Dummy Key" });
try {
  const params = await client.getProtocolParameters();
  console.log("OK   Evolution getProtocolParameters", {
    drepDeposit: params.drepDeposit,
    govActionDeposit: params.govActionDeposit,
  });
} catch (e) {
  failed = true;
  console.log("FAIL Evolution getProtocolParameters");
  console.error(e);
}

process.exit(failed ? 1 : 0);
