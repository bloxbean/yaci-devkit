# Declarative Scenarios

Describe Cardano transactions and devnet orchestration in YAML and run them against a Yaci
DevKit devnet — no SDK code, no keys in the file. Transaction bodies are powered by
`cardano-client-lib`'s **TxPlan** and **TxFlow**. DevKit L3 scenarios wrap those CCL documents
with time, assertions, topups, snapshots, rollback, loops, groups, and logs.

## Running a scenario

```bash
# CLI (inside the devnet shell)
run-scenario --file examples/scenarios/payment.yaml

# REST
curl -X POST http://localhost:10000/local-cluster/api/scenarios/run \
  -H "Content-Type: application/json" \
  -d "$(python3 -c "import json;print(json.dumps({'yaml':open('examples/scenarios/payment.yaml').read()}))")"
```

The MCP tool `devnet_run_scenario(yaml)` runs the same engine, so an AI agent can author and
execute scenarios directly.

## Signer references (no keys in YAML)

Yaci DevKit auto-wires a signer registry from the 20 pre-funded default accounts:

| Reference | Resolves to |
|---|---|
| `account://acc0` … `account://acc19` | the default accounts (test mnemonic) |
| `policy://default` | a deterministic single-sig native-script policy keyed by account 0 |

Reference them in a TxPlan `context.signers` block (and `from_ref`/`fee_payer_ref`) or a TxFlow
step's `context.signers`. The engine resolves them at build time.

## Document Types

- **TxPlan** — top-level `transaction:`; one (or grouped) on-chain transaction. Returns one tx hash.
- **TxFlow** — top-level `flow:`; ordered `steps:` with `depends_on:` UTXO chaining, confirmation
  tracking and rollback recovery. Returns a tx hash per step.
- **DevKit L3 Scenario** — top-level `scenario:`; ordered orchestration steps around one or more
  CCL TxPlan/TxFlow operations.

The engine auto-detects the format from the top-level key.

## L3 scenario wrapper

Use L3 when the scenario needs behavior outside a single transaction flow: waiting for blocks or
epochs, asserting state, looping, snapshot/restore, rollback, or topups.

```yaml
scenario:
  name: payment-with-assert
  steps:
    - tx:
        file: examples/scenarios/payment.yaml
      id: payment
    - assert:
        tx: { ref: payment, status: success }
```

Operation steps preserve the CCL boundary:

- `tx` and `flow` can be a file path, `{ file, with }`, `{ inline }`, or an inline CCL document.
- DevKit does not parse CCL intents, datums, signers, dependencies, or chaining.
- CCL TxFlow `context.chaining_mode`, confirmation, retry, and rollback recovery stay in the CCL YAML.

Implemented L3 actions:

- `tx`, `flow`
- `advance` / `wait`
- `assert`
- `topup`
- `snapshot`, `restore`, `rollback`, `reset`
- `repeat`, `for_each`, `parallel`, `group`, `log`

### TxFlow chaining mode (same-block inclusion)

By default a TxFlow runs `SEQUENTIAL` — each step is confirmed before the next, so dependent
transactions land in **separate blocks** (reproducible state). To put dependent steps in the
**same block**, set `chaining_mode: BATCH` in the TxFlow's top-level `context:` block (native
cardano-client-lib execution context, ≥ 0.8.0-pre5):

```yaml
version: "1.0"
context:
  chaining_mode: BATCH    # SEQUENTIAL (default) | PIPELINED | BATCH
  confirmation: devnet    # optional: defaults | devnet | testnet | quick (or inline fields)
flow:
  id: fund-and-forward
  steps: [ ... ]
```

`BATCH` computes transaction hashes client-side, so a later step can reference an earlier step's
output before it is confirmed — giving the best chance of same-block inclusion (ideal on fast
devnets). Only meaningful for TxFlow; ignored for TxPlan.

## State determinism

Seed scenarios are **state-deterministic** (the resulting UTxO set / balances / datums are
reproducible across runs) when you pin the non-deterministic knobs: explicit validity intervals,
explicit/deterministic input selection, and sequential step ordering (TxFlow runs `SEQUENTIAL`).
Byte-identical tx hashes (hash-determinism) are not guaranteed; reference earlier outputs via
TxFlow `depends_on` or TxPlan `utxo_filter` rather than hardcoding `tx_hash#idx`.

## Examples

- `payment.yaml` — TxPlan ADA payment (acc0 → acc1).
- `flow-payment.yaml` — minimal TxFlow; extend with more `steps:` + `depends_on:` to chain.
- `flow-batch.yaml` — TxFlow with native CCL `context.chaining_mode: BATCH`.
- `l3-payment-assert.yaml` — DevKit L3 wrapper around a CCL TxPlan.
- `l3-repeat-topup.yaml` — DevKit L3 loop/topup/assert example.

Seeding a fresh devnet is supported with `create-node --seed <file>`. The REST create endpoint
also accepts `seedYaml`.
