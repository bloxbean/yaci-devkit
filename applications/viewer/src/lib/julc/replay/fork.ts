import type { ReplayInput, StoreScriptRun, StoreUtxo } from './build';
import { purposeOf } from './build';

/*
 * A fork of a transaction: edits applied to the store-shaped transaction before it is rebuilt, so everything
 * downstream (ledger order, purposes, the context fixes, the redeemer map) stays consistent for every script.
 * Absent fields mean "as on chain".
 */

export interface OutputEdit {
	removed?: boolean;
	address?: string;
	lovelace?: string;
	/** unit → quantity; "0" removes the asset. */
	assets?: Record<string, string>;
}

export interface ForkEdits {
	/** Run key (spend#0, …) → redeemer CBOR. */
	redeemers?: Record<string, string>;
	/** Spent input ("txHash#index") → datum CBOR. A datum given by hash keeps its hash. */
	datums?: Record<string, string>;
	/** Replaces the required signers (key hashes). */
	signers?: string[];
	/** Slots; null removes the bound. */
	validFrom?: number | null;
	validTo?: number | null;
	fee?: string;
	/** By output index. */
	outputs?: Record<number, OutputEdit>;
	/** unit → quantity minted (negative burns); "0" removes. */
	mints?: Record<string, string>;
	/** Stake address → amount. */
	withdrawals?: Record<string, string>;
	/** Run with this protocol version's rules (julc supports 10 and 11). */
	protocol?: 10 | 11;
}

export const refOf = (u: { tx_hash: string; output_index: number }) => `${u.tx_hash}#${u.output_index}`;

function redeemerOf(s: StoreScriptRun) {
	if (!s.redeemer) return null;
	if (typeof s.redeemer !== 'string') return s.redeemer;
	try {
		return JSON.parse(s.redeemer);
	} catch {
		return null;
	}
}

export const runKeyOf = (s: StoreScriptRun): string | null => {
	const r = redeemerOf(s);
	return r?.tag ? `${purposeOf(r.tag) ?? String(r.tag).toLowerCase()}#${Number(r.index ?? 0)}` : null;
};

function editAmounts(amount: StoreUtxo['amount'], lovelace: string | undefined, assets: Record<string, string> | undefined): StoreUtxo['amount'] {
	let out = amount.map((a) => (a.unit === 'lovelace' && lovelace != null ? { ...a, quantity: lovelace } : a));
	for (const [unit, quantity] of Object.entries(assets ?? {})) {
		out = out.filter((a) => a.unit !== unit);
		if (quantity !== '0') out.push({ unit, quantity });
	}
	return out;
}

/** The transaction with the edits applied. `params` (another epoch's) replaces the transaction's own. */
export function applyEdits(base: ReplayInput, e: ForkEdits, params?: ReplayInput['params']): ReplayInput {
	const datums = { ...base.datums };
	const inputs = base.tx.inputs.map((u) => {
		const datum = e.datums?.[refOf(u)];
		if (datum == null) return u;
		if (u.data_hash) {
			datums[u.data_hash] = datum;
			return u;
		}
		return { ...u, inline_datum: datum };
	});
	// The scripts endpoint also carries the spent datum; keep it in step with the edit.
	const scripts = base.scripts.map((s) => {
		const key = runKeyOf(s);
		const r = redeemerOf(s);
		let next = s;
		if (key && r && e.redeemers?.[key] != null) next = { ...next, redeemer: { ...r, data: e.redeemers[key] } };
		if (s.datum_hash && datums[s.datum_hash] !== base.datums[s.datum_hash]) next = { ...next, datum: datums[s.datum_hash] };
		return next;
	});
	const outputs = base.tx.outputs
		.filter((u) => !e.outputs?.[u.output_index]?.removed)
		.map((u) => {
			const o = e.outputs?.[u.output_index];
			return o ? { ...u, address: o.address ?? u.address, amount: editAmounts(u.amount, o.lovelace, o.assets) } : u;
		});
	let mints = base.mints.map((m) => (e.mints?.[m.unit] != null ? { ...m, quantity: e.mints[m.unit] } : m));
	for (const [unit, quantity] of Object.entries(e.mints ?? {})) if (!mints.some((m) => m.unit === unit)) mints.push({ unit, quantity });
	mints = mints.filter((m) => String(m.quantity) !== '0');
	const withdrawals = base.withdrawals.map((w) => (e.withdrawals?.[w.address] != null ? { ...w, amount: e.withdrawals[w.address] } : w));
	const p = params ?? base.params;

	return {
		...base,
		tx: {
			...base.tx,
			inputs,
			outputs,
			fees: e.fee ?? base.tx.fees,
			required_signers: e.signers ?? base.tx.required_signers,
			validity_interval_start: e.validFrom !== undefined ? e.validFrom : base.tx.validity_interval_start,
			ttl: e.validTo !== undefined ? e.validTo : base.tx.ttl
		},
		scripts,
		mints,
		withdrawals,
		datums,
		params: p && e.protocol ? { ...p, protocol_major_ver: e.protocol } : p,
		paramsEpoch: params ? undefined : base.paramsEpoch
	};
}

/** How many things a fork changes, for the "N edits" badge. */
export function countEdits(e: ForkEdits): number {
	return (
		Object.keys(e.redeemers ?? {}).length +
		Object.keys(e.datums ?? {}).length +
		(e.signers ? 1 : 0) +
		(e.validFrom !== undefined ? 1 : 0) +
		(e.validTo !== undefined ? 1 : 0) +
		(e.fee != null ? 1 : 0) +
		Object.keys(e.outputs ?? {}).length +
		Object.keys(e.mints ?? {}).length +
		Object.keys(e.withdrawals ?? {}).length +
		(e.protocol ? 1 : 0)
	);
}
