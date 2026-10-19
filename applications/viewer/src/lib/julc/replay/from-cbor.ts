import { bech32Decode, bech32Encode } from '$lib/bech32';
import { encodePlutusData, type PlutusData } from '$lib/cbor';
import { bytesToHex, untag, type CborItem } from '$lib/tx/cbor-item';
import { paymentCredential, type Credential, type DecodedTx, type Proposal, type TxOutput } from '$lib/tx/decode';
import type { ReplayInput, StoreCert, StoreProposal, StoreScriptRun, StoreUtxo, StoreVote } from './build';

/*
 * A replay from the transaction's own CBOR (when Yaci Store saved it): the decoded body and witnesses are turned into
 * the same ReplayInput the store-JSON path builds, so the rest of the pipeline (buildRuns, context fixes, debugger)
 * is shared. What the CBOR makes exact: every redeemer and the script it runs, witnessed datums, outputs as encoded,
 * certificates in order, votes, and proposals (their governance action is encoded from the CBOR, not from JSON).
 * The spent inputs and reference inputs aren't in a transaction; they come resolved from the store.
 */

const hexCmp = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);
/** Ledger order of credentials: script hashes before key hashes, then by hash. */
const credKey = (c: Credential) => (c.script ? '0' : '1') + c.hash;

function stakeAddress(c: Credential, mainnet: boolean): string {
	const header = (c.script ? 0xf0 : 0xe0) | (mainnet ? 1 : 0);
	return bech32Encode(mainnet ? 'stake' : 'stake_test', new Uint8Array([header, ...c.hash.match(/../g)!.map((h) => parseInt(h, 16))]));
}

function addressCredential(address: string): Credential | null {
	const d = bech32Decode(address);
	return d ? paymentCredential(bytesToHex(d.bytes)) : null;
}

function outputUtxo(hash: string, o: TxOutput, index: number): StoreUtxo {
	return {
		tx_hash: hash,
		output_index: index,
		address: o.address,
		amount: [{ unit: 'lovelace', quantity: String(o.value.lovelace) }, ...o.value.assets.map((a) => ({ unit: a.policyId + a.name, quantity: String(a.quantity) }))],
		data_hash: o.datum.kind === 'hash' ? o.datum.hash : null,
		inline_datum: o.datum.kind === 'inline' ? o.datum.cbor : null,
		reference_script_hash: o.scriptRef?.hash ?? null
	};
}

// --- governance actions straight from their CBOR (cardano-ledger's ToPlutusData) ---

const I = (n: bigint | number): PlutusData => ({ int: String(n) });
const B = (h: string): PlutusData => ({ bytes: h });
const C = (constructor: number, fields: PlutusData[] = []): PlutusData => ({ constructor, fields });
const L = (list: PlutusData[]): PlutusData => ({ list });
const nothing = C(1);

function itemBytes(i: CborItem | undefined): string {
	if (i?.type !== 'bytes') throw new Error('expected bytes');
	return bytesToHex(i.value);
}
function itemInt(i: CborItem | undefined): bigint {
	if (i?.type === 'uint' || i?.type === 'nint') return i.value;
	throw new Error('expected an integer');
}

/** A protocol-parameter value as the ledger gives it to Plutus: rationals as [n, d], arrays as lists, maps as maps. */
function paramData(i: CborItem): PlutusData {
	const x = untag(i);
	if (!x) throw new Error('missing value');
	if (x.type === 'uint' || x.type === 'nint') return I(x.value);
	if (x.type === 'tag' && x.tag === 30) return paramData(x.item);
	if (x.type === 'array') return L(x.items.map(paramData));
	if (x.type === 'map') return { map: x.entries.map((e) => ({ k: paramData(e.k), v: paramData(e.v) })).sort((a, b) => Number(BigInt((a.k as { int: string }).int ?? 0) - BigInt((b.k as { int: string }).int ?? 0))) };
	if (x.type === 'bytes') return B(bytesToHex(x.value));
	throw new Error(`unexpected ${x.type} in a parameter update`);
}

function maybePrev(i: CborItem | undefined): PlutusData {
	if (!i || i.type !== 'array') return nothing;
	return C(0, [C(0, [B(itemBytes(i.items[0])), I(itemInt(i.items[1]))])]);
}
const maybeHash = (i: CborItem | undefined): PlutusData => (i?.type === 'bytes' ? C(0, [B(bytesToHex(i.value))]) : nothing);
function credentialData(i: CborItem): { key: string; data: PlutusData } {
	if (i.type !== 'array') throw new Error('expected a credential');
	const c = { script: itemInt(i.items[0]) === 1n, hash: itemBytes(i.items[1]) };
	return { key: credKey(c), data: C(c.script ? 1 : 0, [B(c.hash)]) };
}

function actionData(a: CborItem): PlutusData {
	if (a.type !== 'array') throw new Error('expected a governance action');
	const f = a.items;
	switch (Number(itemInt(f[0]))) {
		case 0: {
			const update = untag(f[2]);
			if (update?.type !== 'map') throw new Error('expected a parameter update');
			const entries = update.entries.map((e) => ({ k: Number(itemInt(e.k)), v: paramData(e.v) })).sort((x, y) => x.k - y.k);
			return C(0, [maybePrev(f[1]), { map: entries.map((e) => ({ k: I(e.k), v: e.v })) }, maybeHash(f[3])]);
		}
		case 1: {
			const v = f[2]?.type === 'array' ? f[2].items : [];
			return C(1, [maybePrev(f[1]), C(0, [I(itemInt(v[0])), I(itemInt(v[1]))])]);
		}
		case 2: {
			const w = untag(f[1]);
			if (w?.type !== 'map') throw new Error('expected withdrawals');
			const entries = w.entries.map((e) => {
				const acct = itemBytes(e.k);
				const c = { script: (parseInt(acct.slice(0, 2), 16) & 0x10) !== 0, hash: acct.slice(2) };
				return { key: credKey(c), k: C(c.script ? 1 : 0, [B(c.hash)]), v: I(itemInt(e.v)) };
			});
			return C(2, [{ map: entries.sort((x, y) => hexCmp(x.key, y.key)).map((e) => ({ k: e.k, v: e.v })) }, maybeHash(f[2])]);
		}
		case 3:
			return C(3, [maybePrev(f[1])]);
		case 4: {
			const removed = (untag(f[2])?.type === 'array' ? (untag(f[2]) as { items: CborItem[] }).items : []).map(credentialData).sort((x, y) => hexCmp(x.key, y.key));
			const added = (untag(f[3])?.type === 'map' ? (untag(f[3]) as { entries: { k: CborItem; v: CborItem }[] }).entries : [])
				.map((e) => ({ ...credentialData(e.k), epoch: itemInt(e.v) }))
				.sort((x, y) => hexCmp(x.key, y.key));
			const q = untag(f[4]);
			const [n, d] = q?.type === 'tag' && q.item.type === 'array' ? q.item.items : q?.type === 'array' ? q.items : [];
			return C(4, [maybePrev(f[1]), L(removed.map((r) => r.data)), { map: added.map((x) => ({ k: x.data, v: I(x.epoch) })) }, C(0, [I(itemInt(n)), I(itemInt(d))])]);
		}
		case 5: {
			const c = f[2]?.type === 'array' ? f[2].items : [];
			return C(5, [maybePrev(f[1]), C(0, [maybeHash(c[1])])]);
		}
		default:
			return C(6);
	}
}

/** A ProposalProcedure as Plutus data CBOR: deposit, return credential, governance action. */
export function procedureCbor(p: Proposal): string {
	const acct = p.rewardAccount;
	const ret = { script: (parseInt(acct.slice(0, 2), 16) & 0x10) !== 0, hash: acct.slice(2) };
	return encodePlutusData(C(0, [I(p.deposit), C(ret.script ? 1 : 0, [B(ret.hash)]), actionData(p.action)]));
}

const PROPOSAL_TYPES: Record<Proposal['kind'], string> = {
	parameterChange: 'PARAMETER_CHANGE_ACTION',
	hardFork: 'HARD_FORK_INITIATION_ACTION',
	treasuryWithdrawals: 'TREASURY_WITHDRAWALS_ACTION',
	noConfidence: 'NO_CONFIDENCE',
	updateCommittee: 'UPDATE_COMMITTEE',
	newConstitution: 'NEW_CONSTITUTION',
	info: 'INFO_ACTION'
};

const VOTER_TYPES = { committeeScript: 'CONSTITUTIONAL_COMMITTEE_HOT_SCRIPT_HASH', committeeKey: 'CONSTITUTIONAL_COMMITTEE_HOT_KEY_HASH', drepScript: 'DREP_SCRIPT_HASH', drepKey: 'DREP_KEY_HASH', pool: 'STAKING_POOL_KEY_HASH' } as const;
const VOTER_ORDER = ['committeeScript', 'committeeKey', 'drepScript', 'drepKey', 'pool'];

export interface CborReplayContext {
	/** The spent inputs and reference inputs, resolved from the store (address, value, datum, reference script). */
	resolve: (ref: { txHash: string; index: number }) => StoreUtxo | null;
	params: ReplayInput['params'];
	paramsEpoch?: number | null;
	slots: ReplayInput['slots'];
	mainnet: boolean;
}

/**
 * The ReplayInput for a decoded transaction. `missing` lists inputs the store couldn't resolve (their runs can't be
 * rebuilt). Scripts that only exist as reference scripts come without a body; load.ts fetches those by hash.
 */
export function replayInputFromCbor(tx: DecodedTx, ctx: CborReplayContext): { input: ReplayInput; missing: string[] } {
	const missing: string[] = [];
	const resolve = (ref: { txHash: string; index: number }) => {
		const u = ctx.resolve(ref);
		if (!u) missing.push(`${ref.txHash}#${ref.index}`);
		return u;
	};
	const inputs = tx.inputs.map(resolve).filter((u): u is StoreUtxo => !!u);
	const referenceInputs = tx.referenceInputs.map(resolve).filter((u): u is StoreUtxo => !!u);

	const witnessed = new Map(tx.witnesses.scripts.map((s) => [s.hash, s]));
	const datums: Record<string, string> = Object.fromEntries(tx.witnesses.datums.map((d) => [d.hash, d.cbor]));

	// Which script each redeemer runs, by the ledger's ordering of what the purpose points into.
	const sortedInputs = [...tx.inputs].sort((a, b) => hexCmp(a.txHash, b.txHash) || a.index - b.index);
	const policies = [...new Set(tx.mint.map((m) => m.policyId))].sort(hexCmp);
	const withdrawals = [...tx.withdrawals].sort((a, b) => hexCmp(credKey(a.credential), credKey(b.credential)));
	const voters = [...new Map(tx.votes.map((v) => [`${v.voter.kind}:${v.voter.hash}`, v.voter])).values()].sort((a, b) => VOTER_ORDER.indexOf(a.kind) - VOTER_ORDER.indexOf(b.kind) || hexCmp(a.hash, b.hash));
	const scriptOf = (tag: string, index: number): string | null => {
		switch (tag) {
			case 'spend': {
				const ref = sortedInputs[index];
				const u = ref ? ctx.resolve(ref) : null;
				const c = u ? addressCredential(u.address) : null;
				return c?.script ? c.hash : null;
			}
			case 'mint':
				return policies[index] ?? null;
			case 'reward':
				return withdrawals[index]?.credential.script ? withdrawals[index].credential.hash : null;
			case 'cert': {
				const c = tx.certificates[index]?.credential;
				return c?.script ? c.hash : null;
			}
			case 'vote':
				return voters[index] && /Script$/.test(voters[index].kind) ? voters[index].hash : null;
			case 'propose':
				return tx.proposals[index]?.policy ?? null;
		}
		return null;
	};

	const scripts: StoreScriptRun[] = tx.witnesses.redeemers.map((r) => {
		const hash = scriptOf(r.tag, r.index) ?? '';
		const s = witnessed.get(hash);
		const spent = r.tag === 'spend' ? sortedInputs[r.index] && ctx.resolve(sortedInputs[r.index]) : null;
		return {
			script_hash: hash,
			type: s && s.language !== 'native' ? `plutus${s.language}` : null,
			script_content: s ? JSON.stringify({ content: s.cbor }) : null,
			redeemer: { tag: r.tag, index: r.index, data: r.data, ex_units: { mem: Number(r.exUnits.mem), steps: Number(r.exUnits.steps) } },
			datum: spent ? (spent.inline_datum ?? (spent.data_hash ? (datums[spent.data_hash] ?? null) : null)) : null,
			datum_hash: spent?.data_hash ?? null
		};
	});

	const certificates: StoreCert[] = tx.certificates.map((c) => {
		const address = c.credential ? stakeAddress(c.credential, ctx.mainnet) : '';
		if (c.kind === 'stakeRegistration' || c.kind === 'registration') return { cert_index: c.index, type: 'registration', address };
		if (c.kind === 'stakeDeregistration' || c.kind === 'unregistration') return { cert_index: c.index, type: 'deregistration', address };
		if (c.kind === 'stakeDelegation') return { cert_index: c.index, type: 'delegation', address, pool_id: c.poolId ? bech32Encode('pool', new Uint8Array(c.poolId.match(/../g)!.map((h) => parseInt(h, 16)))) : null };
		return { cert_index: c.index, type: 'other', address };
	});

	const votes: StoreVote[] = tx.votes.map((v) => ({ voter_type: VOTER_TYPES[v.voter.kind], voter_hash: v.voter.hash, gov_action_tx_hash: v.action.txHash, gov_action_index: v.action.index, vote: v.vote.toUpperCase() }));

	const proposals: StoreProposal[] = tx.proposals.map((p, index) => ({
		index,
		type: PROPOSAL_TYPES[p.kind],
		deposit: String(p.deposit),
		return_address: p.returnAddress,
		details: {
			policyHash: p.policy,
			...(p.kind === 'treasuryWithdrawals' && p.action.type === 'array' && untag(p.action.items[1])?.type === 'map'
				? { withdrawals: Object.fromEntries((untag(p.action.items[1]) as { entries: { k: CborItem; v: CborItem }[] }).entries.map((e) => [itemBytes(e.k), String(itemInt(e.v))])) }
				: {})
		}
	}));

	return {
		missing,
		input: {
			hash: tx.hash,
			tx: {
				fees: String(tx.fee),
				ttl: tx.ttl == null ? null : Number(tx.ttl),
				validity_interval_start: tx.validityStart == null ? null : Number(tx.validityStart),
				required_signers: tx.requiredSigners,
				inputs,
				outputs: tx.outputs.map((o, i) => outputUtxo(tx.hash, o, i)),
				reference_inputs: referenceInputs
			},
			scripts,
			withdrawals: tx.withdrawals.map((w) => ({ address: w.address, amount: String(w.amount) })),
			mints: tx.mint.map((m) => ({ unit: m.policyId + m.name, quantity: String(m.quantity) })),
			votes,
			certificates,
			proposals,
			datums,
			paramsEpoch: ctx.paramsEpoch,
			params: ctx.params,
			slots: ctx.slots,
			source: 'cbor',
			witnessDatums: tx.witnesses.datums.map((d) => ({ hash: d.hash, datum: d.cbor })),
			procedures: tx.proposals.map(procedureCbor)
		}
	};
}
