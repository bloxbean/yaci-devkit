<script lang="ts">
	import { shorten } from '$lib/format';
	import Ada from '$lib/ui/Ada.svelte';
	import AssetChip from '$lib/ui/AssetChip.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import Braces from '@lucide/svelte/icons/braces';
	import FileCode from '@lucide/svelte/icons/file-code';
	import CornerDownRight from '@lucide/svelte/icons/corner-down-right';
	import { assetsOf, lovelaceOf, type Utxo } from './types';
	import { hover } from './hover.svelte';

	/* One input or output: address, value, assets, and markers for datums and reference scripts. */
	let {
		utxo,
		side,
		tag = undefined,
		ondatum = undefined
	}: { utxo: Utxo; side: 'in' | 'out'; tag?: string; ondatum?: (u: Utxo) => void } = $props();

	const assets = $derived(assetsOf(utxo.amount));
	const lit = $derived(hover.address === utxo.address);
	const isScript = $derived(/^(addr1|addr_test1)[zx2w]/.test(utxo.address));
	let showAll = $state(false);
</script>

<div
	class="group relative rounded-xl border bg-surface p-3.5 transition-all {lit ? 'border-brand/60 shadow-glow' : 'border-line hover:border-line'}"
	onmouseenter={() => (hover.address = utxo.address)}
	onmouseleave={() => (hover.address = null)}
	role="group"
>
	<div class="flex items-start justify-between gap-2">
		<div class="min-w-0">
			<div class="flex items-center gap-1.5">
				<a href="/addresses/{utxo.address}" class="mono truncate text-[13px] font-medium text-ink hover:text-brand" title={utxo.address}
					>{shorten(utxo.address, 14, 8)}</a
				>
				<CopyButton value={utxo.address} size="xs" label="Address copied" />
			</div>
			<div class="mt-1 flex flex-wrap items-center gap-1.5 text-[11px]">
				{#if isScript}<span class="y-pill border-violet/25 bg-violet/10 py-0 text-violet">script</span>{/if}
				{#if tag}<span class="y-pill border-line bg-surface-2 py-0 text-muted">{tag}</span>{/if}
				{#if utxo.stake_address}
					<a href="/stake/{utxo.stake_address}" class="truncate text-faint hover:text-brand" title={utxo.stake_address}
						>{shorten(utxo.stake_address, 9, 5)}</a
					>
				{/if}
			</div>
		</div>
		<div class="shrink-0 text-right">
			<Ada value={lovelaceOf(utxo.amount)} decimals={6} class="text-[13px] font-semibold" />
			{#if side === 'in'}
				<a href="/transactions/{utxo.tx_hash}" class="mt-0.5 flex items-center justify-end gap-1 text-[11px] text-faint hover:text-brand" title="Spent output {utxo.tx_hash}#{utxo.output_index}">
					<CornerDownRight class="h-3 w-3" />{shorten(utxo.tx_hash, 6, 4)}#{utxo.output_index}
				</a>
			{:else}
				<div class="mt-0.5 text-[11px] text-faint">#{utxo.output_index}</div>
			{/if}
		</div>
	</div>

	{#if assets.length}
		<div class="mt-2.5 flex flex-wrap gap-1.5">
			{#each showAll ? assets : assets.slice(0, 4) as a (a.unit)}
				<AssetChip unit={a.unit} quantity={a.quantity} />
			{/each}
			{#if assets.length > 4}
				<button class="rounded-lg px-1.5 text-xs text-brand hover:underline" onclick={() => (showAll = !showAll)}
					>{showAll ? 'Show less' : `+${assets.length - 4} more`}</button
				>
			{/if}
		</div>
	{/if}

	{#if utxo.inline_datum || utxo.data_hash || utxo.reference_script_hash}
		<div class="mt-2.5 flex flex-wrap gap-1.5 border-t border-line/70 pt-2.5 text-[11px]">
			{#if utxo.inline_datum}
				<button class="y-pill border-cyan/25 bg-cyan/10 text-cyan hover:bg-cyan/20" onclick={() => ondatum?.(utxo)}><Braces class="h-3 w-3" />Inline datum</button>
			{:else if utxo.data_hash}
				<a class="y-pill border-cyan/25 bg-cyan/10 text-cyan hover:bg-cyan/20" href="/datums/{utxo.data_hash}"><Braces class="h-3 w-3" />Datum {shorten(utxo.data_hash, 6, 4)}</a>
			{/if}
			{#if utxo.reference_script_hash}
				<a class="y-pill border-violet/25 bg-violet/10 text-violet hover:bg-violet/20" href="/scripts/{utxo.reference_script_hash}"
					><FileCode class="h-3 w-3" />Reference script</a
				>
			{/if}
		</div>
	{/if}
</div>
