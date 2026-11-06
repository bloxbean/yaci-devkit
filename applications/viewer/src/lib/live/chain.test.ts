import { describe, expect, it } from 'vitest';
import { contiguous, replacesChain, type LiveBlock } from './chain.svelte';

const block = (number: number, time: number): LiveBlock => ({
	number,
	slot: time,
	epoch: 1,
	time,
	txCount: 0,
	size: 0,
	receivedAt: 0
});

describe('contiguous', () => {
	it('keeps the newest run of consecutive blocks', () => {
		// The tip after a night asleep, then blocks from before it: the old ones would make the average block time hours long.
		const list = [block(1000, 50000), block(10, 200), block(9, 180), block(8, 160)];
		expect(contiguous(list).map((b) => b.number)).toEqual([1000]);
	});

	it('keeps a list without gaps as is', () => {
		const list = [block(12, 240), block(11, 220), block(10, 200)];
		expect(contiguous(list)).toEqual(list);
	});

	it('handles an empty list', () => {
		expect(contiguous([])).toEqual([]);
	});
});

describe('replacesChain', () => {
	const held = [12, 11, 10].map((n) => ({ ...block(n, n * 20), hash: `h${n}` }));

	it('spots a devnet reset: a block below the tip at a height not held', () => {
		expect(replacesChain(held, { ...block(3, 60), hash: 'new3' })).toBe(true);
	});

	it('spots a rollback: another hash at a held height below the tip', () => {
		expect(replacesChain(held, { ...block(11, 220), hash: 'other' })).toBe(true);
	});

	it('leaves the same block, the tip and new blocks alone', () => {
		expect(replacesChain(held, { ...block(11, 220), hash: 'h11' })).toBe(false);
		expect(replacesChain(held, { ...block(11, 220) })).toBe(false);
		expect(replacesChain(held, { ...block(12, 240), hash: 'other' })).toBe(false);
		expect(replacesChain(held, { ...block(13, 260), hash: 'h13' })).toBe(false);
		expect(replacesChain([], block(1, 20))).toBe(false);
	});
});
