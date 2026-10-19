<script lang="ts">
	import { num, parseUnit, shorten, tokenAmount } from '$lib/format';
	import { tokenName } from '$lib/names/names.svelte';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import KV from '$lib/ui/KV.svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Tabs from '$lib/ui/Tabs.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import AssetChip from '$lib/ui/AssetChip.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import AssetAvatar from '$lib/assets/AssetAvatar.svelte';
	import MintHistory from '$lib/assets/MintHistory.svelte';
	import { assetsOf, lovelaceOf } from '$lib/tx/types';
	import Coins from '@lucide/svelte/icons/coins';

	let { data } = $props();

	const info = $derived(parseUnit(data.unit));
	const supplyZero = $derived(String(data.supply.value) === '0');
	const tabs = [
		{ id: 'history', label: 'Mint & burn' },
		{ id: 'transactions', label: 'Transactions' },
		{ id: 'utxos', label: 'UTxOs' },
		{ id: 'holders', label: 'Holders' }
	];
	// On Mainnet, a token in the Cardano token registry: its ticker, name and decimals.
	const registered = $derived(tokenName(data.unit));
	const decimals = $derived(registered?.decimals ?? 0);
	const supplyText = $derived(data.supply.value != null ? (decimals ? tokenAmount(data.supply.value, decimals) : num(data.supply.value)) : '—');
</script>

<svelte:head><title>{info.name} · Asset · Yaci Viewer</title></svelte:head>

<PageHeader kind="Asset" title={info.name}>
	{#snippet icon()}<Coins class="h-5 w-5" />{/snippet}
	<div class="flex items-start gap-1.5">
		<span class="mono break-all text-xs text-faint">{data.unit}</span>
		<CopyButton value={data.unit} label="Asset unit copied" />
	</div>
	{#if registered}
		<div class="mt-1 text-xs text-muted" title="From the Cardano token registry (CIP-26)" data-name="token">
			{[registered.ticker, registered.name].filter(Boolean).join(' · ')}{#if registered.decimals}{' · '}{registered.decimals} decimals{/if}
		</div>
	{/if}
	{#snippet actions()}
		{#if registered?.ticker}<Badge tone="cyan">{registered.ticker}</Badge>{/if}
		{#if info.label}<Badge tone="violet">CIP-68 {info.label}</Badge>{/if}
		{#if supplyZero}<Badge tone="warn">Fully burned</Badge>{/if}
	{/snippet}
</PageHeader>

<section class="mb-6 grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
	<div class="y-panel relative flex items-center gap-5 overflow-hidden p-5">
		<div class="pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-brand/10 blur-2xl"></div>
		<AssetAvatar policy={info.policy} name={info.name} size={64} />
		<div class="min-w-0">
			<div class="y-label">Supply</div>
			<div
				class="mt-1 break-all font-display font-semibold tabular-nums tracking-tight {supplyText.length > 22 ? 'text-xl' : supplyText.length > 14 ? 'text-2xl' : 'text-3xl'}"
				title={String(data.supply.value ?? '')}
			>
				{supplyText}
			</div>
			<div class="mt-1 text-xs text-muted">{data.supply.value == null ? 'The store did not report a supply.' : decimals ? `in circulation (${registered?.ticker ?? 'token'}, ${decimals} decimals)` : 'units in circulation, before decimals'}</div>
		</div>
	</div>
	<div class="y-panel p-5">
		<dl class="grid gap-x-6 gap-y-5 sm:grid-cols-2">
			<KV label="Policy id"><Hash value={info.policy} href="/assets/policy/{info.policy}" head={12} tail={8} /></KV>
			<KV label="Fingerprint">{#if data.fingerprint}<Hash value={data.fingerprint} head={12} tail={8} />{:else}<span class="text-faint">—</span>{/if}</KV>
			<KV label="Asset name">
				{#if info.nameHex}
					<span class="font-medium">{info.name}</span>
					<span class="mono ml-1.5 text-xs text-faint" title={info.nameHex}>0x{shorten(info.nameHex, 12, 6)}</span>
				{:else}<span class="text-muted">Empty name</span>{/if}
			</KV>
			<KV label="Policy script"><a class="y-link" href="/scripts/{info.policy}">View the minting policy</a></KV>
		</dl>
	</div>
</section>

<section class="y-panel overflow-hidden">
	<Tabs {tabs} active={data.tab} link />
	{#if data.listError}
		<ErrorBox
			title="Couldn't load this list"
			message="{data.listError}. On a large chain this query can be too slow for the store; mint and burn history still works."
		/>
	{:else if data.tab === 'history'}
		<MintHistory rows={data.rows} />
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{:else if data.tab === 'transactions'}
		<DataTable
			rows={data.rows}
			columns={[{ key: 'tx', label: 'Transaction' }, { key: 'block', label: 'Block' }, { key: 'age', label: 'Age', align: 'right' }]}
			key={(r) => r.tx_hash}
			rowHref={(r) => `/transactions/${r.tx_hash}`}
			emptyTitle="No transactions"
		>
			{#snippet cell(r, key)}
				{#if key === 'tx'}<a class="mono font-medium text-ink hover:text-brand" href="/transactions/{r.tx_hash}">{shorten(r.tx_hash, 14, 10)}</a>
				{:else if key === 'block'}{@const b = r.block_number ?? r.block_height}{#if b != null}<a class="y-link mono" href="/blocks/{b}">#{num(b)}</a>{:else}—{/if}
				{:else}<TimeAgo ts={r.block_time} class="text-muted" />{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{:else if data.tab === 'utxos'}
		<DataTable
			rows={data.rows}
			columns={[{ key: 'ref', label: 'Output' }, { key: 'address', label: 'Address' }, { key: 'ada', label: 'ADA', align: 'right', minor: true }, { key: 'qty', label: 'This asset', align: 'right' }]}
			key={(r) => r.tx_hash + '#' + r.output_index}
			emptyTitle="No unspent outputs hold this asset"
		>
			{#snippet cell(r, key)}
				{#if key === 'ref'}<a class="mono text-[13px] hover:text-brand" href="/transactions/{r.tx_hash}">{shorten(r.tx_hash, 10, 6)}<span class="text-faint">#{r.output_index}</span></a>
				{:else if key === 'address'}<Hash value={r.address} href="/addresses/{r.address}" head={12} tail={6} />
				{:else if key === 'ada'}<Ada value={lovelaceOf(r.amount)} class="text-muted" />
				{:else}
					{@const mine = assetsOf(r.amount).find((a) => a.unit === data.unit)}
					{#if mine}<AssetChip unit={mine.unit} quantity={mine.quantity} />{:else}—{/if}
				{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{:else}
		<DataTable
			rows={data.rows}
			columns={[{ key: 'address', label: 'Address' }, { key: 'qty', label: 'Quantity', align: 'right' }]}
			key={(r) => r.address}
			rowHref={(r) => `/addresses/${r.address}`}
			emptyTitle="No holders listed"
			emptyText="This store doesn't report holders for this asset."
		>
			{#snippet cell(r, key)}
				{#if key === 'address'}<Hash value={r.address} href="/addresses/{r.address}" head={16} tail={8} />
				{:else}<span class="mono font-semibold tabular-nums">{num(r.quantity)}</span>{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{/if}
</section>
