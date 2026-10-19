import { tryApi } from '$lib/api/client';
import { networkInsights } from '$lib/server/insights';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/* First paint for the dashboard. After that the live layer keeps it current. Analytics charts stream in after. */
export const load: PageServerLoad = async ({ fetch }) => {
	const [blocks, txs, network, epoch, params] = await Promise.all([
		tryApi<Any>('/blocks', { fetch, params: { page: 1, count: 16 } }),
		tryApi<Any>('/txs', { fetch, params: { page: 1, count: 12 } }),
		tryApi<Any>('/network', { fetch }),
		tryApi<Any>('/epochs/latest', { fetch }),
		tryApi<Any>('/epochs/latest/parameters', { fetch })
	]);
	return {
		blocks: blocks?.blocks ?? [],
		txs: txs?.transaction_summaries ?? [],
		network: network as { supply?: Record<string, number>; stake?: { active?: number } } | null,
		epoch: epoch as { epoch: number; start_time: number; end_time: number; tx_count?: number; block_count?: number } | null,
		maxBlockSize: Number(params?.max_block_size) || 90112,
		reachable: blocks !== null,
		// Charts from Yaci Store analytics, where the store has it (null otherwise). Streamed, and cached on the server.
		insights: networkInsights(fetch, epoch?.epoch != null ? Number(epoch.epoch) : null).catch(() => null)
	};
};
