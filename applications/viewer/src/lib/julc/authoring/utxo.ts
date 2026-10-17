import { tryApi } from '$lib/api/client';
import type { StoreUtxo } from '../replay/build';

/* Finding chain state for the validator lab: one UTxO by reference, or the unspent outputs at an address. */

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/** A /utxos/{tx}/{index} or /addresses/{a}/utxos row as the replay builder's UTxO. */
export function utxoOf(u: Any): StoreUtxo | null {
	if (!u?.tx_hash) return null;
	return {
		tx_hash: u.tx_hash,
		output_index: Number(u.output_index),
		address: u.owner_addr ?? u.address ?? '',
		amount: (u.amounts ?? u.amount ?? []).map((a: Any) => ({ unit: a.unit, quantity: String(a.quantity) })),
		data_hash: u.data_hash ?? null,
		inline_datum: u.inline_datum ?? null,
		reference_script_hash: u.reference_script_hash ?? null
	};
}

export const parseRef = (text: string) => text.trim().match(/^([0-9a-f]{64})[#_:](\d+)$/i);

/** Look up "<tx>#<index>" (spent or not) or an address's unspent outputs (newest first, up to 20). */
export async function findUtxos(query: string): Promise<{ utxos: StoreUtxo[]; error?: string }> {
	const q = query.trim();
	const ref = parseRef(q);
	if (ref) {
		const u = utxoOf(await tryApi<Any>(`/utxos/${ref[1].toLowerCase()}/${ref[2]}`));
		return u ? { utxos: [u] } : { utxos: [], error: 'The store has no such output.' };
	}
	if (/^addr(_test)?1[0-9a-z]+$/.test(q)) {
		const rows = await tryApi<Any[]>(`/addresses/${q}/utxos`, { params: { count: 20, order: 'desc' } });
		const utxos = (rows ?? []).map(utxoOf).filter((u): u is StoreUtxo => !!u);
		return utxos.length ? { utxos } : { utxos: [], error: 'No unspent outputs at this address.' };
	}
	return { utxos: [], error: 'Enter <tx hash>#<index> or an address (addr…).' };
}

/** The CBOR of a datum given by hash, if the store has it. */
export async function datumByHash(hash: string): Promise<string | null> {
	return (await tryApi<{ cbor?: string }>(`/scripts/datum/${hash}/cbor`))?.cbor ?? null;
}
