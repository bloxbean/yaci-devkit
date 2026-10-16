import { error, redirect } from '@sveltejs/kit';
import { tryApi } from '$lib/api/client';
import { loadSupply } from '$lib/assets/load';
import type { MintEvent } from '$lib/assets/types';
import type { PageServerLoad } from './$types';

/*
 * A fingerprint (CIP-14) names one asset. Its history gives the unit, so we send people to the asset page; if the
 * store has no history for it, show what we know here.
 */
export const load: PageServerLoad = async ({ params, fetch }) => {
	const fingerprint = params.fingerprint.trim().toLowerCase();
	if (!/^asset1[0-9a-z]{38}$/.test(fingerprint)) error(404, 'That is not an asset fingerprint (asset1…).');

	const [history, supply] = await Promise.all([
		tryApi<MintEvent[]>(`/assets/fingerprint/${fingerprint}/history`, { fetch, params: { page: 1, count: 1 }, timeoutMs: 15000 }),
		loadSupply(`/assets/fingerprint/${fingerprint}/supply`, fetch)
	]);
	const unit = history?.[0]?.unit;
	if (unit) redirect(302, `/assets/unit/${unit}`);
	if (supply.notFound) error(404, 'No asset with that fingerprint on this chain.');

	return { fingerprint, supply };
};
