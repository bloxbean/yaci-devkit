# Evolution SDK Compatibility Tests

Examples that exercise Yaci DevKit's devnet with the
[Evolution SDK](https://www.npmjs.com/package/@evolution-sdk/evolution) v0.5.8.

> **Note**: The Evolution SDK is a different package family from
> `@lucid-evolution/lucid` (used in the sibling `lucid-evo/` folder). Its API is
> namespaced under `import * as Evolution from "@evolution-sdk/evolution"` and
> follows an Effect-based, lower-level design — there is no `Lucid` facade.

## Install Bun

```
curl -fsSL https://bun.sh/install | bash
```

## Install Dependencies

```shell
bun install
```

## Run Tests

```shell
bun payment.ts      # ADA payment — works end-to-end
bun plutus_v2.ts    # Plutus V2 always-succeeds — lock works, spend currently blocked (see below)
bun plutus_v3.ts    # Plutus V3 always-succeeds — same as v2
```

## How chain config is wired up

The examples fetch the live shelley genesis from Yaci DevKit's admin endpoint
(`http://localhost:10000/local-cluster/api/admin/devnet/genesis/shelley`) and
build an `Evolution.Chain` descriptor from `systemStart`, `slotLength`,
`networkMagic`, and `epochLength`. See `devnet.ts`. The same approach is
documented for `@lucid-evolution/lucid` at
<https://devkit.yaci.xyz/tutorials/lucid-evolution/overview>.

## Yaci Store / Evolution SDK schema gaps

`devnet.ts` installs a small `fetch` shim that normalizes two Yaci Store
Blockfrost-compatible responses so Evolution SDK's stricter schemas accept
them. Without the shim, every request fails with an Effect Schema `ParseError`.

| Endpoint | Field | Yaci Store sends | Evolution SDK expects |
|---|---|---|---|
| `/epochs/latest/parameters` | `drep_deposit` | number (`500000000`) | string (`"500000000"`) |
| `/epochs/latest/parameters` | `gov_action_deposit` | number | string |
| `/addresses/{addr}/utxos` | `tx_index` | absent | required `Schema.Number` |
| `/addresses/{addr}/utxos` | `block` | absent (sends `block_number`) | required `Schema.String` |

These are reasonable fixes to make upstream in Yaci Store — once those land,
the shim can be removed.

## Plutus spend-phase blocker

`payment.ts` and the lock phase of `plutus_v{2,3}.ts` complete successfully
against a default devnet. The spend phase fails at build time because
Evolution SDK calls `POST /utils/txs/evaluate/utxos` on the Blockfrost provider
to evaluate script execution units, and Yaci Store's implementation of that
endpoint returns `500 Internal Server Error` (empty body). For example:

```
$ curl -X POST -H 'Content-Type: application/json' \
       -d '{"cbor":"...","additionalUtxoSet":[]}' \
       http://localhost:8080/api/v1/utils/txs/evaluate/utxos
{"status_code":500,"error":"Internal Server Error","message":"Error evaluating transaction"}
```

Workarounds attempted:

- `BuildOptions.evaluator: createAikenEvaluator` (from `@evolution-sdk/aiken-uplc`)
  bypasses the remote endpoint, but currently fails on the Lucid-style
  CBOR-wrapped always-succeeds script bytes with `cannot evaluate an open term:
  Term i_2`. Likely a CBOR unwrap-depth mismatch between how the SDK stores
  PlutusV2 bytes and how the Aiken evaluator decodes them — needs upstream
  investigation in `@evolution-sdk/aiken-uplc`.
- Kupmios (`Ogmios` + `Kupo`) provider works in principle, but neither service
  is started by `create-node` by default in this devnet.

For now, `payment.ts` is the proven end-to-end path; the Plutus examples will
start running once either Yaci Store's `evaluateTx` endpoint is fixed or a
working local evaluator path is wired up.
