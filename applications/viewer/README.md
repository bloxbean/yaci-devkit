# Yaci Viewer

A live Cardano explorer for your Yaci DevKit devnet, or any [Yaci Store](https://github.com/bloxbean/yaci-store).

- **Live dashboard:** new blocks slide in as they're made, with block time, transaction and block-load trends,
  epoch progress and ADA supply.
- **Search everything:** press <kbd>/</kbd> or <kbd>⌘</kbd><kbd>K</kbd> (or just paste) a block number or hash,
  transaction, address, stake address, pool, DRep, policy, asset, fingerprint, script, datum or epoch.
- **Transactions you can read:** inputs flow into outputs, scripts show their execution budget, redeemers and datums
  are decoded from CBOR, plus mints, metadata, collateral, reference inputs and the raw JSON.
- **Script Lab:** in your browser, with the [julc](https://github.com/bloxbean/julc) WebAssembly engine:
  - decode any Plutus script into searchable UPLC, see the builtins it uses and verify its hash;
  - re-run every script of a transaction with the cost model of its epoch, compare the budget with what was declared
    on chain, and see the exact ScriptContext the validator received;
  - when Yaci Store saves transaction CBOR (`store.transaction.save-cbor`), replays use the transaction's own bytes
    (exact redeemers, witnessed datums, outputs, certificates and proposals) and a CBOR tab shows those bytes as a tree,
    checked against the transaction id; otherwise the transaction is rebuilt from the store's JSON, and every replay
    says which source it used;
  - step through any run in a time-travel debugger: step, over, out, continue and back, breakpoints on lines,
    builtins, traces and failures, environment, frames and budget per step, and links to an exact step
    (`/transactions/<hash>/debug/<purpose>/<index>?step=N`);
  - fork a transaction in the what-if lab (`/transactions/<hash>/fork`): edit redeemers, datums, signers, the validity
    range, outputs, mint, withdrawals, fee, the cost model's epoch or the protocol version, and every script runs again
    as you type, next to the on-chain result; debug the forked runs and export any run as a JSON test fixture;
  - watch scripts live in the script monitor (`/scripts/monitor`): every new script transaction is re-run as its
    block arrives, with its budget against the per-transaction limits (made for devnets; sampled on public networks);
  - see a script's recent activity (budgets, redeemer variants) on its page, and attach its CIP-57 blueprint
    (`plutus.json`) to show datums and redeemers with type, variant and field names.
  - check a draft parameter change or treasury withdrawal against the constitution's guardrail script before it is
    submitted (`/governance/guardrail`): the verdict updates as you type, says which parameter the guardrail rejects,
    and the run can be debugged and shared as a link; proposal pages show what a change does and run its guardrail.
  - try a validator against real chain state in the validator lab (`/scripts/lab`): write it in Java (compiled in the
    browser by julc, with typed datum and redeemer views and its `plutus.json`), paste a compiled script or pick one on
    chain, then spend a real UTxO as if it were locked at it (or mint, or withdraw) with your redeemer, datum, signers,
    validity range and withdrawals. The verdict, budget and traces update as you type; a rejected Java validator shows
    the line it failed on, and runs open in the UPLC debugger or the (experimental) Java source debugger.
- Addresses, stake accounts, assets, epochs and protocol parameters, pools, rewards, certificates, Ada pots and
  Conway governance.
- Light and dark themes; works on a phone.

## Run it

```bash
npm install -g @bloxbean/yaci-viewer
yaci-viewer
```

Yaci Viewer reads from Yaci Store at `http://localhost:8080/api/v1` and serves the UI at `http://localhost:3000`.
Set `PORT` to use another port, for example `PORT=5173 yaci-viewer` to match the Docker distribution.

The Docker distribution of Yaci DevKit already includes Yaci Viewer at `http://localhost:5173`.

## Configuration

Set these in the environment, or in a `.env` file in the directory you start Yaci Viewer from:

| Variable | Default | What it is |
|---|---|---|
| `PUBLIC_INDEXER_BASE_URL` | `http://localhost:8080/api/v1` | Yaci Store REST API, used by the server |
| `PUBLIC_INDEXER_CLIENT_BASE_URL` | same as above | Yaci Store as the browser sees it, when that differs (e.g. Docker) |
| `PUBLIC_INDEXER_WS_URL` | `ws://localhost:8080/ws/liveblocks` | Live updates. Leave empty for a store without the live module; the viewer polls instead |
| `PUBLIC_MAINTENANCE_MSG` | | Optional banner shown on every page |
| `PUBLIC_JULC_BASE_URL` | `/julc/` | Where the browser loads the julc engine from, if not served by Yaci Viewer itself |
| `PORT`, `HOST` | `3000`, `0.0.0.0` | Where Yaci Viewer listens |
| `HEAVY_ADDRESS_GUARD` | `true` | Protects a large Yaci Store from busy addresses (below). `false` restores the plain behaviour |
| `YACI_ANALYTICS` | `auto` | `off` stops Yaci Viewer from using the store's analytics query API (below) even when the store has it |
| `NAMES` | `auto` | `off` turns off names on Mainnet (below). On other networks they are always off |
| `NAMES_DIR` | `.cache/names` | Where the names are saved between restarts. Without a writable folder they are kept in memory only |

### Busy addresses on a large Yaci Store

On a Mainnet-sized Yaci Store, `/addresses/{a}/amounts`, `/addresses/{a}/transactions` and `/accounts/{stake}` can take
minutes to hours for busy addresses, and the store keeps running the query after the viewer gives up
([yaci-store#1269](https://github.com/bloxbean/yaci-store/issues/1269)). So Yaci Viewer:

- shows address and stake pages at once, and streams the slow parts in, each with its own loading and error state;
- never calls `/amounts`: without `/balance`, it sums the address's UTxOs when one page (100) holds them all; past that
  it takes the exact balance from the store's Blockfrost-compatible API (`/blockfrost/addresses/{a}`) when the store has
  it, and only if that fails sums up to 500 UTxOs and shows a lower bound ("≥");
- reads UTxOs and transactions from the Blockfrost-compatible API too, when the store has it: the store stops those
  queries itself after 15 s, so a busy address leaves nothing running (the devkit's own store has no such API, and gets
  the native endpoints);
- opens address pages on the UTxOs tab (the transactions are the last tab, loaded only when opened);
- remembers an endpoint that timed out for an address for 15 minutes and doesn't ask again (a UTxO page for 2), shares
  identical requests in flight, keeps stake accounts for a minute, and runs at most 4 such calls at once;
- keeps crawlers off per-address pages (`robots.txt`).

The limits are in `src/lib/server/heavy-guard.ts`. Set `HEAVY_ADDRESS_GUARD=false` once the store cancels abandoned
queries.

When the store has its analytics query API enabled (`yaci.store.analytics.query.rest-api-enabled`, not available on
the devkit's own store), Yaci Viewer uses it where a regular endpoint is too slow for a busy **stake account**: what it
controls and its pool, as of the last export, and its **rewards**, 20 epochs a page. (Address pages don't use it: the
answers would be partial, and DuckDB isn't meant to serve every page view.)

Those sections are labelled "From Yaci Store analytics" with how fresh the data is. Analytics queries are bounded by the
store's own 30 s timeout, at most two run at once, and answers are kept for 10 minutes. Yaci Viewer checks whether the
store has the API every 10 minutes (one `SELECT 1`), and does nothing different when it doesn't.

The same API adds charts and numbers, again only where the store has it:

| Page | What |
|---|---|
| Dashboard | Transactions, script runs, script CPU and fees per day (90 days); treasury and reserves; pools making blocks; the most-run scripts (7 days) |
| Governance | Voting power: the 10 largest DReps and options, with their share |
| Script | Runs, CPU steps and the costliest run per day (90 days) |
| Pool | Blocks, active stake and delegators per epoch (50 epochs) |
| Stake account | Rewards and active stake per epoch (50 epochs) |

They read the store's daily exports, which DuckDB aggregates in well under a second. Yaci Viewer keeps the answers in
memory and serves them while it refreshes them in the background: network-wide ones for an hour, one script's, pool's or
account's for 30 minutes (`src/lib/server/insights.ts`). Pages stream them in, so they never hold up a page.

### Names on Mainnet

On Mainnet, Yaci Viewer shows names next to ids (never instead of them):

| Name | Where it comes from | Shown on |
|---|---|---|
| dApp label for a script, e.g. "Minswap Order Contract v2" | [Strica's Cardano Contracts Registry](https://github.com/StricaHQ/cardano-contracts-registry) and [Cardano Fans' registry](https://github.com/Cardano-Fans/crfa-offchain-data-registry) (Strica's wins on overlap) | Transaction inputs, outputs and scripts; address, stake, script and policy pages; most-run scripts |
| DRep name | `givenName` in the DRep's CIP-119 metadata | DRep list and page, votes, certificates, voting power, stake accounts |
| Pool ticker | The pool's CIP-6 metadata | Block producer, stake accounts, delegations, epoch stakes, rewards, pool certificates, votes |
| Governance action title | `title` in the action's CIP-108 metadata | Proposals, votes, the action's page, a DRep's votes |
| Token ticker and decimals | The [Cardano token registry](https://tokens.cardano.org) (CIP-26) | Every token chip (quantities with decimals), the asset page |

DRep names, pool tickers and action titles are what their owners wrote; Yaci Viewer keeps one only when its document
matches the hash on chain, and labels it as self-declared. Documents are fetched by the server with the same
protections as the anchor viewer: public addresses only, size and time limits.

The server loads them in the background after it starts, after checking with the store that the network is Mainnet
(`network_magic` 764824073); on any other network nothing is fetched. dApp labels are refreshed daily, DRep names and
action titles every 6 hours, pool tickers every 12 (the first run reads every pool registration, a few minutes); a
document that couldn't be read is tried again after a day (pools: three). Tokens are looked up when a page shows them
and kept for a week. Everything is saved in `NAMES_DIR`, so after a restart the names are there at once and only what
changed is read again. Until they are loaded, pages show the ids as before.

The base and WebSocket URLs can also be passed on the command line:

```bash
yaci-viewer --indexerBaseUrl http://my-store:8080/api/v1 --indexerWsUrl ws://my-store:8080/ws/liveblocks
```

## Develop

```bash
npm install
PUBLIC_INDEXER_BASE_URL=http://localhost:8080/api/v1 npm run dev   # http://localhost:5173
npm run check        # svelte-check
npm run test:unit    # vitest
npm test             # Playwright smoke tests (needs a running Yaci Store)
npm run build        # production build in build/, run with `node build`
```

### Script Lab and the julc engine

Script Lab uses the julc WebAssembly engine. It is not built here: `npm run dev` and `npm run build` run
`scripts/fetch-julc.mjs`, which downloads the julc release pinned in `julc.lock.json`, checks its SHA-256 and unpacks it
into `static/julc/` (git-ignored). The engine is only downloaded by the browser when a user decodes a script (about
5.5 MB with compression, then cached).

- A production build fails if the engine can't be installed. Set `JULC_SKIP=1` to build without Script Lab (its
  panels then say so), or `JULC_ARCHIVE=/path/to/julc-wasm-<version>.tar.gz` to install from a local copy.
- To upgrade julc, change `tag`, `version`, `file` and `sha256` in `julc.lock.json` together; the checksum is in the
  release's `julc-wasm-<version>-SHA256SUMS.txt`.
- The engine needs WebAssembly GC and exception handling (exnref): current Chrome, Edge, Firefox and Safari. Other
  browsers get a message instead. Playwright's bundled Chromium is too old, so run the Script Lab test with Chrome:
  `SCRIPT_LAB_HASH=<plutus script hash> SCRIPT_LAB_TX=<tx with redeemers> SCRIPT_LAB_RUN=<purpose/index> SCRIPT_LAB_PROPOSAL=<tx>_<index> PW_CHANNEL=chrome npx playwright test script-lab`.
- Re-runs rebuild the transaction from Yaci Store data (`src/lib/julc/replay/`). Until julc builds contexts exactly
  as the ledger does ([julc#244](https://github.com/bloxbean/julc/issues/244)), the viewer corrects julc's context
  (asset order, the other redeemers, datums given by hash, Plutus V1 shapes, the governance action of a proposal) and
  runs the script again; each result says what was corrected. Most certificate kinds aren't replayed yet, and Plutus V1
  (pre-2023 history) is partly reproduced.

SvelteKit 2, Svelte 5 and Tailwind. Shared code lives in `src/lib`: `api/` (Yaci Store client), `live/` (WebSocket
with a polling fallback), `ui/` (components), `shell/` (layout, navigation, search), `julc/` (Script Lab) and `format.ts`, `cbor.ts`,
`bech32.ts`.
