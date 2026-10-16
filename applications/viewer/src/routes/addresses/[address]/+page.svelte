<script lang="ts">
	import { page as pageState } from '$app/state';
	import { goto } from '$app/navigation';
	import { addressInfo } from '$lib/address';
	import { num, shorten, parseUnit } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
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
	const lovelace = $derived(lovelaceOf(data.amounts));
	const tokens = $derived(assetsOf(data.amounts).sort((a, b) => parseUnit(a.unit).name.localeCompare(parseUnit(b.unit).name)));

	function toggleOrder() {
		const u = new URL(pageState.url);
		u.searchParams.set('order', data.order === 'desc' ? 'asc' : 'desc');
		u.searchParams.delete('page');
		goto(u.pathname + u.search, { noScroll: true });
	}

	let datumOf = $state<Utxo | null>(null);
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
		{#if info.script}<Badge tone="violet"><FileCode class="h-3.5 w-3.5" />Smart contract</Badge>{/if}
		{#if info.network}<Badge tone="neutral">{info.network}</Badge>{/if}
	{/snippet}
</PageHeader>

<section class="mb-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
	<div class="y-panel relative overflow-hidden p-5 xl:col-span-2">
		<div class="pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-brand/10 blur-2xl"></div>
		<div class="y-label">Balance</div>
		<div class="mt-1 font-display text-4xl font-semibold tracking-tight">
			{#if data.balanceKnown}<Ada value={lovelace} decimals={6} />{:else}<span class="text-muted">—</span>{/if}
		</div>
		<div class="mt-2 text-sm text-muted">
			{tokens.length ? `${num(tokens.length)} native token${tokens.length === 1 ? '' : 's'}` : 'No native tokens'}
		</div>
	</div>
	<div class="y-panel p-5">
		<div class="y-label flex items-center gap-1.5"><KeyRound class="h-3.5 w-3.5" />Stake account</div>
		{#if data.stake}
			<a href="/stake/{data.stake}" class="mono mt-2 block truncate text-sm text-brand hover:underline" title={data.stake}>{shorten(data.stake, 12, 8)}</a>
			{#if data.account?.controlled_amount != null}
				<div class="mt-1 text-xs text-muted">controls <Ada value={data.account.controlled_amount} /></div>
			{/if}
		{:else}
			<div class="mt-2 text-sm text-muted">{info.era === 'byron' ? 'Byron addresses cannot stake.' : 'This address has no stake part.'}</div>
		{/if}
	</div>
	<div class="y-panel p-5">
		<div class="y-label flex items-center gap-1.5"><Server class="h-3.5 w-3.5" />Delegation</div>
		{#if data.account?.pool_id}
			<a href="/pools/{data.account.pool_id}" class="mono mt-2 block truncate text-sm text-brand hover:underline">{shorten(data.account.pool_id, 12, 8)}</a>
			<div class="mt-1 text-xs text-muted">delegated stake pool</div>
		{:else}
			<div class="mt-2 text-sm text-muted">{data.stake ? 'Not delegated' : '—'}</div>
		{/if}
	</div>
</section>

<section class="y-panel overflow-hidden">
	<div class="flex items-center justify-between gap-2 pr-3">
		<div class="min-w-0 flex-1">
			<Tabs tabs={[{ id: 'txs', label: 'Transactions' }, { id: 'utxos', label: 'UTxOs' }, { id: 'tokens', label: 'Tokens', count: tokens.length }]} active={data.tab} link />
		</div>
		{#if data.tab !== 'tokens'}
			<button class="y-btn h-8 shrink-0 text-xs" onclick={toggleOrder} title="Change order"><ArrowUpDown class="h-3.5 w-3.5" />{data.order === 'desc' ? 'Newest first' : 'Oldest first'}</button>
		{:else}
			<input class="y-input h-8 w-48 shrink-0 text-xs" placeholder="Filter tokens…" bind:value={tokenFilter} />
		{/if}
	</div>

	{#if data.listError}
		<ErrorBox title="Couldn't load this list" message={data.listError} />
	{:else if data.tab === 'txs'}
		<DataTable
			rows={data.rows}
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
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{:else if data.tab === 'utxos'}
		<DataTable
			rows={data.rows}
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
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{:else}
		{#if !tokens.length}
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
