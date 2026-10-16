import { tryApi } from '$lib/api/client';
import { must } from '$lib/api/load';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/* A block by number or hash, its transactions, and the max block size for the fill gauge. */
export const load: PageServerLoad = async ({ params, fetch }) => {
	const id = params.number.trim();
	const block = await must<Any>(`/blocks/${id}`, { fetch }, `No block ${id} on this chain.`);
	const [txs, protocol] = await Promise.all([
		tryApi<Any[]>(`/blocks/${block.number}/txs`, { fetch }),
		tryApi<Any>('/epochs/latest/parameters', { fetch })
	]);
	return { block, txs: txs ?? [], maxBlockSize: Number(protocol?.max_block_size) || 90112 };
};
