# Yaci Viewer

A live Cardano explorer for your Yaci DevKit devnet, or any [Yaci Store](https://github.com/bloxbean/yaci-store).

- **Live dashboard:** new blocks slide in as they're made, with block time, transaction and block-load trends,
  epoch progress and ADA supply.
- **Search everything:** press <kbd>/</kbd> or <kbd>⌘</kbd><kbd>K</kbd> (or just paste) a block number or hash,
  transaction, address, stake address, pool, DRep, policy, asset, fingerprint, script, datum or epoch.
- **Transactions you can read:** inputs flow into outputs, scripts show their execution budget, redeemers and datums
  are decoded from CBOR, plus mints, metadata, collateral, reference inputs and the raw JSON.
- **Script Lab:** decode any Plutus script into searchable UPLC, see the builtins it uses and verify its hash. It runs
  in your browser with the [julc](https://github.com/bloxbean/julc) WebAssembly engine.
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
  `SCRIPT_LAB_HASH=<plutus script hash> PW_CHANNEL=chrome npx playwright test script-lab`.

SvelteKit 2, Svelte 5 and Tailwind. Shared code lives in `src/lib`: `api/` (Yaci Store client), `live/` (WebSocket
with a polling fallback), `ui/` (components), `shell/` (layout, navigation, search), `julc/` (Script Lab) and `format.ts`, `cbor.ts`,
`bech32.ts`.
