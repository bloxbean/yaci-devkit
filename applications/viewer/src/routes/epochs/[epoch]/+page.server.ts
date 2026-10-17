import { error } from '@sveltejs/kit';
import { paging, tryApi } from '$lib/api/client';
import { activeStake, fromAggregate, latestEpoch, uniformBounds, type EpochRow } from '$lib/epochs/epochs';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/*
 * One epoch: its numbers, ada pots, and either its blocks (1-based pages) or its protocol parameters, depending on
 * the tab. Works without epoch aggregates by falling back to the current epoch's boundaries and the block count.
 */
export const load: PageServerLoad = async ({ params, url, fetch }) => {
	const n = Number(params.epoch);
	if (!Number.isInteger(n) || n < 0) error(404, 'Epochs are whole numbers, starting at 0.');

	const tab = url.searchParams.get('tab') === 'params' ? 'params' : 'blocks';
	const { page, count } = paging(url, 20);

	const latest = await latestEpoch(fetch);
	if (latest && n > latest.epoch) error(404, `Epoch ${n} hasn't started yet. The current epoch is ${latest.epoch}.`);

	// The block list is streamed: early epochs on big chains can take Yaci Store a long time to list.
	// It uses the global fetch so the timeout can abort it (SvelteKit's server fetch didn't honour the abort here).
	const blockList = tryApi<Any>(`/blocks/epoch/${n}`, { params: tab === 'blocks' ? { page, count } : { page: 1, count: 1 }, timeoutMs: 8000 });

	const [agg, network, pots, protocol] = await Promise.all([
		tryApi<Any>(`/epochs/${n}`, { fetch }),
		tryApi<Any>(`/network/epochs/${n}`, { fetch }),
		tryApi<Any>(`/adapot/epochs/${n}`, { fetch }),
		tab === 'params' ? tryApi<Record<string, unknown>>(`/epochs/${n}/parameters`, { fetch }) : null
	]);

	// Without aggregates (a fresh devnet) the block count comes from the list, which is quick there.
	const listed = agg ? null : await blockList;
	if (!latest && !agg && !listed?.total) error(404, `No data for epoch ${n} on this chain.`);

	const epoch: EpochRow = agg
		? fromAggregate(agg, latest)
		: { number: n, ...(latest ? uniformBounds(latest, n) : { start: null, end: null }), exact: !!latest, blocks: listed?.total ?? null, txs: null, output: null, fees: null, activeStake: null };
	const isCurrent = latest?.epoch === n;
	if (isCurrent && latest) {
		epoch.start = latest.start;
		epoch.end = latest.end;
		epoch.exact = true;
	}

	const supply = network?.supply?.circulating != null ? network.supply : null;
	const knownStake = network?.stake?.active ?? null;

	return {
		epoch,
		isCurrent,
		latestEpoch: latest?.epoch ?? null,
		tab,
		page,
		count,
		blockList: blockList.then((b) => (b ? { blocks: (b.blocks ?? []) as Any[], totalPages: (b.total_pages as number) || null } : null)),
		supply: supply as { circulating: number; treasury: number; reserves: number; max: number } | null,
		pots: pots as Record<string, number> | null,
		protocol,
		// Active stake: from the network snapshot when it has it, otherwise a slower lookup streamed in later.
		activeStake: knownStake != null ? Promise.resolve(knownStake as number | string | null) : activeStake(fetch, n, 10000)
	};
};
