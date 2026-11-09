import { error } from '@sveltejs/kit';
import { api, paging, tryApi } from '$lib/api/client';
import { must } from '$lib/api/load';
import { isPoolId } from '$lib/staking/pool';
import { fetchPoolMetadata } from '$lib/staking/poolMetadata.server';
import { poolInsights } from '$lib/server/insights';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

const TABS = ['delegators', 'blocks', 'rewards'] as const;
type Tab = (typeof TABS)[number];

/*
 * A pool by bech32 id or hex hash: its latest registration, stake this epoch (and a few before, for a trend), blocks
 * made this epoch, and the open tab's list. Yaci Store lists here are 1-based.
 */
export const load: PageServerLoad = async ({ params, url, fetch }) => {
	const id = params.pool_id.trim();
	if (!isPoolId(id)) error(404, 'That is not a pool id. Pool ids start with pool1, or are a 56-character hex hash.');

	const latest = await tryApi<Any>('/epochs/latest', { fetch });
	const epoch = Number(latest?.epoch ?? 0);
	const pool = await must<Any>(`/pools/pools/${id}/epochs/${epoch}`, { fetch }, 'No stake pool with that id on this chain.');
	const hash: string = pool.pool_hash;

	const tab: Tab = (TABS as readonly string[]).includes(url.searchParams.get('tab') ?? '') ? (url.searchParams.get('tab') as Tab) : 'delegators';
	const { page, count } = paging(url, 20);
	const rewardEpoch = Math.max(0, parseInt(url.searchParams.get('epoch') ?? '', 10) || epoch);

	const stakeAt = (e: number) =>
		e < 0 ? Promise.resolve(null) : tryApi<Any>(`/epochs/${e}/pools/${hash}/stake`, { fetch, timeoutMs: 8000 }).then((s) => (s ? { epoch: e, stake: Number(s.active_stake) } : null));

	const list = async (): Promise<{ rows: Any[]; error: string | null }> => {
		if (tab === 'blocks') return { rows: [], error: null };
		const path = tab === 'delegators' ? `/epochs/${epoch}/pools/${hash}/delegators` : `/pools/${hash}/epochs/${rewardEpoch}/rewards`;
		try {
			const rows = await api<Any[]>(path, { fetch, params: { page, count }, timeoutMs: 25000 });
			return { rows: rows ?? [], error: null };
		} catch (e) {
			return { rows: [], error: (e as Error).message };
		}
	};

	const [trend, total, blocks, metadata, rows] = await Promise.all([
		Promise.all([0, 1, 2, 3, 4, 5].map((i) => stakeAt(epoch - i))),
		tryApi<Any>(`/epochs/${epoch}/total-stake`, { fetch, timeoutMs: 8000 }),
		tryApi<Any[]>(`/blocks/pool/${hash}`, { fetch, params: { epoch }, timeoutMs: 10000 }),
		fetchPoolMetadata(pool.metadata_url),
		list()
	]);

	return {
		pool,
		epoch,
		tab,
		page,
		count,
		rewardEpoch,
		stake: trend[0]?.stake ?? null,
		stakeTrend: trend.filter((t): t is { epoch: number; stake: number } => t !== null).reverse(),
		totalStake: total?.active_stake != null ? Number(total.active_stake) : null,
		blocks: blocks ? [...blocks].reverse() : null,
		metadata,
		rows: rows.rows,
		listError: rows.error,
		// Blocks, stake and delegators over 50 epochs, from Yaci Store analytics where the store has it. Streamed.
		history: /^[0-9a-f]{56}$/.test(hash) ? poolInsights(hash, fetch, epoch || null).catch(() => null) : null
	};
};
