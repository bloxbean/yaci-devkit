import { error } from '@sveltejs/kit';
import { paging, tryApi } from '$lib/api/client';
import { GUARD, guardedApi, settle } from '$lib/server/heavy-guard';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

const TABS = { rewards: 'rewards', withdrawals: 'withdrawals', other: 'reward_rest' } as const;

type Account = { controlled_amount?: number; withdrawable_amount?: number; pool_id?: string | null };

/*
 * A stake account: what it controls, where it delegates (pool and DRep), and its rewards history. The account and the
 * list stream in after the page renders: a busy account can take Yaci Store half a minute or more, and the store keeps
 * running a query after the viewer gives up (bloxbean/yaci-store#1269), so they go through the guard.
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

	return {
		address,
		account: settle(guardedApi<Account | null>(`/accounts/${address}`, `/accounts/${address}`, { fetch, timeoutMs: GUARD.timeoutMs.account, cacheMs: GUARD.accountCacheMs })),
		drep: (drep?.[0] ?? null) as { drep_id?: string; drep_hash?: string; drep_type?: string; block_time?: number } | null,
		tab: tab in TABS ? tab : 'rewards',
		page,
		count,
		list: settle(guardedApi<Any[]>(listKey, listKey, { fetch, params: { page, count, order: 'desc' }, timeoutMs: GUARD.timeoutMs.accountLists }).then((r) => r ?? []))
	};
};
