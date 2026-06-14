# Evolution SDK Example

Examples that exercise Yaci DevKit's devnet with the
[Evolution SDK](https://www.npmjs.com/package/@evolution-sdk/evolution) v0.5.8.

The Evolution SDK is a different package family from `@lucid-evolution/lucid`.
Its API is namespaced under `import * as Evolution from "@evolution-sdk/evolution"`
and follows an Effect-based, lower-level design.

## Prerequisites

- Yaci DevKit running with Yaci Store enabled
- Admin API available at `http://localhost:10000`
- Yaci Store Blockfrost-compatible API available at `http://localhost:8080/api/v1`
- Bun installed

```shell
curl -fsSL https://bun.sh/install | bash
```

## Install Dependencies

```shell
bun install
```

## Run Examples

```shell
bun run payment     # ADA payment
bun run plutus:v2   # Plutus V2 lock and spend
bun run plutus:v3   # Plutus V3 lock and spend
```

You can also run the TypeScript files directly:

```shell
bun payment.ts
bun plutus_v2.ts
bun plutus_v3.ts
```

## How Chain Config Is Wired Up

The examples fetch the live Shelley genesis from Yaci DevKit's admin endpoint:

```text
http://localhost:10000/local-cluster/api/admin/devnet/genesis/shelley
```

`devnet.ts` builds an `Evolution.Chain` descriptor from `systemStart`,
`slotLength`, `networkMagic`, and `epochLength`, so the SDK matches the running
devnet after every reset.

## Yaci Store Response Shim

`devnet.ts` installs a small `fetch` shim that normalizes a few Yaci Store
Blockfrost-compatible responses so Evolution SDK's stricter schemas accept
them.

| Endpoint | Field | Yaci Store sends | Evolution SDK expects |
|---|---|---|---|
| `/epochs/latest/parameters` | `drep_deposit` | number (`500000000`) | string (`"500000000"`) |
| `/epochs/latest/parameters` | `gov_action_deposit` | number | string |
| `/addresses/{addr}/utxos` | `tx_index` | absent | required `Schema.Number` |
| `/addresses/{addr}/utxos` | `block` | absent (`block_number` is present) | required `Schema.String` |

Once these responses are aligned upstream, the shim can be removed.
