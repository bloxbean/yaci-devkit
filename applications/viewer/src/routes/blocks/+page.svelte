<script lang="ts">
	import { chain, toBlock, type LiveBlock } from '$lib/live/chain.svelte';
	import { num, shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import Blocks from '@lucide/svelte/icons/blocks';
	import Box from '@lucide/svelte/icons/box';

	let { data } = $props();

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const fromServer = $derived(data.blocks.map((b: any) => ({ ...toBlock(b, 0), output: b.output })));

	// On the first page, new blocks from the live layer slide in at the top.
	const rows = $derived.by(() => {
		if (data.page !== 1 || !chain.blocks.length) return fromServer;
		const top = fromServer[0]?.number ?? 0;
		const fresh = chain.blocks.filter((b) => b.number > top);
		return [...fresh, ...fromServer].slice(0, data.count);
	});

	const columns = [
		{ key: 'number', label: 'Block' },
		{ key: 'age', label: 'Age' },
		{ key: 'epoch', label: 'Epoch / slot', minor: true },
		{ key: 'txs', label: 'Transactions', align: 'right' as const },
		{ key: 'size', label: 'Size' },
		{ key: 'fees', label: 'Fees', align: 'right' as const, minor: true },
		{ key: 'leader', label: 'Producer', minor: true }
	];
	const isNew = (b: LiveBlock) => b.receivedAt > 0 && chain.now - b.receivedAt < 2000;
</script>

<svelte:head><title>Blocks · Yaci Viewer</title></svelte:head>

<PageHeader kind="Explore" title="Blocks">
	{#snippet icon()}<Blocks class="h-5 w-5" />{/snippet}
	{#if chain.tip}Tip <a class="y-link mono" href="/blocks/{chain.tip.number}">#{num(chain.tip.number)}</a> · epoch {chain.tip.epoch}{#if data.page === 1 && chain.status === 'live'} · new blocks appear live{/if}{/if}
</PageHeader>

<section class="y-panel overflow-hidden">
	<DataTable rows={rows} {columns} key={(b) => b.number} rowHref={(b) => `/blocks/${b.number}`} fresh={isNew} emptyTitle="No blocks yet">
		{#snippet cell(b, key)}
			{#if key === 'number'}
				<a href="/blocks/{b.number}" class="group flex items-center gap-2.5">
					<span class="grid h-8 w-8 place-items-center rounded-lg border border-line bg-surface-2 text-faint group-hover:border-brand/40 group-hover:text-brand"><Box class="h-4 w-4" /></span>
					<span class="mono font-semibold text-ink group-hover:text-brand">#{num(b.number)}</span>
				</a>
			{:else if key === 'age'}
				<TimeAgo ts={b.time} class="text-muted" />
			{:else if key === 'epoch'}
				<span class="tabular-nums">{b.epoch}</span><span class="text-faint"> / </span><span class="mono text-muted">{num(b.slot)}</span>
			{:else if key === 'txs'}
				<span class="inline-flex min-w-8 justify-center rounded-full px-2 py-0.5 text-xs font-semibold tabular-nums {b.txCount ? 'bg-brand/10 text-brand' : 'bg-surface-2 text-faint'}">{b.txCount}</span>
			{:else if key === 'size'}
				<div class="flex w-36 items-center gap-2">
					<div class="h-1.5 flex-1 overflow-hidden rounded-full bg-line">
						<div class="h-full rounded-full bg-grad" style="width:{Math.max(2, (b.size / data.maxBlockSize) * 100)}%"></div>
					</div>
					<span class="w-14 text-right text-xs tabular-nums text-muted">{b.size >= 1024 ? `${(b.size / 1024).toFixed(1)} KB` : `${b.size} B`}</span>
				</div>
			{:else if key === 'fees'}
				{#if b.fees}<Ada value={b.fees} decimals={4} class="text-muted" />{:else}<span class="text-faint">—</span>{/if}
			{:else if key === 'leader'}
				{#if b.leader}<span class="inline-flex items-center gap-1"><span class="mono text-xs text-muted" title={b.leader}>{shorten(b.leader, 8, 6)}</span><CopyButton value={b.leader} size="xs" /></span>{/if}
			{/if}
		{/snippet}
	</DataTable>
	<Pager page={data.page} hasNext={data.blocks.length === data.count} totalPages={data.totalPages} />
</section>
