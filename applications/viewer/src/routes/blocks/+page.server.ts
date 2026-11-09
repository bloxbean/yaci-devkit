import { api, paging } from '$lib/api/client';
import { error } from '@sveltejs/kit';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

export const load: PageServerLoad = async ({ url, fetch }) => {
	const { page, count } = paging(url, 20);
	try {
		const [data, params] = await Promise.all([
			api<Any>('/blocks', { fetch, params: { page, count } }),
			api<Any>('/epochs/latest/parameters', { fetch }).catch(() => null)
		]);
		return {
			blocks: (data?.blocks ?? []) as Any[],
			totalPages: (data?.total_pages as number) || null,
			page,
			count,
			maxBlockSize: Number(params?.max_block_size) || 90112
		};
	} catch (e) {
		error(503, (e as Error).message);
	}
};
