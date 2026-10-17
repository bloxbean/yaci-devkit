export interface Amount {
	unit: string;
	policy_id?: string | null;
	asset_name?: string | null;
	quantity: string | number;
}

export interface Utxo {
	tx_hash: string;
	output_index: number;
	address: string;
	stake_address?: string | null;
	amount: Amount[];
	data_hash?: string | null;
	inline_datum?: string | null;
	inline_datum_json?: unknown;
	script_ref?: string | null;
	reference_script_hash?: string | null;
}

export const lovelaceOf = (amounts: Amount[] | undefined) =>
	(amounts ?? []).filter((a) => a.unit === 'lovelace').reduce((s, a) => s + BigInt(a.quantity), 0n);

export const assetsOf = (amounts: Amount[] | undefined) => (amounts ?? []).filter((a) => a.unit !== 'lovelace');

export const sumLovelace = (utxos: Utxo[] | undefined) => (utxos ?? []).reduce((s, u) => s + lovelaceOf(u.amount), 0n);
