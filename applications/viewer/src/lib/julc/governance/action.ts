import { bech32Decode } from '$lib/bech32';
import { decodePlutusData, type PlutusData } from '$lib/cbor';

/*
 * Governance actions as Plutus V3 sees them: the ProposalProcedure in a proposing script's ScriptInfo and in
 * txInfoProposalProcedures. julc 0.1.0-pre18 builds parameter changes with an empty change set (and other actions
 * only roughly), so the viewer encodes the real action from Yaci Store data and puts it into the context
 * (fix-context.ts).
 *
 * Encodings follow cardano-ledger's ToPlutusData:
 *   ProposalProcedure   Constr 0 [deposit, return credential, action]
 *   ParameterChange     Constr 0 [Maybe prevId, Map key value, Maybe policy]
 *   HardForkInitiation  Constr 1 [Maybe prevId, Constr 0 [major, minor]]
 *   TreasuryWithdrawals Constr 2 [Map credential amount, Maybe policy]
 *   NoConfidence 3, UpdateCommittee 4, NewConstitution 5, InfoAction 6
 * In a parameter change, rationals are [numerator, denominator] lists, ex-units [mem, steps], prices
 * [mem, steps] of rationals, cost models a map from language (0, 1, 2) to the list of costs, and voting
 * thresholds lists of rationals in ledger order.
 */

export type Rational = { n: bigint; d: bigint };

export type ParamKind = 'int' | 'rational';

export interface ParamDef {
	/** Field name in Yaci Store's protocolParamUpdate (yaci core). Thresholds use "group.field". */
	name: string;
	/** Key in the ledger's PParamsUpdate. */
	key: number;
	/** Position within a composite value (prices, ex-units, thresholds). */
	slot?: number;
	kind: ParamKind;
	label: string;
	/** Field of /epochs/{n}/parameters with the current value. */
	current: string;
	group: 'Network' | 'Economic' | 'Technical' | 'Governance';
	unit?: 'lovelace' | 'bytes' | 'epochs';
}

const P = (name: string, key: number, kind: ParamKind, label: string, current: string, group: ParamDef['group'], extra: Partial<ParamDef> = {}): ParamDef => ({ name, key, kind, label, current, group, ...extra });

const POOL_THRESHOLDS = ['pvtMotionNoConfidence', 'pvtCommitteeNormal', 'pvtCommitteeNoConfidence', 'pvtHardForkInitiation', 'pvtPPSecurityGroup'];
const DREP_THRESHOLDS = ['dvtMotionNoConfidence', 'dvtCommitteeNormal', 'dvtCommitteeNoConfidence', 'dvtUpdateToConstitution', 'dvtHardForkInitiation', 'dvtPPNetworkGroup', 'dvtPPEconomicGroup', 'dvtPPTechnicalGroup', 'dvtPPGovGroup', 'dvtTreasuryWithdrawal'];
const POOL_CURRENT = ['pvt_motion_no_confidence', 'pvt_committee_normal', 'pvt_committee_no_confidence', 'pvt_hard_fork_initiation', 'pvt_p_p_security_group'];
const DREP_CURRENT = ['dvt_motion_no_confidence', 'dvt_committee_normal', 'dvt_committee_no_confidence', 'dvt_update_to_constitution', 'dvt_hard_fork_initiation', 'dvt_p_p_network_group', 'dvt_p_p_economic_group', 'dvt_p_p_technical_group', 'dvt_p_p_gov_group', 'dvt_treasury_withdrawal'];
const POOL_LABELS = ['no confidence', 'update committee', 'committee in no confidence', 'hard fork', 'security parameters'];
const DREP_LABELS = ['no confidence', 'update committee', 'committee in no confidence', 'new constitution', 'hard fork', 'network parameters', 'economic parameters', 'technical parameters', 'governance parameters', 'treasury withdrawal'];

/** The parameters a parameter-change proposal can set (cost models aside), in ledger key order. */
export const PARAMS: ParamDef[] = [
	P('minFeeA', 0, 'int', 'Fee per byte', 'min_fee_a', 'Economic'),
	P('minFeeB', 1, 'int', 'Base fee', 'min_fee_b', 'Economic', { unit: 'lovelace' }),
	P('maxBlockSize', 2, 'int', 'Max block body size', 'max_block_size', 'Network', { unit: 'bytes' }),
	P('maxTxSize', 3, 'int', 'Max transaction size', 'max_tx_size', 'Network', { unit: 'bytes' }),
	P('maxBlockHeaderSize', 4, 'int', 'Max block header size', 'max_block_header_size', 'Network', { unit: 'bytes' }),
	P('keyDeposit', 5, 'int', 'Stake key deposit', 'key_deposit', 'Economic', { unit: 'lovelace' }),
	P('poolDeposit', 6, 'int', 'Pool deposit', 'pool_deposit', 'Economic', { unit: 'lovelace' }),
	P('maxEpoch', 7, 'int', 'Max pool retirement', 'e_max', 'Technical', { unit: 'epochs' }),
	P('nOpt', 8, 'int', 'Target number of pools (k)', 'n_opt', 'Technical'),
	P('poolPledgeInfluence', 9, 'rational', 'Pledge influence (a0)', 'a0', 'Technical'),
	P('expansionRate', 10, 'rational', 'Monetary expansion (ρ)', 'rho', 'Economic'),
	P('treasuryGrowthRate', 11, 'rational', 'Treasury cut (τ)', 'tau', 'Economic'),
	P('minPoolCost', 16, 'int', 'Min pool cost', 'min_pool_cost', 'Economic', { unit: 'lovelace' }),
	P('adaPerUtxoByte', 17, 'int', 'Lovelace per UTxO byte', 'coins_per_utxo_size', 'Economic'),
	P('priceMem', 19, 'rational', 'Price per memory unit', 'price_mem', 'Economic', { slot: 0 }),
	P('priceStep', 19, 'rational', 'Price per CPU step', 'price_step', 'Economic', { slot: 1 }),
	P('maxTxExMem', 20, 'int', 'Max memory per transaction', 'max_tx_ex_mem', 'Network', { slot: 0 }),
	P('maxTxExSteps', 20, 'int', 'Max CPU steps per transaction', 'max_tx_ex_steps', 'Network', { slot: 1 }),
	P('maxBlockExMem', 21, 'int', 'Max memory per block', 'max_block_ex_mem', 'Network', { slot: 0 }),
	P('maxBlockExSteps', 21, 'int', 'Max CPU steps per block', 'max_block_ex_steps', 'Network', { slot: 1 }),
	P('maxValSize', 22, 'int', 'Max value size', 'max_val_size', 'Network', { unit: 'bytes' }),
	P('collateralPercent', 23, 'int', 'Collateral (% of fee)', 'collateral_percent', 'Technical'),
	P('maxCollateralInputs', 24, 'int', 'Max collateral inputs', 'max_collateral_inputs', 'Network'),
	...POOL_THRESHOLDS.map((f, i) => P(`poolVotingThresholds.${f}`, 25, 'rational', `SPO threshold: ${POOL_LABELS[i]}`, POOL_CURRENT[i], 'Governance', { slot: i })),
	...DREP_THRESHOLDS.map((f, i) => P(`drepVotingThresholds.${f}`, 26, 'rational', `DRep threshold: ${DREP_LABELS[i]}`, DREP_CURRENT[i], 'Governance', { slot: i })),
	P('committeeMinSize', 27, 'int', 'Committee min size', 'committee_min_size', 'Governance'),
	P('committeeMaxTermLength', 28, 'int', 'Committee max term', 'committee_max_term_length', 'Governance', { unit: 'epochs' }),
	P('govActionLifetime', 29, 'int', 'Governance action lifetime', 'gov_action_lifetime', 'Governance', { unit: 'epochs' }),
	P('govActionDeposit', 30, 'int', 'Governance action deposit', 'gov_action_deposit', 'Governance', { unit: 'lovelace' }),
	P('drepDeposit', 31, 'int', 'DRep deposit', 'drep_deposit', 'Governance', { unit: 'lovelace' }),
	P('drepActivity', 32, 'int', 'DRep activity period', 'drep_activity', 'Governance', { unit: 'epochs' }),
	P('minFeeRefScriptCostPerByte', 33, 'rational', 'Reference script fee per byte', 'min_fee_ref_script_cost_per_byte', 'Economic')
];

export const paramByName: Record<string, ParamDef> = Object.fromEntries(PARAMS.map((p) => [p.name, p]));
/** Store field names are matched ignoring case (Jackson writes nOpt as "nopt"). */
const byLowerName: Record<string, ParamDef> = Object.fromEntries(PARAMS.map((p) => [p.name.toLowerCase(), p]));

/** Parse "12", "3/4", "0.0577", "7.21e-05" or {numerator, denominator}; null when it isn't a number. */
export function toRational(v: unknown): Rational | null {
	if (v && typeof v === 'object' && 'numerator' in v && 'denominator' in v) {
		try {
			const r = v as { numerator: number | string; denominator: number | string };
			const d = BigInt(r.denominator);
			return d > 0n ? { n: BigInt(r.numerator), d } : null;
		} catch {
			return null;
		}
	}
	const s = String(v ?? '').trim();
	const frac = s.match(/^(-?\d+)\s*\/\s*(\d+)$/);
	if (frac) return BigInt(frac[2]) > 0n ? { n: BigInt(frac[1]), d: BigInt(frac[2]) } : null;
	const dec = s.match(/^(-?)(\d*)(?:\.(\d*))?(?:e([+-]?\d+))?$/i);
	if (!dec || (!dec[2] && !dec[3])) return null;
	let digits = (dec[2] || '') + (dec[3] || '');
	let scale = (dec[3] || '').length - Number(dec[4] || 0);
	if (scale < 0) {
		digits += '0'.repeat(-scale);
		scale = 0;
	}
	const n = BigInt((dec[1] || '') + (digits.replace(/^0+(?=\d)/, '') || '0'));
	return reduce({ n, d: 10n ** BigInt(scale) });
}

function gcd(a: bigint, b: bigint): bigint {
	a = a < 0n ? -a : a;
	while (b) [a, b] = [b, a % b];
	return a || 1n;
}
const reduce = (r: Rational): Rational => {
	const g = gcd(r.n, r.d);
	return { n: r.n / g, d: r.d / g };
};

export const formatRational = (r: Rational) => (r.d === 1n ? String(r.n) : `${r.n}/${r.d}`);

/** An integer from a form or the store; null when it isn't one. */
export function toInt(v: unknown): bigint | null {
	const s = String(v ?? '').trim().replace(/[_,\s]/g, '');
	return /^-?\d+$/.test(s) ? BigInt(s) : null;
}

/** A parameter change as the viewer edits it: values by parameter name (strings), plus raw cost models. */
export interface ParamChange {
	values: Record<string, string>;
	/** Language (0 = V1, 1 = V2, 2 = V3) → CBOR list of costs, as Yaci Store gives them. */
	costModels?: Record<string, string>;
}

/** Yaci Store's protocolParamUpdate → the editable form. */
export function paramChangeOf(update: Record<string, unknown> | null | undefined): ParamChange {
	const values: Record<string, string> = {};
	const costModels: Record<string, string> = {};
	for (const [k, v] of Object.entries(update ?? {})) {
		if (v == null) continue;
		if (k === 'costModels' && typeof v === 'object') {
			for (const [lang, hex] of Object.entries(v as Record<string, string>)) costModels[lang] = String(hex);
		} else if ((k === 'poolVotingThresholds' || k === 'drepVotingThresholds') && typeof v === 'object') {
			for (const [f, r] of Object.entries(v as Record<string, unknown>)) {
				const rat = toRational(r);
				const def = byLowerName[`${k}.${f}`.toLowerCase()];
				if (rat && def) values[def.name] = formatRational(rat);
			}
		} else if (byLowerName[k.toLowerCase()]) {
			const def = byLowerName[k.toLowerCase()];
			const parsed = def.kind === 'rational' ? toRational(v) : toInt(v);
			if (parsed != null) values[def.name] = typeof parsed === 'bigint' ? String(parsed) : formatRational(parsed);
		}
	}
	return Object.keys(costModels).length ? { values, costModels } : { values };
}

const I = (n: bigint | number): PlutusData => ({ int: String(n) });
const B = (hex: string): PlutusData => ({ bytes: hex.toLowerCase() });
const C = (constructor: number, fields: PlutusData[] = []): PlutusData => ({ constructor, fields });
const L = (list: PlutusData[]): PlutusData => ({ list });
const rat = (r: Rational): PlutusData => L([I(r.n), I(r.d)]);
const just = (d: PlutusData): PlutusData => C(0, [d]);
const nothing: PlutusData = C(1);

export class ParamError extends Error {
	constructor(
		readonly param: string,
		message: string
	) {
		super(message);
	}
}

/**
 * The ChangedParameters map. A composite value (prices, ex-units, thresholds) is set as a whole in the ledger, so
 * the parts a change leaves out take their current value from `current` (an /epochs/{n}/parameters response).
 */
export function encodeParamChange(change: ParamChange, current: Record<string, unknown> | null = null): PlutusData {
	const parsed = new Map<string, bigint | Rational>();
	for (const [name, raw] of Object.entries(change.values)) {
		const def = paramByName[name];
		if (!def) continue;
		const v = def.kind === 'rational' ? toRational(raw) : toInt(raw);
		if (v == null) throw new ParamError(name, `${def.label}: “${raw}” is not ${def.kind === 'rational' ? 'a number or fraction' : 'a whole number'}.`);
		parsed.set(name, v);
	}
	const keys = new Map<number, PlutusData>();
	const valueOf = (def: ParamDef): bigint | Rational => {
		const v = parsed.get(def.name);
		if (v != null) return v;
		const c = def.kind === 'rational' ? toRational(current?.[def.current]) : toInt(current?.[def.current]);
		if (c == null) throw new ParamError(def.name, `${def.label} is part of a value that is changed as a whole, and its current value is unknown. Set it too.`);
		return c;
	};
	const encode = (def: ParamDef, v: bigint | Rational) => (typeof v === 'bigint' ? I(v) : rat(v));
	for (const def of PARAMS) {
		if (keys.has(def.key) || ![...parsed.keys()].some((n) => paramByName[n].key === def.key)) continue;
		if (def.slot == null) keys.set(def.key, encode(def, parsed.get(def.name)!));
		else {
			const parts = PARAMS.filter((p) => p.key === def.key).sort((a, b) => a.slot! - b.slot!);
			keys.set(def.key, L(parts.map((p) => encode(p, valueOf(p)))));
		}
	}
	if (change.costModels && Object.keys(change.costModels).length) {
		const entries = Object.entries(change.costModels)
			.map(([lang, hex]) => ({ k: I(Number(lang)), v: decodePlutusData(hex) }))
			.sort((a, b) => Number((a.k as { int: string }).int) - Number((b.k as { int: string }).int));
		keys.set(18, { map: entries });
	}
	return { map: [...keys.entries()].sort((a, b) => a[0] - b[0]).map(([k, v]) => ({ k: I(k), v })) };
}

/** A credential from a stake address (bech32) or reward account (hex, header byte first). */
export function credentialOf(address: string): { script: boolean; hash: string } | null {
	let bytes: Uint8Array | null = null;
	if (/^[0-9a-f]{58}$/i.test(address)) bytes = new Uint8Array(address.match(/../g)!.map((h) => parseInt(h, 16)));
	else bytes = bech32Decode(address)?.bytes ?? null;
	if (!bytes || bytes.length !== 29) return null;
	return { script: (bytes[0] & 0x10) !== 0, hash: [...bytes.slice(1)].map((b) => b.toString(16).padStart(2, '0')).join('') };
}

const credentialData = (c: { script: boolean; hash: string }): PlutusData => C(c.script ? 1 : 0, [B(c.hash)]);
/** Ledger order of credentials: script hashes first, then by hash. */
const credOrder = (a: { script: boolean; hash: string }, b: { script: boolean; hash: string }) => (a.script === b.script ? (a.hash < b.hash ? -1 : a.hash > b.hash ? 1 : 0) : a.script ? -1 : 1);

type GovActionId = { transactionId?: string; gov_action_index?: number } | null | undefined;
const prevId = (id: GovActionId): PlutusData => (id?.transactionId ? just(C(0, [B(id.transactionId), I(id.gov_action_index ?? 0)])) : nothing);
const policy = (h: string | null | undefined): PlutusData => (h ? just(B(h)) : nothing);

/** Yaci Store's proposal details (`details` of /governance/proposals) → GovernanceAction data. */
// eslint-disable-next-line @typescript-eslint/no-explicit-any
export function encodeAction(type: string, details: any, current: Record<string, unknown> | null = null): PlutusData {
	switch (type) {
		case 'PARAMETER_CHANGE_ACTION':
			return C(0, [prevId(details?.govActionId), encodeParamChange(paramChangeOf(details?.protocolParamUpdate), current), policy(details?.policyHash)]);
		case 'HARD_FORK_INITIATION_ACTION': {
			const v = details?.protocolVersion ?? {};
			return C(1, [prevId(details?.govActionId), C(0, [I(v._1 ?? v.major ?? 0), I(v._2 ?? v.minor ?? 0)])]);
		}
		case 'TREASURY_WITHDRAWALS_ACTION':
			return C(2, [encodeWithdrawals(Object.entries(details?.withdrawals ?? {}).map(([address, amount]) => ({ address, amount: String(amount) }))), policy(details?.policyHash)]);
		case 'NO_CONFIDENCE':
			return C(3, [prevId(details?.govActionId)]);
		case 'UPDATE_COMMITTEE': {
			const cred = (c: { type?: string; hash?: string }) => ({ script: c?.type === 'SCRIPTHASH', hash: String(c?.hash ?? '') });
			const removed = ((details?.membersForRemoval ?? []) as { type?: string; hash?: string }[]).map(cred).sort(credOrder);
			const added = Object.entries((details?.newMembersAndTerms ?? {}) as Record<string, number>)
				.map(([k, epoch]) => {
					let c: { type?: string; hash?: string } = {};
					try {
						c = JSON.parse(k);
					} catch {
						/* not a JSON key */
					}
					return { c: cred(c), epoch };
				})
				.sort((a, b) => credOrder(a.c, b.c));
			const q = toRational(details?.threshold) ?? { n: 0n, d: 1n };
			return C(4, [prevId(details?.govActionId), L(removed.map(credentialData)), { map: added.map((a) => ({ k: credentialData(a.c), v: I(a.epoch) })) }, C(0, [I(q.n), I(q.d)])]);
		}
		case 'NEW_CONSTITUTION':
			return C(5, [prevId(details?.govActionId), C(0, [policy(details?.constitution?.scripthash)])]);
		default:
			return C(6);
	}
}

export function encodeWithdrawals(list: { address: string; amount: string }[]): PlutusData {
	const entries = list.map((w) => {
		const c = credentialOf(w.address);
		if (!c) throw new ParamError('withdrawals', `“${w.address}” is not a stake address.`);
		const amount = toInt(w.amount);
		if (amount == null) throw new ParamError('withdrawals', `“${w.amount}” is not a whole number of lovelace.`);
		return { c, amount };
	});
	return { map: entries.sort((a, b) => credOrder(a.c, b.c)).map((e) => ({ k: credentialData(e.c), v: I(e.amount) })) };
}

/** ProposalProcedure: deposit, return credential (from the stake address) and action. */
export function encodeProcedure(deposit: string | number, returnAddress: string, action: PlutusData): PlutusData {
	const c = credentialOf(returnAddress) ?? { script: false, hash: '00'.repeat(28) };
	return C(0, [I(BigInt(deposit)), credentialData(c), action]);
}

/** julc's name for an action type in a MockTransaction proposal. */
export function mockActionType(type: string): string {
	if (type === 'PARAMETER_CHANGE_ACTION') return 'parameterChange';
	if (type === 'TREASURY_WITHDRAWALS_ACTION') return 'treasuryWithdrawals';
	if (type === 'NO_CONFIDENCE') return 'noConfidence';
	return 'info';
}
