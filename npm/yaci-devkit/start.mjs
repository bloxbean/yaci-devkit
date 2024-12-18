#!/usr/bin/env node

import { spawn } from "node:child_process";
import { platform } from "node:os";
import { fileURLToPath } from 'node:url';

const osType = platform();


/**
 * Find the script for the corresponding os-specific package.
 * 
 * Note: it's tempting to solve this in a few ways that are actually bad:
 * 1. Call `npx <package> <script>` on the corresponding package.
 *    This is bad as there are package managers other than npm
 * 2. Try to compute the path for the package manually.
 *    This is bad as different package managers & runtimes have different folder structures for dependencies
 * 
 * Instead, we use `import.meta.resolve` which returns the path to the `main` entry in `package.json`.
 * Conveniently, we've made the `main` in the packages point directly to the yaci-cli 
 * 
 * @return {Record<string, () => string}
 */
const packageMap = {
    // darwin: () => import.meta.resolve("@bloxbean/yaci-devkit-macos-arm64"),
    linux: () => import.meta.resolve("@bloxbean/yaci-devkit-linux-x64"),
};

if (packageMap[osType] == null) {
    console.error(`Unsupported platform: ${osType}`);
    process.exit(1);
}
const binPath = fileURLToPath(packageMap[osType]());

const child = spawn(binPath, [...process.argv.slice(2)], {
    stdio: "inherit",
});

child.on("close", (code) => {
    process.exit(code ?? 0);
});
