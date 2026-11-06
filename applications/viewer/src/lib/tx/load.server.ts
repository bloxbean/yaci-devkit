import { error } from '@sveltejs/kit';
import { tryApi } from '$lib/api/client';
import { must } from '$lib/api/load';
import { wholeTxHex } from '$lib/tx/decode';
import type { Utxo } from '$lib/tx/types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

export interface TxDetail {
	hash: string;
	block_height: number;
	slot: number;
	inputs: Utxo[];
	outputs: Utxo[];
	fees: number | string;
	total_output: number | string;
	ttl?: number | null;
	validity_interval_start?: number | null;
	auxiliary_data_hash?: string | null;
	script_data_hash?: string | null;
	collateral_inputs?: Utxo[];
	collateral_return?: Utxo | null;
	total_collateral?: number | string | null;
	reference_inputs?: Utxo[];
	required_signers?: string[] | null;
	invalid?: boolean;
}

/** Everything the transaction pages show: the transaction, its script runs, metadata, withdrawals, mints and block. */
export async function loadTransaction(id: string, fetch: typeof globalThis.fetch) {
	const hash = id.trim().toLowerCase();
	if (!/^[0-9a-f]{64}$/.test(hash)) error(404, 'No transaction with that hash on this chain.');
	const tx = await must<TxDetail>(`/txs/${hash}`, { fetch }, 'No transaction with that hash on this chain.');
	const [scripts, metadata, withdrawals, mints, block, protocol, collateralUtxo] = await Promise.all([
		tryApi<Any[]>(`/txs/${hash}/scripts`, { fetch }),
		tryApi<Any[]>(`/txs/${hash}/metadata`, { fetch }),
		tryApi<Any[]>(`/txs/${hash}/withdrawals`, { fetch }),
		tryApi<Any[]>(`/assets/txs/${hash}`, { fetch }),
		tx.block_height >= 0 ? tryApi<Any>(`/blocks/${tx.block_height}`, { fetch }) : null,
		tryApi<Any>('/epochs/latest/parameters', { fetch }),
		// Yaci Store's /txs/{hash} always says `invalid: false`. A failed transaction's only output on chain is its
		// collateral return, which the store keeps at index outputs.length marked is_collateral_return: proof it failed.
		tx.collateral_return && !tx.invalid ? tryApi<Any>(`/utxos/${hash}/${tx.outputs?.length ?? 0}`, { fetch }) : null
	]);
	if (collateralUtxo?.is_collateral_return === true) tx.invalid = true;
	if (tx.collateral_return) tx.collateral_return = collateralReturn(tx.collateral_return, hash, tx.outputs?.length ?? 0);
	return {
		tx,
		// The transaction's original CBOR, when the store saves it (store.transaction.save-cbor). Streamed: the page
		// doesn't wait for it.
		cbor: tryApi<{ cbor_hex?: string; cborHex?: string }>(`/txs/${hash}/cbor/hex`, { fetch }).then((r) => wholeTxHex(r?.cbor_hex ?? r?.cborHex)),
		scripts: scripts ?? [],
		metadata: metadata ?? [],
		withdrawals: withdrawals ?? [],
		mints: mints ?? [],
		block: block ? { time: block.time as number, epoch: block.epoch as number, hash: block.hash as string } : null,
		limits: { mem: Number(protocol?.max_tx_ex_mem) || 14_000_000, steps: Number(protocol?.max_tx_ex_steps) || 10_000_000_000 }
	};
}

/**
 * Yaci Store sends the collateral return in its own shape (`amounts`, camelCase, no tx_hash or index), not as a UTxO
 * like the inputs and outputs. It is the output after the regular ones.
 */
export function collateralReturn(r: Any, txHash: string, index: number): Utxo {
	return {
		...r,
		tx_hash: r.tx_hash ?? txHash,
		output_index: r.output_index ?? index,
		amount: r.amount ?? r.amounts ?? [],
		data_hash: r.data_hash ?? r.dataHash ?? null,
		inline_datum: r.inline_datum ?? r.inlineDatum ?? null,
		reference_script_hash: r.reference_script_hash ?? r.referenceScriptHash ?? null
	} as Utxo;
}
