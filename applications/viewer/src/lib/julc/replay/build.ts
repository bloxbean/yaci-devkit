import { bech32Decode } from '$lib/bech32';
import { encodePlutusData } from '$lib/cbor';
import { encodeAction, encodeProcedure, mockActionType } from '../governance/action';
import type { DataInput, MockCertificate, MockDatum, MockTransaction, MockTxIn, MockTxOut, MockValue, MockVote, PlutusVersion, Target, TransactionRequest } from '../types';
import { slotToPosixMs, type SlotConfig } from './slots';

/*
 * Rebuild a historical transaction from Yaci Store data as a julc MockTransaction, one per redeemer, so each script
 * can be re-run in the browser with the cost model of the epoch it ran in. Pure: all store data is passed in.
 *
 * Ledger order matters (purpose indexes and map order in the ScriptContext): inputs and reference inputs by
 * (tx id, index); mint and values by policy, then asset name; withdrawals and votes by credential, with script
 * credentials before key hashes, as in cardano-ledger.
 *
 * Known gaps (julc#244) are reported as notes instead of failing silently.
 */

export interface StoreUtxo {
	tx_hash: string;
	output_index: number;
	address: string;
	amount: { unit: string; quantity: string | number }[];
	data_hash?: string | null;
	inline_datum?: string | null;
	reference_script_hash?: string | null;
}

export interface StoreScriptRun {
	script_hash: string;
	type?: string | null;
	script_content?: string | null;
	redeemer?: { tag?: string; index?: number; data?: string; ex_units?: { mem: number; steps: number } } | string | null;
	datum?: string | null;
	datum_hash?: string | null;
}

export interface StoreVote {
	voter_type: string;
	voter_hash: string;
	gov_action_tx_hash: string;
	gov_action_index: number;
	vote: string;
}

export interface StoreCert {
	cert_index: number;
	/** 'other': a certificate kind julc can't put into a context yet (julc#244), kept so indexes stay right. */
	type: 'registration' | 'deregistration' | 'delegation' | 'other';
	address: string;
	pool_id?: string | null;
}

/** A proposal of the transaction (/governance/proposals/{tx}). */
export interface StoreProposal {
	index: number;
	type: string;
	deposit: number | string;
	return_address: string;
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	details?: any;
}

export interface ReplayInput {
	hash: string;
	tx: {
		fees: number | string;
		ttl?: number | null;
		validity_interval_start?: number | null;
		required_signers?: string[] | null;
		inputs: StoreUtxo[];
		outputs: StoreUtxo[];
		reference_inputs?: StoreUtxo[] | null;
	};
	scripts: StoreScriptRun[];
	withdrawals: { address: string; amount: number | string }[];
	mints: { unit: string; quantity: number | string }[];
	votes: StoreVote[];
	certificates: StoreCert[];
	/** The transaction's proposals, needed by proposing (guardrail) scripts. */
	proposals?: StoreProposal[];
	/** CBOR of datums known by hash (for inputs that carry a datum hash). */
	datums: Record<string, string>;
	/** The epoch the parameters are from; null when the transaction's own epoch wasn't in the store. */
	paramsEpoch?: number | null;
	params: {
		cost_models_raw?: Record<string, (number | string)[]> | null;
		protocol_major_ver?: number | null;
		max_tx_ex_mem?: number | string | null;
		max_tx_ex_steps?: number | string | null;
		[param: string]: unknown;
	} | null;
	slots: SlotConfig | null;
	/** Where the transaction came from: its stored CBOR (exact) or Yaci Store's JSON (rebuilt). */
	source?: 'cbor' | 'store';
	/** The datums the transaction witnesses, when known exactly (from its CBOR); otherwise they are inferred. */
	witnessDatums?: { hash: string; datum: string }[];
	/** The ProposalProcedures as Plutus data CBOR, when known exactly (from its CBOR); otherwise encoded from JSON. */
	procedures?: string[];
}

export type Purpose = 'spend' | 'mint' | 'reward' | 'cert' | 'vote' | 'propose';

export interface RunPlan {
	key: string;
	purpose: Purpose;
	index: number;
	scriptHash: string;
	language: PlutusVersion | null;
	declared: { mem: bigint; steps: bigint } | null;
	redeemer: string | null;
	/** CBOR of the spent output's datum (spend runs): the first argument of a V1/V2 spending script. */
	datum: string | null;
	/**
	 * Inputs whose datum is given by hash. julc ignores the datum of an input that also has a hash (it only adds
	 * datums given without one, under a hash it computes itself), so a spending script's datum would be missing.
	 * julc gets such datums inline (V2/V3) or without their hash (V1, which has no inline datums), and
	 * fix-context.ts puts the on-chain hash back.
	 */
	hashedDatums: { input: number; hash: string; datum: string }[];
	/**
	 * Datums the transaction likely witnesses (inputs' and outputs' datums known by hash), added to txInfoData by
	 * fix-context.ts under their on-chain hash (V2/V3). An extra entry is harmless; a missing one breaks lookups.
	 */
	witnessDatums: { hash: string; datum: string }[];
	/** CBOR of the transaction's ProposalProcedures, put into the context by fix-context.ts (julc builds them roughly). */
	proposals: string[];
	/** null when this run can't be rebuilt; `skip` says why. */
	request: TransactionRequest | null;
	skip?: string;
	/** What may differ from the ledger's context, shown with the result. */
	notes: string[];
	/** Rebuilt from the transaction's stored CBOR (exact) or from Yaci Store's JSON (the store has no CBOR). */
	source?: 'cbor' | 'store';
}

const hexCmp = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);
const byRef = (a: StoreUtxo, b: StoreUtxo) => hexCmp(a.tx_hash, b.tx_hash) || a.output_index - b.output_index;

/** Purpose names vary by store and era: spend/spending, cert/certifying, reward/rewarding, vote/voting, … */
export function purposeOf(tag: unknown): Purpose | null {
	const t = String(tag ?? '').toLowerCase();
	if (t.startsWith('spend')) return 'spend';
	if (t.startsWith('mint')) return 'mint';
	if (t.startsWith('cert')) return 'cert';
	if (t.startsWith('reward') || t.startsWith('withdraw')) return 'reward';
	if (t.startsWith('vot')) return 'vote';
	if (t.startsWith('propos')) return 'propose';
	return null;
}

export function plutusVersion(type: string | null | undefined): PlutusVersion | null {
	const m = String(type ?? '').match(/plutus\D*v?_?([123])/i);
	return m ? (`V${m[1]}` as PlutusVersion) : null;
}

function redeemerOf(s: StoreScriptRun) {
	if (!s.redeemer) return null;
	if (typeof s.redeemer !== 'string') return s.redeemer;
	try {
		return JSON.parse(s.redeemer);
	} catch {
		return null;
	}
}

function scriptCbor(s: StoreScriptRun): string | null {
	if (!s.script_content) return null;
	try {
		const c = JSON.parse(s.script_content);
		return typeof c?.content === 'string' ? c.content : null;
	} catch {
		return /^[0-9a-f]+$/i.test(s.script_content) ? s.script_content : null;
	}
}

/** A store amount list as a julc value, in ledger order. */
export function value(amount: StoreUtxo['amount']): MockValue {
	const assets = amount
		.filter((a) => a.unit !== 'lovelace')
		.map((a) => ({ policyId: a.unit.slice(0, 56), tokenName: a.unit.slice(56), quantity: String(a.quantity) }))
		.sort((a, b) => hexCmp(a.policyId, b.policyId) || hexCmp(a.tokenName, b.tokenName));
	return { lovelace: String(amount.find((a) => a.unit === 'lovelace')?.quantity ?? 0), assets };
}

const cbor = (value: string): DataInput => ({ format: 'cbor', value });

function datum(u: StoreUtxo, datums: Record<string, string>, attach: boolean): MockDatum {
	if (u.inline_datum) return { kind: 'inline', data: cbor(u.inline_datum) };
	if (u.data_hash) {
		const known = attach ? datums[u.data_hash] : undefined;
		return known ? { kind: 'hash', hash: u.data_hash, data: cbor(known) } : { kind: 'hash', hash: u.data_hash };
	}
	return { kind: 'none' };
}

const isShelley = (address: string) => /^addr(_test)?1/.test(address);


/** Ledger order for credentials: script hashes before key hashes, then by hash. */
function credentialKey(stakeAddress: string): string {
	const d = bech32Decode(stakeAddress);
	if (!d || d.bytes.length < 29) return '2' + stakeAddress;
	const script = (d.bytes[0] & 0x10) !== 0;
	const hash = [...d.bytes.slice(1)].map((b) => b.toString(16).padStart(2, '0')).join('');
	return (script ? '0' : '1') + hash;
}

/** A reward account (hex, header byte first) as a julc credential. */
function rewardCredential(hex: string): string {
	const script = (parseInt(hex.slice(0, 2), 16) & 0x10) !== 0;
	return (script ? 'script:' : 'key:') + hex.slice(2);
}

const VOTERS: Record<string, { voterType: MockVote['voterType']; prefix: string; order: number }> = {
	CONSTITUTIONAL_COMMITTEE_HOT_SCRIPT_HASH: { voterType: 'committee', prefix: 'script:', order: 0 },
	CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH: { voterType: 'committee', prefix: 'key:', order: 1 },
	DREP_SCRIPT_HASH: { voterType: 'drep', prefix: 'script:', order: 2 },
	DREP_KEY_HASH: { voterType: 'drep', prefix: 'key:', order: 3 },
	STAKING_POOL_KEY_HASH: { voterType: 'pool', prefix: '', order: 4 }
};

function votes(list: StoreVote[]): MockVote[] {
	return list
		.filter((v) => VOTERS[v.voter_type])
		.sort((a, b) => VOTERS[a.voter_type].order - VOTERS[b.voter_type].order || hexCmp(a.voter_hash, b.voter_hash) || hexCmp(a.gov_action_tx_hash, b.gov_action_tx_hash) || a.gov_action_index - b.gov_action_index)
		.map((v) => ({
			voterType: VOTERS[v.voter_type].voterType,
			voter: VOTERS[v.voter_type].prefix + v.voter_hash,
			actionTxId: v.gov_action_tx_hash,
			actionIndex: BigInt(v.gov_action_index),
			vote: v.vote.toLowerCase() as MockVote['vote']
		}));
}

function certificates(list: StoreCert[]): { certs: MockCertificate[]; complete: boolean } {
	const sorted = [...list].sort((a, b) => a.cert_index - b.cert_index);
	const complete = sorted.every((c, i) => c.cert_index === i && c.type !== 'other');
	const certs = sorted.filter((c) => c.type !== 'other').map<MockCertificate>((c) =>
		c.type === 'delegation'
			? { type: 'delegStaking', credential: c.address, poolId: c.pool_id ?? null }
			: { type: c.type === 'registration' ? 'regStaking' : 'unregStaking', credential: c.address }
	);
	return { certs, complete };
}

export function buildRuns(input: ReplayInput): RunPlan[] {
	// Yaci Store reports a missing TTL or validity start as 0. A real TTL of slot 0 is impossible, and a start of
	// slot 0 is the same as none for any script, so 0 means "no bound".
	const tx = { ...input.tx, ttl: input.tx.ttl || null, validity_interval_start: input.tx.validity_interval_start || null };
	const inputs = [...tx.inputs].sort(byRef);
	const refs = [...(tx.reference_inputs ?? [])].sort(byRef);
	const outputs = [...tx.outputs].sort((a, b) => a.output_index - b.output_index);
	const runs = input.scripts.map((s) => ({ s, r: redeemerOf(s) })).filter((x) => x.r?.tag);
	const proposals = [...(input.proposals ?? [])].sort((a, b) => a.index - b.index);

	// Datums the scripts endpoint resolved for spent inputs are as good as /scripts/datum lookups.
	const datums = { ...input.datums };
	for (const { s } of runs) if (s.datum && s.datum_hash) datums[s.datum_hash] ??= s.datum;

	const pv = input.params?.protocol_major_ver ?? null;
	const protocol = pv == null ? 11 : Math.min(11, Math.max(10, pv));
	const { certs, complete: certsComplete } = certificates(input.certificates);
	const byron = [...inputs, ...refs, ...outputs].some((u) => !isShelley(u.address));

	const common: Omit<MockTransaction, 'purpose' | 'redeemer'> = {
		inputs: inputs.map<MockTxIn>((u) => ({ txId: u.tx_hash, index: BigInt(u.output_index), address: { payment: u.address }, value: value(u.amount), datum: datum(u, datums, true), referenceScript: u.reference_script_hash || null })),
		referenceInputs: refs.map<MockTxIn>((u) => ({ txId: u.tx_hash, index: BigInt(u.output_index), address: { payment: u.address }, value: value(u.amount), datum: datum(u, datums, false), referenceScript: u.reference_script_hash || null })),
		// Output datums given by hash are left unresolved: the ledger only includes them when the tx witnesses them.
		outputs: outputs.map<MockTxOut>((u) => ({ address: { payment: u.address }, value: value(u.amount), datum: datum(u, datums, false), referenceScript: u.reference_script_hash || null })),
		fee: String(tx.fees),
		mint: input.mints
			.map((m) => ({ policyId: m.unit.slice(0, 56), tokenName: m.unit.slice(56), quantity: String(m.quantity) }))
			.sort((a, b) => hexCmp(a.policyId, b.policyId) || hexCmp(a.tokenName, b.tokenName)),
		certificates: certs,
		withdrawals: [...input.withdrawals]
			.sort((a, b) => hexCmp(credentialKey(a.address), credentialKey(b.address)))
			.map((w) => ({ credential: w.address, amount: String(w.amount) })),
		validRange: {
			from: tx.validity_interval_start != null && input.slots ? slotToPosixMs(tx.validity_interval_start, input.slots) : null,
			fromInclusive: true,
			to: tx.ttl != null && input.slots ? slotToPosixMs(tx.ttl, input.slots) : null,
			toInclusive: false
		},
		signatories: [...(tx.required_signers ?? [])].sort(hexCmp),
		datums: [],
		txId: input.hash,
		votes: votes(input.votes),
		proposals: proposals.map((p) => {
			const c = credentialKey(p.return_address);
			return {
				deposit: String(p.deposit),
				returnCredential: (c.startsWith('0') ? 'script:' : 'key:') + c.slice(1),
				actionType: mockActionType(p.type),
				guardrail: p.details?.policyHash ?? null,
				withdrawals: p.type === 'TREASURY_WITHDRAWALS_ACTION' ? Object.entries(p.details?.withdrawals ?? {}).map(([address, amount]) => ({ credential: rewardCredential(address), amount: String(amount) })) : null
			};
		})
	};

	// The real governance actions, which replace julc's in the context.
	let procedures: string[] = [];
	let proceduresError: string | null = null;
	try {
		procedures = input.procedures ?? proposals.map((p) => encodePlutusData(encodeProcedure(p.deposit, p.return_address, encodeAction(p.type, p.details, input.params))));
	} catch (e) {
		proceduresError = (e as Error).message;
	}

	// Datums known by hash go to julc inline (V2/V3) or without their hash (V1), and are restored afterwards.
	const hashedDatums: RunPlan['hashedDatums'] = [];
	const inlinedInputs = common.inputs.map((i, n) => {
		if (i.datum.kind !== 'hash' || !('data' in i.datum) || !i.datum.data) return i;
		hashedDatums.push({ input: n, hash: i.datum.hash, datum: i.datum.data.value });
		return { ...i, datum: { kind: 'inline' as const, data: i.datum.data } };
	});

	const v1HashedDatums: RunPlan['hashedDatums'] = [];
	const v1Inputs = common.inputs.map((i, n) => {
		if (i.datum.kind !== 'hash' || !('data' in i.datum) || !i.datum.data) return i;
		v1HashedDatums.push({ input: n, hash: i.datum.hash, datum: i.datum.data.value });
		return { ...i, datum: { kind: 'hash' as const, hash: '', data: i.datum.data } };
	});

	const witnessDatums =
		input.witnessDatums ??
		[...new Set([...inputs, ...outputs].map((u) => u.data_hash).filter((h): h is string => !!h && !!datums[h]))].map((hash) => ({
			hash,
			datum: datums[hash]
		}));
	const source = input.source ?? 'store';

	return runs.map(({ s, r }) => {
		const tag = purposeOf(r.tag) ?? (String(r.tag) as Purpose);
		const index = Number(r.index ?? 0);
		// Voting and proposing scripts only exist in Plutus V3, so a missing type can be inferred.
		const language = plutusVersion(s.type) ?? (tag === 'vote' || tag === 'propose' ? 'V3' : null);
		const plan: RunPlan = {
			key: `${tag}#${index}`,
			purpose: tag,
			index,
			scriptHash: s.script_hash,
			language,
			declared: r.ex_units ? { mem: BigInt(r.ex_units.mem), steps: BigInt(r.ex_units.steps) } : null,
			redeemer: r.data ?? null,
			datum: null,
			hashedDatums: [],
			witnessDatums: [],
			proposals: [],
			request: null,
			notes: [],
			source
		};
		const code = scriptCbor(s);
		const fail = (why: string) => ((plan.skip = why), plan);
		if (tag === 'propose' && !proposals[index]) return fail(`The store did not return proposal #${index} of this transaction.`);
		if (tag === 'propose' && proceduresError) return fail(`The proposal couldn’t be encoded: ${proceduresError}`);
		if (!language) return fail('Not a Plutus script.');
		if (!code) return fail('The store did not return the script code.');
		if (!r.data) return fail('The store did not return the redeemer data.');
		if (byron) return fail('The transaction has Byron-era (bootstrap) addresses, which julc can’t represent yet.');
		if ((tx.validity_interval_start != null || tx.ttl != null) && !input.slots) return fail('Slot times for this network are unknown, so the validity range can’t be rebuilt.');

		let purpose: MockTransaction['purpose'];
		switch (tag) {
			case 'spend':
				if (index >= inputs.length) return fail(`The transaction has no input #${index}.`);
				purpose = { type: 'spend', index };
				plan.datum = inputs[index].inline_datum ?? (inputs[index].data_hash ? (datums[inputs[index].data_hash!] ?? null) : null);
				break;
			case 'mint':
				purpose = { type: 'mint', target: s.script_hash };
				break;
			case 'reward':
				purpose = { type: 'reward', target: `script:${s.script_hash}` };
				break;
			case 'cert':
				if (!certsComplete || index >= certs.length) return fail('This certificate kind can’t be rebuilt from the store yet (julc#244).');
				purpose = { type: 'certify', index };
				break;
			case 'vote': {
				const voter = common.votes.find((v) => v.voter === `script:${s.script_hash}`);
				if (!voter) return fail('The vote cast by this script was not found in the store.');
				purpose = { type: 'vote', target: voter.voter, voterType: voter.voterType };
				break;
			}
			case 'propose':
				purpose = { type: 'propose', index };
				break;
			default:
				return fail(`Unknown purpose “${tag}”.`);
		}

		if (runs.length > 1) plan.notes.push('Only this script’s redeemer is in the context (julc#244). A script that reads the other redeemers, such as a withdraw-zero validator, can fail here although it passed on chain.');
		if (input.certificates.length && !certsComplete) plan.notes.push('Some certificates in this transaction couldn’t be rebuilt, so the context lists only part of them.');
		if (input.paramsEpoch === null) plan.notes.push('The store has no parameters for this transaction’s epoch, so the latest epoch’s cost model was used.');
		if (language === 'V1') plan.notes.push('Plutus V1 contexts from julc 0.1.0-pre18 can differ from the ledger’s (V1/V2 translation fixes are on julc main), so some V1 runs fail here although they passed on chain.');
		if (pv != null && pv !== protocol) plan.notes.push(`Evaluated with protocol ${protocol} rules; the transaction ran under protocol ${pv}.`);
		if (pv === 10 && language === 'V3') plan.notes.push('Under protocol 10 the ledger costed some builtins differently from julc, so CPU can differ slightly from the declared budget (memory is unaffected).');

		const target: Target = { language: `Plutus${language}`, protocol };
		const costs = input.params?.cost_models_raw?.[`Plutus${language}`];
		if (!costs?.length) plan.notes.push('The store has no cost model for this epoch, so julc’s built-in one was used.');
		plan.hashedDatums = language === 'V1' ? v1HashedDatums : hashedDatums;
		plan.witnessDatums = witnessDatums;
		plan.proposals = language === 'V3' ? procedures : [];
		plan.request = {
			script: { script: code, language },
			transaction: { ...common, inputs: language === 'V1' ? v1Inputs : inlinedInputs, purpose, redeemer: cbor(r.data) },
			target,
			costModel: costs?.length ? { target, parameters: costs.map((c) => BigInt(c)) } : null,
			// The tx limits of that epoch (julc's defaults otherwise), so a script that ran near them still fits.
			...(input.params?.max_tx_ex_steps ? { maxCpu: BigInt(input.params.max_tx_ex_steps) } : {}),
			...(input.params?.max_tx_ex_mem ? { maxMem: BigInt(input.params.max_tx_ex_mem) } : {})
		};
		return plan;
	});
}
