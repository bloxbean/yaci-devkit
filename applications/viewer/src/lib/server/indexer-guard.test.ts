import { describe, expect, it } from 'vitest';
import { ApiError } from '$lib/api/client';
import { BusyError, HeavyError, IndexerGuard } from './indexer-guard';

const timeout = (path = '/x') => new ApiError('The request timed out', 0, path, true);

function setup(opts: Partial<{ maxEntries: number; maxConcurrent: number }> = {}) {
	let t = 0;
	const guard = new IndexerGuard({ heavyMs: 15 * 60_000, maxEntries: opts.maxEntries ?? 1000, maxConcurrent: opts.maxConcurrent ?? 4, now: () => t });
	return { guard, advance: (ms: number) => (t += ms) };
}

const deferred = <T>() => {
	let resolve!: (v: T) => void;
	const promise = new Promise<T>((r) => (resolve = r));
	return { promise, resolve };
};

describe('IndexerGuard', () => {
	it('skips a key for 15 minutes after a timeout, then tries again', async () => {
		const { guard, advance } = setup();
		let calls = 0;
		const run = async () => {
			calls++;
			throw timeout();
		};
		await expect(guard.call('/addresses/a/transactions', '/addresses/a/transactions?page=1', run)).rejects.toBeInstanceOf(HeavyError);
		// Any page of the same endpoint is skipped without reaching the store.
		await expect(guard.call('/addresses/a/transactions', '/addresses/a/transactions?page=2', run)).rejects.toBeInstanceOf(HeavyError);
		expect(calls).toBe(1);
		expect(guard.isHeavy('/addresses/a/transactions')).toBe(true);
		expect(guard.isHeavy('/addresses/b/transactions')).toBe(false);

		advance(15 * 60_000 - 1);
		expect(guard.isHeavy('/addresses/a/transactions')).toBe(true);
		advance(1);
		expect(guard.isHeavy('/addresses/a/transactions')).toBe(false);
		await expect(guard.call('/addresses/a/transactions', '/addresses/a/transactions?page=1', async () => 'ok')).resolves.toBe('ok');
	});

	it('can mark a key heavy for a shorter time', async () => {
		const { guard, advance } = setup();
		await expect(guard.call('/u', '/u?page=1', async () => Promise.reject(timeout()), { heavyMs: 120_000 })).rejects.toBeInstanceOf(HeavyError);
		advance(119_999);
		expect(guard.isHeavy('/u')).toBe(true);
		advance(1);
		expect(guard.isHeavy('/u')).toBe(false);
	});

	it('does not mark a key heavy for other errors', async () => {
		const { guard } = setup();
		await expect(guard.call('/k', '/k', async () => Promise.reject(new ApiError('Not found', 404, '/k')))).rejects.toBeInstanceOf(ApiError);
		expect(guard.isHeavy('/k')).toBe(false);
	});

	it('shares one call between identical requests in flight', async () => {
		const { guard } = setup();
		const d = deferred<string>();
		let calls = 0;
		const run = () => (calls++, d.promise);
		const a = guard.call('/accounts/s', '/accounts/s', run);
		const b = guard.call('/accounts/s', '/accounts/s', run);
		d.resolve('account');
		expect(await Promise.all([a, b])).toEqual(['account', 'account']);
		expect(calls).toBe(1);
		// Once settled, a new request calls again (nothing cached without cacheMs).
		await guard.call('/accounts/s', '/accounts/s', async () => (calls++, 'again'));
		expect(calls).toBe(2);
	});

	it('keeps a successful answer for cacheMs', async () => {
		const { guard, advance } = setup();
		let calls = 0;
		const run = async () => ++calls;
		expect(await guard.call('/accounts/s', '/accounts/s', run, { cacheMs: 60_000 })).toBe(1);
		advance(59_999);
		expect(await guard.call('/accounts/s', '/accounts/s', run, { cacheMs: 60_000 })).toBe(1);
		advance(1);
		expect(await guard.call('/accounts/s', '/accounts/s', run, { cacheMs: 60_000 })).toBe(2);
	});

	it('keeps at most maxEntries, dropping the least recently used', async () => {
		const { guard } = setup({ maxEntries: 2 });
		guard.markHeavy('/a');
		guard.markHeavy('/b');
		expect(guard.isHeavy('/a')).toBe(true); // /a is now the most recently used
		guard.markHeavy('/c');
		expect(guard.isHeavy('/b')).toBe(false);
		expect(guard.isHeavy('/a')).toBe(true);
		expect(guard.isHeavy('/c')).toBe(true);
	});

	it('runs at most maxConcurrent calls at once and gives up after waitMs', async () => {
		const { guard } = setup({ maxConcurrent: 1 });
		const d = deferred<string>();
		const first = guard.call('/a', '/a', () => d.promise);
		let started = false;
		const second = guard.call('/b', '/b', async () => ((started = true), 'b'));
		await new Promise((r) => setTimeout(r, 5));
		expect(started).toBe(false);
		d.resolve('a');
		expect(await first).toBe('a');
		expect(await second).toBe('b');

		const hold = deferred<string>();
		const busy = guard.call('/c', '/c', () => hold.promise);
		await expect(guard.call('/d', '/d', async () => 'd', { waitMs: 10 })).rejects.toBeInstanceOf(BusyError);
		hold.resolve('c');
		await busy;
		await expect(guard.call('/e', '/e', async () => 'e', { waitMs: 10 })).resolves.toBe('e');
	});
});
