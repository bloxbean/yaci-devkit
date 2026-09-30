# Evolution SDK Compatibility Tests

Tests [Evolution SDK](https://github.com/IntersectMBO/evolution-sdk) (`@evolution-sdk/evolution@0.5.14`)
against a running Yaci DevKit, using Yaci Store as the Blockfrost provider.

## Prerequisites

- Yaci DevKit running with Yaci Store enabled (Store on port 8080, CLI admin API on port 10000)
- [Bun](https://bun.sh): `curl -fsSL https://bun.sh/install | bash`

Override the endpoints if needed:

```shell
export YACI_STORE_URL=http://localhost:8080/api/v1
export YACI_ADMIN_URL=http://localhost:10000/local-cluster/api
```

## Run Tests

```shell
bun install
```

1. Protocol parameters (reproduces [yaci-store#1187](https://github.com/bloxbean/yaci-store/issues/1187))

```shell
bun protocol_params.ts
```

Checks that `drep_deposit` and `gov_action_deposit` in `/epochs/latest/parameters` are strings, as in the
Blockfrost OpenAPI spec, and that Evolution's Blockfrost provider can parse the response. With Yaci Store 2.0.x
it fails with `ParseError ... ["drep_deposit"] Expected string, actual 500000000`.

2. Payment

```shell
bun payment.ts
```

Sends 5 ADA from DevKit's default account and waits for confirmation. It also fails while #1187 is present,
because transaction building fetches protocol parameters first.

3. PlutusV3 payment splitter

```shell
bun plutus_v3.ts
```

Same parameterized validator and flow as `meshjs/payment_splitter_plutusV3.ts`: applies the payees' key hashes
to the script, locks 20 ADA with the owner's key hash as inline datum, then unlocks it (redeemer, owner as
required signer) and splits it equally between the five payees. Script evaluation goes through Yaci Store's
`/utils/txs/evaluate`.
