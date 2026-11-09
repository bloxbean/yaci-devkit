import { bech32Decode } from '$lib/bech32';

/*
 * The keys names are stored under, from whatever form an id comes in:
 *  - scripts: the 28-byte script hash (hex), also from a script address or a script stake address.
 *  - dreps:   the 28-byte credential hash (hex), from a CIP-129 or CIP-105 id or the hash itself.
 *  - pools:   the 28-byte pool hash (hex), from pool1… or hex.
 *  - actions: "<tx hash>#<index>", from the pair or a CIP-129 gov_action1… id.
 *  - tokens:  the unit (policy id + asset name, hex).
 */

const HASH = /^[0-9a-f]{56}$/;
const toHex = (b: Uint8Array) => Array.from(b, (x) => x.toString(16).padStart(2, '0')).join('');

/** The script hash in an address's payment part, or of a script stake address; null for keys and Byron. */
export function scriptKeyOfAddress(address: string | null | undefined): string | null {
	if (!address) return null;
	const d = bech32Decode(address);
	if (!d || d.bytes.length < 29) return null;
	const type = d.bytes[0] >> 4;
	if (d.hrp === 'addr' || d.hrp === 'addr_test') return [1, 3, 5, 7].includes(type) ? toHex(d.bytes.subarray(1, 29)) : null;
	if (d.hrp === 'stake' || d.hrp === 'stake_test') return type === 0xf ? toHex(d.bytes.subarray(1, 29)) : null;
	return null;
}

export function scriptKey(hash: string | null | undefined): string | null {
	const h = (hash ?? '').toLowerCase();
	return HASH.test(h) ? h : null;
}

export function drepKey(id: string | null | undefined): string | null {
	const s = (id ?? '').trim();
	if (HASH.test(s.toLowerCase())) return s.toLowerCase();
	const d = bech32Decode(s);
	if (!d) return null;
	if ((d.hrp === 'drep' || d.hrp === 'drep_script') && d.bytes.length === 28) return toHex(d.bytes);
	if (d.hrp === 'drep' && d.bytes.length === 29) return toHex(d.bytes.subarray(1));
	return null;
}

export function poolKey(id: string | null | undefined): string | null {
	const s = (id ?? '').trim();
	if (HASH.test(s.toLowerCase())) return s.toLowerCase();
	const d = bech32Decode(s);
	return d?.hrp === 'pool' && d.bytes.length === 28 ? toHex(d.bytes) : null;
}

export function actionKey(txHashOrId: string | null | undefined, index?: number | string | null): string | null {
	const s = (txHashOrId ?? '').trim().toLowerCase();
	if (/^[0-9a-f]{64}$/.test(s) && index != null && /^\d+$/.test(String(index))) return `${s}#${Number(index)}`;
	const m = s.match(/^([0-9a-f]{64})[#_](\d+)$/);
	if (m) return `${m[1]}#${Number(m[2])}`;
	const d = bech32Decode(s);
	if (d?.hrp === 'gov_action' && d.bytes.length >= 33) {
		let i = 0;
		for (const b of d.bytes.subarray(32)) i = i * 256 + b;
		return `${toHex(d.bytes.subarray(0, 32))}#${i}`;
	}
	return null;
}

export function tokenKey(unit: string | null | undefined): string | null {
	const u = (unit ?? '').toLowerCase();
	return /^[0-9a-f]{56}([0-9a-f]{2}){0,32}$/.test(u) ? u : null;
}

/** The key function for each kind. */
export const KEY = { scripts: scriptKey, dreps: drepKey, pools: poolKey, actions: (id: string) => actionKey(id), tokens: tokenKey };
