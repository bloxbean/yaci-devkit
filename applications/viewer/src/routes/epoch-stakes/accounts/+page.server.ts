import { tryApi } from '$lib/api/client';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/* One stake account's stake in an epoch's snapshot, and a few epochs before it for context. */
export const load: PageServerLoad = async ({ url, fetch }) => {
	const latest = await tryApi<Any>('/epochs/latest', { fetch });
	const current = Number(latest?.epoch ?? 0);
	const asked = parseInt(url.searchParams.get('epoch') ?? '', 10);
	const epoch = Number.isFinite(asked) && asked >= 0 ? asked : current;
	const address = (url.searchParams.get('address') ?? '').trim();
	const valid = /^(stake|stake_test)1[0-9a-z]+$/.test(address);
	if (!address || !valid) return { address: address || null, valid: !address || valid, epoch, current, stake: null, history: [] as Any[] };

	const history = await Promise.all(
		[0, 1, 2, 3, 4, 5].map((i) => (epoch - i < 0 ? null : tryApi<Any>(`/epochs/${epoch - i}/accounts/${address}/stake`, { fetch, timeoutMs: 8000 })))
	);
	const rows = history.filter((h): h is Any => !!h);
	return { address, valid, epoch, current, stake: rows.find((r) => r.epoch === epoch) ?? null, history: rows };
};
