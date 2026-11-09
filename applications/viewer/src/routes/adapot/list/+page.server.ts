import { error } from '@sveltejs/kit';
import { api, paging } from '$lib/api/client';
import type { AdaPot } from '$lib/epochs/pots';
import type { PageServerLoad } from './$types';

/* Ada pots by epoch, newest first. /adapot/list pages are 1-based. */
export const load: PageServerLoad = async ({ url, fetch }) => {
	const { page, count } = paging(url, 20, 100);
	try {
		const rows = await api<AdaPot[]>('/adapot/list', { fetch, params: { page, count } });
		return { rows: rows ?? [], page, count };
	} catch (e) {
		error(503, (e as Error).message);
	}
};
