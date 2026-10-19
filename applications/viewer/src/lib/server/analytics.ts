import { env } from '$env/dynamic/private';
import { api, ApiError } from '$lib/api/client';
import type { Tables } from '$lib/analytics/queries';
import { IndexerGuard } from './indexer-guard';

/*
 * Yaci Store's analytics query API (DuckDB over the store's daily Parquet exports, optionally joined with live
 * PostgreSQL), where the store has it enabled. The devkit's own store doesn't, and answers 404, so everything that
 * uses it is a fallback: the viewer asks the regular endpoints first and comes here when one is too slow for a busy
 * stake account (bloxbean/yaci-store#1269). Every query is bounded by the store's own timeout, unlike those endpoints.
 *
 * YACI_ANALYTICS=off turns it off even when the store has it.
 */
export const ANALYTICS = {
	/** How long a probe result is kept, and how soon to try again after a probe failed. */
	probeMs: 10 * 60_000,
	retryProbeMs: 60_000,
	/** The store cancels a query after 30 s; a little longer here, so its answer (408) arrives first. */
	timeoutMs: 35_000,
	/** How long answers are kept. History only changes with the next daily export. */
	cacheMs: 10 * 60_000,
	/** Analytics queries running at once: the store has one reader per CPU core, shared with everyone. */
	maxConcurrent: 2,
	waitMs: 10_000,
	heavyMs: 15 * 60_000
} as const;

/**
 * Tables read through the live views (exports joined with PostgreSQL for the newest data). Others are read from the
 * exports only: on yaci-store 3.0.0-beta5 some live queries outlive the store's timeout (bloxbean/yaci-store#1270).
 */
const LIVE_OK = new Set(['reward']);

export interface AnalyticsInfo {
	/** The store joins its exports with live PostgreSQL (its exported tables are then also `parquet_<table>`). */
	live: boolean;
	/** The last exported day (ISO date), when known. */
	exportedTo: string | null;
	tables: Tables;
}

const guard = new IndexerGuard({ heavyMs: ANALYTICS.heavyMs, maxEntries: 500, maxConcurrent: ANALYTICS.maxConcurrent });

let probed: { info: AnalyticsInfo | null; until: number } | null = null;
let probing: Promise<AnalyticsInfo | null> | null = null;

/** Whether the store has the analytics API, and how to query it; null when it doesn't. Probed every 10 minutes. */
export function analyticsInfo(fetch: typeof globalThis.fetch): Promise<AnalyticsInfo | null> {
	if ((env.YACI_ANALYTICS ?? '').trim().toLowerCase() === 'off') return Promise.resolve(null);
	if (probed && probed.until > Date.now()) return Promise.resolve(probed.info);
	probing ??= probe(fetch)
		.then(
			(info) => ((probed = { info, until: Date.now() + ANALYTICS.probeMs }), info),
			() => ((probed = { info: null, until: Date.now() + ANALYTICS.retryProbeMs }), null)
		)
		.finally(() => (probing = null));
	return probing;
}

async function probe(fetch: typeof globalThis.fetch): Promise<AnalyticsInfo | null> {
	try {
		await query('SELECT 1 AS ok', { fetch, timeoutMs: 8_000 });
	} catch (e) {
		// 404: the store doesn't have the API. 401/403: it does, but not for this caller (for example a reverse proxy
		// that keeps it internal). Either way, there is nothing to use until the next probe.
		if (e instanceof ApiError && [401, 403, 404].includes(e.status)) return null;
		throw e;
	}
	// With live data on, the exports are also available on their own as parquet_<table>.
	const live = await query<{ d: string | null }>('SELECT max(date) AS d FROM parquet_transaction', { fetch, timeoutMs: 20_000 }).then(
		(rows) => ({ live: true, exportedTo: rows[0]?.d ?? null }),
		() => null
	);
	const exportedTo = live?.exportedTo ?? (await query<{ d: string | null }>('SELECT max(date) AS d FROM transaction', { fetch, timeoutMs: 20_000 }).then((r) => r[0]?.d ?? null, () => null));
	return { live: !!live, exportedTo, tables: tablesFor(!!live) };
}

export function tablesFor(live: boolean): Tables {
	const read = (t: string) => (live && !LIVE_OK.has(t) ? `parquet_${t}` : t);
	return {
		reward: read('reward'),
		rewardRest: read('reward_rest'),
		stakeBalance: read('stake_address_balance'),
		delegation: read('delegation')
	};
}

function query<T>(sql: string, opts: { fetch: typeof globalThis.fetch; maxRows?: number; timeoutMs?: number }): Promise<T[]> {
	return api<T[]>('/analytics/query/sql', { fetch: opts.fetch, body: { sql, maxRows: opts.maxRows ?? 100 }, timeoutMs: opts.timeoutMs ?? ANALYTICS.timeoutMs }).then(
		(rows) => rows ?? [],
		(e) => {
			// The store's own timeout: treat it like ours, so the guard skips the query for a while.
			if (e instanceof ApiError && e.status === 408) throw new ApiError(e.message, 408, e.path, true);
			throw e;
		}
	);
}

/**
 * Runs a query through the guard: identical queries share one call and answers are kept for `cacheMs`; a query that
 * timed out is skipped for 15 minutes; at most two run at once.
 */
export function analyticsSql<T>(sql: string, opts: { fetch: typeof globalThis.fetch; maxRows?: number; cacheMs?: number }): Promise<T[]> {
	return guard.call(sql, sql, () => query<T>(sql, opts), { cacheMs: opts.cacheMs ?? ANALYTICS.cacheMs, waitMs: ANALYTICS.waitMs });
}
