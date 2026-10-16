import { api, paging, tryApi } from '$lib/api/client';
import { isPoolId } from '$lib/staking/pool';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/* A pool's stake and delegators in an epoch's snapshot. Without ?id it is a lookup form. Delegators are 1-based. */
export const load: PageServerLoad = async ({ url, fetch }) => {
	const latest = await tryApi<Any>('/epochs/latest', { fetch });
	const current = Number(latest?.epoch ?? 0);
	const asked = parseInt(url.searchParams.get('epoch') ?? '', 10);
	const epoch = Number.isFinite(asked) && asked >= 0 ? asked : current;
	const id = (url.searchParams.get('id') ?? '').trim();
	const { page, count } = paging(url, 20);

	if (!id) return { id: null, valid: true, epoch, current, page, count, stake: null, total: null, rows: [] as Any[], listError: null };
	if (!isPoolId(id)) return { id, valid: false, epoch, current, page, count, stake: null, total: null, rows: [] as Any[], listError: null };

	const [stake, total, rows] = await Promise.all([
		tryApi<Any>(`/epochs/${epoch}/pools/${id}/stake`, { fetch, timeoutMs: 10000 }),
		tryApi<Any>(`/epochs/${epoch}/total-stake`, { fetch, timeoutMs: 10000 }),
		api<Any[]>(`/epochs/${epoch}/pools/${id}/delegators`, { fetch, params: { page, count }, timeoutMs: 25000 }).then(
			(r) => ({ rows: r ?? [], error: null as string | null }),
			(e) => ({ rows: [] as Any[], error: (e as Error).message })
		)
	]);
	return {
		id,
		valid: true,
		epoch,
		current,
		page,
		count,
		stake: stake ? { poolId: stake.pool_id as string, poolHash: stake.pool_hash as string, amount: Number(stake.active_stake) } : null,
		total: total?.active_stake != null ? Number(total.active_stake) : null,
		rows: rows.rows,
		listError: rows.error
	};
};
