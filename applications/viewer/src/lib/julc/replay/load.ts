import { api, tryApi } from '$lib/api/client';
import { purposeOf, type ReplayInput, type StoreCert, type StoreProposal, type StoreScriptRun, type StoreUtxo, type StoreVote } from './build';
import { slotConfigFor, type Genesis } from './slots';

/*
 * The store data a replay needs beyond what the transaction page already loaded: the epoch's cost models and
 * protocol version, genesis (slot times), votes, certificates and datums referenced by hash. Fetched in the
 * browser only when the user asks for a re-run.
 */

export interface PageTx {
	hash: string;
	tx: ReplayInput['tx'];
	scripts: StoreScriptRun[];
	withdrawals: { address: string; amount: number | string }[];
	mints: { unit: string; quantity: number | string }[];
	epoch: number | null;
}

function safeJson(text: string) {
	try {
		return JSON.parse(text);
	} catch {
		return null;
	}
}

interface BfStake {
	cert_index: number;
	address: string;
	registration: boolean;
}
interface BfDelegation {
	cert_index: number;
	address: string;
	pool_id: string;
}

export async function loadReplayInput(page: PageTx): Promise<ReplayInput> {
	const { hash, tx } = page;
	// Certificates and votes are only fetched when a script runs for one: some stores don't allow these
	// endpoints from the browser (no CORS), and most transactions have neither.
	const tags = new Set(page.scripts.map((s) => purposeOf((typeof s.redeemer === 'string' ? safeJson(s.redeemer) : s.redeemer)?.tag)));
	const [epochParams, genesis, votes, stakes, delegations, proposals] = await Promise.all([
		page.epoch != null ? tryApi<ReplayInput['params']>(`/epochs/${page.epoch}/parameters`) : null,
		tryApi<Genesis>('/genesis'),
		tags.has('vote') ? tryApi<StoreVote[]>(`/governance/votes/txs/${hash}`) : null,
		tags.has('cert') ? tryApi<BfStake[]>(`/blockfrost/txs/${hash}/stakes`) : null,
		tags.has('cert') ? tryApi<BfDelegation[]>(`/blockfrost/txs/${hash}/delegations`) : null,
		tags.has('propose') ? tryApi<StoreProposal[]>(`/governance/proposals/${hash}`) : null
	]);
	// A store may lack an epoch's parameters (a devnet's epoch 0): use the latest ones, and say so.
	const params = epochParams ?? (await api<ReplayInput['params']>('/epochs/latest/parameters'));

	// Datums given by hash on inputs and outputs, unless the scripts endpoint already resolved them.
	const known = new Set(page.scripts.filter((s) => s.datum && s.datum_hash).map((s) => s.datum_hash));
	const wanted = [...new Set([...tx.inputs, ...tx.outputs].map((u: StoreUtxo) => u.data_hash).filter((h): h is string => !!h && !known.has(h)))];
	const found = await Promise.all(wanted.map((h) => tryApi<{ cbor: string }>(`/scripts/datum/${h}/cbor`).then((d) => [h, d?.cbor] as const)));
	const datums: Record<string, string> = {};
	for (const [h, c] of found) if (c) datums[h] = c;

	const certificates: StoreCert[] = [
		...(Array.isArray(stakes) ? stakes : []).map((s) => ({ cert_index: s.cert_index, type: s.registration ? ('registration' as const) : ('deregistration' as const), address: s.address })),
		...(Array.isArray(delegations) ? delegations : []).map((d) => ({ cert_index: d.cert_index, type: 'delegation' as const, address: d.address, pool_id: d.pool_id }))
	];

	// Some stores leave out the script body (or its type) of a run; fetch those by hash. A proposing run has no
	// script hash in the store: it is the guardrail its proposal names.
	const byIndex = new Map((Array.isArray(proposals) ? proposals : []).map((p) => [Number(p.index), p]));
	const scripts = await Promise.all(
		page.scripts.map(async (run) => {
			const r = typeof run.redeemer === 'string' ? safeJson(run.redeemer) : run.redeemer;
			const guardrail = !run.script_hash && purposeOf(r?.tag) === 'propose' ? byIndex.get(Number(r?.index ?? 0))?.details?.policyHash : null;
			const s = guardrail ? { ...run, script_hash: guardrail } : run;
			if (!s.script_hash) return s;
			if (!s.redeemer || (s.script_content && s.type)) return s;
			const [info, body] = await Promise.all([s.type ? null : tryApi<{ type?: string }>(`/scripts/${s.script_hash}`), s.script_content ? null : tryApi<{ cbor?: string }>(`/scripts/${s.script_hash}/cbor`)]);
			return { ...s, type: s.type ?? info?.type ?? null, script_content: s.script_content ?? (body?.cbor ? JSON.stringify({ content: body.cbor }) : null) };
		})
	);

	return {
		paramsEpoch: epochParams ? page.epoch : null,
		hash,
		tx,
		scripts,
		withdrawals: page.withdrawals,
		mints: page.mints,
		votes: Array.isArray(votes) ? votes : [],
		certificates,
		proposals: Array.isArray(proposals) ? proposals : [],
		datums,
		params,
		slots: slotConfigFor(genesis?.network_magic, genesis)
	};
}

/** Another epoch's parameters (cost models, protocol version), to run a transaction under them. */
export const loadParams = (epoch: number) => api<ReplayInput['params']>(`/epochs/${epoch}/parameters`);

/** A transaction as the re-run needs it, loaded in the browser (the transaction page loads the same on the server). */
export async function loadPageTx(hash: string, epoch: number | null = null, scripts?: StoreScriptRun[]): Promise<PageTx> {
	const [tx, s, withdrawals, mints] = await Promise.all([
		api<PageTx['tx'] & { block_height: number }>(`/txs/${hash}`),
		scripts ? Promise.resolve(scripts) : tryApi<StoreScriptRun[]>(`/txs/${hash}/scripts`),
		tryApi<PageTx['withdrawals']>(`/txs/${hash}/withdrawals`),
		tryApi<{ unit: string; quantity: number | string }[]>(`/assets/txs/${hash}`)
	]);
	const e = epoch ?? (await tryApi<{ epoch: number }>(`/blocks/${tx.block_height}`))?.epoch ?? null;
	return { hash, tx, scripts: s ?? [], withdrawals: withdrawals ?? [], mints: (mints ?? []).map((m) => ({ unit: m.unit, quantity: m.quantity })), epoch: e };
}
