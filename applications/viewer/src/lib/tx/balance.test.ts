import { describe, expect, it } from 'vitest';
import { balanceOfPages, mergeAmounts, utxoBalance } from './balance';
import type { Utxo } from './types';

const POLICY = 'a0028f350aaabe0545fdcb56b039bfb08e4bb4d8c4d7c3c7d481c235';
const utxo = (i: number, lovelace: number | string, assets: Record<string, number | string> = {}): Utxo => ({
	tx_hash: i.toString(16).padStart(64, '0'),
	output_index: 0,
	address: 'addr_test1',
	amount: [{ unit: 'lovelace', quantity: String(lovelace) }, ...Object.entries(assets).map(([unit, quantity]) => ({ unit, quantity }))]
});
const page = (from: number, n: number) => Array.from({ length: n }, (_, i) => utxo(from + i, 1_000_000));

describe('mergeAmounts', () => {
	it('adds up lovelace and native assets by unit, lovelace first', () => {
		const merged = mergeAmounts([utxo(1, 2_000_000, { [POLICY + '01']: 5 }).amount, utxo(2, 3_000_000, { [POLICY + '01']: 7, [POLICY + '02']: 1 }).amount]);
		expect(merged).toEqual([
			{ unit: 'lovelace', quantity: '5000000' },
			{ unit: POLICY + '01', quantity: '12' },
			{ unit: POLICY + '02', quantity: '1' }
		]);
	});

	it('keeps quantities past 2^53 exact', () => {
		const big = '9223372036854775807';
		expect(mergeAmounts([utxo(1, 1, { [POLICY]: big }).amount, utxo(2, 1, { [POLICY]: 1 }).amount])[1].quantity).toBe('9223372036854775808');
	});

	it('handles no outputs', () => {
		expect(mergeAmounts([])).toEqual([]);
	});
});

describe('balanceOfPages', () => {
	it('is exact when the last page is short', () => {
		const b = balanceOfPages([page(0, 100), page(100, 42)], 100);
		expect(b).toMatchObject({ exact: true, utxos: 142 });
		expect(b.amounts[0].quantity).toBe('142000000');
	});

	it('is a lower bound when the last page is full', () => {
		expect(balanceOfPages([page(0, 100), page(100, 100)], 100)).toMatchObject({ exact: false, utxos: 200 });
	});

	it('is exact for an empty address', () => {
		expect(balanceOfPages([[]], 100)).toEqual({ amounts: [], exact: true, utxos: 0 });
	});
});

describe('utxoBalance', () => {
	it('stops at the first short page', async () => {
		const asked: number[] = [];
		const b = await utxoBalance(async (p) => (asked.push(p), p < 3 ? page(p * 100, 100) : page(300, 7)), { pageSize: 100, maxPages: 5 });
		expect(asked).toEqual([1, 2, 3]);
		expect(b).toMatchObject({ exact: true, utxos: 207 });
	});

	it('stops at maxPages and reports a lower bound', async () => {
		const asked: number[] = [];
		const b = await utxoBalance(async (p) => (asked.push(p), page(p * 100, 100)), { pageSize: 100, maxPages: 5 });
		expect(asked).toEqual([1, 2, 3, 4, 5]);
		expect(b).toMatchObject({ exact: false, utxos: 500 });
	});

	it('keeps the pages read when a later one fails', async () => {
		const b = await utxoBalance(
			async (p) => {
				if (p === 3) throw new Error('timeout');
				return page(p * 100, 100);
			},
			{ pageSize: 100, maxPages: 5 }
		);
		expect(b).toMatchObject({ exact: false, utxos: 200 });
	});

	it('fails when the first page does', async () => {
		await expect(utxoBalance(async () => Promise.reject(new Error('timeout')), { pageSize: 100, maxPages: 5 })).rejects.toThrow('timeout');
	});

	it('uses a first page it is given and reads pages one at a time', async () => {
		let inFlight = 0;
		let most = 0;
		const asked: number[] = [];
		await utxoBalance(
			async (p) => {
				asked.push(p);
				most = Math.max(most, ++inFlight);
				await new Promise((r) => setTimeout(r, 1));
				inFlight--;
				return p < 3 ? page(p * 100, 100) : [];
			},
			{ pageSize: 100, maxPages: 5, first: page(0, 100) }
		);
		expect(asked).toEqual([2, 3]);
		expect(most).toBe(1);
	});
});
