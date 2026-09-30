# Yaci Devkit

A set of development tools for building on Cardano by creating a local devnet.

### Installation

```bash
npm i -g @bloxbean/yaci-devkit
```

### **Commands**

1. **Download and Start Cardano Node**
   ```bash
   yaci-devkit up
   ```
    - Downloads the Cardano node (if not already downloaded) and starts a new devnet.

2. **Interactive Mode**
   ```bash
   yaci-devkit up --interactive
   ```
    - Downloads the node (if not already downloaded), starts a new devnet, and launches the `yaci-cli` prompt.

3. **Yaci Store Mode (Blockfrost-Compatible API Endpoints)**
   ```bash
   yaci-devkit up --enable-yaci-store
   ```
    - Downloads and starts the Cardano node, Yaci Store, and Ogmios (used for script cost evaluation).
    - Add `--interactive` to enter the `yaci-cli` prompt.

4. **Kupomios Mode (Ogmios + Kupo)**
   ```bash
   yaci-devkit up --enable-kupomios
   ```
    - Downloads and starts the Cardano node, Ogmios, and Kupo.

> **Note on platforms**: Ogmios and Kupo are published for Linux (x86_64, arm64) and macOS arm64. On macOS x86_64 and Windows they are unavailable; there Yaci Store falls back to the embedded `scalus` script cost evaluator, or you can use the Docker distribution.

For a full list of Yaci CLI commands, check the Yaci DevKit [documentation](https://devkit.yaci.xyz/commands).
