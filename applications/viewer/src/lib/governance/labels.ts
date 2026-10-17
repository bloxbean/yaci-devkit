import { drepHrefFor } from './drep';
/** Same tones as $lib/ui/Badge. */
export type Tone = 'neutral' | 'brand' | 'ok' | 'warn' | 'bad' | 'info' | 'violet' | 'cyan';

/* Human names and colours for Conway governance values from Yaci Store. */

export const ACTION_LABEL: Record<string, string> = {
	PARAMETER_CHANGE_ACTION: 'Parameter change',
	HARD_FORK_INITIATION_ACTION: 'Hard fork',
	TREASURY_WITHDRAWALS_ACTION: 'Treasury withdrawal',
	NO_CONFIDENCE: 'No confidence',
	UPDATE_COMMITTEE: 'Update committee',
	NEW_CONSTITUTION: 'New constitution',
	INFO_ACTION: 'Info'
};

export const ACTION_TONE: Record<string, Tone> = {
	PARAMETER_CHANGE_ACTION: 'cyan',
	HARD_FORK_INITIATION_ACTION: 'bad',
	TREASURY_WITHDRAWALS_ACTION: 'warn',
	NO_CONFIDENCE: 'bad',
	UPDATE_COMMITTEE: 'violet',
	NEW_CONSTITUTION: 'info',
	INFO_ACTION: 'neutral'
};

export const actionLabel = (t: string | null | undefined) => (t ? (ACTION_LABEL[t] ?? titleCase(t)) : 'Unknown');
export const actionTone = (t: string | null | undefined): Tone => (t ? (ACTION_TONE[t] ?? 'neutral') : 'neutral');

export const STATUS_TONE: Record<string, Tone> = {
	LIVE: 'ok',
	ACTIVE: 'ok',
	RATIFIED: 'info',
	ENACTED: 'violet',
	EXPIRED: 'neutral',
	DROPPED: 'neutral',
	INACTIVE: 'warn',
	RETIRED: 'neutral'
};

export const statusTone = (s: string | null | undefined): Tone => (s ? (STATUS_TONE[s.toUpperCase()] ?? 'neutral') : 'neutral');
export const statusLabel = (s: string | null | undefined) => (s ? (s.toUpperCase() === 'LIVE' ? 'Voting' : titleCase(s)) : 'Unknown');

export type VoterGroup = 'drep' | 'spo' | 'cc';

export function voterGroup(voterType: string | null | undefined): VoterGroup {
	const t = (voterType ?? '').toUpperCase();
	if (t.startsWith('CONSTITUTIONAL_COMMITTEE')) return 'cc';
	if (t.startsWith('STAKING_POOL')) return 'spo';
	return 'drep';
}

export const VOTER_LABEL: Record<VoterGroup, string> = { drep: 'DReps', spo: 'Stake pools', cc: 'Constitutional committee' };

export const VOTE_TONE: Record<string, Tone> = { YES: 'ok', NO: 'bad', ABSTAIN: 'neutral' };
export const voteTone = (v: string | null | undefined): Tone => VOTE_TONE[(v ?? '').toUpperCase()] ?? 'neutral';
export const voteLabel = (v: string | null | undefined) => titleCase(v ?? '—');

export const CRED_LABEL: Record<string, string> = { ADDR_KEYHASH: 'Key', SCRIPTHASH: 'Script' };
export const credLabel = (c: string | null | undefined) => (c ? (CRED_LABEL[c] ?? titleCase(c)) : '—');

export function titleCase(s: string) {
	const words = s.replace(/_ACTION$/, '').toLowerCase().split('_');
	return words.map((w, i) => (i === 0 ? w.charAt(0).toUpperCase() + w.slice(1) : w)).join(' ');
}

/** Gov action route id: "<txhash>_<index>". */
export const actionHref = (txHash: string, index: number | string) => `/governance/govactions/${txHash}_${index}`;

/** Where a voter's hash links to. DReps by hash work with the DRep page; others have no page yet. */
export function voterHref(v: { voter_type?: string; voter_hash?: string; drep_id?: string | null }): string | undefined {
	const g = voterGroup(v.voter_type);
	if (g === 'drep' && v.voter_hash) return drepHrefFor(v.voter_type, v.voter_hash, v.drep_id);
	if (g === 'spo' && v.voter_hash) return `/pools/${v.voter_hash}`;
	return undefined;
}

export const anchorHref = (url: string, hash?: string | null) =>
	`/governance/anchor?url=${encodeURIComponent(url)}${hash ? `&hash=${hash}` : ''}`;

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

export interface Proposal {
	tx_hash: string;
	index: number;
	type: string;
	status?: string | null;
	deposit?: number | string | null;
	return_address?: string | null;
	anchor_url?: string | null;
	anchor_hash?: string | null;
	epoch?: number | null;
	block_number?: number | null;
	block_time?: number | null;
	slot?: number | null;
	gov_action_id?: string | null;
	details?: unknown;
	voting_stats?: Record<string, number | null> | null;
}

/** One shape for proposals from /governance-state/proposals (has status) and /governance/proposals (has type). */
export function toProposal(p: Any): Proposal {
	const details = p.gov_action ?? p.details ?? null;
	let type = p.type ?? details?.type;
	if (!type && details) {
		if (details.protocolParamUpdate) type = 'PARAMETER_CHANGE_ACTION';
		else if (details.withdrawals) type = 'TREASURY_WITHDRAWALS_ACTION';
		else if (details.protocolVersion) type = 'HARD_FORK_INITIATION_ACTION';
		else if (details.constitution) type = 'NEW_CONSTITUTION';
		else if (details.newMembersAndTerms || details.membersForRemoval) type = 'UPDATE_COMMITTEE';
	}
	return {
		tx_hash: p.tx_hash,
		index: Number(p.index ?? 0),
		type: type ?? 'INFO_ACTION',
		status: p.status ?? null,
		deposit: p.deposit ?? null,
		return_address: p.return_address ?? null,
		anchor_url: p.anchor_url ?? null,
		anchor_hash: p.anchor_hash ?? null,
		epoch: p.epoch ?? null,
		block_number: p.block_number ?? null,
		block_time: p.block_time ?? null,
		slot: p.slot ?? null,
		gov_action_id: p.gov_action_id ?? null,
		details,
		voting_stats: p.voting_stats ?? null
	};
}

export interface Tally {
	yes: number;
	no: number;
	abstain: number;
}

export function tallyVotes(votes: { voter_type?: string; vote?: string }[]): Record<VoterGroup, Tally> {
	const t: Record<VoterGroup, Tally> = { drep: { yes: 0, no: 0, abstain: 0 }, spo: { yes: 0, no: 0, abstain: 0 }, cc: { yes: 0, no: 0, abstain: 0 } };
	for (const v of votes) {
		const k = (v.vote ?? '').toLowerCase() as keyof Tally;
		if (k in t.drep) t[voterGroup(v.voter_type)][k]++;
	}
	return t;
}
