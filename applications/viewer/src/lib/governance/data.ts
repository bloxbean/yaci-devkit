import { tryApi } from '$lib/api/client';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

export interface CommitteeMember {
	hash: string;
	cred_type: string;
	start_epoch: number | null;
	expired_epoch: number | null;
	active: boolean;
}

export interface Committee {
	threshold: { numerator: number; denominator: number } | null;
	members: CommitteeMember[];
	epoch: number | null;
}

/* The current committee, members de-duplicated by hash (the store can list a member once per term). */
export async function loadCommittee(f: typeof fetch): Promise<Committee | null> {
	const [c, epoch] = await Promise.all([tryApi<Any>('/governance/committees/current', { fetch: f }), tryApi<Any>('/epochs/latest', { fetch: f })]);
	if (!c) return null;
	const current = epoch?.epoch ?? null;
	const byHash = new Map<string, CommitteeMember>();
	for (const m of c.members ?? []) {
		const prev = byHash.get(m.hash);
		if (prev && (prev.expired_epoch ?? 0) >= (m.expired_epoch ?? 0)) continue;
		byHash.set(m.hash, {
			hash: m.hash,
			cred_type: m.cred_type,
			start_epoch: m.start_epoch ?? null,
			expired_epoch: m.expired_epoch ?? null,
			active: current == null || m.expired_epoch == null || m.expired_epoch >= current
		});
	}
	const members = [...byHash.values()].sort((a, b) => Number(b.active) - Number(a.active) || (a.expired_epoch ?? 0) - (b.expired_epoch ?? 0));
	const d = Number(c.threshold_denominator);
	return {
		threshold: d ? { numerator: Number(c.threshold_numerator), denominator: d } : null,
		members,
		epoch: current
	};
}

export interface Constitution {
	active_epoch?: number;
	anchor_url?: string;
	anchor_hash?: string;
	script?: string | null;
}

export async function loadConstitution(f: typeof fetch): Promise<Constitution | null> {
	return (await tryApi<Constitution>('/governance/constitution', { fetch: f })) ?? (await tryApi<Constitution>('/governance/live/constitution', { fetch: f }));
}
