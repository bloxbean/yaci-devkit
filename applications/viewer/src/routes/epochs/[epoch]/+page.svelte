<script lang="ts">
	import { goto } from '$app/navigation';
	import { chain } from '$lib/live/chain.svelte';
	import { adaCompact, dateTime, duration, num } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import KV from '$lib/ui/KV.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import Ring from '$lib/ui/Ring.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Tabs from '$lib/ui/Tabs.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import Skeleton from '$lib/ui/Skeleton.svelte';
	import ParamsView from '$lib/epochs/ParamsView.svelte';
	import Timer from '@lucide/svelte/icons/timer';
	import ChevronLeft from '@lucide/svelte/icons/chevron-left';
	import ChevronRight from '@lucide/svelte/icons/chevron-right';
	import PiggyBank from '@lucide/svelte/icons/piggy-bank';
	import Gift from '@lucide/svelte/icons/gift';
	import Box from '@lucide/svelte/icons/box';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';

	let { data } = $props();
	const e = $derived(data.epoch);

	const nowSec = $derived(chain.now / 1000);
	const progress = $derived(data.isCurrent && e.start && e.end ? Math.min(1, Math.max(0, (nowSec - e.start) / (e.end - e.start))) : 1);
	const hasNext = $derived(data.latestEpoch != null && e.number < data.latestEpoch);

	function onKey(ev: KeyboardEvent) {
		if ((ev.target as HTMLElement).closest('input,textarea')) return;
		if (ev.key === 'ArrowLeft' && e.number > 0) goto(`/epochs/${e.number - 1}${data.tab === 'params' ? '?tab=params' : ''}`);
		if (ev.key === 'ArrowRight' && hasNext) goto(`/epochs/${e.number + 1}${data.tab === 'params' ? '?tab=params' : ''}`);
	}

	const potRows = $derived(
		data.pots
			? [
					{ label: 'Treasury', value: data.pots.treasury, dot: 'bg-cyan' },
					{ label: 'Reserves', value: data.pots.reserves, dot: 'bg-violet' },
					{ label: 'Rewards pot', value: data.pots.rewards_pot, dot: 'bg-mint' },
					{ label: 'Distributed rewards', value: data.pots.distributed_rewards, dot: 'bg-brand' },
					{ label: 'Fees', value: data.pots.fees, dot: 'bg-amber' },
					{ label: 'Stake deposits', value: data.pots.deposits_stake, dot: 'bg-faint' }
				].filter((r) => r.value != null)
			: []
	);
</script>

<svelte:window onkeydown={onKey} />
<svelte:head><title>Epoch {num(e.number)} · Yaci Viewer</title></svelte:head>

<PageHeader kind="Epoch" title="Epoch {num(e.number)}">
	{#snippet icon()}<Timer class="h-5 w-5" />{/snippet}
	{#if e.start}{dateTime(e.start)}{#if e.end} → {dateTime(e.end)}{/if}{/if}
	{#snippet actions()}
		{#if data.isCurrent}<Badge tone="brand">Current</Badge>{/if}
		<a class="y-btn {e.number <= 0 ? 'pointer-events-none opacity-40' : ''}" href="/epochs/{e.number - 1}" title="Previous epoch (←)"><ChevronLeft class="h-4 w-4" /> Prev</a>
		<a class="y-btn {hasNext ? '' : 'pointer-events-none opacity-40'}" href="/epochs/{e.number + 1}" title="Next epoch (→)">Next <ChevronRight class="h-4 w-4" /></a>
	{/snippet}
</PageHeader>

<div class="mb-6 grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
	<Panel title="Overview">
		<div class="flex flex-col gap-6 sm:flex-row sm:items-center">
			<Ring value={progress} size={120} stroke={10}>
				{#if data.isCurrent}
					<div>
						<div class="font-display text-xl font-semibold tabular-nums">{(progress * 100).toFixed(1)}<span class="text-xs text-muted">%</span></div>
						<div class="text-[11px] text-faint">done</div>
					</div>
				{:else}
					<div class="text-[11px] font-medium text-muted">Finished</div>
				{/if}
			</Ring>
			<dl class="grid flex-1 grid-cols-2 gap-x-6 gap-y-5 lg:grid-cols-3">
				<KV label={data.isCurrent ? 'Ends in' : 'Length'}>
					{#if e.start && e.end}<span class="font-medium tabular-nums">{duration(data.isCurrent ? e.end - nowSec : e.end - e.start)}</span>{:else}—{/if}
				</KV>
				<KV label={e.exact ? 'Started' : 'First block'}>{#if e.start}<TimeAgo ts={e.start} />{:else}—{/if}</KV>
				<KV label="Blocks"><span class="font-semibold tabular-nums">{e.blocks != null ? num(e.blocks) : '—'}</span></KV>
				<KV label="Transactions"><span class="font-semibold tabular-nums">{e.txs != null ? num(e.txs) : '—'}</span></KV>
				<KV label="Output">{#if e.output != null}<span class="tabular-nums">₳{adaCompact(e.output)}</span>{:else}—{/if}</KV>
				<KV label="Fees">{#if e.fees != null}<Ada value={e.fees} decimals={2} />{:else}—{/if}</KV>
				<KV label="Active stake">
					{#await data.activeStake}
						<span class="y-skel inline-block h-4 w-20 align-middle"></span>
					{:then stake}
						{#if stake}<span class="tabular-nums">₳{adaCompact(stake)}</span>{:else}<span class="text-muted">No snapshot</span>{/if}
					{/await}
				</KV>
				{#if data.supply}
					<KV label="Circulating"><span class="tabular-nums">₳{adaCompact(data.supply.circulating)}</span></KV>
				{/if}
			</dl>
		</div>
	</Panel>

	<Panel title="Ada pots">
		{#snippet icon()}<PiggyBank class="h-4 w-4" />{/snippet}
		{#snippet actions()}<a class="y-btn h-8 text-xs" href="/adapot/current?epoch={e.number}">Details <ArrowRight class="h-3.5 w-3.5" /></a>{/snippet}
		{#if potRows.length}
			<dl class="grid grid-cols-[1fr_auto] gap-x-4 gap-y-2 text-[13px]">
				{#each potRows as r (r.label)}
					<dt class="flex items-center gap-2 text-muted"><span class="h-2 w-2 rounded-full {r.dot}"></span>{r.label}</dt>
					<dd class="text-right font-medium tabular-nums">₳{adaCompact(r.value)}</dd>
				{/each}
			</dl>
		{:else}
			<p class="text-sm text-muted">No ada pot snapshot for this epoch. Pots are calculated at each epoch boundary.</p>
		{/if}
		<a class="mt-4 flex items-center gap-2 border-t border-line pt-3 text-sm text-brand hover:underline" href="/rewards/epoch/{e.number}"><Gift class="h-4 w-4" />Rewards for epoch {num(e.number)}</a>
	</Panel>
</div>

<section class="y-panel overflow-hidden">
	<Tabs tabs={[{ id: 'blocks', label: 'Blocks', count: e.blocks }, { id: 'params', label: 'Protocol parameters' }]} active={data.tab} link />
	{#if data.tab === 'params'}
		<div class="p-4 sm:p-5">
			{#if data.protocol}<ParamsView params={data.protocol} />{:else}<Empty title="No parameters for this epoch" text="Yaci Store keeps parameters from the epoch it started syncing." />{/if}
		</div>
	{:else}
		{#await data.blockList}
			<Skeleton rows={8} />
		{:then list}
			{#if !list}
				<ErrorBox title="Couldn't load this epoch's blocks" message="Yaci Store didn't answer in time. Early epochs on large chains can be slow to list." />
			{:else}
				<DataTable
					rows={list.blocks}
					columns={[
						{ key: 'number', label: 'Block' },
						{ key: 'age', label: 'Age' },
						{ key: 'slot', label: 'Slot', minor: true },
						{ key: 'txs', label: 'Transactions', align: 'right' },
						{ key: 'size', label: 'Size', align: 'right', minor: true },
						{ key: 'fees', label: 'Fees', align: 'right', minor: true }
					]}
					key={(b) => b.number}
					rowHref={(b) => `/blocks/${b.number}`}
					emptyTitle="No blocks in this epoch"
				>
					{#snippet cell(b, key)}
						{#if key === 'number'}
							<a href="/blocks/{b.number}" class="group flex items-center gap-2.5">
								<span class="grid h-8 w-8 place-items-center rounded-lg border border-line bg-surface-2 text-faint group-hover:border-brand/40 group-hover:text-brand"><Box class="h-4 w-4" /></span>
								<span class="mono font-semibold text-ink group-hover:text-brand">#{num(b.number)}</span>
							</a>
						{:else if key === 'age'}<TimeAgo ts={b.time} class="text-muted" />
						{:else if key === 'slot'}<span class="mono text-muted">{num(b.slot)}</span>
						{:else if key === 'txs'}<span class="inline-flex min-w-8 justify-center rounded-full px-2 py-0.5 text-xs font-semibold tabular-nums {b.tx_count ? 'bg-brand/10 text-brand' : 'bg-surface-2 text-faint'}">{b.tx_count}</span>
						{:else if key === 'size'}<span class="text-xs tabular-nums text-muted">{b.size >= 1024 ? `${(b.size / 1024).toFixed(1)} KB` : `${b.size} B`}</span>
						{:else if key === 'fees'}{#if b.fees}<Ada value={b.fees} decimals={4} class="text-muted" />{:else}<span class="text-faint">—</span>{/if}{/if}
					{/snippet}
				</DataTable>
				<Pager page={data.page} hasNext={list.totalPages ? data.page < list.totalPages : list.blocks.length === data.count} totalPages={list.totalPages} />
			{/if}
		{/await}
	{/if}
</section>
