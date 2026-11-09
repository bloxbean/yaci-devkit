<script lang="ts">
	import { chain, toTx, type LiveTx } from '$lib/live/chain.svelte';
	import { num, shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import ArrowLeftRight from '@lucide/svelte/icons/arrow-left-right';

	let { data } = $props();

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const fromServer = $derived(data.txs.map((t: any) => toTx(t, 0)));

	// On the first page, transactions from new blocks appear at the top.
	const rows = $derived.by<LiveTx[]>(() => {
		if (data.page !== 1 || !chain.txs.length) return fromServer;
		const known = new Set(fromServer.map((t) => t.hash));
		const topSlot = fromServer[0]?.slot ?? 0;
		const fresh = chain.txs.filter((t) => !known.has(t.hash) && t.slot >= topSlot);
		return [...fresh, ...fromServer].slice(0, data.count);
	});
	const blockTime = $derived(new Map(chain.blocks.map((b) => [b.number, b.time])));

	const columns = [
		{ key: 'hash', label: 'Transaction' },
		{ key: 'block', label: 'Block' },
		{ key: 'to', label: 'Sent to', minor: true },
		{ key: 'fee', label: 'Fee', align: 'right' as const, minor: true },
		{ key: 'out', label: 'Output', align: 'right' as const }
	];
	const isNew = (t: LiveTx) => t.receivedAt > 0 && chain.now - t.receivedAt < 2500;
</script>

<svelte:head><title>Transactions · Yaci Viewer</title></svelte:head>

<PageHeader kind="Explore" title="Transactions">
	{#snippet icon()}<ArrowLeftRight class="h-5 w-5" />{/snippet}
	Newest first{#if data.page === 1} · new transactions appear as blocks land{/if}
</PageHeader>

<section class="y-panel overflow-hidden">
	<DataTable {rows} {columns} key={(t) => t.hash} rowHref={(t) => `/transactions/${t.hash}`} fresh={isNew} emptyTitle="No transactions yet" emptyText="Send ADA to an address on your devnet and it shows up here.">
		{#snippet cell(t, key)}
			{#if key === 'hash'}
				<a class="group flex items-center gap-2.5" href="/transactions/{t.hash}">
					<span class="grid h-8 w-8 place-items-center rounded-lg border border-line bg-surface-2 text-faint group-hover:border-brand/40 group-hover:text-brand"><ArrowLeftRight class="h-4 w-4" /></span>
					<span class="mono font-medium text-ink group-hover:text-brand">{shorten(t.hash, 12, 10)}</span>
				</a>
			{:else if key === 'block'}
				{#if t.block < 0}<span class="text-muted">Genesis</span>{:else}
					<a class="y-link mono" href="/blocks/{t.block}">#{num(t.block)}</a>
					{#if blockTime.get(t.block)}<div class="text-[11px] text-faint"><TimeAgo ts={blockTime.get(t.block)} /></div>{/if}
				{/if}
			{:else if key === 'to'}
				<span class="mono text-xs text-muted" title={t.outputAddresses.join('\n')}>{shorten(t.outputAddresses[0], 12, 6)}</span>{#if t.outputAddresses.length > 1}<span class="ml-1.5 rounded-full bg-surface-2 px-1.5 text-[11px] text-faint">+{t.outputAddresses.length - 1}</span>{/if}
			{:else if key === 'fee'}
				{#if t.fee != null}<Ada value={t.fee} decimals={6} class="text-muted" />{:else}—{/if}
			{:else if key === 'out'}
				<Ada value={t.output} class="font-semibold" />
			{/if}
		{/snippet}
	</DataTable>
	<Pager page={data.page} hasNext={data.txs.length === data.count} totalPages={data.totalPages} />
</section>
