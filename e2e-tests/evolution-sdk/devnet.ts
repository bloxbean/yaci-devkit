import type { Chain } from "@evolution-sdk/evolution";

export const YACI_STORE_URL = process.env.YACI_STORE_URL ?? "http://localhost:8080/api/v1";
export const YACI_ADMIN_URL = process.env.YACI_ADMIN_URL ?? "http://localhost:10000/local-cluster/api";

// Default Yaci DevKit account 0 (pre-funded on devnet creation)
export const SEED_PHRASE =
  "test test test test test test test test test test test test test test test test test test test test test test test sauce";

export const RECEIVER =
  "addr_test1qqm87edtdxc7vu2u34dpf9jzzny4qhk3wqezv6ejpx3vgrwt46dz4zq7vqll88fkaxrm4nac0m5cq50jytzlu0hax5xqwlraql";

// Build an Evolution Chain from the running devnet's genesis info
export async function devnetChain(): Promise<Chain> {
  const res = await fetch(`${YACI_ADMIN_URL}/admin/devnet`);
  if (!res.ok) throw new Error(`Failed to fetch devnet info: HTTP ${res.status}`);
  const info = await res.json();

  return {
    id: 0,
    name: "Yaci DevKit",
    networkMagic: info.protocolMagic,
    epochLength: Math.round(info.epochLength * info.slotLength),
    slotConfig: {
      zeroTime: BigInt(info.startTime) * 1000n,
      zeroSlot: 0n,
      slotLength: Math.round(info.slotLength * 1000),
    },
  };
}
