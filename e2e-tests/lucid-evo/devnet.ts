import type { SlotConfig } from "@lucid-evolution/lucid";

export const YACI_STORE_URL = process.env.YACI_STORE_URL ?? "http://localhost:8080/api/v1";
export const YACI_ADMIN_URL = process.env.YACI_ADMIN_URL ?? "http://localhost:10000/local-cluster/api";

// Lucid 0.6+ requires an explicit slot config for the "Custom" network
export async function devnetSlotConfig(): Promise<SlotConfig> {
  const res = await fetch(`${YACI_ADMIN_URL}/admin/devnet`);
  if (!res.ok) throw new Error(`Failed to fetch devnet info: HTTP ${res.status}`);
  const info = await res.json();
  return {
    zeroTime: info.startTime * 1000,
    zeroSlot: 0,
    slotLength: Math.round(info.slotLength * 1000),
  };
}
