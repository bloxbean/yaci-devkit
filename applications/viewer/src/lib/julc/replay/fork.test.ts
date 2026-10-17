import { describe, expect, it } from 'vitest';
import { buildRuns, type ReplayInput } from './build';
import { applyEdits, countEdits, refOf } from './fork';
import { slotConfigFor } from './slots';
import withdrawZero from './fixtures/mainnet-withdrawZero.json';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const f = withdrawZero as any;
const base: ReplayInput = {
	hash: f.hash,
	tx: f.tx,
	scripts: f.scripts,
	withdrawals: f.withdrawals,
	mints: f.mints,
	votes: [],
	certificates: [],
	datums: {},
	params: f.params,
	slots: slotConfigFor(f.genesis.network_magic, f.genesis)
};
const runs = (input: ReplayInput) => Object.fromEntries(buildRuns(input).map((r) => [r.key, r]));

describe('applyEdits', () => {
	it('changes nothing without edits', () => {
		expect(buildRuns(applyEdits(base, {}))).toEqual(buildRuns(base));
		expect(countEdits({})).toBe(0);
	});

	it('replaces a redeemer of one run only', () => {
		const r = runs(applyEdits(base, { redeemers: { 'spend#0': 'd87a80' } }));
		expect(r['spend#0'].request!.transaction.redeemer).toEqual({ format: 'cbor', value: 'd87a80' });
		expect(r['spend#1'].request!.transaction.redeemer).toEqual({ format: 'cbor', value: 'd87980' });
	});

	it('replaces an inline datum of a spent input', () => {
		const input = [...f.tx.inputs].sort((a, b) => (a.tx_hash < b.tx_hash ? -1 : 1))[0];
		const r = runs(applyEdits(base, { datums: { [refOf(input)]: '182a' } }));
		expect(r['spend#0'].datum).toBe('182a');
		expect(r['spend#0'].request!.transaction.inputs[0].datum).toEqual({ kind: 'inline', data: { format: 'cbor', value: '182a' } });
	});

	it('edits signers, validity, fee, outputs, mint and withdrawals', () => {
		const edits = {
			signers: [],
			validFrom: null,
			validTo: f.tx.ttl + 100,
			fee: '1',
			outputs: { 0: { lovelace: '5', assets: { [`${'aa'.repeat(28)}01`]: '7' } }, 1: { removed: true } },
			mints: { [`${'bb'.repeat(28)}02`]: '-3' },
			withdrawals: { [f.withdrawals[0].address]: '10' },
			protocol: 10 as const
		};
		const tx = runs(applyEdits(base, edits))['spend#0'].request!;
		expect(tx.transaction.signatories).toEqual([]);
		expect(tx.transaction.validRange.from).toBeNull();
		expect(tx.transaction.validRange.to).toBe(BigInt(base.slots!.zeroTime) + BigInt(f.tx.ttl + 100 - base.slots!.zeroSlot) * 1000n);
		expect(tx.transaction.fee).toBe('1');
		expect(tx.transaction.outputs.length).toBe(f.tx.outputs.length - 1);
		expect(tx.transaction.outputs[0].value.lovelace).toBe('5');
		expect(tx.transaction.outputs[0].value.assets).toContainEqual({ policyId: 'aa'.repeat(28), tokenName: '01', quantity: '7' });
		expect(tx.transaction.mint).toEqual([{ policyId: 'bb'.repeat(28), tokenName: '02', quantity: '-3' }]);
		expect(tx.transaction.withdrawals).toEqual([{ credential: f.withdrawals[0].address, amount: '10' }]);
		expect(tx.target.protocol).toBe(10);
		expect(countEdits(edits)).toBe(9);
	});

	it('runs with another epoch’s parameters', () => {
		const other = { ...f.params, cost_models_raw: { ...f.params.cost_models_raw, PlutusV2: [1, 2, 3] } };
		const tx = runs(applyEdits(base, {}, other))['spend#0'].request!;
		expect(tx.costModel!.parameters).toEqual([1n, 2n, 3n]);
	});
});
