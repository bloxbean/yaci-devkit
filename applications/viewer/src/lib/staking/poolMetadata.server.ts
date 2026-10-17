import { safeGet } from '$lib/server/safe-fetch';

/*
 * Pool metadata (CIP-6) is a small JSON document at a URL the pool operator chose, so it goes through safeGet (no
 * private addresses, checked after DNS), with no redirects, a short timeout and a 4 KB cap. Only a few display
 * fields are returned, and any failure just means no metadata.
 */

export interface PoolMetadata {
	name?: string;
	ticker?: string;
	description?: string;
	homepage?: string;
}

const clean = (v: unknown, max: number) =>
	typeof v === 'string' && v.trim() ? v.replace(/[\u0000-\u001f\u007f]/g, ' ').trim().slice(0, max) : undefined;

export async function fetchPoolMetadata(raw: string | null | undefined): Promise<PoolMetadata | null> {
	if (!raw) return null;
	try {
		const res = await safeGet(new URL(raw), { maxBytes: 4096, timeoutMs: 3000, maxRedirects: 0 });
		const j = JSON.parse(new TextDecoder().decode(res.body));
		const homepage = clean(j.homepage, 200);
		return {
			name: clean(j.name, 80),
			ticker: clean(j.ticker, 12),
			description: clean(j.description, 300),
			homepage: homepage && /^https?:\/\//i.test(homepage) ? homepage : undefined
		};
	} catch {
		return null;
	}
}
