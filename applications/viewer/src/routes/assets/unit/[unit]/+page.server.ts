import { error } from '@sveltejs/kit';
import { paging, tryApi } from '$lib/api/client';
import { loadList, loadSupply } from '$lib/assets/load';
import type { MintEvent } from '$lib/assets/types';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

const TABS = ['history', 'transactions', 'utxos', 'holders'] as const;
type Tab = (typeof TABS)[number];

/*
 * An asset: supply, plus one list for the open tab. Yaci Store pages are 1-based here (page 0 is treated as 1).
 * Transactions, UTxOs and holders can be too slow on a big chain, so they time out into an inline error.
 */
export const load: PageServerLoad = async ({ params, url, fetch }) => {
	const unit = params.unit.trim().toLowerCase();
	if (!/^[0-9a-f]{56,120}$/.test(unit)) error(404, 'That is not an asset unit (policy id followed by the asset name in hex).');

	const tabParam = url.searchParams.get('tab') ?? '';
	const tab: Tab = (TABS as readonly string[]).includes(tabParam) ? (tabParam as Tab) : 'history';
	const { page, count } = paging(url, 20);

	const path = {
		history: `/assets/${unit}/history`,
		transactions: `/assets/${unit}/transactions`,
		utxos: `/assets/${unit}/utxos`,
		holders: `/assets/${unit}/addresses`
	}[tab];
	const listParams: Record<string, string | number> = tab === 'holders' ? { page, count, sort: 'desc' } : tab === 'history' ? { page, count } : { page, count, order: 'desc' };

	const [supply, list, latest] = await Promise.all([
		loadSupply(`/assets/${unit}/supply`, fetch),
		loadList<Any>(path, fetch, listParams, tab === 'history' ? 20000 : 12000),
		// The newest event carries the fingerprint; the history tab already has it.
		tab === 'history' ? null : tryApi<MintEvent[]>(`/assets/${unit}/history`, { fetch, params: { page: 1, count: 1 } })
	]);

	const first = (tab === 'history' ? list.rows[0] : latest?.[0]) as MintEvent | undefined;
	if (supply.notFound && !first) error(404, 'No asset with that unit on this chain.');

	return {
		unit,
		supply,
		fingerprint: first?.fingerprint ?? null,
		tab,
		page,
		count,
		rows: list.rows,
		listError: list.error
	};
};
