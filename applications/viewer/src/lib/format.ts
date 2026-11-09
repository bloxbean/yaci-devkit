/*
 * Formatting helpers. Lovelace amounts go through BigInt: Mainnet supply (4.5e16) is past Number's safe range.
 */

export type Amountish = number | string | bigint | null | undefined;

const LOVELACE = 1_000_000n;

function toBig(v: Amountish): bigint {
	if (v === null || v === undefined || v === '') return 0n;
	if (typeof v === 'bigint') return v;
	if (typeof v === 'number') return BigInt(Math.trunc(v));
	try {
		return BigInt(v.split('.')[0]);
	} catch {
		return 0n;
	}
}

const group = (s: string) => s.replace(/\B(?=(\d{3})+(?!\d))/g, ',');

/** Lovelace → ADA split into whole and fractional parts, e.g. { whole: "1,234", frac: "567891" }. */
export function adaParts(lovelace: Amountish): { neg: boolean; whole: string; frac: string } {
	let v = toBig(lovelace);
	const neg = v < 0n;
	if (neg) v = -v;
	return { neg, whole: group((v / LOVELACE).toString()), frac: (v % LOVELACE).toString().padStart(6, '0') };
}

/** Lovelace → "1,234.56" (decimals: fixed places; 'auto' trims trailing zeros, keeping at least 2). */
export function ada(lovelace: Amountish, decimals: number | 'auto' = 2): string {
	const { neg, whole, frac } = adaParts(lovelace);
	let f: string;
	if (decimals === 'auto') {
		f = frac.replace(/0+$/, '');
		if (f.length < 2) f = f.padEnd(2, '0');
	} else {
		f = frac.slice(0, decimals);
	}
	return (neg ? '−' : '') + whole + (f ? '.' + f : '');
}

/** Lovelace → compact ADA, e.g. 45B, 1.2M, 950.4K. */
export function adaCompact(lovelace: Amountish): string {
	const v = toBig(lovelace);
	const whole = Number(v / LOVELACE) + Number(v % LOVELACE) / 1e6;
	return compact(whole);
}

export function compact(n: number | string | null | undefined, digits = 1): string {
	const v = Number(n ?? 0);
	if (!Number.isFinite(v)) return '—';
	const abs = Math.abs(v);
	const units: [number, string][] = [
		[1e12, 'T'],
		[1e9, 'B'],
		[1e6, 'M'],
		[1e3, 'K']
	];
	for (const [size, unit] of units) {
		if (abs >= size) return (v / size).toFixed(digits).replace(/\.0+$/, '') + unit;
	}
	return v % 1 === 0 ? String(v) : v.toFixed(2);
}

/** A token quantity in its smallest unit → "1,234.5678" with the registry's decimals (trailing zeros trimmed). */
export function tokenAmount(quantity: Amountish, decimals: number): string {
	let v = toBig(quantity);
	const neg = v < 0n;
	if (neg) v = -v;
	if (!(decimals > 0)) return (neg ? '−' : '') + group(v.toString());
	const scale = 10n ** BigInt(decimals);
	const frac = (v % scale).toString().padStart(decimals, '0').replace(/0+$/, '');
	return (neg ? '−' : '') + group((v / scale).toString()) + (frac ? '.' + frac : '');
}

/** Integer-ish values with thousands separators. Accepts big strings. */
export function num(n: number | string | bigint | null | undefined): string {
	if (n === null || n === undefined || n === '') return '—';
	if (typeof n === 'number' && !Number.isInteger(n)) return n.toLocaleString('en-US', { maximumFractionDigits: 2 });
	return group(String(n));
}

/** Seconds (or milliseconds, detected) since epoch → "12s ago", "3m ago", "2h ago", "4d ago". */
export function timeAgo(ts: number | null | undefined, now = Date.now()): string {
	if (!ts) return '—';
	const ms = ts > 1e12 ? ts : ts * 1000;
	const s = Math.round((now - ms) / 1000);
	if (s < 0) return 'just now';
	if (s < 5) return 'just now';
	if (s < 60) return `${s}s ago`;
	const m = Math.floor(s / 60);
	if (m < 60) return `${m}m ago`;
	const h = Math.floor(m / 60);
	if (h < 24) return `${h}h ago`;
	const d = Math.floor(h / 24);
	if (d < 30) return `${d}d ago`;
	const mo = Math.floor(d / 30);
	if (mo < 12) return `${mo}mo ago`;
	return `${Math.floor(mo / 12)}y ago`;
}

/** Seconds → "3m 12s", "2h 05m", "4d 3h". */
export function duration(totalSeconds: number): string {
	const s = Math.max(0, Math.round(totalSeconds));
	const d = Math.floor(s / 86400);
	const h = Math.floor((s % 86400) / 3600);
	const m = Math.floor((s % 3600) / 60);
	const sec = s % 60;
	if (d > 0) return `${d}d ${h}h`;
	if (h > 0) return `${h}h ${String(m).padStart(2, '0')}m`;
	if (m > 0) return `${m}m ${String(sec).padStart(2, '0')}s`;
	return `${sec}s`;
}

/** Seconds since epoch → "Oct 17, 2026, 14:03:22". */
export function dateTime(ts: number | null | undefined): string {
	if (!ts) return '—';
	const ms = ts > 1e12 ? ts : ts * 1000;
	return new Date(ms).toLocaleString('en-US', {
		month: 'short',
		day: 'numeric',
		year: 'numeric',
		hour: '2-digit',
		minute: '2-digit',
		second: '2-digit',
		hour12: false
	});
}

/** Middle-truncate: "addr_test1qz…k8ujs". */
export function shorten(s: string | null | undefined, head = 8, tail = 6): string {
	if (!s) return '';
	if (s.length <= head + tail + 1) return s;
	return `${s.slice(0, head)}…${s.slice(-tail)}`;
}

export function percent(part: number, whole: number, digits = 1): string {
	if (!whole) return '0%';
	return `${((part / whole) * 100).toFixed(digits)}%`;
}

/** Hex asset name → readable text when it is printable UTF-8, otherwise null. */
export function hexToText(hex: string | null | undefined): string | null {
	if (!hex || hex.length % 2 !== 0 || !/^[0-9a-fA-F]*$/.test(hex)) return null;
	try {
		const bytes = new Uint8Array(hex.match(/../g)!.map((b) => parseInt(b, 16)));
		const text = new TextDecoder('utf-8', { fatal: true }).decode(bytes);
		return /^[\p{L}\p{N}\p{P}\p{S} ]+$/u.test(text) ? text : null;
	} catch {
		return null;
	}
}

/** CIP-68 asset name labels: a 4-byte prefix before the readable name. */
export const CIP68_LABELS: Record<string, string> = {
	'000643b0': 'reference',
	'000de140': 'NFT',
	'0014df10': 'FT',
	'001bc280': 'RFT'
};

/** Asset unit (policy + hex name) → { policy, nameHex, name, label }. "lovelace" → ADA. */
export function parseUnit(unit: string): { policy: string; nameHex: string; name: string; label?: string } {
	if (!unit || unit === 'lovelace') return { policy: '', nameHex: '', name: 'ADA' };
	const policy = unit.slice(0, 56);
	const nameHex = unit.slice(56);
	const label = CIP68_LABELS[nameHex.slice(0, 8)];
	const body = label ? nameHex.slice(8) : nameHex;
	const text = hexToText(body);
	return {
		policy,
		nameHex,
		label,
		name: text ?? (nameHex ? shorten(nameHex, 6, 4) : shorten(policy, 6, 4))
	};
}
