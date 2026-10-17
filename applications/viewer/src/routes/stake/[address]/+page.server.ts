import { error } from '@sveltejs/kit';
import { api, paging, tryApi } from '$lib/api/client';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

const TABS = { rewards: 'rewards', withdrawals: 'withdrawals', other: 'reward_rest' } as const;

/* A stake account: what it controls, where it delegates (pool and DRep), and its rewards history. */
export const load: PageServerLoad = async ({ params, url, fetch }) => {
	const address = params.address.trim();
	if (!/^(stake|stake_test)1/.test(address)) error(404, 'That is not a stake address.');

	const tab = (url.searchParams.get('tab') ?? 'rewards') as keyof typeof TABS;
	const endpoint = TABS[tab] ?? TABS.rewards;
	const { page, count } = paging(url, 20);

	const [account, drep, rows] = await Promise.all([
		tryApi<Any>(`/accounts/${address}`, { fetch }),
		tryApi<Any[]>(`/governance/delegation-votes/address/${address}`, { fetch, params: { page: 1, count: 1, order: 'desc' } }),
		api<Any[]>(`/accounts/${address}/${endpoint}`, { fetch, params: { page, count, order: 'desc' } }).then(
			(r) => ({ rows: r ?? [], error: null as string | null }),
			(e) => ({ rows: [] as Any[], error: (e as Error).message })
		)
	]);

	return {
		address,
		account: account as { controlled_amount?: number; withdrawable_amount?: number; pool_id?: string | null } | null,
		drep: (drep?.[0] ?? null) as { drep_id?: string; drep_hash?: string; drep_type?: string; block_time?: number } | null,
		tab: tab in TABS ? tab : 'rewards',
		page,
		count,
		rows: rows.rows,
		listError: rows.error
	};
};
