import { describe, expect, it } from 'vitest';
import { SwrCache } from './swr-cache';

function setup(maxEntries = 10) {
	let t = 0;
	const cache = new SwrCache({ maxEntries, maxStaleMs: 1000, now: () => t });
	return { cache, advance: (ms: number) => (t += ms) };
}
const tick = () => new Promise((r) => setTimeout(r, 0));

describe('SwrCache', () => {
	it('serves a fresh answer without loading again', async () => {
		const { cache, advance } = setup();
		let calls = 0;
		const load = async () => ++calls;
		expect(await cache.get('k', 100, load)).toBe(1);
		advance(100);
		expect(await cache.get('k', 100, load)).toBe(1);
		expect(calls).toBe(1);
	});

	it('serves a stale answer at once and refreshes it in the background', async () => {
		const { cache, advance } = setup();
		let calls = 0;
		const load = async () => ++calls;
		await cache.get('k', 100, load);
		advance(101);
		expect(await cache.get('k', 100, load)).toBe(1); // stale, at once
		await tick();
		expect(calls).toBe(2);
		expect(await cache.get('k', 100, load)).toBe(2); // the refreshed answer
	});

	it('loads again, and waits, once an answer is older than maxStaleMs', async () => {
		const { cache, advance } = setup();
		let calls = 0;
		await cache.get('k', 100, async () => ++calls);
		advance(1001);
		expect(await cache.get('k', 100, async () => ++calls)).toBe(2);
	});

	it('shares one load between concurrent callers', async () => {
		const { cache } = setup();
		let calls = 0;
		let release!: () => void;
		const gate = new Promise<void>((r) => (release = r));
		const load = async () => (calls++, await gate, 'v');
		const all = Promise.all([cache.get('k', 100, load), cache.get('k', 100, load), cache.get('k', 100, load)]);
		release();
		expect(await all).toEqual(['v', 'v', 'v']);
		expect(calls).toBe(1);
	});

	it('keeps the stale answer when a refresh fails', async () => {
		const { cache, advance } = setup();
		await cache.get('k', 100, async () => 'old');
		advance(150);
		expect(await cache.get('k', 100, async () => Promise.reject(new Error('down')))).toBe('old');
		await tick();
		expect(await cache.get('k', 100, async () => 'new')).toBe('old'); // still stale: refreshes again
		await tick();
		expect(await cache.get('k', 100, async () => 'newer')).toBe('new');
	});

	it('passes on a failed first load', async () => {
		const { cache } = setup();
		await expect(cache.get('k', 100, async () => Promise.reject(new Error('down')))).rejects.toThrow('down');
		expect(await cache.get('k', 100, async () => 'ok')).toBe('ok');
	});

	it('keeps at most maxEntries, dropping the least recently used', async () => {
		const { cache } = setup(2);
		let calls = 0;
		const load = async () => ++calls;
		await cache.get('a', 100, load);
		await cache.get('b', 100, load);
		await cache.get('a', 100, load); // a is now the most recently used
		await cache.get('c', 100, load); // drops b
		await cache.get('a', 100, load);
		expect(calls).toBe(3);
		await cache.get('b', 100, load);
		expect(calls).toBe(4);
	});
});
