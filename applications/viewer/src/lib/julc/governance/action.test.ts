import { describe, expect, it } from 'vitest';
import { bech32Encode } from '$lib/bech32';
import type { PlutusData } from '$lib/cbor';
import { ParamError, credentialOf, encodeAction, encodeParamChange, encodeProcedure, paramChangeOf, toRational } from './action';

const i = (n: number | bigint): PlutusData => ({ int: String(n) });
const b = (hex: string): PlutusData => ({ bytes: hex });
const c = (constructor: number, ...fields: PlutusData[]): PlutusData => ({ constructor, fields });
const list = (...items: PlutusData[]): PlutusData => ({ list: items });
const keys = (d: PlutusData) => (d as { map: { k: { int: string } }[] }).map.map((e) => Number(e.k.int));
const valueAt = (d: PlutusData, key: number) => (d as { map: { k: { int: string }; v: PlutusData }[] }).map.find((e) => Number(e.k.int) === key)?.v;
const rat = (n: number, d: number) => list(i(n), i(d));

describe('toRational', () => {
	it('reads fractions, decimals, exponents and the store’s {numerator, denominator}', () => {
		expect(toRational('3/10')).toEqual({ n: 3n, d: 10n });
		expect(toRational('0.0577')).toEqual({ n: 577n, d: 10000n });
		expect(toRational('7.21e-05')).toEqual({ n: 721n, d: 10000000n });
		expect(toRational(7.21e-5)).toEqual({ n: 721n, d: 10000000n });
		expect(toRational('0.30')).toEqual({ n: 3n, d: 10n });
		expect(toRational('1e3')).toEqual({ n: 1000n, d: 1n });
		expect(toRational({ numerator: 1, denominator: 10 })).toEqual({ n: 1n, d: 10n });
		expect(toRational('abc')).toBeNull();
		expect(toRational('1/0')).toBeNull();
	});
});

describe('encodeParamChange', () => {
	it('encodes a Mainnet proposal’s update in ledger key order (execution limits as [mem, steps])', () => {
		// gov_action14dr5yg75pchr2sz42djtuflpvx5qnsek29qg7s7cft8lzrqt5vrqqtqntpk
		const change = paramChangeOf({ maxTxExMem: 17500000, minPoolCost: 75000000, maxTxExSteps: 10000000000, maxBlockExMem: 77500000, maxBlockExSteps: 20000000000 });
		const d = encodeParamChange(change);
		expect(keys(d)).toEqual([16, 20, 21]);
		expect(valueAt(d, 16)).toEqual(i(75000000));
		expect(valueAt(d, 20)).toEqual(list(i(17500000), i(10000000000)));
		expect(valueAt(d, 21)).toEqual(list(i(77500000), i(20000000000)));
	});

	it('fills the unchanged parts of a composite value from the current parameters', () => {
		const current = { price_mem: 0.0577, price_step: 7.21e-5, dvt_motion_no_confidence: 0.67, dvt_committee_normal: 0.67, dvt_committee_no_confidence: 0.6, dvt_update_to_constitution: 0.75, dvt_hard_fork_initiation: 0.6, dvt_p_p_network_group: 0.67, dvt_p_p_economic_group: 0.67, dvt_p_p_technical_group: 0.67, dvt_p_p_gov_group: 0.75, dvt_treasury_withdrawal: 0.67 };
		const prices = encodeParamChange({ values: { priceMem: '0.06' } }, current);
		expect(valueAt(prices, 19)).toEqual(list(rat(3, 50), rat(721, 10000000)));
		const thresholds = encodeParamChange({ values: { 'drepVotingThresholds.dvtPPGovGroup': '4/5' } }, current);
		const t = valueAt(thresholds, 26) as { list: PlutusData[] };
		expect(t.list).toHaveLength(10);
		expect(t.list[8]).toEqual(rat(4, 5));
		expect(t.list[0]).toEqual(rat(67, 100));
		expect(() => encodeParamChange({ values: { priceMem: '0.06' } }, null)).toThrow(ParamError);
	});

	it('rejects values that aren’t numbers', () => {
		expect(() => encodeParamChange({ values: { minPoolCost: '1.5' } })).toThrow(/whole number/);
		expect(() => encodeParamChange({ values: { treasuryGrowthRate: 'x' } })).toThrow(/fraction/);
	});

	it('reads the store’s names case-insensitively, thresholds and cost models', () => {
		const change = paramChangeOf({ nopt: 500, treasuryGrowthRate: { numerator: 1, denominator: 10 }, poolVotingThresholds: { pvtPPSecurityGroup: { numerator: 51, denominator: 100 } }, costModels: { '2': '9f0102ff' }, costModelsHash: 'ab' });
		expect(change.values).toEqual({ nOpt: '500', treasuryGrowthRate: '1/10', 'poolVotingThresholds.pvtPPSecurityGroup': '51/100' });
		const d = encodeParamChange({ values: { nOpt: '500' }, costModels: change.costModels });
		expect(keys(d)).toEqual([8, 18]);
		expect(valueAt(d, 18)).toEqual({ map: [{ k: i(2), v: list(i(1), i(2)) }] });
	});
});

describe('governance actions', () => {
	const key = 'aa'.repeat(28);
	const script = '11'.repeat(28);
	it('reads credentials from stake addresses and reward accounts', () => {
		expect(credentialOf('e1' + key)).toEqual({ script: false, hash: key });
		expect(credentialOf('f1' + script)).toEqual({ script: true, hash: script });
		expect(credentialOf(bech32Encode('stake', new Uint8Array([0xe1, ...Array(28).fill(0xaa)])))).toEqual({ script: false, hash: key });
		expect(credentialOf('nope')).toBeNull();
	});

	it('orders treasury withdrawals as the ledger does: script credentials first', () => {
		const d = encodeAction('TREASURY_WITHDRAWALS_ACTION', { policyHash: 'fa'.repeat(28), withdrawals: { ['e1' + key]: 5, ['f1' + script]: 7 } });
		expect(d).toEqual(c(2, { map: [{ k: c(1, b(script)), v: i(7) }, { k: c(0, b(key)), v: i(5) }] }, c(0, b('fa'.repeat(28)))));
	});

	it('encodes a parameter change with its previous action and policy, inside a procedure', () => {
		const action = encodeAction('PARAMETER_CHANGE_ACTION', { policyHash: 'fa'.repeat(28), govActionId: { transactionId: 'cc'.repeat(32), gov_action_index: 0 }, protocolParamUpdate: { committeeMinSize: 5 } });
		expect(action).toEqual(c(0, c(0, c(0, b('cc'.repeat(32)), i(0))), { map: [{ k: i(27), v: i(5) }] }, c(0, b('fa'.repeat(28)))));
		expect(encodeProcedure(100000000000, 'e0' + key, action)).toEqual(c(0, i(100000000000), c(0, b(key)), action));
		expect(encodeAction('INFO_ACTION', {})).toEqual(c(6));
		expect(encodeAction('HARD_FORK_INITIATION_ACTION', { protocolVersion: { _1: 11, _2: 0 } })).toEqual(c(1, c(1), c(0, i(11), i(0))));
	});
});
