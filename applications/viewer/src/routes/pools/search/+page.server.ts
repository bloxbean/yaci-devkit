import { tryApi } from '$lib/api/client';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/* Recent pool activity, so the lookup page has something to click. */
export const load: PageServerLoad = async ({ fetch }) => {
	const [registrations, retirements, epoch] = await Promise.all([
		tryApi<Any[]>('/pools/registrations', { fetch, params: { page: 1, count: 10 } }),
		tryApi<Any[]>('/pools/retirements', { fetch, params: { page: 1, count: 6 } }),
		tryApi<Any>('/epochs/latest', { fetch })
	]);
	return { registrations: registrations ?? [], retirements: retirements ?? [], epoch: (epoch?.epoch as number) ?? null };
};
