#!/usr/bin/env node

import { spawn } from "node:child_process";
import { platform, homedir } from "node:os";
import { fileURLToPath } from "node:url";
import { dirname, resolve, join } from "node:path";
import { existsSync, readFileSync, writeFileSync, unlinkSync, mkdirSync } from "node:fs";

// import { createHash } from "node:crypto";

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
    darwin: () => import.meta.resolve("@bloxbean/yaci-devkit-macos-arm64"),
    linux: () => import.meta.resolve("@bloxbean/yaci-devkit-linux-x64"),
};

if (packageMap[osType] == null) {
    console.error(`Unsupported platform: ${osType}`);
    process.exit(1);
}
const binPath = fileURLToPath(packageMap[osType]());

const devkitDir = dirname(binPath);

const configPath = resolve(devkitDir, 'config');

// Determine the path to the user's `.yaci-cli` directory
const yaciCliHome = join(homedir(), ".yaci-cli");
if (!existsSync(yaciCliHome)) {
    mkdirSync(yaciCliHome, { recursive: true });
}

// Path for the PID file
const pidFilePath = join(yaciCliHome, "yaci-cli.pid");

function killProcessTree(pid) {
    try {
        if (osType === "linux" || osType === "darwin") {
            // Use `pkill` to kill the entire process tree
            spawn("pkill", ["-TERM", "-P", pid], { stdio: "ignore" });
            process.kill(pid, "SIGKILL"); // Kill the main process
        } else {
            console.error("Unsupported platform for process termination.");
        }
    } catch (err) {
        console.error(`Failed to kill process tree for PID ${pid}: ${err.message}`);
    }
}

if (existsSync(pidFilePath)) {
    try {
        const existingPid = parseInt(readFileSync(pidFilePath, "utf-8"), 10);
        if (!isNaN(existingPid)) {
            console.log(`Killing existing yaci-cli process and its subprocesses with PID: ${existingPid}`);
            killProcessTree(existingPid); // Kill the process tree
            unlinkSync(pidFilePath); // Remove the stale PID file
        }
    } catch (err) {
        console.error(`Failed to clean up existing yaci-cli process: ${err.message}`);
    }
}

// const tmpSuffix = createHash('md5').update(workDir).digest("hex");
// const yaciCLIHome = resolve("/tmp", ".yaci-cli" + tmpSuffix )

const additionalConfig = "optional:file:" + configPath + "/application.properties,"
    + "optional:file:" + configPath + "/download.properties,"
    + "optional:file:" + configPath + "/node.properties"

const additionalConfigArg = "-Dspring.config.import=" + additionalConfig;

const child = spawn(binPath, [additionalConfigArg, ...process.argv.slice(2)], {
    stdio: "inherit",
    env: {
        // YACI_CLI_HOME: yaciCLIHome
    },

});


writeFileSync(pidFilePath, child.pid.toString());
console.log(`yaci-cli process started with PID: ${child.pid}`);

// Handle process exit
child.on("close", (code) => {
    console.log(`yaci-cli process exited with code: ${code}`);
    try {
        unlinkSync(pidFilePath);
    } catch (err) {
        console.error(`Failed to remove PID file: ${err.message}`);
    }
    process.exit(code ?? 0);
});
