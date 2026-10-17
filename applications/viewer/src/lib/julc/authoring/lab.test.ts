import { describe, expect, it } from 'vitest';
import { bech32Encode } from '$lib/bech32';
import { decodePlutusData, type PlutusData } from '$lib/cbor';
import { labPlan, labPurposeOf, paramData, paymentKeyHash, scriptAddress, templateOf, type LabTx } from './lab';
import { utxoOf } from './utxo';

const key = 'a0f1aa7dca95017c11e7e373aebcf0c4568cf47ec12b94f8eb5bba8b';
const addr = bech32Encode('addr_test', new Uint8Array([0x60, ...key.match(/../g)!.map((h) => parseInt(h, 16))]));
const utxo = { tx_hash: 'ff'.repeat(32), output_index: 1, address: addr, amount: [{ unit: 'lovelace', quantity: '5000000' }, { unit: 'bb'.repeat(28) + '01', quantity: '2' }] };
const script = { cbor: '4e4d01000033222220051200120011', language: 'V3' as const, params: [], hash: 'cc'.repeat(28) };
const tx = (over: Partial<LabTx> = {}): LabTx => ({ purpose: 'spend', utxo, atScript: true, datum: 'd87980', redeemer: 'd87980', referenceInputs: [], outputs: [{ address: addr, lovelace: '4800000' }], mint: [], withdrawal: '0', signers: [key], validFrom: null, validTo: null, fee: '200000', ...over });
const slots = { zeroTime: 1792193424000, zeroSlot: 0, slotLength: 200 };

describe('labPlan', () => {
	it('spends a real UTxO as if locked at the script, with the datum inline', () => {
		const plan = labPlan(script, tx({ validFrom: 10 }), { params: null, slots });
		const t = plan.request!.transaction;
		expect(plan.key).toBe('spend#0');
		expect(t.inputs[0]).toMatchObject({ txId: utxo.tx_hash, index: 1n, address: { payment: '$self' }, datum: { kind: 'inline', data: { format: 'cbor', value: 'd87980' } } });
		expect(t.inputs[0].value.assets).toEqual([{ policyId: 'bb'.repeat(28), tokenName: '01', quantity: '2' }]);
		expect(t.validRange.from).toBe(1792193426000n);
		expect(t.signatories).toEqual([key]);
		expect(plan.datum).toBe('d87980');
	});

	it('mints under the script’s policy and withdraws from its stake credential', () => {
		const mint = labPlan(script, tx({ purpose: 'mint', mint: [{ tokenName: 'cafe', quantity: '5' }] }), { params: null, slots: null });
		expect(mint.request!.transaction.purpose).toEqual({ type: 'mint', target: script.hash });
		expect(mint.request!.transaction.mint).toEqual([{ policyId: script.hash, tokenName: 'cafe', quantity: '5' }]);
		expect(mint.request!.transaction.inputs[0].address.payment).toBe(addr);
		const reward = labPlan(script, tx({ purpose: 'reward' }), { params: null, slots: null });
		expect(reward.request!.transaction.withdrawals).toEqual([{ credential: `script:${script.hash}`, amount: '0' }]);
	});

	it('applies parameters, the epoch’s cost model and limits', () => {
		const plan = labPlan({ ...script, params: ['4100'] }, tx(), { params: { cost_models_raw: { PlutusV3: [1, 2] }, protocol_major_ver: 10, max_tx_ex_steps: '10' }, slots });
		expect(plan.request!.script.params).toEqual([{ format: 'cbor', value: '4100' }]);
		expect(plan.request!.costModel!.parameters).toEqual([1n, 2n]);
		expect(plan.request!.target.protocol).toBe(10);
		expect(plan.request!.maxCpu).toBe(10n);
	});

	it('explains what it can’t build', () => {
		expect(() => labPlan(script, tx({ validFrom: 5 }), { params: null, slots: null })).toThrow(/Slot times/);
		expect(() => labPlan({ ...script, language: 'V2' }, tx({ datum: null }), { params: null, slots })).toThrow(/needs a datum/);
		expect(() => labPlan(script, tx({ signers: ['zz'] }), { params: null, slots })).toThrow(/key hash/);
		expect(labPlan(script, tx({ utxo: null }), { params: null, slots }).notes[0]).toMatch(/sample/);
	});
});

describe('lab helpers', () => {
	it('types parameters by their declared type', () => {
		expect(decodePlutusData(paramData('byte[]', key, JSON.parse))).toEqual({ bytes: key });
		expect(decodePlutusData(paramData('BigInteger', '42', JSON.parse))).toEqual({ int: '42' });
		expect(decodePlutusData(paramData('PlutusData', '{"constructor":1,"fields":[]}', JSON.parse))).toEqual({ constructor: 1, fields: [] });
		expect(() => paramData('byte[]', 'xyz', JSON.parse)).toThrow(/hex/);
	});

	it('makes templates, purposes and addresses', () => {
		expect(decodePlutusData(templateOf(1, [{ type: 'Integer' }, { type: 'ByteString' }, { type: 'Address' }]))).toEqual({ constructor: 1, fields: [{ int: '0' }, { bytes: '' }, { constructor: 0, fields: [] }] } as PlutusData);
		expect(labPurposeOf('MINTING')).toBe('mint');
		expect(labPurposeOf('WITHDRAW')).toBe('reward');
		expect(labPurposeOf('VOTING')).toBeNull();
		expect(paymentKeyHash(addr)).toBe(key);
		expect(paymentKeyHash(bech32Encode('addr_test', new Uint8Array([0x70, ...Array(28).fill(1)])))).toBeNull();
		expect(scriptAddress('cc'.repeat(28), false)).toMatch(/^addr_test1w/);
		expect(scriptAddress('cc'.repeat(28), true)).toMatch(/^addr1w/);
	});

	it('reads the store’s UTxO rows', () => {
		expect(utxoOf({ tx_hash: 'ab', output_index: 2, owner_addr: addr, amounts: [{ unit: 'lovelace', quantity: 7 }], inline_datum: '01' })).toEqual({ tx_hash: 'ab', output_index: 2, address: addr, amount: [{ unit: 'lovelace', quantity: '7' }], data_hash: null, inline_datum: '01', reference_script_hash: null });
		expect(utxoOf(null)).toBeNull();
	});
});

describe('labPlan: other withdrawals', () => {
	it('adds them in ledger order, script credentials first', () => {
		const stake = bech32Encode('stake_test', new Uint8Array([0xe0, ...Array(28).fill(0xaa)]));
		const plan = labPlan(script, tx({ purpose: 'reward', withdrawals: [{ credential: stake, amount: '5' }, { credential: 'script:' + '11'.repeat(28), amount: '0' }] }), { params: null, slots: null });
		expect(plan.request!.transaction.withdrawals.map((w) => w.credential)).toEqual(['script:' + '11'.repeat(28), `script:${script.hash}`, stake]);
		expect(() => labPlan(script, tx({ withdrawals: [{ credential: 'nope', amount: '0' }] }), { params: null, slots: null })).toThrow(/stake address/);
	});
});
