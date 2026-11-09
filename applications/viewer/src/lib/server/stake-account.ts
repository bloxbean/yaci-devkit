import type { Slow } from '$lib/api/slow';
import type { AnalyticsSource } from '$lib/analytics/types';
import { analyticsStakeAccount } from './analytics-sections';
import { GUARD, guardedApi, settle } from './heavy-guard';

export type StakeAccount = {
	controlled_amount?: number;
	withdrawable_amount?: number;
	pool_id?: string | null;
	/** Set when the account came from analytics (as of the last export) because /accounts was too slow. */
	analytics?: AnalyticsSource;
};

/**
 * A stake account for a page section: /accounts/{stake} through the guard (it can take ~30 s for a busy account, so
 * it streams, and a minute's answer is kept). When that is too slow and the store has analytics, what the account
 * controls and its pool come from there instead.
 */
export function stakeAccountSection(stake: string, fetch: typeof globalThis.fetch): Promise<Slow<StakeAccount | null>> {
	const path = `/accounts/${stake}`;
	return settle(guardedApi<StakeAccount | null>(path, path, { fetch, timeoutMs: GUARD.timeoutMs.account, cacheMs: GUARD.accountCacheMs })).then(async (r) => {
		if (r.ok || r.reason !== 'heavy') return r;
		const fromAnalytics = await analyticsStakeAccount(stake, fetch).catch(() => null);
		if (!fromAnalytics) return r;
		const { exportedTo, live, ...account } = fromAnalytics;
		return { ok: true, value: { ...account, analytics: { exportedTo, live } } };
	});
}
