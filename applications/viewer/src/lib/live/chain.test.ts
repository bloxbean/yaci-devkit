import { describe, expect, it } from 'vitest';
import { contiguous, type LiveBlock } from './chain.svelte';

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
