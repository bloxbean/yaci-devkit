import { redirect } from '@sveltejs/kit';
import { api, paging, tryApi } from '$lib/api/client';
import { stakeAddressOf } from '$lib/bech32';
import type { Amount, Utxo } from '$lib/tx/types';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

const TABS = ['txs', 'utxos', 'tokens'] as const;

/*
 * Balance comes from /balance when the store aggregates balances, otherwise /amounts. Only the open tab's list is
 * fetched, so busy addresses stay quick. Yaci Store pages are 1-based.
 */
export const load: PageServerLoad = async ({ params, url, fetch }) => {
	const address = params.address.trim();
	if (address.startsWith('stake')) redirect(302, `/stake/${address}`);

	const tab = (TABS as readonly string[]).includes(url.searchParams.get('tab') ?? '') ? (url.searchParams.get('tab') as (typeof TABS)[number]) : 'txs';
	const { page, count } = paging(url, 20);
	const order = url.searchParams.get('order') === 'asc' ? 'asc' : 'desc';
	const stake = stakeAddressOf(address);

	const balance = async (): Promise<Amount[] | null> => {
		const b = await tryApi<Any>(`/addresses/${address}/balance`, { fetch });
		if (b?.amounts) return b.amounts;
		return tryApi<Amount[]>(`/addresses/${address}/amounts`, { fetch });
	};

	const list = async (): Promise<{ rows: Any[]; error: string | null }> => {
		if (tab === 'tokens') return { rows: [], error: null };
		try {
			const rows = await api<Any[]>(`/addresses/${address}/${tab === 'txs' ? 'transactions' : 'utxos'}`, {
				fetch,
				params: { page, count, order },
				timeoutMs: 25000
			});
			return { rows: rows ?? [], error: null };
		} catch (e) {
			return { rows: [], error: (e as Error).message };
		}
	};

	const [amounts, rows, account] = await Promise.all([balance(), list(), stake ? tryApi<Any>(`/accounts/${stake}`, { fetch }) : null]);

	return {
		address,
		stake,
		account: account as { controlled_amount?: number; withdrawable_amount?: number; pool_id?: string | null } | null,
		amounts: amounts ?? [],
		balanceKnown: amounts !== null,
		tab,
		page,
		count,
		order,
		rows: rows.rows as Any[] as (Utxo & { block_height?: number; block_time?: number })[],
		listError: rows.error
	};
};
