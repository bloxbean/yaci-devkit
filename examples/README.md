# Yaci DevKit Examples

This folder contains runnable examples for trying Yaci DevKit from the zip
distributions without a separate download.

Start Yaci DevKit before running examples. Most examples expect:

- CLI/admin API: `http://localhost:10000`
- Yaci Store API: `http://localhost:8080/api/v1`
- Built-in wallet page: `http://localhost:10000/wallet`
- Built-in wallet SDK: `http://localhost:10000/wallet-sdk.js`

The wallet page is useful for quickly inspecting and switching local dev wallet
accounts. The wallet SDK injects `window.cardano.yacidevkit` for CIP-30 dApp
testing.

## meshjs-mint-nft

Browser example that mints an NFT on the local devnet using MeshJS and a CIP-30
wallet. It can use the Yaci DevKit wallet injected by `wallet-sdk.js` or another
CIP-30 wallet such as Eternl.

```shell
cd examples/meshjs-mint-nft
npm install
npm run dev
```

Open the Vite URL shown by the command. See
[`meshjs-mint-nft/README.md`](meshjs-mint-nft/README.md) for details.

## wallet-demo

Static browser demo for discovering CIP-30 wallets, connecting to a wallet,
viewing wallet data, signing data, and running a transfer flow.

```shell
cd examples/wallet-demo
python3 -m http.server 3000
```

Open `http://localhost:3000/index.html`. See
[`wallet-demo/README.md`](wallet-demo/README.md) for details.

## evolution-sdk

Bun/TypeScript examples that use Evolution SDK against Yaci DevKit. Includes ADA
payment plus Plutus V2 and Plutus V3 lock/spend flows.

```shell
cd examples/evolution-sdk
bun install
bun run payment
bun run plutus:v2
bun run plutus:v3
```

See [`evolution-sdk/README.md`](evolution-sdk/README.md) for details.

## sutra-elixir

Elixir scripts that use Sutra Cardano against Yaci DevKit. Includes native
script minting, Plutus minting, stake registration, DRep vote delegation, and
stake withdrawal examples.

```shell
cd examples/sutra-elixir
elixir nativescript_mint.exs
```

See [`sutra-elixir/README.md`](sutra-elixir/README.md) for details.
