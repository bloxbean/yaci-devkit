import { error } from '@sveltejs/kit';
import { paging, tryApi } from '$lib/api/client';
import { activeStake, blockCount, fromAggregate, latestEpoch, uniformBounds, type EpochRow } from '$lib/epochs/epochs';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

/*
 * Epochs, newest first. Uses the store's epoch aggregates when it has them (pages are 1-based); a fresh devnet has
 * none, so the list is built from the current epoch's boundaries and per-epoch block counts instead.
 */
export const load: PageServerLoad = async ({ url, fetch }) => {
	const { page, count } = paging(url, 20, 50);
	const [latest, agg] = await Promise.all([latestEpoch(fetch), tryApi<Any>('/epochs', { fetch, params: { page, count } })]);

	let rows: EpochRow[] = [];
	let totalPages: number | null = null;
	const aggregated = !!agg?.epochs?.length;

	if (aggregated) {
		rows = agg.epochs.map((e: Any) => fromAggregate(e, latest));
		totalPages = agg.total_pages || null;
		// Aggregates can lag the chain: make sure the current epoch is on page 1.
		if (page === 1 && latest && !rows.some((r) => r.number === latest.epoch)) {
			rows.unshift({ number: latest.epoch, start: latest.start, end: latest.end, exact: true, blocks: null, txs: null, output: null, fees: null, activeStake: null });
		}
	} else if (latest) {
		const top = latest.epoch - (page - 1) * count;
		for (let n = top; n > top - count && n >= 0; n--) {
			rows.push({ number: n, ...uniformBounds(latest, n), exact: true, blocks: null, txs: null, output: null, fees: null, activeStake: null });
		}
		totalPages = Math.ceil((latest.epoch + 1) / count);
		const counts = await Promise.all(rows.map((r) => blockCount(fetch, r.number)));
		rows.forEach((r, i) => (r.blocks = counts[i]));
	} else {
		error(503, 'Yaci Store did not return epoch information.');
	}

	// Active stake is a separate, slow lookup (missing for a chain's first epochs), so it streams in after the table.
	const stakes = Promise.all(rows.map((r) => (r.activeStake ? r.activeStake : activeStake(fetch, r.number, 8000)))).then((v) =>
		Object.fromEntries(rows.map((r, i) => [r.number, v[i]]))
	);

	return { rows, page, count, totalPages, latest, aggregated, stakes };
};
