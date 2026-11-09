import { tryApi } from '$lib/api/client';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

export interface PoolStake {
	poolHash: string;
	poolId: string;
	stake: number;
}

/*
 * Stake per pool for an epoch. Yaci Store has no "all pools by stake" endpoint, so this takes the pools from the
 * newest registrations and asks for each one's stake. On a devnet that is every pool; on a big network it is the
 * recently registered ones, and `complete` says which.
 */
export async function poolDistribution(epoch: number, f: typeof fetch, limit = 40): Promise<{ pools: PoolStake[]; complete: boolean }> {
	const regs = await tryApi<Any[]>('/pools/registrations', { fetch: f, params: { page: 1, count: limit }, timeoutMs: 15000 });
	if (!regs) return { pools: [], complete: false };
	const hashes = [...new Set(regs.map((r) => r.pool_id as string))];
	const stakes = await Promise.all(
		hashes.map((h) => tryApi<Any>(`/epochs/${epoch}/pools/${h}/stake`, { fetch: f, timeoutMs: 8000 }))
	);
	const pools = stakes
		.filter((s): s is Any => !!s && Number(s.active_stake) > 0)
		.map((s) => ({ poolHash: s.pool_hash, poolId: s.pool_id, stake: Number(s.active_stake) }))
		.sort((a, b) => b.stake - a.stake);
	return { pools, complete: regs.length < limit };
}
