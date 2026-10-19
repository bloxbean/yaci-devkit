<script lang="ts">
	import { page as pageState } from '$app/state';
	import { goto, replaceState } from '$app/navigation';
	import { addressInfo } from '$lib/address';
	import { num, shorten, parseUnit } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import NameTag from '$lib/names/NameTag.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Tabs from '$lib/ui/Tabs.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import AssetChip from '$lib/ui/AssetChip.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import DatumDialog from '$lib/tx/DatumDialog.svelte';
	import Skeleton from '$lib/ui/Skeleton.svelte';
	import SlowNotice from '$lib/ui/SlowNotice.svelte';
	import { resolved } from '$lib/ui/resolved.svelte';
	import { assetsOf, lovelaceOf, type Utxo } from '$lib/tx/types';
	import Wallet from '@lucide/svelte/icons/wallet';
	import FileCode from '@lucide/svelte/icons/file-code';
	import KeyRound from '@lucide/svelte/icons/key-round';
	import Server from '@lucide/svelte/icons/server';
	import Coins from '@lucide/svelte/icons/coins';
	import Braces from '@lucide/svelte/icons/braces';
	import ArrowUpDown from '@lucide/svelte/icons/arrow-up-down';

	let { data } = $props();

	const info = $derived(addressInfo(data.address));
	// Sections that may stream in after the page renders: undefined while they load.
	const balance = resolved(() => data.balance);
	const list = resolved(() => data.list);
	const accountResult = resolved(() => data.account);
	const account = $derived(accountResult.value?.ok ? accountResult.value.value : null);
	const amounts = $derived(balance.value?.amounts ?? []);
	const lovelace = $derived(lovelaceOf(amounts));
	const tokens = $derived(assetsOf(amounts).sort((a, b) => parseUnit(a.unit).name.localeCompare(parseUnit(b.unit).name)));
	const rows = $derived(list.value?.ok ? list.value.value : []);
	const partialNote = $derived(
		balance.value && !balance.value.exact && balance.value.amounts
			? `Lower bound: the sum of the newest ${num(balance.value.utxos ?? 0)} unspent outputs. This address has more than the viewer totals here.`
			: null
	);

	function toggleOrder() {
		const u = new URL(pageState.url);
		u.searchParams.set('order', data.order === 'desc' ? 'asc' : 'desc');
		u.searchParams.delete('page');
		goto(u.pathname + u.search, { noScroll: true });
	}

	let datumOf = $state<Utxo | null>(null);

	// "Try again" asks the server to stop skipping this address's slow calls (?retry). Once the page has loaded, the
	// parameter comes off the address bar, so a reload or a tab change doesn't force it again.
	const retryHref = $derived.by(() => {
		const u = new URL(pageState.url);
		u.searchParams.set('retry', '1');
		return u.pathname + u.search;
	});
	$effect(() => {
		if (!pageState.url.searchParams.has('retry')) return;
		const u = new URL(pageState.url);
		u.searchParams.delete('retry');
		replaceState(u.pathname + u.search, pageState.state);
	});
	let tokenFilter = $state('');
	const shownTokens = $derived(
		tokenFilter ? tokens.filter((t) => (parseUnit(t.unit).name + t.unit).toLowerCase().includes(tokenFilter.toLowerCase())) : tokens
	);
</script>

<svelte:head><title>Address {shorten(data.address, 10, 6)} · Yaci Viewer</title></svelte:head>

<PageHeader kind={info.label} title={shorten(data.address, 16, 10)}>
	{#snippet icon()}{#if info.script}<FileCode class="h-5 w-5" />{:else}<Wallet class="h-5 w-5" />{/if}{/snippet}
	<div class="flex items-start gap-1.5">
		<span class="mono break-all text-xs text-faint">{data.address}</span>
		<CopyButton value={data.address} label="Address copied" />
	</div>
	{#snippet actions()}
		{#if info.script}<Badge tone="violet"><FileCode class="h-3.5 w-3.5" />Smart contract</Badge><NameTag kind="script" id={data.address} class="text-xs" />{/if}
		{#if info.network}<Badge tone="neutral">{info.network}</Badge>{/if}
	{/snippet}
</PageHeader>

<section class="mb-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
	<div class="y-panel relative overflow-hidden p-5 xl:col-span-2">
		<div class="pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-brand/10 blur-2xl"></div>
		<div class="y-label">Balance</div>
		<div class="mt-1 font-display text-4xl font-semibold tracking-tight" data-balance={balance.value === undefined ? 'loading' : !balance.value.amounts ? 'unknown' : balance.value.exact ? 'exact' : 'partial'}>
			{#if balance.value === undefined}<span class="y-skel inline-block h-9 w-56 align-middle" aria-label="Loading balance"></span>
			{:else if !balance.value.amounts}<span class="text-muted">—</span>
			{:else}{#if !balance.value.exact}<span class="text-muted" title="At least">≥ </span>{/if}<Ada value={lovelace} decimals={2} />{/if}
		</div>
		{#if balance.value?.amounts}
			<div class="mono mt-1 text-xs text-faint tabular-nums" data-lovelace>{balance.value.exact ? '' : '≥ '}{num(lovelace)} lovelace</div>
		{/if}
		<div class="mt-2 text-sm text-muted">
			{#if balance.value === undefined}Adding up unspent outputs…
			{:else if !balance.value.amounts}Yaci Store didn’t return a balance for this address.
			{:else}{tokens.length ? `${!balance.value.exact ? 'At least ' : ''}${num(tokens.length)} native token${tokens.length === 1 ? '' : 's'}` : 'No native tokens'}{#if balance.value.utxos != null}{' · '}{num(balance.value.utxos)}{balance.value.exact ? '' : '+'} UTxO{balance.value.utxos === 1 ? '' : 's'}{/if}{/if}
		</div>
		{#if partialNote}<div class="mt-2 text-xs text-muted" data-partial-note>{partialNote}</div>{/if}
	</div>
	<div class="y-panel p-5">
		<div class="y-label flex items-center gap-1.5"><KeyRound class="h-3.5 w-3.5" />Stake account</div>
		{#if data.stake}
			<a href="/stake/{data.stake}" class="mono mt-2 block truncate text-sm text-brand hover:underline" title={data.stake}>{shorten(data.stake, 12, 8)}</a>
			{#if accountResult.value === undefined}
				<div class="mt-1 text-xs text-faint">Loading the stake account…</div>
			{:else if accountResult.value && !accountResult.value.ok}
				<div class="mt-1 text-xs text-faint" title={accountResult.value.message}>{accountResult.value.reason === 'error' ? 'Couldn’t load the stake account' : 'The stake account is too slow to load right now'}</div>
			{:else if account?.controlled_amount != null}
				<div class="mt-1 text-xs text-muted">controls <Ada value={account.controlled_amount} /></div>
			{/if}
		{:else}
			<div class="mt-2 text-sm text-muted">{info.era === 'byron' ? 'Byron addresses cannot stake.' : 'This address has no stake part.'}</div>
		{/if}
	</div>
	<div class="y-panel p-5">
		<div class="y-label flex items-center gap-1.5"><Server class="h-3.5 w-3.5" />Delegation</div>
		{#if account?.pool_id}
			<a href="/pools/{account.pool_id}" class="mono mt-2 block truncate text-sm text-brand hover:underline">{shorten(account.pool_id, 12, 8)}</a>
			<div class="mt-1 flex items-center gap-1.5 text-xs text-muted">delegated stake pool <NameTag kind="pool" id={account.pool_id} /></div>
		{:else if data.stake && accountResult.value === undefined}
			<div class="y-skel mt-3 h-4 w-40"></div>
		{:else}
			<div class="mt-2 text-sm text-muted">{data.stake && accountResult.value?.ok ? 'Not delegated' : '—'}</div>
		{/if}
	</div>
</section>

<section class="y-panel overflow-hidden">
	<div class="flex items-center justify-between gap-2 pr-3">
		<div class="min-w-0 flex-1">
			<Tabs tabs={[{ id: 'utxos', label: 'UTxOs' }, { id: 'tokens', label: 'Tokens', count: tokens.length }, { id: 'txs', label: 'Transactions' }]} active={data.tab} link />
		</div>
		{#if data.tab !== 'tokens'}
			<button class="y-btn h-8 shrink-0 text-xs" onclick={toggleOrder} title="Change order"><ArrowUpDown class="h-3.5 w-3.5" />{data.order === 'desc' ? 'Newest first' : 'Oldest first'}</button>
		{:else}
			<input class="y-input h-8 w-48 shrink-0 text-xs" placeholder="Filter tokens…" bind:value={tokenFilter} />
		{/if}
	</div>

	{#if data.tab !== 'tokens' && list.value === undefined}
		<Skeleton rows={8} />
	{:else if data.tab !== 'tokens' && list.value && !list.value.ok}
		{#if list.value.reason === 'error'}
			<ErrorBox title="Couldn't load this list" message={list.value.message} />
		{:else}
			<SlowNotice reason={list.value.reason} bounded={data.canRetry}>
				{#if data.canRetry}<a class="y-btn y-btn-primary" href={retryHref} data-sveltekit-noscroll>Try again</a>{/if}
				{#if data.tab === 'txs'}<a class="y-btn" href="?tab=utxos">Show UTxOs instead</a>{/if}
			</SlowNotice>
		{/if}
	{:else if data.tab === 'txs'}
		<DataTable
			{rows}
			columns={[{ key: 'hash', label: 'Transaction' }, { key: 'block', label: 'Block' }, { key: 'age', label: 'Age', align: 'right' }]}
			key={(r) => r.tx_hash}
			rowHref={(r) => `/transactions/${r.tx_hash}`}
			emptyTitle="No transactions"
			emptyText="Nothing has been sent to or from this address yet."
		>
			{#snippet cell(r, key)}
				{#if key === 'hash'}<a class="mono font-medium text-ink hover:text-brand" href="/transactions/{r.tx_hash}">{shorten(r.tx_hash, 14, 10)}</a>
				{:else if key === 'block'}<a class="y-link mono" href="/blocks/{r.block_height}">#{num(r.block_height)}</a>
				{:else if key === 'age'}<TimeAgo ts={r.block_time} class="text-muted" />{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={rows.length === data.count} />
	{:else if data.tab === 'utxos'}
		<DataTable
			{rows}
			columns={[{ key: 'ref', label: 'Output' }, { key: 'value', label: 'ADA', align: 'right' }, { key: 'assets', label: 'Tokens' }, { key: 'datum', label: 'Datum', minor: true }]}
			key={(r) => r.tx_hash + r.output_index}
			emptyTitle="No unspent outputs"
			emptyText="Everything sent here has been spent."
		>
			{#snippet cell(r, key)}
				{#if key === 'ref'}<a class="mono text-[13px] text-ink hover:text-brand" href="/transactions/{r.tx_hash}">{shorten(r.tx_hash, 10, 6)}<span class="text-faint">#{r.output_index}</span></a>
				{:else if key === 'value'}<Ada value={lovelaceOf(r.amount)} decimals={6} class="font-semibold" />
				{:else if key === 'assets'}
					{@const a = assetsOf(r.amount)}
					<div class="flex max-w-md flex-wrap gap-1">{#each a.slice(0, 3) as x (x.unit)}<AssetChip unit={x.unit} quantity={x.quantity} />{/each}{#if a.length > 3}<span class="text-xs text-faint">+{a.length - 3}</span>{/if}{#if !a.length}<span class="text-faint">—</span>{/if}</div>
				{:else if key === 'datum'}
					{#if r.inline_datum}<button class="y-pill border-cyan/25 bg-cyan/10 text-cyan" onclick={() => (datumOf = r)}><Braces class="h-3 w-3" />Inline</button>
					{:else if r.data_hash}<a class="y-pill border-cyan/25 bg-cyan/10 text-cyan" href="/datums/{r.data_hash}"><Braces class="h-3 w-3" />Hash</a>
					{:else}<span class="text-faint">—</span>{/if}
				{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={rows.length === data.count} />
	{:else}
		{#if partialNote}<div class="border-b border-line px-4 py-2.5 text-xs text-muted">Tokens in the newest {num(balance.value?.utxos ?? 0)} unspent outputs only.</div>{/if}
		{#if balance.value === undefined}
			<Skeleton rows={4} />
		{:else if !tokens.length}
			<Empty title="No native tokens" text="This address only holds ADA." />
		{:else}
			<div class="grid gap-2 p-4 sm:grid-cols-2 xl:grid-cols-3">
				{#each shownTokens as t (t.unit)}
					{@const p = parseUnit(t.unit)}
					<a href="/assets/unit/{t.unit}" class="group flex items-center gap-3 rounded-xl border border-line bg-surface p-3 transition hover:border-brand/40">
						<span class="grid h-9 w-9 shrink-0 place-items-center rounded-xl text-sm font-bold text-white" style="background:hsl({parseInt(p.policy.slice(0, 6), 16) % 360} 65% 50%)">{p.name.slice(0, 1).toUpperCase()}</span>
						<span class="min-w-0 flex-1">
							<span class="flex items-center gap-1.5"><span class="truncate font-medium group-hover:text-brand">{p.name}</span>{#if p.label}<Badge tone="neutral" class="py-0 text-[10px]">{p.label}</Badge>{/if}</span>
							<span class="mono block truncate text-[11px] text-faint">{shorten(p.policy, 10, 6)}</span>
						</span>
						<span class="mono shrink-0 text-sm font-semibold tabular-nums">{num(t.quantity)}</span>
					</a>
				{/each}
			</div>
		{/if}
	{/if}
</section>

<DatumDialog bind:utxo={datumOf} />
