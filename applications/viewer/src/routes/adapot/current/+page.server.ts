import { error } from '@sveltejs/kit';
import { ApiError, api, tryApi } from '$lib/api/client';
import type { AdaPot } from '$lib/epochs/pots';
import type { PageServerLoad } from './$types';

/*
 * The ada pots for the latest epoch, or for ?epoch=N, plus the last 30 epochs for the trend charts.
 * /adapot/list pages are 1-based. A fresh devnet has no pots until its first epoch boundary.
 */
export const load: PageServerLoad = async ({ url, fetch }) => {
	const raw = url.searchParams.get('epoch');
	const epoch = raw != null && raw !== '' ? Number(raw) : null;
	if (epoch != null && (!Number.isInteger(epoch) || epoch < 0)) error(404, 'Epochs are whole numbers, starting at 0.');

	const [pot, history] = await Promise.all([
		api<AdaPot>(epoch != null ? `/adapot/epochs/${epoch}` : '/adapot', { fetch }).then(
			(p) => ({ pot: p, error: null as string | null }),
			(e) => ({ pot: null, error: e instanceof ApiError && e.status === 404 ? null : (e as Error).message })
		),
		tryApi<AdaPot[]>('/adapot/list', { fetch, params: { page: 1, count: 30 } })
	]);

	const latest = history?.[0]?.epoch ?? pot.pot?.epoch ?? null;
	return { pot: pot.pot, error: pot.error, requested: epoch, latest, history: history ?? [] };
};
