import { error } from '@sveltejs/kit';
import { paging, tryApi } from '$lib/api/client';
import { loadList, loadSupply } from '$lib/assets/load';
import type { MintEvent } from '$lib/assets/types';
import type { PageServerLoad } from './$types';

/* A minting policy: total supply across its assets, its script, and mint/burn history (1-based pages). */
export const load: PageServerLoad = async ({ params, url, fetch }) => {
	const policy = params.policy.trim().toLowerCase();
	if (!/^[0-9a-f]{56}$/.test(policy)) error(404, 'That is not a policy id (56 hex characters).');
	const { page, count } = paging(url, 20);

	const [supply, history, script] = await Promise.all([
		loadSupply(`/assets/policy/${policy}/supply`, fetch),
		loadList<MintEvent>(`/assets/policy/${policy}/history`, fetch, { page, count }),
		tryApi<{ type?: string; serialised_size?: number | null }>(`/scripts/${policy}`, { fetch })
	]);
	if (supply.notFound && !history.rows.length && !script) error(404, 'No assets under that policy on this chain.');

	return { policy, supply, script, page, count, rows: history.rows, listError: history.error };
};
