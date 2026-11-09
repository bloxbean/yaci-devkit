import { describe, expect, it, vi } from 'vitest';

vi.mock('@sveltejs/kit', () => ({ error: (status: number, message: string) => Promise.reject({ status, message }) }));

const { collateralReturn } = await import('./load.server');
const { wholeTxHex } = await import('./decode');

describe('collateralReturn', () => {
	it('turns the store collateral return into a UTxO like the outputs', () => {
		const r = collateralReturn({ address: 'addr1x', amounts: [{ unit: 'lovelace', quantity: '4500000' }], dataHash: 'dh' }, 'ab'.repeat(32), 2);
		expect(r).toMatchObject({ tx_hash: 'ab'.repeat(32), output_index: 2, address: 'addr1x', amount: [{ unit: 'lovelace', quantity: '4500000' }], data_hash: 'dh' });
	});

	it('keeps a collateral return that is already shaped like a UTxO', () => {
		const r = collateralReturn({ tx_hash: 'cd', output_index: 0, address: 'a', amount: [{ unit: 'lovelace', quantity: '1' }] }, 'ab', 5);
		expect(r).toMatchObject({ tx_hash: 'cd', output_index: 0, amount: [{ unit: 'lovelace', quantity: '1' }] });
	});
});

describe('wholeTxHex', () => {
	it('keeps a whole transaction (a CBOR array) and drops a bare body (a map)', () => {
		expect(wholeTxHex('84a400')).toBe('84a400');
		expect(wholeTxHex('a40081')).toBeNull();
		expect(wholeTxHex('zz')).toBeNull();
		expect(wholeTxHex(null)).toBeNull();
	});
});
