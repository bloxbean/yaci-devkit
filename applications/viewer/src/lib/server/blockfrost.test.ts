import { beforeEach, describe, expect, it, vi } from 'vitest';
import { BlockfrostAbsent, bfApi, blockfrostPresent, resetBlockfrost } from './blockfrost';
import { indexerGuard } from './heavy-guard';
import { HeavyError } from './indexer-guard';

const reply = (status: number, body: unknown) =>
	vi.fn(async (_url: string | URL | Request, _init?: RequestInit) => new Response(JSON.stringify(body), { status, headers: { 'content-type': 'application/json' } }));

describe('bfApi', () => {
	beforeEach(() => resetBlockfrost());

	it('answers from the Blockfrost-compatible endpoint', async () => {
		const fetch = reply(200, { amount: [{ unit: 'lovelace', quantity: '5' }] });
		expect(await bfApi('k-ok', '/addresses/a1', { fetch })).toEqual({ amount: [{ unit: 'lovelace', quantity: '5' }] });
		expect(String(fetch.mock.calls[0][0])).toContain('/blockfrost/addresses/a1');
	});

	it('treats "No endpoint" as a store without the API, and stops asking for a while', async () => {
		const fetch = reply(404, { status: 404, detail: 'No endpoint GET /api/v1/blockfrost/addresses/a2.' });
		await expect(bfApi('k-absent', '/addresses/a2', { fetch })).rejects.toBeInstanceOf(BlockfrostAbsent);
		await expect(bfApi('k-absent-2', '/addresses/a3', { fetch })).rejects.toBeInstanceOf(BlockfrostAbsent);
		expect(fetch).toHaveBeenCalledTimes(1);
	});

	it("marks the key heavy on a 500 (the store's own timeout)", async () => {
		const fetch = reply(500, { status: 500, error: 'Internal Server Error' });
		await expect(bfApi('k-slow', '/addresses/a4/transactions', { fetch })).rejects.toBeInstanceOf(HeavyError);
		expect(indexerGuard.isHeavy('k-slow')).toBe(true);
		await expect(bfApi('k-slow', '/addresses/a4/transactions', { fetch })).rejects.toBeInstanceOf(HeavyError);
		expect(fetch).toHaveBeenCalledTimes(1);
	});

	it('counts as present once the store answers, and not after "No endpoint"', async () => {
		expect(blockfrostPresent()).toBe(false);
		await bfApi('k-p1', '/addresses/p1', { fetch: reply(200, {}) });
		expect(blockfrostPresent()).toBe(true);
		await expect(bfApi('k-p2', '/addresses/p2', { fetch: reply(404, { detail: 'No endpoint GET /x.' }) })).rejects.toBeInstanceOf(BlockfrostAbsent);
		expect(blockfrostPresent()).toBe(false);
	});

	it('passes other errors on', async () => {
		const fetch = reply(400, { status: 400, detail: 'Invalid address' });
		await expect(bfApi('k-bad', '/addresses/nope', { fetch })).rejects.toMatchObject({ status: 400 });
	});
});
