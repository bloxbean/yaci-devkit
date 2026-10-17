import { tryApi } from '$lib/api/client';

/*
 * Epoch helpers. Yaci Store's /epochs aggregates are optional (a fresh DevKit devnet has none), so pages fall back
 * to what every store has: /epochs/latest (the current epoch's real boundaries) and /blocks/epoch/{n}.
 */

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;
type Fetch = typeof fetch;

export interface EpochRow {
	number: number;
	start: number | null; // seconds
	end: number | null; // seconds
	/** True when start/end are the epoch's boundaries, false when they are first/last block times. */
	exact: boolean;
	blocks: number | null;
	txs: number | null;
	output: string | number | null;
	fees: string | number | null;
	activeStake: string | number | null;
}

export interface LatestEpoch {
	epoch: number;
	start: number;
	end: number;
}

export async function latestEpoch(fetch: Fetch): Promise<LatestEpoch | null> {
	const l = await tryApi<Any>('/epochs/latest', { fetch });
	if (!l || l.epoch == null) return null;
	return { epoch: Number(l.epoch), start: Number(l.start_time), end: Number(l.end_time) };
}

/**
 * Boundaries for epoch n, assuming every epoch since n has the current epoch's length. That holds on devnets and for
 * every Shelley-era epoch; `fits` checks it against a block time when there is one.
 */
export function uniformBounds(latest: LatestEpoch, n: number) {
	const len = latest.end - latest.start;
	const start = latest.start - (latest.epoch - n) * len;
	return { start, end: start + len };
}

export function fromAggregate(e: Any, latest: LatestEpoch | null): EpochRow {
	const n = Number(e.number ?? e.epoch);
	let start: number | null = e.start_time ?? e.first_block_time ?? null;
	let end: number | null = e.end_time ?? e.last_block_time ?? null;
	let exact = false;
	if (latest) {
		const u = uniformBounds(latest, n);
		// Use real boundaries when the block times sit inside them.
		if (start == null || (start >= u.start && start <= u.end)) {
			start = u.start;
			end = u.end;
			exact = true;
		}
	}
	return {
		number: n,
		start,
		end,
		exact,
		blocks: e.block_count ?? null,
		txs: e.transaction_count ?? e.tx_count ?? null,
		output: e.total_output ?? e.output ?? null,
		fees: e.total_fees ?? e.fees ?? null,
		activeStake: e.active_stake && e.active_stake !== '0' ? e.active_stake : null
	};
}

/** Active stake for epoch n (missing for the first epochs and on stores without stake snapshots). */
export async function activeStake(fetch: Fetch, n: number, timeoutMs = 8000): Promise<string | number | null> {
	const s = await tryApi<Any>(`/epochs/${n}/total-stake`, { fetch, timeoutMs });
	return s?.active_stake ?? null;
}

/** Block count for epoch n from the paged blocks endpoint's total. */
export async function blockCount(fetch: Fetch, n: number): Promise<number | null> {
	const b = await tryApi<Any>(`/blocks/epoch/${n}`, { fetch, params: { page: 1, count: 1 }, timeoutMs: 8000 });
	return b?.total ?? null;
}
