import { tryApi } from '$lib/api/client';
import { loadConstitution } from '$lib/governance/data';
import { paramByName, paramChangeOf } from '$lib/julc/governance/action';
import type { Draft } from '$lib/julc/governance/draft';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

export interface RecentProposal {
	id: string;
	type: string;
	epoch: number | null;
	policyHash: string | null;
	summary: string;
	draft: Draft;
}

function draftOf(p: Any): Draft {
	const id = `${p.tx_hash}_${p.index}`;
	if (p.type === 'TREASURY_WITHDRAWALS_ACTION')
		return { kind: 'treasury', values: {}, withdrawals: Object.entries(p.details?.withdrawals ?? {}).map(([address, amount]) => ({ address, amount: String(amount) })), from: id };
	const change = paramChangeOf(p.details?.protocolParamUpdate);
	return { kind: 'param', values: change.values, costModels: change.costModels, withdrawals: [], from: id };
}

function summaryOf(p: Any): string {
	if (p.type === 'TREASURY_WITHDRAWALS_ACTION') {
		const total = Object.values(p.details?.withdrawals ?? {}).reduce<bigint>((s, a) => s + BigInt(String(a)), 0n);
		return `Withdraw ${(Number(total) / 1e6).toLocaleString('en-US', { maximumFractionDigits: 0 })} ADA`;
	}
	const names = Object.keys(p.details?.protocolParamUpdate ?? {}).filter((k) => k !== 'costModelsHash');
	return names.length ? names.map((n) => paramByName[n]?.label ?? (n === 'costModels' ? 'Cost models' : n)).join(', ') : 'Parameter change';
}

/*
 * The guardrail check page. The draft comes from the URL (shareable): ?type=treasury, p.<param>=<value>,
 * w=<stake address>:<lovelace>, or ?from=<tx>_<index> to start from a proposal; ?script=<hash> checks against
 * another script than the constitution's guardrail.
 */
export const load: PageServerLoad = async ({ fetch, url }) => {
	const from = url.searchParams.get('from');
	const fromMatch = from?.match(/^([0-9a-f]{64})[_#](\d+)$/i);
	const [constitution, params, paramProposals, treasuryProposals, fromRows] = await Promise.all([
		loadConstitution(fetch),
		tryApi<Any>('/epochs/latest/parameters', { fetch }),
		tryApi<Any[]>('/governance/proposals/gov-action-type/PARAMETER_CHANGE_ACTION', { fetch, params: { count: 12 } }),
		tryApi<Any[]>('/governance/proposals/gov-action-type/TREASURY_WITHDRAWALS_ACTION', { fetch, params: { count: 8 } }),
		fromMatch ? tryApi<Any[]>(`/governance/proposals/${fromMatch[1].toLowerCase()}`, { fetch }) : null
	]);

	const recent: RecentProposal[] = [...(paramProposals ?? []), ...(treasuryProposals ?? [])]
		.sort((a, b) => (b.block_time ?? 0) - (a.block_time ?? 0))
		.map((p) => ({ id: `${p.tx_hash}_${p.index}`, type: p.type, epoch: p.epoch ?? null, policyHash: p.details?.policyHash ?? null, summary: summaryOf(p), draft: draftOf(p) }));

	const fromRow = fromMatch ? (fromRows ?? []).find((p) => Number(p.index) === Number(fromMatch[2])) : null;
	let draft: Draft;
	if (fromRow && (fromRow.type === 'PARAMETER_CHANGE_ACTION' || fromRow.type === 'TREASURY_WITHDRAWALS_ACTION')) draft = draftOf(fromRow);
	else {
		const values: Record<string, string> = {};
		for (const [k, v] of url.searchParams) if (k.startsWith('p.') && paramByName[k.slice(2)]) values[k.slice(2)] = v;
		const withdrawals = url.searchParams.getAll('w').map((w) => {
			const i = w.lastIndexOf(':');
			return i > 0 ? { address: w.slice(0, i), amount: w.slice(i + 1) } : { address: w, amount: '' };
		});
		const kind = url.searchParams.get('type') === 'treasury' ? 'treasury' : 'param';
		draft = { kind, values, withdrawals, from: null };
	}

	// The script: ?script=, else the proposal's own guardrail, else the constitution's.
	const wanted = url.searchParams.get('script')?.toLowerCase() ?? fromRow?.details?.policyHash ?? constitution?.script ?? null;
	const hash = wanted && /^[0-9a-f]{56}$/.test(wanted) ? wanted : null;
	const [scriptInfo, scriptBody] = hash ? await Promise.all([tryApi<Any>(`/scripts/${hash}`, { fetch }), tryApi<{ cbor?: string }>(`/scripts/${hash}/cbor`, { fetch })]) : [null, null];

	return {
		constitution,
		params,
		recent,
		draft,
		script: hash ? { hash, type: scriptInfo?.type ?? null, cbor: scriptBody?.cbor ?? null, constitutional: hash === constitution?.script } : null
	};
};
