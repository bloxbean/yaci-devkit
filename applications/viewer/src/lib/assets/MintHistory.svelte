<script lang="ts">
	import { num, parseUnit, shorten } from '$lib/format';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import Sparkles from '@lucide/svelte/icons/sparkles';
	import Flame from '@lucide/svelte/icons/flame';
	import type { MintEvent } from './types';

	/* Mint and burn events. `showAsset` adds an asset column, for policy pages that span several assets. */
	let { rows, showAsset = false }: { rows: MintEvent[]; showAsset?: boolean } = $props();

	const columns = $derived([
		{ key: 'tx', label: 'Transaction' },
		{ key: 'type', label: 'Event' },
		...(showAsset ? [{ key: 'asset', label: 'Asset' }] : []),
		{ key: 'qty', label: 'Quantity', align: 'right' as const },
		{ key: 'block', label: 'Block', minor: true },
		{ key: 'age', label: 'Age', align: 'right' as const }
	]);
	const burn = (r: MintEvent) => r.mint_type === 'BURN' || Number(r.quantity) < 0;
</script>

<DataTable
	{rows}
	{columns}
	key={(r) => `${r.tx_hash}-${r.unit}-${r.quantity}`}
	rowHref={(r) => `/transactions/${r.tx_hash}`}
	emptyTitle="No mint or burn events"
	emptyText="Nothing has minted or burned this asset in the indexed history."
>
	{#snippet cell(r, key)}
		{#if key === 'tx'}
			<a class="mono font-medium text-ink hover:text-brand" href="/transactions/{r.tx_hash}">{shorten(r.tx_hash, 12, 8)}</a>
		{:else if key === 'type'}
			{#if burn(r)}<Badge tone="warn"><Flame class="h-3 w-3" />Burn</Badge>{:else}<Badge tone="brand"><Sparkles class="h-3 w-3" />Mint</Badge>{/if}
		{:else if key === 'asset'}
			{@const p = parseUnit(r.unit)}
			<a class="font-medium hover:text-brand" href="/assets/unit/{r.unit}" title={r.unit}>{p.name}</a>
		{:else if key === 'qty'}
			{@const q = String(r.quantity)}
			<span class="mono font-semibold tabular-nums {burn(r) ? 'text-danger' : 'text-mint'}">{burn(r) ? '−' : '+'}{num(q.replace('-', ''))}</span>
		{:else if key === 'block'}
			<a class="y-link mono" href="/blocks/{r.block_number}">#{num(r.block_number)}</a>
		{:else if key === 'age'}
			<TimeAgo ts={r.block_time} class="text-muted" />
		{/if}
	{/snippet}
</DataTable>
