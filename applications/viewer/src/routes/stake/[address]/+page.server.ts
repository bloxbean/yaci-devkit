import { error } from '@sveltejs/kit';
import { paging, tryApi } from '$lib/api/client';
import type { Slow } from '$lib/api/slow';
import type { AnalyticsSource } from '$lib/analytics/types';
import { analyticsRewards, REWARD_EPOCHS } from '$lib/server/analytics-sections';
import { GUARD, guardedApi, settle } from '$lib/server/heavy-guard';
import { stakeAccountSection } from '$lib/server/stake-account';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

const TABS = { rewards: 'rewards', withdrawals: 'withdrawals', other: 'reward_rest' } as const;

/** A list section: its rows, and the epochs and source when they came from analytics. */
type List = { rows: Any[]; analytics: (AnalyticsSource & { fromEpoch: number; toEpoch: number }) | null };

/*
 * A stake account: what it controls, where it delegates (pool and DRep), and its rewards history. The account and the
 * list stream in after the page renders: a busy account can take Yaci Store half a minute or more, and the store keeps
 * running a query after the viewer gives up (bloxbean/yaci-store#1269), so they go through the guard. When they are
 * too slow and the store has its analytics API, the account and the rewards lists come from there instead (rewards
 * then page by 20 epochs).
 */
export const load: PageServerLoad = async ({ params, url, fetch }) => {
	const address = params.address.trim();
	if (!/^(stake|stake_test)1/.test(address)) error(404, 'That is not a stake address.');

	const tab = (url.searchParams.get('tab') ?? 'rewards') as keyof typeof TABS;
	const endpoint = TABS[tab] ?? TABS.rewards;
	const { page, count } = paging(url, 20);
	const listKey = `/accounts/${address}/${endpoint}`;

	const drep = await tryApi<Any[]>(`/governance/delegation-votes/address/${address}`, {
		fetch,
		params: { page: 1, count: 1, order: 'desc' },
		timeoutMs: GUARD.timeoutMs.balance
	});

	const fromAnalytics = async (otherwise: Slow<List>): Promise<Slow<List>> => {
		if (endpoint === TABS.withdrawals) return otherwise;
		const latest = await tryApi<Any>('/epochs/latest', { fetch, timeoutMs: GUARD.timeoutMs.balance });
		if (latest?.epoch == null) return otherwise;
		const r = await settle(analyticsRewards(address, { fetch, page, currentEpoch: Number(latest.epoch), rest: endpoint === TABS.other }));
		if (!r.ok || !r.value) return otherwise;
		return { ok: true, value: { rows: r.value.rows, analytics: { ...r.value.source, fromEpoch: r.value.fromEpoch, toEpoch: r.value.toEpoch } } };
	};
	const list = settle(guardedApi<Any[]>(listKey, listKey, { fetch, params: { page, count, order: 'desc' }, timeoutMs: GUARD.timeoutMs.accountLists }).then((rows) => ({ rows: rows ?? [], analytics: null }))).then(
		(r): Slow<List> | Promise<Slow<List>> => (!r.ok && r.reason === 'heavy' ? fromAnalytics(r) : r)
	);

	return {
		address,
		account: stakeAccountSection(address, fetch),
		drep: (drep?.[0] ?? null) as { drep_id?: string; drep_hash?: string; drep_type?: string; block_time?: number } | null,
		tab: tab in TABS ? tab : 'rewards',
		page,
		count,
		list,
		rewardEpochs: REWARD_EPOCHS
	};
};
