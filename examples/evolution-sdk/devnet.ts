import * as Evolution from "@evolution-sdk/evolution";

const DEVKIT_ADMIN_URL = "http://localhost:10000/local-cluster/api/admin/devnet";

/**
 * Build an Evolution SDK `Chain` descriptor by fetching the live shelley genesis
 * from Yaci DevKit's admin API. This ensures `zeroTime`/`slotLength`/`networkMagic`
 * always match the running devnet (which resets each time the cluster restarts).
 *
 * See: https://devkit.yaci.xyz/tutorials/lucid-evolution/overview
 */
export async function fetchDevnetChain(): Promise<Evolution.Chain> {
  const shelley = await fetch(`${DEVKIT_ADMIN_URL}/genesis/shelley`).then((r) => r.json());

  const zeroTime = BigInt(new Date(shelley.systemStart).getTime());
  const slotLength = shelley.slotLength * 1000; // seconds → milliseconds

  return {
    id: 0,
    name: "Yaci DevKit",
    networkMagic: shelley.networkMagic,
    epochLength: shelley.epochLength,
    slotConfig: {
      zeroTime,
      zeroSlot: 0n,
      slotLength,
    },
  };
}

/**
 * Install a global fetch shim that normalizes a few Yaci Store responses so
 * they parse against Evolution SDK's stricter Blockfrost schemas.
 *
 * Known divergences from upstream Blockfrost handled here:
 *   - `/epochs/latest/parameters`: `drep_deposit` and `gov_action_deposit` come
 *     back as numbers; SDK expects strings.
 *   - `/addresses/{addr}/utxos`: the SDK requires `tx_index` and `block` on
 *     every UTxO row; Yaci Store emits `output_index`/`block_number` instead.
 *
 * Without these patches, `client.newTx().build()` and `client.getUtxos()` fail
 * with schema parse errors.
 */
export function installYaciStoreFetchShim(): void {
  const originalFetch = globalThis.fetch;
  if ((originalFetch as any).__yaciShimInstalled) return;

  const shimmed: typeof fetch = async (input, init) => {
    const url = typeof input === "string" ? input : input instanceof URL ? input.href : input.url;
    const response = await originalFetch(input, init);
    if (!response.ok) return response;

    let body: any;
    if (url.includes("/epochs/latest/parameters")) {
      body = await response.json();
      for (const k of ["drep_deposit", "gov_action_deposit"] as const) {
        if (typeof body[k] === "number") body[k] = String(body[k]);
      }
    } else if (/\/addresses\/[^/]+\/utxos/.test(url)) {
      body = await response.json();
      if (Array.isArray(body)) {
        for (const u of body) {
          if (u.tx_index === undefined && typeof u.output_index === "number") {
            u.tx_index = u.output_index;
          }
          if (typeof u.block !== "string") {
            u.block = typeof u.block_hash === "string"
              ? u.block_hash
              : typeof u.block_number === "number"
              ? String(u.block_number)
              : "";
          }
        }
      }
    } else {
      return response;
    }

    return new Response(JSON.stringify(body), {
      status: response.status,
      statusText: response.statusText,
      headers: response.headers,
    });
  };

  (shimmed as any).__yaciShimInstalled = true;
  globalThis.fetch = shimmed;
}
