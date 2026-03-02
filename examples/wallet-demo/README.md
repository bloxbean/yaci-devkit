# CIP-30 Wallet Connection Demo

A sample dApp demonstrating how to connect to any CIP-30 compatible Cardano wallet (Eternl, Lace, Nami, Yoroi, etc.) and the Yaci DevKit wallet for local development.

## Prerequisites

1. **Yaci DevKit running** with Yaci Store enabled (for local development):
   ```bash
   cd applications/cli
   java -Dyaci.store.enabled=true -jar build/libs/yaci-cli.jar create-node -o --start --epoch-length 40
   ```

2. **A local web server** to serve this demo (due to CORS restrictions)

## Running the Demo

### Option 1: Using Python (simplest)

```bash
cd examples/wallet-demo
python3 -m http.server 3000
```

Then open http://localhost:3000/cip30-wallets.html in your browser.

### Option 2: Using Node.js

```bash
npx serve examples/wallet-demo -p 3000
```

### Option 3: Using any static file server

Serve the `examples/wallet-demo` directory on any port.

## What This Demo Shows

1. **Wallet Discovery** - Detects all installed CIP-30 wallets (browser extensions + Yaci DevKit)

2. **Wallet Connection** - Connect to any detected wallet with a single click

3. **Wallet Information** - View network ID, balance, and stake address

4. **CIP-30 API Demo** - Fetch addresses, UTXOs, reward addresses, and sign data

5. **Raw CBOR Responses** - Inspect raw CBOR hex responses from CIP-30 methods

6. **Transfer ADA** - Full CIP-30 transaction flow: build unsigned tx on server, sign with wallet (`signTx`), submit via wallet (`submitTx`)

## Architecture

The demo uses [cardano-connect-with-wallet-core](https://github.com/nicholasmcconnell/cardano-connect-with-wallet-core) for wallet discovery and event-based state management, plus the raw CIP-30 API for direct wallet interaction.

When Yaci DevKit CLI is running, the wallet SDK (`http://localhost:10000/wallet-sdk.js`) injects a `yacidevkit` CIP-30 provider. The transfer flow uses CLI backend APIs:

| Operation | Backend | Port |
|-----------|---------|------|
| Build unsigned tx | CLI | 10000 |
| Assemble signed tx | CLI | 10000 |
| Address decoding | CLI | 10000 |
| Balance queries | Yaci Store | 8080 |
| UTXO queries | Yaci Store | 8080 |
| Transaction submission | Yaci Store | 8080 |
| Transaction signing | CLI | 10000 |
| Data signing | CLI | 10000 |

## CIP-30 Methods Used

- `getNetworkId()` - Returns network ID (0 = testnet)
- `getBalance()` - Returns balance as CBOR hex
- `getUsedAddresses()` - Returns used addresses as hex
- `getUnusedAddresses()` - Returns unused addresses as hex
- `getChangeAddress()` - Returns change address as hex
- `getRewardAddresses()` - Returns stake/reward addresses as hex
- `getUtxos()` - Returns UTXOs as CBOR hex array
- `signTx()` - Signs an unsigned transaction, returns witness set CBOR
- `signData()` - Signs arbitrary data (CIP-8)
- `submitTx()` - Submits a signed transaction, returns tx hash

## Troubleshooting

### No wallets detected
- Install a CIP-30 wallet browser extension (Eternl, Lace, Nami, etc.), or
- Start Yaci DevKit CLI on port 10000 and refresh the page

### Transfer fails at "Build Transaction"
- Ensure the CLI wallet API is responding: `curl http://localhost:10000/api/v1/wallet/health`
- The build-tx endpoint requires Yaci DevKit CLI to be running

### Balance shows 0
- The default accounts are pre-funded when the devnet starts
- Wait a few seconds for the genesis block to be indexed
- Try refreshing after a moment