import { redirect } from '@sveltejs/kit';
import { paging, tryApi, type ApiOptions } from '$lib/api/client';
import type { Slow } from '$lib/api/slow';
import { stakeAddressOf } from '$lib/bech32';
import { BlockfrostAbsent, bfApi, blockfrostPresent } from '$lib/server/blockfrost';
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
 * from the address's UTxOs: summed when one page holds them all, otherwise the exact balance from the store's
 * Blockfrost-compatible API, and only when that fails the sum of a few pages (a lower bound); never /amounts while the
 * guard is on. Where the store has the Blockfrost-compatible API, the UTxO pages and the transactions come from it too:
 * the store stops its queries after 15 s, so a busy address leaves nothing running. Yaci Store pages are 1-based.
 */
export const load: PageServerLoad = async ({ params, url, fetch }) => {
	const address = params.address.trim();
	if (address.startsWith('stake')) redirect(302, `/stake/${address}`);

	const guarded = guardEnabled();
	const txKey = `/addresses/${address}/transactions`;
	const utxosPath = `/addresses/${address}/utxos`;
	// The page opens on the UTxOs: quick even for busy addresses, where the transaction list can take minutes.
	const asked = url.searchParams.get('tab') ?? '';
	const tab = (TABS as readonly string[]).includes(asked) ? (asked as (typeof TABS)[number]) : 'utxos';
	const { page, count } = paging(url, 20);
	const order = url.searchParams.get('order') === 'asc' ? 'asc' : 'desc';
	const utxoListKey = `${utxosPath} page ${page} of ${count} ${order}`;

	// "Try again" on a list or balance the store was too slow for. Offered only where the calls go to the
	// Blockfrost-compatible API, whose queries the store stops itself after 15 s; the native ones keep running.
	const canRetry = guarded && blockfrostPresent();
	if (canRetry && url.searchParams.has('retry')) {
		const balancePages = Array.from({ length: GUARD.balancePages }, (_, i) => `${utxosPath} balance page ${i + 1}`);
		for (const k of [txKey, utxoListKey, `/blockfrost/addresses/${address}`, ...balancePages]) indexerGuard.forget(k);
	}
	// The transaction list timed out for this address recently: its tab says so instead of asking again.
	const heavy = guarded && indexerGuard.isHeavy(txKey);
	const stake = stakeAddressOf(address);
	const timeouts = GUARD.timeoutMs;

	/** A guarded call to the Blockfrost-compatible endpoint, or to the native one where the store has none. */
	const either = <T>(key: string, bfPath: string, nativePath: string, opts: ApiOptions & { cacheMs?: number; heavyMs?: number }) =>
		bfApi<T>(key, bfPath, { ...opts, timeoutMs: timeouts.blockfrost }).catch((e) => (e instanceof BlockfrostAbsent ? guardedApi<T>(key, nativePath, opts) : Promise.reject(e)));

	const bfKey = `/blockfrost/addresses/${address}`;
	/** The exact balance from the Blockfrost-compatible API; null where the store has none or it failed. */
	const bfBalance = () =>
		bfApi<{ amount?: Amount[] } | null>(bfKey, `/addresses/${address}`, { fetch, timeoutMs: timeouts.blockfrost, cacheMs: GUARD.balanceCacheMs })
			.then((r) => (Array.isArray(r?.amount) ? r.amount : null))
			.catch(() => null);

	/**
	 * The balance now from /balance, or `later` from the UTxOs (see above). Each page goes through the guard and is kept
	 * for a minute: a deep page can be the slow one, and then later visits stop before it.
	 */
	const balance = async (): Promise<{ now: Balance } | { later: Promise<Balance> }> => {
		const b = await tryApi<Any>(`/addresses/${address}/balance`, { fetch, timeoutMs: timeouts.balance });
		if (b?.amounts) return { now: { amounts: b.amounts, exact: true } };
		if (!guarded) {
			const amounts = (await bfBalance()) ?? (await tryApi<Amount[]>(`/addresses/${address}/amounts`, { fetch }));
			return { now: { amounts, exact: amounts !== null } };
		}
		const pageParams = (p: number) => ({ page: p, count: GUARD.pageSize, order: 'desc' });
		const utxoPage = (p: number) =>
			either<Utxo[]>(`${utxosPath} balance page ${p}`, `/addresses/${address}/utxos`, utxosPath, {
				fetch,
				params: pageParams(p),
				timeoutMs: timeouts.utxos,
				cacheMs: GUARD.balanceCacheMs,
				heavyMs: GUARD.utxoHeavyMs
			}).then((r) => r ?? []);
		const summed = async (): Promise<Balance> => {
			let first: Utxo[] | null = null;
			try {
				first = await utxoPage(1);
			} catch {
				// The first page failed: the Blockfrost-compatible balance is the only other source.
			}
			// One page holds them all: their sum is the exact balance.
			if (first && first.length < GUARD.pageSize) return utxoBalance(utxoPage, { pageSize: GUARD.pageSize, maxPages: 1, first });
			// More than a page: the exact balance, where the store can give it; else the sum of a few pages (a lower bound
			// past GUARD.balancePages, or after a page fails). One after the other, so a busy address gets one slow query.
			const exact = await bfBalance();
			if (exact) return { amounts: exact, exact: true };
			if (!first) return { amounts: null, exact: false };
			// A later page failing still leaves a lower bound over the pages read.
			return utxoBalance(utxoPage, { pageSize: GUARD.pageSize, maxPages: GUARD.balancePages, first });
		};
		return { later: summed() };
	};

	// The open tab's list, streamed: even UTxOs can be slow when the store is busy, mostly on deep pages, so a UTxO page
	// that timed out is skipped for a couple of minutes (that page only).
	const utxoList =
		tab === 'utxos'
			? settle(
					either<Row[]>(utxoListKey, `/addresses/${address}/utxos`, utxosPath, {
						fetch,
						params: { page, count, order },
						timeoutMs: timeouts.utxos,
						heavyMs: GUARD.utxoHeavyMs
					}).then((r) => r ?? [])
				)
			: null;
	// The Blockfrost-compatible list when the store has it (the store stops it after 15 s), else the native one. Both
	// share txKey, so a timeout on either skips the tab for a while.
	const txs = () => either<Row[]>(txKey, `/addresses/${address}/transactions`, txKey, { fetch, params: { page, count, order }, timeoutMs: timeouts.transactions });
	const txList: Slow<Row[]> | Promise<Slow<Row[]>> | null =
		tab !== 'txs' ? null : heavy ? { ok: false, reason: 'heavy', message: 'Skipped: this address timed out recently' } : settle(txs().then((r) => r ?? []));

	// The stake account can take ~30 s for a busy one: always streamed, and kept for a minute.
	const account = stake ? settle(guardedApi<Account | null>(`/accounts/${stake}`, `/accounts/${stake}`, { fetch, timeoutMs: timeouts.account, cacheMs: GUARD.accountCacheMs })) : null;

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
		canRetry,
		list: utxoList ?? txList ?? ({ ok: true, value: [] } as Slow<Row[]>)
	};
};
