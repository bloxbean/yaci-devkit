import { env } from '$env/dynamic/private';
import { api, ApiError, type ApiOptions } from '$lib/api/client';
import type { Slow } from '$lib/api/slow';
import { BusyError, HeavyError, IndexerGuard } from './indexer-guard';

/*
 * Settings for busy addresses on a large Yaci Store, in one place (bloxbean/yaci-store#1269). Once Yaci Store cancels
 * queries the viewer abandons, set HEAVY_ADDRESS_GUARD=false to go back to the plain behaviour: balance from
 * /amounts when /balance isn't there, no skipped requests, the default timeout.
 */
export const GUARD = {
	/** UTxO pages summed for a balance when /balance isn't available, and their size. */
	balancePages: 5,
	pageSize: 100,
	/** How long an endpoint that timed out is skipped for that address. */
	heavyMs: 15 * 60_000,
	/** The same for a page of UTxOs, which is usually quick: a timeout there more likely means a busy store. */
	utxoHeavyMs: 2 * 60_000,
	/** How long a stake account's answer is kept (a busy one can take ~30 s). */
	accountCacheMs: 60_000,
	/** How long the UTxO pages after the first, read for a balance, are kept. */
	balanceCacheMs: 60_000,
	maxEntries: 1000,
	/** Guarded calls running against the store at once, and how long a call waits for a free slot. */
	maxConcurrent: 4,
	waitMs: 10_000,
	timeoutMs: {
		balance: 10_000,
		utxos: 30_000,
		transactions: 15_000,
		account: 40_000,
		accountLists: 15_000,
		/** Blockfrost-compatible calls: the store gives up on them itself after 15 s. */
		blockfrost: 20_000
	}
} as const;

export function guardEnabled(): boolean {
	return (env.HEAVY_ADDRESS_GUARD ?? '').trim().toLowerCase() !== 'false';
}

export const indexerGuard = new IndexerGuard({ heavyMs: GUARD.heavyMs, maxEntries: GUARD.maxEntries, maxConcurrent: GUARD.maxConcurrent });

/**
 * A Yaci Store call through the guard under `key` (the endpoint without paging): skipped while the key is heavy (for
 * `heavyMs` after a timeout), shared with an identical call in flight, kept for `cacheMs`. With the guard off, a plain
 * call.
 */
export function guardedApi<T>(key: string, path: string, opts: ApiOptions & { cacheMs?: number; heavyMs?: number } = {}): Promise<T> {
	const { cacheMs, heavyMs, ...apiOpts } = opts;
	if (!guardEnabled()) return api<T>(path, apiOpts);
	const query = new URLSearchParams(Object.entries(opts.params ?? {}).flatMap(([k, v]) => (v == null || v === '' ? [] : [[k, String(v)]]))).toString();
	return indexerGuard.call(key, query ? `${path}?${query}` : path, () => api<T>(path, apiOpts), { cacheMs, heavyMs, waitMs: GUARD.waitMs });
}

/** Turns a call into a section result that never rejects, so a page can stream it. */
export function settle<T>(p: Promise<T>): Promise<Slow<T>> {
	return p.then(
		(value) => ({ ok: true as const, value }),
		(e) => ({
			ok: false as const,
			reason: e instanceof HeavyError || (e instanceof ApiError && e.timedOut) ? ('heavy' as const) : e instanceof BusyError ? ('busy' as const) : ('error' as const),
			message: (e as Error)?.message ?? String(e)
		})
	);
}
