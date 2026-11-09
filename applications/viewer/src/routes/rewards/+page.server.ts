import { tryApi } from '$lib/api/client';
import type { PageServerLoad } from './$types';

export const load: PageServerLoad = async ({ fetch }) => {
	const latest = await tryApi<{ epoch: number }>('/epochs/latest', { fetch });
	return { epoch: latest?.epoch ?? null };
};
