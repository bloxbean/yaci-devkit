import { addDays, addressTxsSql, stakeAccountSql, stakeRewardsSql } from '$lib/analytics/queries';
import type { AddressTx, AddressTxPage, AnalyticsSource } from '$lib/analytics/types';
import { analyticsInfo, analyticsSql } from './analytics';

/*
 * Page sections answered from Yaci Store analytics when the regular endpoint is too slow for a busy address or
 * account. Each returns null when the store has no analytics, so the caller keeps its own answer (a notice).
 */

type Fetch = typeof globalThis.fetch;

/** Days searched for a page of transactions, widening until the page fills. */
const TX_WINDOWS = [7, 30, 90];
/** Spends are matched against outputs created up to this many days before the window. */
const SPEND_LOOKBACK_DAYS = 90;
/** Epochs per page of rewards. */
export const REWARD_EPOCHS = 20;

const today = () => new Date().toISOString().slice(0, 10);
const seconds = (t: unknown) => (typeof t === 'string' ? Math.floor(Date.parse(t) / 1000) : Number(t ?? 0));

/** Where a cursor for the next page of an address's analytics transactions points: before this slot, on or before this day. */
export interface TxCursor {
	before?: number;
	day: string;
}

export function txCursorOf(url: URL): TxCursor | null {
	const day = url.searchParams.get('day');
	if (!day || !/^\d{4}-\d{2}-\d{2}$/.test(day)) return null;
	const before = Number(url.searchParams.get('before'));
	return { day, before: Number.isSafeInteger(before) && before > 0 ? before : undefined };
}

/**
 * A page of transactions that paid the address or spent from it, newest first: from the newest day (or the cursor)
 * back 7 days, widening to 30 and 90 until it has `limit` of them.
 */
export async function analyticsAddressTxs(address: string, opts: { fetch: Fetch; limit: number; cursor?: TxCursor | null }): Promise<AddressTxPage | null> {
	const info = await analyticsInfo(opts.fetch);
	if (!info) return null;
	const to = opts.cursor?.day ?? today();
	let rows: AddressTx[] = [];
	let from = to;
	let lookbackFrom = to;
	for (const days of TX_WINDOWS) {
		from = addDays(to, -(days - 1));
		lookbackFrom = addDays(from, -SPEND_LOOKBACK_DAYS);
		const sql = addressTxsSql({ address, from, to, lookbackFrom, beforeSlot: opts.cursor?.before, limit: opts.limit + 1, tables: info.tables });
		const found = await analyticsSql<Record<string, unknown>>(sql, { fetch: opts.fetch, maxRows: opts.limit + 1 });
		rows = found.map((r) => ({ tx_hash: String(r.tx_hash), slot: Number(r.slot), block_time: seconds(r.block_time), received: !!r.received, sent: !!r.sent }));
		if (rows.length > opts.limit) break;
	}
	const full = rows.length > opts.limit;
	rows = rows.slice(0, opts.limit);
	const last = rows[rows.length - 1];
	// A full page continues before its oldest transaction; a short one continues before the days it searched.
	const next: TxCursor | null = full && last ? { before: last.slot, day: new Date(last.block_time * 1000).toISOString().slice(0, 10) } : from > '2017-09-23' ? { day: addDays(from, -1) } : null;
	const older = next ? new URLSearchParams({ tab: 'txs', day: next.day, ...(next.before ? { before: String(next.before) } : {}) }).toString() : null;
	return { rows, from, to, lookbackFrom, older, exportedTo: info.exportedTo, live: info.live };
}

/** What a stake account controls and the pool it last delegated to, as of the last export. */
export async function analyticsStakeAccount(stake: string, fetch: Fetch): Promise<({ controlled_amount?: number; pool_id?: string | null } & AnalyticsSource) | null> {
	const info = await analyticsInfo(fetch);
	if (!info) return null;
	const [row] = await analyticsSql<{ controlled_amount: number | string | null; pool_id: string | null }>(stakeAccountSql(stake, info.tables), { fetch, maxRows: 1 });
	if (!row || (row.controlled_amount == null && row.pool_id == null)) return null;
	return {
		controlled_amount: row.controlled_amount == null ? undefined : Number(row.controlled_amount),
		pool_id: row.pool_id,
		exportedTo: info.exportedTo,
		live: false
	};
}

/** A page of a stake account's rewards (or other rewards), `REWARD_EPOCHS` epochs at a time, newest first. */
export async function analyticsRewards(stake: string, opts: { fetch: Fetch; page: number; currentEpoch: number; rest?: boolean }): Promise<{ rows: Record<string, unknown>[]; fromEpoch: number; toEpoch: number; source: AnalyticsSource } | null> {
	const info = await analyticsInfo(opts.fetch);
	if (!info) return null;
	const toEpoch = opts.currentEpoch - REWARD_EPOCHS * (opts.page - 1);
	const fromEpoch = Math.max(0, toEpoch - REWARD_EPOCHS + 1);
	const rows = await analyticsSql<Record<string, unknown>>(stakeRewardsSql({ stake, fromEpoch, toEpoch, rest: opts.rest, tables: info.tables }), { fetch: opts.fetch, maxRows: 1000 });
	return {
		rows: rows.map((r) => ({ ...r, amount: r.amount == null ? r.amount : Number(r.amount) })),
		fromEpoch,
		toEpoch,
		source: { exportedTo: info.exportedTo, live: info.live && !opts.rest }
	};
}
