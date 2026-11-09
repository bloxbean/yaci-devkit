import { api, paging, tryApi } from '$lib/api/client';
import { poolDistribution } from '$lib/staking/distribution';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/* The stake snapshot for an epoch: total, by pool, and by account. The account list is 1-based. */
export const load: PageServerLoad = async ({ url, fetch }) => {
	const latest = await tryApi<Any>('/epochs/latest', { fetch });
	const current = Number(latest?.epoch ?? 0);
	const asked = parseInt(url.searchParams.get('epoch') ?? '', 10);
	const epoch = Number.isFinite(asked) && asked >= 0 ? asked : current;
	const { page, count } = paging(url, 20);

	const [totals, distribution, accounts] = await Promise.all([
		Promise.all([5, 4, 3, 2, 1, 0].map((i) => (epoch - i < 0 ? null : tryApi<Any>(`/epochs/${epoch - i}/total-stake`, { fetch, timeoutMs: 8000 })))),
		poolDistribution(epoch, fetch),
		api<Any[]>(`/epochs/${epoch}/stake`, { fetch, params: { page, count }, timeoutMs: 25000 }).then(
			(r) => ({ rows: r ?? [], error: null as string | null }),
			(e) => ({ rows: [] as Any[], error: (e as Error).message })
		)
	]);

	const trend = totals.filter((t): t is Any => !!t).map((t) => ({ epoch: t.epoch as number, stake: Number(t.active_stake) }));
	return {
		epoch,
		current,
		page,
		count,
		total: trend.find((t) => t.epoch === epoch)?.stake ?? null,
		trend,
		distribution,
		accounts: accounts.rows,
		listError: accounts.error
	};
};
