/*
 * Whether the viewer's server may fetch a URL that came from chain data or user input. It must never become a proxy
 * into the local network: only http(s) on the standard ports, no credentials, and no private, loopback, link-local
 * or other special addresses. Hostnames are checked again after DNS resolution, when connecting (see
 * $lib/server/safe-fetch.ts).
 */

const ALLOWED_PORTS = new Set(['', '80', '443']);

export type PublicUrlCheck = { ok: true; url: URL } | { ok: false; reason: string };

export function checkPublicUrl(input: string): PublicUrlCheck {
	if (input.length > 2048) return { ok: false, reason: 'That URL is too long.' };
	let url: URL;
	try {
		url = new URL(input);
	} catch {
		return { ok: false, reason: 'That is not a valid URL.' };
	}
	if (url.protocol !== 'http:' && url.protocol !== 'https:') return { ok: false, reason: 'Only http and https URLs can be opened.' };
	if (url.username || url.password) return { ok: false, reason: 'URLs with credentials are not allowed.' };
	if (!ALLOWED_PORTS.has(url.port)) return { ok: false, reason: 'Only the standard web ports (80 and 443) are allowed.' };

	const host = url.hostname.replace(/^\[|\]$/g, '').toLowerCase();
	if (!host) return { ok: false, reason: 'That URL has no host.' };
	if (
		host === 'localhost' ||
		host.endsWith('.localhost') ||
		host.endsWith('.local') ||
		host.endsWith('.internal') ||
		(!host.includes('.') && !host.includes(':'))
	) {
		return { ok: false, reason: 'Local and internal hosts are not allowed.' };
	}
	const literal = ipKind(host);
	if (literal && isBlockedIp(host)) return { ok: false, reason: 'Private, loopback and reserved addresses are not allowed.' };
	if (literal === null && /^[0-9.]+$/.test(host)) return { ok: false, reason: 'That address is not valid.' };
	return { ok: true, url };
}

/* ---------- IP ranges ---------- */

export function ipKind(s: string): 4 | 6 | null {
	if (parseV4(s) !== null) return 4;
	if (parseV6(s) !== null) return 6;
	return null;
}

function parseV4(s: string): number | null {
	const m = s.match(/^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/);
	if (!m) return null;
	const parts = m.slice(1).map(Number);
	if (parts.some((p) => p > 255)) return null;
	return ((parts[0] << 24) >>> 0) + (parts[1] << 16) + (parts[2] << 8) + parts[3];
}

function parseV6(input: string): bigint | null {
	let s = input.toLowerCase().split('%')[0];
	if (!s.includes(':')) return null;
	// Embedded IPv4 at the end (::ffff:1.2.3.4)
	const v4 = s.match(/(\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3})$/);
	if (v4) {
		const n = parseV4(v4[1]);
		if (n === null) return null;
		s = s.slice(0, -v4[1].length) + ((n >>> 16) & 0xffff).toString(16) + ':' + (n & 0xffff).toString(16);
	}
	const halves = s.split('::');
	if (halves.length > 2) return null;
	const head = halves[0] ? halves[0].split(':') : [];
	const tail = halves.length === 2 && halves[1] ? halves[1].split(':') : [];
	const missing = 8 - head.length - tail.length;
	if (halves.length === 1 ? head.length !== 8 : missing < 1) return null;
	const groups = [...head, ...Array(halves.length === 2 ? missing : 0).fill('0'), ...tail];
	let v = 0n;
	for (const g of groups) {
		if (!/^[0-9a-f]{1,4}$/.test(g)) return null;
		v = (v << 16n) | BigInt(parseInt(g, 16));
	}
	return v;
}

const V4_BLOCKED: [string, number][] = [
	['0.0.0.0', 8],
	['10.0.0.0', 8],
	['100.64.0.0', 10],
	['127.0.0.0', 8],
	['169.254.0.0', 16],
	['172.16.0.0', 12],
	['192.0.0.0', 24],
	['192.0.2.0', 24],
	['192.88.99.0', 24],
	['192.168.0.0', 16],
	['198.18.0.0', 15],
	['198.51.100.0', 24],
	['203.0.113.0', 24],
	['224.0.0.0', 4],
	['240.0.0.0', 4]
];

const V6_BLOCKED: [string, number][] = [
	['::', 128],
	['::1', 128],
	['fc00::', 7],
	['fe80::', 10],
	['fec0::', 10],
	['ff00::', 8],
	['2001:db8::', 32],
	['2001::', 32],
	['100::', 64]
];

function inV4(n: number, base: string, bits: number) {
	const b = parseV4(base)!;
	const mask = bits === 0 ? 0 : (~0 << (32 - bits)) >>> 0;
	return (n & mask) >>> 0 === (b & mask) >>> 0;
}

function inV6(n: bigint, base: string, bits: number) {
	const b = parseV6(base)!;
	const shift = BigInt(128 - bits);
	return n >> shift === b >> shift;
}

/** True for any address the server must not connect to. Unparseable input counts as blocked. */
export function isBlockedIp(ip: string): boolean {
	const v4 = parseV4(ip);
	if (v4 !== null) return V4_BLOCKED.some(([b, bits]) => inV4(v4, b, bits));
	const v6 = parseV6(ip);
	if (v6 === null) return true;
	// IPv4-mapped (::ffff:a.b.c.d), IPv4-compatible (::a.b.c.d) and NAT64 (64:ff9b::/96): check the IPv4 inside.
	const low32 = Number(v6 & 0xffffffffn);
	const asV4 = `${low32 >>> 24}.${(low32 >>> 16) & 255}.${(low32 >>> 8) & 255}.${low32 & 255}`;
	if (v6 >> 32n === 0xffffn || (v6 >> 32n === 0n && v6 > 1n) || inV6(v6, '64:ff9b::', 96) || inV6(v6, '64:ff9b:1::', 48)) {
		return isBlockedIp(asV4);
	}
	// 6to4 (2002::/16) embeds an IPv4 in bits 16-48.
	if (inV6(v6, '2002::', 16)) {
		const embedded = Number((v6 >> 80n) & 0xffffffffn);
		return isBlockedIp(`${embedded >>> 24}.${(embedded >>> 16) & 255}.${(embedded >>> 8) & 255}.${embedded & 255}`);
	}
	return V6_BLOCKED.some(([b, bits]) => inV6(v6, b, bits));
}
