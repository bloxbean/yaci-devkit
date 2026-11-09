import { error } from '@sveltejs/kit';
import { api, paging, tryApi } from '$lib/api/client';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

const TABS = { rewards: 'rewards', other: 'reward_rest', unclaimed: 'unclaimed_reward_rest' } as const;
type Tab = keyof typeof TABS;

/* Rewards that became spendable in an epoch, plus that epoch's pots. Lists are 1-based. */
export const load: PageServerLoad = async ({ params, url, fetch }) => {
	if (!/^\d+$/.test(params.epoch)) error(404, 'Epochs are whole numbers.');
	const epoch = Number(params.epoch);
	const tab: Tab = (url.searchParams.get('tab') as Tab) in TABS ? (url.searchParams.get('tab') as Tab) : 'rewards';
	const { page, count } = paging(url, 20);

	const [rows, pots, latest] = await Promise.all([
		api<Any[]>(`/epochs/${epoch}/${TABS[tab]}`, { fetch, params: { page, count }, timeoutMs: 25000 }).then(
			(r) => ({ rows: r ?? [], error: null as string | null }),
			(e) => ({ rows: [] as Any[], error: (e as Error).message })
		),
		tryApi<Any>(`/adapot/epochs/${epoch}`, { fetch }),
		tryApi<Any>('/epochs/latest', { fetch })
	]);

	return {
		epoch,
		latest: (latest?.epoch as number) ?? null,
		tab,
		page,
		count,
		rows: rows.rows,
		listError: rows.error,
		pots: pots as Record<string, number> | null
	};
};
