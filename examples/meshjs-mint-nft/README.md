# MeshJS NFT Minting Example

Mint an NFT on a local Yaci DevKit devnet using **MeshJS** for client-side transaction building and a **CIP-30 wallet** (yacidevkit, Eternl, etc.) for signing and submission.

## Prerequisites

- Node.js 18+
- A running Yaci DevKit devnet with Store enabled

## Quick Start

1. **Start the devnet** (from the repo root):

   ```bash
   cd applications/cli
   java -Dyaci.store.enabled=true -jar build/libs/yaci-cli.jar create-node -o --start --epoch-length 40
   ```

2. **Install and run**:

   ```bash
   cd examples/meshjs-mint-nft
   npm install
   npm run dev
   ```

3. **Open** the Vite dev server URL (default `http://localhost:5173`).

4. **Connect** the Yaci DevKit wallet, fill in the NFT details, and click **Mint NFT**.

5. **Verify** the minted NFT via Yaci Store API or the Viewer UI.

## How It Works

1. `wallet-sdk.js` (loaded from the CLI at port 10000) injects a CIP-30 wallet extension (`window.cardano.yacidevkit`).
2. `YaciProvider` fetches protocol parameters from Yaci Store (port 8080) for accurate fee/minUTXO calculation.
3. MeshJS `BrowserWallet.enable()` wraps the CIP-30 API.
4. `ForgeScript.withOneSignature(address)` creates a native script minting policy from the wallet's payment key.
5. `Transaction.mintAsset()` registers the mint, then `sendAssets()` creates the NFT output with explicit lovelace for proper coin selection.
6. The CIP-30 wallet signs (`signTx`) and submits (`submitTx`) the transaction.

## MeshJS + Vite Notes

The `vite.config.ts` includes two workarounds for known MeshJS issues:

- **`vite-plugin-node-polyfills`** — MeshJS uses Node.js `Buffer` internally; this polyfills it for the browser.
- **`libsodium-wrappers-sumo` alias** — The ESM build has a broken relative import; the alias redirects to the working CJS build.
