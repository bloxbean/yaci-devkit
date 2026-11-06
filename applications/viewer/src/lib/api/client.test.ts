import { describe, expect, it, vi } from 'vitest';
import { ApiError, api } from './client';

const ok = () => vi.fn(async (_url: string | URL | Request, _init?: RequestInit) => new Response('{"ok":true}', { status: 200 }));

describe('api paths', () => {
	it('sends an ordinary path to the store', async () => {
		const fetch = ok();
		await api('/blocks/12', { fetch, params: { page: 1 } });
		expect(String(fetch.mock.calls[0][0])).toBe('http://localhost:8080/api/v1/blocks/12?page=1');
	});

	// Route params arrive decoded: /blocks/..%2F..%2Factuator becomes "../../actuator".
	it.each([
		'/blocks/../../../actuator/env',
		'/blocks/..',
		'/blocks/%2e%2e/%2E%2E/actuator',
		'/blocks/.\t./x',
		'/addresses/a?x=1',
		'/addresses/a#frag',
		'/addresses/a\\..\\b'
	])('refuses %j without calling the store', async (path) => {
		const fetch = ok();
		await expect(api(path, { fetch })).rejects.toMatchObject({ status: 400 });
		await expect(api(path, { fetch })).rejects.toBeInstanceOf(ApiError);
		expect(fetch).not.toHaveBeenCalled();
	});
});
