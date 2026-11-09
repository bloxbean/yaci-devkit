import { error } from '@sveltejs/kit';
import { api, paging, tryApi } from '$lib/api/client';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/* Registered DReps (/governance-state/dreps, 1-based) and the two built-in voting options. */
export const load: PageServerLoad = async ({ url, fetch }) => {
	const { page, count } = paging(url, 20);
	const order = url.searchParams.get('order') === 'asc' ? 'asc' : 'desc';
	try {
		const [dreps, special] = await Promise.all([
			api<Any[]>('/governance-state/dreps', { fetch, params: { page, count, order } }),
			tryApi<Any[]>('/governance-state/dreps/special', { fetch })
		]);
		return {
			dreps: (dreps ?? []) as { drep_id: string; drep_hash: string; deposit: number; status: string; voting_power: number; registration_slot: number; drep_type: string }[],
			special: (special ?? []) as { drep_type: string; voting_power: number }[],
			page,
			count,
			order
		};
	} catch (e) {
		error(503, (e as Error).message);
	}
};
