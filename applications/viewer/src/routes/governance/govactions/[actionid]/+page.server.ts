import { error } from '@sveltejs/kit';
import { tryApi } from '$lib/api/client';
import { toProposal } from '$lib/governance/labels';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

const VOTE_PAGE = 100;
const VOTE_PAGES = 5;

/*
 * One governance action, by "<txhash>_<index>", "<txhash>#<index>" or its gov_action1… id. Status and voting
 * stats come from /governance-state; type, details and time from /governance/proposals. Votes are read in pages
 * of 100 (1-based), up to 500, for the tally.
 */
export const load: PageServerLoad = async ({ params, fetch }) => {
	const id = decodeURIComponent(params.actionid).trim();
	let txHash: string;
	let index: number;
	let state: Any = null;

	if (/^gov_action1[0-9a-z]+$/.test(id)) {
		state = await tryApi<Any>(`/governance-state/proposals/gov-action-id/${id}`, { fetch });
		if (!state) error(404, 'No governance action with that id on this chain.');
		txHash = state.tx_hash;
		index = Number(state.index);
	} else {
		const m = id.match(/^([0-9a-fA-F]{64})(?:[_#](\d+))?$/);
		if (!m) error(404, 'That is not a governance action id. Use <txhash>_<index> or gov_action1….');
		txHash = m[1].toLowerCase();
		index = Number(m[2] ?? 0);
	}

	const [stateRow, base, firstVotes] = await Promise.all([
		state ? Promise.resolve(state) : tryApi<Any>(`/governance-state/proposals/${txHash}/${index}`, { fetch }),
		tryApi<Any[]>(`/governance/proposals/${txHash}`, { fetch }),
		tryApi<Any[]>(`/governance/proposals/${txHash}/${index}/votes`, { fetch, params: { page: 1, count: VOTE_PAGE, order: 'desc' } })
	]);
	const baseRow = (base ?? []).find((p) => Number(p.index) === index) ?? null;
	if (!stateRow && !baseRow) error(404, 'No governance action with that id on this chain.');

	let votes = firstVotes ?? [];
	if (votes.length === VOTE_PAGE) {
		const more = await Promise.all(
			Array.from({ length: VOTE_PAGES - 1 }, (_, i) =>
				tryApi<Any[]>(`/governance/proposals/${txHash}/${index}/votes`, { fetch, params: { page: i + 2, count: VOTE_PAGE, order: 'desc' } })
			)
		);
		for (const page of more) votes = votes.concat(page ?? []);
	}

	const proposal = toProposal({ ...(baseRow ?? {}), ...(stateRow ?? {}), type: baseRow?.type ?? stateRow?.gov_action?.type, details: baseRow?.details });
	return {
		proposal,
		votes,
		votesPartial: votes.length >= VOTE_PAGE * VOTE_PAGES,
		votesAvailable: firstVotes !== null
	};
};
