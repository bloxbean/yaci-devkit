import { bech32Encode } from '$lib/bech32';

/* Pool ids come as bech32 (pool1…) or as the 28-byte hex hash. Yaci Store accepts both. */

export function isPoolId(id: string): boolean {
	return /^pool1[0-9a-z]{20,}$/.test(id) || /^[0-9a-f]{56}$/i.test(id);
}

/** Hex pool hash → pool1… (returns the input when it isn't a hex hash). */
export function poolBech32(id: string | null | undefined): string {
	if (!id) return '';
	if (!/^[0-9a-f]{56}$/i.test(id)) return id;
	const bytes = new Uint8Array(id.match(/../g)!.map((h) => parseInt(h, 16)));
	return bech32Encode('pool', bytes);
}

export type PoolStatus = 'active' | 'retiring' | 'retired';

export function poolStatus(retireEpoch: number | null | undefined, currentEpoch: number): PoolStatus {
	if (retireEpoch == null) return 'active';
	return retireEpoch <= currentEpoch ? 'retired' : 'retiring';
}

/** Margin as "2%" from "0.02" or 0.02. */
export function marginPct(m: string | number | null | undefined): string {
	const v = Number(m ?? 0);
	if (!Number.isFinite(v)) return '—';
	const pct = v * 100;
	return `${pct % 1 === 0 ? pct.toFixed(0) : pct.toFixed(2).replace(/0+$/, '')}%`;
}

/** Genesis certificates have no real transaction or block. */
export const isGenesisTx = (tx: string | null | undefined) => !tx || tx === 'Genesis';
