import { redirect } from '@sveltejs/kit';
import { paging, tryApi } from '$lib/api/client';
import type { Slow } from '$lib/api/slow';
import { stakeAddressOf } from '$lib/bech32';
import { GUARD, guardEnabled, guardedApi, indexerGuard, settle } from '$lib/server/heavy-guard';
import { utxoBalance, type UtxoBalance } from '$lib/tx/balance';
import type { Amount, Utxo } from '$lib/tx/types';
import type { PageServerLoad } from './$types';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
type Any = any;

const TABS = ['utxos', 'tokens', 'txs'] as const;

type Row = Utxo & { block_height?: number; block_time?: number };
type Account = { controlled_amount?: number; withdrawable_amount?: number; pool_id?: string | null };
/** null amounts: the balance couldn't be worked out. */
type Balance = Omit<UtxoBalance, 'amounts' | 'utxos'> & { amounts: Amount[] | null; utxos?: number };

/*
 * Busy addresses on a large Yaci Store can take hours for /amounts, /transactions and /accounts/{stake}, and the store
 * keeps running a query after the viewer gives up (bloxbean/yaci-store#1269). So only /balance (quick, even as a 503)
 * holds up the page; the rest stream in. The balance comes from /balance when the store aggregates balances, otherwise
 * from summing the address's UTxOs (never /amounts while the guard is on). Yaci Store pages are 1-based.
 */
export const load: PageServerLoad = async ({ params, url, fetch }) => {
	const address = params.address.trim();
	if (address.startsWith('stake')) redirect(302, `/stake/${address}`);

	const guarded = guardEnabled();
	const txKey = `/addresses/${address}/transactions`;
	// The transaction list timed out for this address recently: its tab says so instead of asking again.
	const heavy = guarded && indexerGuard.isHeavy(txKey);
	// The page opens on the UTxOs: quick even for busy addresses, where the transaction list can take minutes.
	const asked = url.searchParams.get('tab') ?? '';
	const tab = (TABS as readonly string[]).includes(asked) ? (asked as (typeof TABS)[number]) : 'utxos';
	const { page, count } = paging(url, 20);
	const order = url.searchParams.get('order') === 'asc' ? 'asc' : 'desc';
	const stake = stakeAddressOf(address);
	const timeouts = GUARD.timeoutMs;

	/**
	 * The balance now from /balance, or `later`, summed from UTxO pages. Each page goes through the guard and is kept for
	 * a minute: a deep page can be the slow one, and then later visits stop before it.
	 */
	const balance = async (): Promise<{ now: Balance } | { later: Promise<Balance> }> => {
		const b = await tryApi<Any>(`/addresses/${address}/balance`, { fetch, timeoutMs: timeouts.balance });
		if (b?.amounts) return { now: { amounts: b.amounts, exact: true } };
		if (!guarded) {
			const amounts = await tryApi<Amount[]>(`/addresses/${address}/amounts`, { fetch });
			return { now: { amounts, exact: amounts !== null } };
		}
		const pageParams = (p: number) => ({ page: p, count: GUARD.pageSize, order: 'desc' });
		const utxoPage = (p: number) =>
			guardedApi<Utxo[]>(`${utxosPath} balance page ${p}`, utxosPath, { fetch, params: pageParams(p), timeoutMs: timeouts.utxos, cacheMs: GUARD.balanceCacheMs, heavyMs: GUARD.utxoHeavyMs }).then((r) => r ?? []);
		const summed = async (): Promise<Balance> => {
			let first: Utxo[];
			try {
				first = await utxoPage(1);
			} catch {
				return { amounts: null, exact: false };
			}
			// A later page failing still leaves a lower bound over the pages read.
			return utxoBalance(utxoPage, { pageSize: GUARD.pageSize, maxPages: GUARD.balancePages, first });
		};
		return { later: summed() };
	};

	// The open tab's list, streamed: even UTxOs can be slow when the store is busy, mostly on deep pages, so a UTxO page
	// that timed out is skipped for a couple of minutes (that page only).
	const utxosPath = `/addresses/${address}/utxos`;
	const utxoList =
		tab === 'utxos'
			? settle(guardedApi<Row[]>(`${utxosPath} page ${page} of ${count} ${order}`, utxosPath, { fetch, params: { page, count, order }, timeoutMs: timeouts.utxos, heavyMs: GUARD.utxoHeavyMs }).then((r) => r ?? []))
			: null;
	const txList: Slow<Row[]> | Promise<Slow<Row[]>> | null =
		tab !== 'txs'
			? null
			: heavy
				? { ok: false, reason: 'heavy', message: 'Skipped: this address timed out recently' }
				: settle(guardedApi<Row[]>(txKey, txKey, { fetch, params: { page, count, order }, timeoutMs: timeouts.transactions }).then((r) => r ?? []));

	// The stake account can take ~30 s for a busy one: always streamed, and kept for a minute.
	const account = stake
		? settle(guardedApi<Account | null>(`/accounts/${stake}`, `/accounts/${stake}`, { fetch, timeoutMs: timeouts.account, cacheMs: GUARD.accountCacheMs }))
		: null;

	const amounts = await balance();

	return {
		address,
		stake,
		account,
		balance: 'now' in amounts ? amounts.now : amounts.later,
		tab,
		page,
		count,
		order,
		list: utxoList ?? txList ?? ({ ok: true, value: [] } as Slow<Row[]>)
	};
};
