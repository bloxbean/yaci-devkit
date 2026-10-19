import { tryApi } from '$lib/api/client';
import { loadCommittee, loadConstitution } from '$lib/governance/data';
import { toProposal } from '$lib/governance/labels';
import { drepPower } from '$lib/server/insights';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/* Governance at a glance. Every section degrades on its own: a devnet often has no stats, constitution or votes. */
export const load: PageServerLoad = async ({ fetch }) => {
	const [stats, proposals, active, committee, constitution, special, votes] = await Promise.all([
		tryApi<Any>('/governance-state/stats', { fetch }),
		tryApi<Any[]>('/governance-state/proposals', { fetch, params: { page: 1, count: 6, order: 'desc' } }),
		tryApi<Any[]>('/governance-state/proposals/current', { fetch }),
		loadCommittee(fetch),
		loadConstitution(fetch),
		tryApi<Any[]>('/governance-state/dreps/special', { fetch }),
		tryApi<Any[]>('/governance/votes', { fetch, params: { page: 1, count: 6, order: 'desc' } })
	]);
	return {
		stats: stats as {
			epoch?: number;
			drep_stats?: { total_dreps: number; active_dreps: number; inactive_dreps: number; retired_dreps: number; total_voting_power: number };
			proposal_stats?: { active_proposals: number; ratified_proposals: number; expired_proposals: number };
			committee_stats?: { total_members: number; active_members: number; expired_members: number };
			vote_stats?: { total_votes_current_epoch: number; drep_votes: number; spo_votes: number; committee_votes: number };
		} | null,
		proposals: (proposals ?? []).map(toProposal),
		activeCount: active?.length ?? null,
		committee,
		constitution,
		special: (special ?? []) as { drep_type: string; voting_power: number }[],
		votes: (votes ?? []) as Any[],
		// Who holds the voting power, from Yaci Store analytics where the store has it (null otherwise). Streamed.
		drepPower: drepPower(fetch, stats?.epoch != null ? Number(stats.epoch) : null).catch(() => null)
	};
};
