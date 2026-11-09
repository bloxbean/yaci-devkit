/* Protocol parameters grouped for reading, with friendly names and units. */

export type Unit = 'lovelace' | 'percent' | 'ratio' | 'bytes' | 'number' | 'epochs' | 'text';

interface Def {
	key: string;
	label: string;
	unit: Unit;
	hint?: string;
}

const g = (label: string, defs: Def[]) => ({ label, defs });

export const PARAM_GROUPS = [
	g('Fees', [
		{ key: 'min_fee_a', label: 'Fee per byte', unit: 'number', hint: 'lovelace per tx byte' },
		{ key: 'min_fee_b', label: 'Base fee', unit: 'lovelace' },
		{ key: 'min_fee_ref_script_cost_per_byte', label: 'Reference script fee', unit: 'number', hint: 'lovelace per byte' },
		{ key: 'price_mem', label: 'Price per memory unit', unit: 'number', hint: 'lovelace' },
		{ key: 'price_step', label: 'Price per CPU step', unit: 'number', hint: 'lovelace' }
	]),
	g('Sizes and limits', [
		{ key: 'max_block_size', label: 'Max block body size', unit: 'bytes' },
		{ key: 'max_tx_size', label: 'Max transaction size', unit: 'bytes' },
		{ key: 'max_block_header_size', label: 'Max block header size', unit: 'bytes' },
		{ key: 'max_val_size', label: 'Max value size', unit: 'bytes' },
		{ key: 'max_collateral_inputs', label: 'Max collateral inputs', unit: 'number' },
		{ key: 'collateral_percent', label: 'Collateral', unit: 'number', hint: '% of the fee' }
	]),
	g('Plutus execution', [
		{ key: 'max_tx_ex_mem', label: 'Max memory per transaction', unit: 'number' },
		{ key: 'max_tx_ex_steps', label: 'Max CPU steps per transaction', unit: 'number' },
		{ key: 'max_block_ex_mem', label: 'Max memory per block', unit: 'number' },
		{ key: 'max_block_ex_steps', label: 'Max CPU steps per block', unit: 'number' }
	]),
	g('Deposits and minimums', [
		{ key: 'key_deposit', label: 'Stake key deposit', unit: 'lovelace' },
		{ key: 'pool_deposit', label: 'Pool deposit', unit: 'lovelace' },
		{ key: 'drep_deposit', label: 'DRep deposit', unit: 'lovelace' },
		{ key: 'gov_action_deposit', label: 'Governance action deposit', unit: 'lovelace' },
		{ key: 'min_pool_cost', label: 'Min pool cost', unit: 'lovelace' },
		{ key: 'coins_per_utxo_size', label: 'Lovelace per UTxO byte', unit: 'number' },
		{ key: 'min_utxo', label: 'Min UTxO', unit: 'lovelace' }
	]),
	g('Staking and rewards', [
		{ key: 'n_opt', label: 'Target number of pools (k)', unit: 'number' },
		{ key: 'a0', label: 'Pledge influence (a0)', unit: 'ratio' },
		{ key: 'rho', label: 'Monetary expansion (ρ)', unit: 'ratio' },
		{ key: 'tau', label: 'Treasury cut (τ)', unit: 'ratio' },
		{ key: 'e_max', label: 'Max pool retirement', unit: 'epochs' },
		{ key: 'decentralisation_param', label: 'Decentralisation (d)', unit: 'ratio' }
	]),
	g('Governance', [
		{ key: 'gov_action_lifetime', label: 'Governance action lifetime', unit: 'epochs' },
		{ key: 'drep_activity', label: 'DRep activity period', unit: 'epochs' },
		{ key: 'committee_min_size', label: 'Committee min size', unit: 'number' },
		{ key: 'committee_max_term_length', label: 'Committee max term', unit: 'epochs' },
		{ key: 'pvt_motion_no_confidence', label: 'SPO: no confidence', unit: 'percent' },
		{ key: 'pvt_committee_normal', label: 'SPO: update committee', unit: 'percent' },
		{ key: 'pvt_committee_no_confidence', label: 'SPO: committee in no confidence', unit: 'percent' },
		{ key: 'pvt_hard_fork_initiation', label: 'SPO: hard fork', unit: 'percent' },
		{ key: 'pvt_p_p_security_group', label: 'SPO: security parameters', unit: 'percent' },
		{ key: 'dvt_motion_no_confidence', label: 'DRep: no confidence', unit: 'percent' },
		{ key: 'dvt_committee_normal', label: 'DRep: update committee', unit: 'percent' },
		{ key: 'dvt_committee_no_confidence', label: 'DRep: committee in no confidence', unit: 'percent' },
		{ key: 'dvt_update_to_constitution', label: 'DRep: new constitution', unit: 'percent' },
		{ key: 'dvt_hard_fork_initiation', label: 'DRep: hard fork', unit: 'percent' },
		{ key: 'dvt_p_p_network_group', label: 'DRep: network parameters', unit: 'percent' },
		{ key: 'dvt_p_p_economic_group', label: 'DRep: economic parameters', unit: 'percent' },
		{ key: 'dvt_p_p_technical_group', label: 'DRep: technical parameters', unit: 'percent' },
		{ key: 'dvt_p_p_gov_group', label: 'DRep: governance parameters', unit: 'percent' },
		{ key: 'dvt_treasury_withdrawal', label: 'DRep: treasury withdrawal', unit: 'percent' }
	]),
	g('Protocol', [
		{ key: 'protocol_major_ver', label: 'Protocol major version', unit: 'number' },
		{ key: 'protocol_minor_ver', label: 'Protocol minor version', unit: 'number' },
		{ key: 'extra_entropy', label: 'Extra entropy', unit: 'text' },
		{ key: 'nonce', label: 'Epoch nonce', unit: 'text' }
	])
];

/** Keys shown elsewhere or not worth a row. */
export const HIDDEN = new Set(['epoch', 'cost_models', 'cost_models_raw', 'cost_models_hash', 'pvtpp_security_group']);

export interface ParamRow {
	key: string;
	label: string;
	unit: Unit;
	hint?: string;
	value: unknown;
}

/** Groups with only the keys this store returned, plus an "Other" group for anything unknown. */
export function groupParams(params: Record<string, unknown>): { label: string; rows: ParamRow[] }[] {
	const known = new Set<string>();
	const groups = PARAM_GROUPS.map((grp) => ({
		label: grp.label,
		rows: grp.defs
			.filter((d) => {
				known.add(d.key);
				return params[d.key] !== undefined && params[d.key] !== null;
			})
			.map((d) => ({ ...d, value: params[d.key] }))
	}));
	const other = Object.keys(params)
		.filter((k) => !known.has(k) && !HIDDEN.has(k) && params[k] !== null && typeof params[k] !== 'object')
		.sort()
		.map((k) => ({ key: k, label: k.replace(/_/g, ' '), unit: 'text' as Unit, value: params[k] }));
	if (other.length) groups.push({ label: 'Other', rows: other });
	return groups.filter((x) => x.rows.length);
}
