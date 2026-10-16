<script lang="ts">
	import { chain } from '$lib/live/chain.svelte';
	import { adaCompact, compact, dateTime, duration, num } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import Ring from '$lib/ui/Ring.svelte';
	import Stat from '$lib/ui/Stat.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import type { EpochRow } from '$lib/epochs/epochs';
	import Timer from '@lucide/svelte/icons/timer';
	import Zap from '@lucide/svelte/icons/zap';
	import Blocks from '@lucide/svelte/icons/blocks';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';

	let { data } = $props();

	const current = $derived(chain.epoch ?? data.latest);
	const nowSec = $derived(chain.now / 1000);
	const progressOf = (r: EpochRow) => {
		if (!current || r.number !== current.epoch || !r.start || !r.end) return null;
		return Math.min(1, Math.max(0, (nowSec - r.start) / (r.end - r.start)));
	};
	const currentProgress = $derived(current ? Math.min(1, Math.max(0, (nowSec - current.start) / (current.end - current.start))) : 0);

	// Trends over this page, oldest first. Skip the running epoch so a partial count doesn't look like a drop.
	const closed = $derived([...data.rows].reverse().filter((r) => r.number !== current?.epoch));
	const txTrend = $derived(closed.map((r) => r.txs).filter((v): v is number => v != null));
	const blockTrend = $derived(closed.map((r) => r.blocks).filter((v): v is number => v != null));
	const maxTxs = $derived(Math.max(1, ...data.rows.map((r) => r.txs ?? 0)));
	const maxBlocks = $derived(Math.max(1, ...data.rows.map((r) => r.blocks ?? 0)));

	const columns = $derived([
		{ key: 'epoch', label: 'Epoch' },
		{ key: 'time', label: 'Started', minor: true },
		{ key: 'blocks', label: 'Blocks' },
		...(data.aggregated
			? [
					{ key: 'txs', label: 'Transactions' },
					{ key: 'output', label: 'Output', align: 'right' as const, minor: true },
					{ key: 'fees', label: 'Fees', align: 'right' as const, minor: true }
				]
			: []),
		{ key: 'stake', label: 'Active stake', align: 'right' as const, minor: true }
	]);
</script>

<svelte:head><title>Epochs · Yaci Viewer</title></svelte:head>

<PageHeader kind="Explore" title="Epochs">
	{#snippet icon()}<Timer class="h-5 w-5" />{/snippet}
	{#if current}Epoch {num(current.epoch)} is running · {duration(current.end - current.start)} per epoch{/if}
</PageHeader>

{#if current && data.page === 1}
	<section class="mb-6 grid gap-4 lg:grid-cols-[minmax(0,1.3fr)_minmax(0,1fr)_minmax(0,1fr)]">
		<a href="/epochs/{current.epoch}" class="y-panel group flex items-center gap-5 p-5 transition hover:border-brand/40">
			<Ring value={currentProgress} size={104} stroke={10}>
				<span class="font-display text-lg font-semibold tabular-nums">{(currentProgress * 100).toFixed(1)}<span class="text-xs text-muted">%</span></span>
			</Ring>
			<div class="min-w-0">
				<div class="y-label">Current epoch</div>
				<div class="font-display text-3xl font-semibold tabular-nums group-hover:text-brand">{num(current.epoch)}</div>
				<div class="mt-1 text-sm text-muted">Ends in {duration(current.end - nowSec)}</div>
				<div class="truncate text-xs text-faint">{dateTime(current.end)}</div>
			</div>
			<ArrowRight class="ml-auto h-4 w-4 shrink-0 text-faint transition group-hover:translate-x-0.5 group-hover:text-brand" />
		</a>
		<Stat label="Blocks per epoch" value={blockTrend.length ? compact(blockTrend[blockTrend.length - 1]) : '—'} sub="last finished epoch on this page" trend={blockTrend} color="rgb(var(--c-violet))">
			{#snippet icon()}<Blocks class="h-4 w-4" />{/snippet}
		</Stat>
		<Stat
			label="Transactions per epoch"
			value={txTrend.length ? compact(txTrend[txTrend.length - 1]) : '—'}
			sub={data.aggregated ? 'last finished epoch on this page' : 'needs epoch aggregation in Yaci Store'}
			trend={txTrend}
		>
			{#snippet icon()}<Zap class="h-4 w-4" />{/snippet}
		</Stat>
	</section>
{/if}

<section class="y-panel overflow-hidden">
	<DataTable rows={data.rows} {columns} key={(r) => r.number} rowHref={(r) => `/epochs/${r.number}`} emptyTitle="No epochs yet">
		{#snippet cell(r, key)}
			{#if key === 'epoch'}
				{@const p = progressOf(r)}
				<a href="/epochs/{r.number}" class="group flex items-center gap-3">
					<span class="grid h-8 w-8 place-items-center rounded-lg border bg-surface-2 {p != null ? 'border-brand/40 text-brand' : 'border-line text-faint group-hover:text-brand'}"><Timer class="h-4 w-4" /></span>
					<span>
						<span class="flex items-center gap-2">
							<span class="font-semibold tabular-nums text-ink group-hover:text-brand">{num(r.number)}</span>
							{#if p != null}<Badge tone="brand" class="py-0 text-[10px]">current</Badge>{/if}
						</span>
						{#if p != null}
							<span class="mt-1 block h-1 w-28 overflow-hidden rounded-full bg-line"><span class="block h-full rounded-full bg-grad transition-[width] duration-1000" style="width:{p * 100}%"></span></span>
						{/if}
					</span>
				</a>
			{:else if key === 'time'}
				{#if r.start}<span class="text-muted" title={r.exact ? 'Epoch start' : 'First block'}>{dateTime(r.start)}</span>{:else}<span class="text-faint">—</span>{/if}
			{:else if key === 'blocks'}
				{#if r.blocks != null}
					<div class="flex items-center gap-2">
						<span class="w-14 tabular-nums">{num(r.blocks)}</span>
						<span class="hidden h-1.5 w-20 overflow-hidden rounded-full bg-line sm:block"><span class="block h-full rounded-full bg-violet/70" style="width:{(r.blocks / maxBlocks) * 100}%"></span></span>
					</div>
				{:else}<span class="text-faint">—</span>{/if}
			{:else if key === 'txs'}
				{#if r.txs != null}
					<div class="flex items-center gap-2">
						<span class="w-16 tabular-nums">{num(r.txs)}</span>
						<span class="hidden h-1.5 w-20 overflow-hidden rounded-full bg-line sm:block"><span class="block h-full rounded-full bg-grad" style="width:{(r.txs / maxTxs) * 100}%"></span></span>
					</div>
				{:else}<span class="text-faint">—</span>{/if}
			{:else if key === 'output'}
				{#if r.output != null}<span class="tabular-nums">₳{adaCompact(r.output)}</span>{:else}—{/if}
			{:else if key === 'fees'}
				{#if r.fees != null}<span class="tabular-nums text-muted">₳{adaCompact(r.fees)}</span>{:else}—{/if}
			{:else if key === 'stake'}
				{#await data.stakes}
					<span class="y-skel inline-block h-3.5 w-14 align-middle"></span>
				{:then stakes}
					{#if stakes?.[r.number]}<span class="tabular-nums">₳{adaCompact(stakes[r.number])}</span>{:else}<span class="text-faint">—</span>{/if}
				{/await}
			{/if}
		{/snippet}
	</DataTable>
	<Pager page={data.page} hasNext={data.totalPages ? data.page < data.totalPages : data.rows.length === data.count} totalPages={data.totalPages} />
</section>
