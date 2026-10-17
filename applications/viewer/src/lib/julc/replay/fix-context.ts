import { decodePlutusData, encodePlutusData, type PlutusData } from '$lib/cbor';
import type { PlutusVersion } from '../types';
import type { Purpose } from './build';

/*
 * Corrections to the ScriptContext julc builds from a MockTransaction, until julc does them itself (julc#244):
 *
 * - Redeemers: julc puts only the evaluated script's own redeemer into txInfoRedeemers, but scripts such as
 *   withdraw-zero validators look up the others. Every run's context does contain that run's own entry
 *   (purpose → redeemer), so the viewer collects them all, orders them as the ledger does and puts the complete
 *   map into each context.
 * - Values: julc doesn't keep multi-asset values in ledger order (policy, then token name, ada first), so a script
 *   comparing an output with the value it expects can fail. Values are re-sorted where the ledger puts them: the
 *   inputs', reference inputs' and outputs' values, the mint, and the fee in V1/V2. User data (datums) is untouched.
 * - Datums by hash: julc drops the datum of an input that also has a hash, so a spending script's datum would be
 *   missing. build.ts hands such datums to julc inline (V2/V3) or without their hash (V1; julc then hashes them
 *   itself), and here the inputs get their on-chain hash back. Datums of inputs and outputs known by hash go into
 *   txInfoData under their on-chain hashes, as the ledger has every datum the transaction witnesses.
 * - V1 shape: julc 0.1.0-pre18 encodes V1's withdrawals and datums as maps, where Plutus V1 has lists of pairs
 *   (fixed on julc main, #233). They are converted, so V1 scripts can read them.
 *
 * TxInfo field positions:  V1 [inputs, outputs, fee, mint, …]
 *                          V2 [inputs, refInputs, outputs, fee, mint, certs, wdrl, range, signers, redeemers, …]
 *                          V3 same as V2, but the fee is an integer.
 */

type Constr = { constructor: number; fields: PlutusData[] };
type Entry = { k: PlutusData; v: PlutusData };

const REDEEMERS = 9;
const DATA: Record<PlutusVersion, number> = { V1: 8, V2: 10, V3: 10 };
const LAYOUT: Record<PlutusVersion, { lists: number[]; values: number[]; redeemers: number | null }> = {
	V1: { lists: [0, 1], values: [2, 3], redeemers: null },
	V2: { lists: [0, 1, 2], values: [3, 4], redeemers: REDEEMERS },
	V3: { lists: [0, 1, 2], values: [4], redeemers: REDEEMERS }
};

/** Ledger order of redeemers: by tag (spend, mint, cert, reward, vote, propose), then index. */
export const TAG_ORDER: Record<Purpose, number> = { spend: 0, mint: 1, cert: 2, reward: 3, vote: 4, propose: 5 };

const isConstr = (d: PlutusData | undefined): d is Constr => !!d && typeof (d as Constr).constructor === 'number' && Array.isArray((d as Constr).fields);
const isMap = (d: PlutusData | undefined): d is { map: Entry[] } => !!d && Array.isArray((d as { map?: unknown }).map);
const bytesOf = (d: PlutusData) => ('bytes' in d ? d.bytes : '');
const byKey = (a: Entry, b: Entry) => (bytesOf(a.k) < bytesOf(b.k) ? -1 : bytesOf(a.k) > bytesOf(b.k) ? 1 : 0);

function txInfo(context: PlutusData): Constr | null {
	return isConstr(context) && isConstr(context.fields[0]) ? context.fields[0] : null;
}

/** Sort a Value (Map policy (Map name amount)) in place; returns whether anything moved. */
function sortValue(value: PlutusData | undefined): boolean {
	if (!isMap(value)) return false;
	let moved = false;
	const sortMap = (m: { map: Entry[] }) => {
		const before = m.map.map((e) => bytesOf(e.k)).join();
		m.map.sort(byKey);
		if (m.map.map((e) => bytesOf(e.k)).join() !== before) moved = true;
	};
	sortMap(value);
	for (const e of value.map) if (isMap(e.v)) sortMap(e.v);
	return moved;
}

/** The TxOut inside a list element: TxInInfo [outRef, TxOut] or a bare TxOut [address, value, …]. */
function txOutOf(item: PlutusData): Constr | null {
	if (!isConstr(item)) return null;
	if (item.fields.length === 2 && isConstr(item.fields[1])) return item.fields[1];
	return item;
}

/** The single redeemer entry julc put into a context, or null if it isn't there. */
export function ownRedeemer(contextCbor: string): Entry | null {
	try {
		const map = txInfo(decodePlutusData(contextCbor))?.fields[REDEEMERS];
		return isMap(map) && map.map.length === 1 ? map.map[0] : null;
	} catch {
		return null;
	}
}

export interface FixedContext {
	cbor: string;
	sortedValues: boolean;
	completedRedeemers: boolean;
	restoredDatums: boolean;
	v1Shape?: boolean;
}

/** Apply the corrections; null when the context isn't in the expected shape or nothing needed changing. */
export function fixContext(
	contextCbor: string,
	language: PlutusVersion,
	redeemers: Entry[] | null,
	hashedDatums: { input: number; hash: string; datum: string }[] = [],
	witnessDatums: { hash: string; datum: string }[] = []
): FixedContext | null {
	try {
		const context = decodePlutusData(contextCbor);
		const info = txInfo(context);
		if (!info) return null;
		const layout = LAYOUT[language];
		let sortedValues = false;
		for (const i of layout.lists) {
			const list = info.fields[i];
			if (!list || !('list' in list)) continue;
			for (const item of list.list) if (sortValue(txOutOf(item)?.fields[1])) sortedValues = true;
		}
		for (const i of layout.values) if (sortValue(info.fields[i])) sortedValues = true;
		let completedRedeemers = false;
		if (redeemers && layout.redeemers != null && isMap(info.fields[layout.redeemers]) && redeemers.length > (info.fields[layout.redeemers] as { map: Entry[] }).map.length) {
			info.fields[layout.redeemers] = { map: redeemers };
			completedRedeemers = true;
		}
		let v1Shape = false;
		if (language === 'V1') {
			for (const i of [5, 8]) {
				const f = info.fields[i];
				if (isMap(f)) {
					info.fields[i] = { list: f.map.map((e) => ({ constructor: 0, fields: [e.k, e.v] })) };
					v1Shape = true;
				}
			}
		}
		let restoredDatums = false;
		const inputs = info.fields[0];
		const data = info.fields[DATA[language]];
		if (language === 'V1' && data && 'list' in data) {
			// V1: TxOut datum is Maybe DatumHash, txInfoData a list of (hash, datum) pairs.
			if (inputs && 'list' in inputs) {
				for (const h of hashedDatums) {
					const out = txOutOf(inputs.list[h.input]);
					const just = out?.fields[2];
					if (!out || !isConstr(just) || just.constructor !== 0) continue;
					const julcHash = bytesOf(just.fields[0]); // the hash julc computed
					out.fields[2] = { constructor: 0, fields: [{ bytes: h.hash }] }; // Just (on-chain hash)
					for (const pair of data.list) if (isConstr(pair) && bytesOf(pair.fields[0]) === julcHash) pair.fields[0] = { bytes: h.hash };
					restoredDatums = true;
				}
			}
			for (const w of witnessDatums) {
				if (data.list.some((pair) => isConstr(pair) && bytesOf(pair.fields[0]) === w.hash)) continue;
				data.list.push({ constructor: 0, fields: [{ bytes: w.hash }, decodePlutusData(w.datum)] });
				restoredDatums = true;
			}
			if (restoredDatums) data.list.sort((a, b) => (isConstr(a) && isConstr(b) ? byKey({ k: a.fields[0], v: a }, { k: b.fields[0], v: b }) : 0));
		}
		if (language !== 'V1' && isMap(data)) {
			if (inputs && 'list' in inputs) {
				for (const h of hashedDatums) {
					const out = txOutOf(inputs.list[h.input]);
					if (!out || out.fields.length < 3) continue;
					out.fields[2] = { constructor: 1, fields: [{ bytes: h.hash }] }; // OutputDatumHash
					restoredDatums = true;
				}
			}
			for (const w of witnessDatums) {
				if (data.map.some((e) => bytesOf(e.k) === w.hash)) continue;
				data.map.push({ k: { bytes: w.hash }, v: decodePlutusData(w.datum) });
				restoredDatums = true;
			}
			if (restoredDatums) data.map.sort(byKey);
		}
		if (!sortedValues && !completedRedeemers && !restoredDatums && !v1Shape) return null;
		return { cbor: encodePlutusData(context), sortedValues, completedRedeemers, restoredDatums, v1Shape };
	} catch {
		return null;
	}
}
