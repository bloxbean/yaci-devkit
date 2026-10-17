import { env } from '$env/dynamic/private';
import { SafeFetchError, safeGet } from '$lib/server/safe-fetch';
import { IPFS_GATEWAYS, checkAnchorUrl } from './anchor-url';
import { blake2b256Hex } from './blake2b';

/*
 * Fetch a governance anchor document through safeGet (no private addresses, checked again after DNS and on every
 * redirect), with a 1 MB cap and JSON only. IPFS anchors try the next gateway when one fails; a gateway set
 * in IPFS_GATEWAY is tried first (it must be public too). The whole lookup has a 12 s budget.
 */

const BUDGET_MS = 12000;

export class AnchorError extends Error {}

export interface AnchorResult {
	url: string;
	fetchedFrom: string;
	json: unknown;
	/** BLAKE2b-256 of the raw bytes, to compare with the hash on chain. */
	hash: string;
}

export async function fetchAnchor(raw: string): Promise<AnchorResult> {
	const gateways = env.IPFS_GATEWAY ? [env.IPFS_GATEWAY, ...IPFS_GATEWAYS] : IPFS_GATEWAYS;
	const first = checkAnchorUrl(raw, gateways);
	if (!first.ok) throw new AnchorError(first.reason);
	const deadline = Date.now() + BUDGET_MS;
	let lastError: unknown;
	for (const start of [first.url, ...(first.alternates ?? [])]) {
		const left = deadline - Date.now();
		if (left < 500) break;
		try {
			const res = await safeGet(start, {
				maxBytes: 1024 * 1024,
				timeoutMs: Math.min(6000, left),
				maxRedirects: 3,
				accept: 'application/ld+json, application/json;q=0.9, */*;q=0.1'
			});
			let json: unknown;
			try {
				json = JSON.parse(new TextDecoder().decode(res.body));
			} catch {
				throw new AnchorError('The document is not JSON.');
			}
			return { url: first.display, fetchedFrom: res.url.href, json, hash: blake2b256Hex(res.body) };
		} catch (e) {
			lastError = e instanceof SafeFetchError ? new AnchorError(e.message) : e;
		}
	}
	throw lastError ?? new AnchorError('The anchor host took too long to answer.');
}
