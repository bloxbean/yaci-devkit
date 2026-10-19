import { stakeAccountSql, stakeRewardsSql } from '$lib/analytics/queries';
import type { AnalyticsSource } from '$lib/analytics/types';
import { analyticsInfo, analyticsSql } from './analytics';

/*
 * Page sections answered from Yaci Store analytics when the regular endpoint is too slow for a busy stake account.
 * Each returns null when the store has no analytics, so the caller keeps its own answer (a notice).
 */

type Fetch = typeof globalThis.fetch;

/** Epochs per page of rewards. */
export const REWARD_EPOCHS = 20;

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
