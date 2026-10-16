import { error } from '@sveltejs/kit';
import { api, paging, tryApi } from '$lib/api/client';
import { isPoolId } from '$lib/staking/pool';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/* What a pool's leader and members were paid in an epoch. The list is 1-based. */
export const load: PageServerLoad = async ({ params, url, fetch }) => {
	const poolId = params.pool_id.trim();
	if (!isPoolId(poolId)) error(404, 'That is not a pool id.');
	if (!/^\d+$/.test(params.epoch)) error(404, 'Epochs are whole numbers.');
	const epoch = Number(params.epoch);
	const { page, count } = paging(url, 20);

	const [rows, latest] = await Promise.all([
		api<Any[]>(`/pools/${poolId}/epochs/${epoch}/rewards`, { fetch, params: { page, count }, timeoutMs: 25000 }).then(
			(r) => ({ rows: r ?? [], error: null as string | null }),
			(e) => ({ rows: [] as Any[], error: (e as Error).message })
		),
		tryApi<Any>('/epochs/latest', { fetch })
	]);
	return { poolId, epoch, latest: (latest?.epoch as number) ?? null, page, count, rows: rows.rows, listError: rows.error };
};
