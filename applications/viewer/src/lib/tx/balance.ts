import type { Amount, Utxo } from './types';

/*
 * An address's balance summed from its unspent outputs, for stores where /addresses/{a}/balance isn't available and
 * /addresses/{a}/amounts is too slow for busy addresses. Pages through /utxos (newest first) up to a limit: when a
 * page comes back short the sum is exact, otherwise it is a lower bound over the outputs read.
 */

export interface UtxoBalance {
	amounts: Amount[];
	/** Exact when every unspent output was read; otherwise a lower bound. */
	exact: boolean;
	/** Unspent outputs summed. */
	utxos: number;
}

/** Adds up amounts by unit, lovelace first, with quantities as strings (they can pass 2^53). */
export function mergeAmounts(lists: Amount[][]): Amount[] {
	const totals = new Map<string, bigint>();
	for (const list of lists) for (const a of list) totals.set(a.unit, (totals.get(a.unit) ?? 0n) + BigInt(a.quantity));
	return [...totals]
		.filter(([, q]) => q !== 0n)
		.sort(([a], [b]) => (a === 'lovelace' ? -1 : b === 'lovelace' ? 1 : 0))
		.map(([unit, q]) => ({ unit, quantity: q.toString() }));
}

/** The balance of the pages read so far: exact once a page holds fewer rows than the page size. */
export function balanceOfPages(pages: Utxo[][], pageSize: number): UtxoBalance {
	const rows = pages.flat();
	const last = pages[pages.length - 1];
	return { amounts: mergeAmounts(rows.map((u) => u.amount ?? [])), exact: !last || last.length < pageSize, utxos: rows.length };
}

/**
 * Reads pages one after another (never in parallel, to spare the store) until a short page or `maxPages`. If a page
 * after the first fails, the pages read so far give a lower bound. `first` is page 1 when the caller already has it.
 */
export async function utxoBalance(fetchPage: (page: number) => Promise<Utxo[]>, opts: { pageSize: number; maxPages: number; first?: Utxo[] }): Promise<UtxoBalance> {
	const pages: Utxo[][] = [opts.first ?? (await fetchPage(1))];
	while (pages.length < opts.maxPages && pages[pages.length - 1].length >= opts.pageSize) {
		try {
			pages.push(await fetchPage(pages.length + 1));
		} catch {
			return { ...balanceOfPages(pages, opts.pageSize), exact: false };
		}
	}
	return balanceOfPages(pages, opts.pageSize);
}
