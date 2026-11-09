import { error } from '@sveltejs/kit';
import { api, paging } from '$lib/api/client';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/* Every vote on chain, newest first (/governance/votes, 1-based). */
export const load: PageServerLoad = async ({ url, fetch }) => {
	const { page, count } = paging(url, 25);
	try {
		const votes = await api<Any[]>('/governance/votes', { fetch, params: { page, count, order: 'desc' } });
		return { votes: votes ?? [], page, count };
	} catch (e) {
		error(503, (e as Error).message);
	}
};
