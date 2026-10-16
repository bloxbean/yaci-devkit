import { error, redirect } from '@sveltejs/kit';
import { paging, tryApi } from '$lib/api/client';
import { drepIdCandidates } from '$lib/governance/drep';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/*
 * A DRep by CIP-129 id (or a CIP-105 id / hex hash, which redirect to it). State from /governance-state/dreps;
 * activity, metadata and votes from the Blockfrost-compatible endpoints when the store has them. Lists are 1-based.
 */
export const load: PageServerLoad = async ({ params, url, fetch }) => {
	const raw = params.drepId.trim();
	const candidates = drepIdCandidates(raw);
	if (!candidates.length) error(404, 'That is not a DRep id.');

	let id = candidates[0];
	let drep: Any = null;
	for (const c of candidates) {
		drep = await tryApi<Any>(`/governance-state/dreps/${c}`, { fetch });
		if (drep) {
			id = c;
			break;
		}
	}
	if (!drep) error(404, 'No DRep with that id on this chain.');
	if (id !== raw) redirect(302, `/governance/dreps/${id}${url.search}`);

	const tab = url.searchParams.get('tab') === 'votes' ? 'votes' : 'delegators';
	const { page, count } = paging(url, 20);
	const [activity, metadata, rows] = await Promise.all([
		tryApi<Any>(`/blockfrost/governance/dreps/${id}`, { fetch }),
		tryApi<Any>(`/blockfrost/governance/dreps/${id}/metadata`, { fetch }),
		tab === 'votes'
			? tryApi<Any[]>(`/blockfrost/governance/dreps/${id}/votes`, { fetch, params: { page, count, order: 'desc' } })
			: tryApi<Any[]>(`/governance/delegation-votes/drep/${id}`, { fetch, params: { page, count, order: 'desc' } })
	]);

	return {
		drep: drep as { drep_id: string; drep_hash: string; deposit: number; status: string; voting_power: number; registration_slot: number; drep_type: string },
		activity: activity as { active_epoch?: number; last_active_epoch?: number; expired?: boolean; retired?: boolean; has_script?: boolean } | null,
		metadata: metadata as { url?: string; hash?: string } | null,
		tab,
		page,
		count,
		rows: rows ?? [],
		rowsAvailable: rows !== null
	};
};
