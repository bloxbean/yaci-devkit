import { tryApi } from '$lib/api/client';
import { must } from '$lib/api/load';
import type { Utxo } from '$lib/tx/types';
import type { PageServerLoad } from './$types';

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

export const load: PageServerLoad = async ({ params, fetch }) => {
	const hash = params.id.trim().toLowerCase();
	const tx = await must<TxDetail>(`/txs/${hash}`, { fetch }, 'No transaction with that hash on this chain.');
	const [scripts, metadata, withdrawals, mints, block, protocol] = await Promise.all([
		tryApi<Any[]>(`/txs/${hash}/scripts`, { fetch }),
		tryApi<Any[]>(`/txs/${hash}/metadata`, { fetch }),
		tryApi<Any[]>(`/txs/${hash}/withdrawals`, { fetch }),
		tryApi<Any[]>(`/assets/txs/${hash}`, { fetch }),
		tx.block_height >= 0 ? tryApi<Any>(`/blocks/${tx.block_height}`, { fetch }) : null,
		tryApi<Any>('/epochs/latest/parameters', { fetch })
	]);
	return {
		tx,
		scripts: scripts ?? [],
		metadata: metadata ?? [],
		withdrawals: withdrawals ?? [],
		mints: mints ?? [],
		block: block ? { time: block.time as number, epoch: block.epoch as number, hash: block.hash as string } : null,
		limits: { mem: Number(protocol?.max_tx_ex_mem) || 14_000_000, steps: Number(protocol?.max_tx_ex_steps) || 10_000_000_000 }
	};
};
