import { blake2b256Hex } from '$lib/governance/blake2b';
import { fetchAnchor } from '$lib/governance/anchor.server';
import { safeGet } from '$lib/server/safe-fetch';
import { cleanText } from './clean';

/*
 * Names from documents their owners publish, referenced on chain by URL and hash: a DRep's CIP-119 profile
 * (givenName), a governance action's CIP-108 document (title), a pool's CIP-6 metadata (ticker, name). A name is
 * kept only when the document's BLAKE2b-256 matches the hash on chain, so a changed or swapped file shows nothing.
 * All fetches go through safeGet (public addresses only, checked after DNS and on redirects, size and time capped).
 */

/** One document to read: where it is, the hash on chain, and what was read from it. */
export interface DocEntry<T> {
	url: string;
	hash: string;
	value?: T;
	/** When reading it last failed (ms), to retry later rather than on every refresh. */
	failedAt?: number;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

const body = (j: Any) => (j && typeof j === 'object' ? (j.body ?? {}) : {});

export async function drepName(url: string, hash: string): Promise<string | null> {
	const r = await fetchAnchor(url);
	if (r.hash !== hash.toLowerCase()) return null;
	return cleanText(body(r.json).givenName, 60) ?? null;
}

export async function actionTitle(url: string, hash: string): Promise<string | null> {
	const r = await fetchAnchor(url);
	if (r.hash !== hash.toLowerCase()) return null;
	return cleanText(body(r.json).title, 140) ?? null;
}

export async function poolName(url: string, hash: string): Promise<{ ticker?: string; name?: string } | null> {
	const res = await safeGet(new URL(url), { maxBytes: 4096, timeoutMs: 5000, maxRedirects: 2, accept: 'application/json, */*;q=0.1' });
	if (blake2b256Hex(res.body) !== hash.toLowerCase()) return null;
	const j = JSON.parse(new TextDecoder().decode(res.body));
	const ticker = cleanText(j?.ticker, 9);
	const name = cleanText(j?.name, 50);
	return ticker || name ? { ticker, name } : null;
}

/**
 * Read the documents that need it: never read, or failed more than `retryMs` ago. `concurrency` at a time;
 * `onProgress` runs every `every` documents (to save). Returns how many were read and how many gave a name.
 */
export async function fillDocs<T>(
	entries: Record<string, DocEntry<T>>,
	read: (url: string, hash: string) => Promise<T | null>,
	opts: { concurrency: number; retryMs: number; now?: () => number; every?: number; onProgress?: () => Promise<void> }
): Promise<{ read: number; named: number }> {
	const now = opts.now ?? Date.now;
	const todo = Object.values(entries).filter((e) => e.value === undefined && (e.failedAt === undefined || now() - e.failedAt > opts.retryMs));
	let next = 0;
	let named = 0;
	let done = 0;
	const worker = async () => {
		while (next < todo.length) {
			const e = todo[next++];
			try {
				const v = await read(e.url, e.hash);
				if (v == null) e.failedAt = now();
				else {
					e.value = v;
					delete e.failedAt;
					named++;
				}
			} catch {
				e.failedAt = now();
			}
			if (++done % (opts.every ?? 200) === 0) await opts.onProgress?.();
		}
	};
	await Promise.all(Array.from({ length: Math.min(opts.concurrency, todo.length) }, worker));
	return { read: todo.length, named };
}

/** Put the latest url/hash for each key; a changed document is read again. Keys not in `latest` are dropped. */
export function syncDocs<T>(entries: Record<string, DocEntry<T>>, latest: Map<string, { url: string; hash: string }>): Record<string, DocEntry<T>> {
	const out: Record<string, DocEntry<T>> = {};
	for (const [key, { url, hash }] of latest) {
		const prev = entries[key];
		out[key] = prev && prev.url === url && prev.hash === hash ? prev : { url, hash };
	}
	return out;
}
