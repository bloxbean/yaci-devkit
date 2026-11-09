import { error } from '@sveltejs/kit';
import { api, paging } from '$lib/api/client';
import type { PageServerLoad } from './$types';

export interface Withdrawal {
	tx_hash: string;
	address: string;
	amount: number | string;
	epoch?: number;
	block_number?: number;
	block_time?: number;
	slot?: number;
}

/* Reward withdrawals, newest first. /txs/withdrawals pages are 1-based. */
export const load: PageServerLoad = async ({ url, fetch }) => {
	const { page, count } = paging(url, 20, 100);
	try {
		const rows = await api<Withdrawal[]>('/txs/withdrawals', { fetch, params: { page, count } });
		return { rows: rows ?? [], page, count };
	} catch (e) {
		error(503, (e as Error).message);
	}
};
