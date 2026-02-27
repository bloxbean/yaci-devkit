import { defineConfig } from "vite";
import { nodePolyfills } from "vite-plugin-node-polyfills";
import path from "path";

export default defineConfig({
  plugins: [nodePolyfills()],
  resolve: {
    alias: {
      // The ESM build of libsodium-wrappers-sumo has a broken relative import
      // to ./libsodium-sumo.mjs (file lives in a different package).
      // Force Vite to use the working CJS build instead.
      "libsodium-wrappers-sumo": path.resolve(
        __dirname,
        "node_modules/libsodium-wrappers-sumo/dist/modules-sumo/libsodium-wrappers.js"
      ),
    },
  },
});
