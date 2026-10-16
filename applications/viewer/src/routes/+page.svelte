<script lang="ts">
	import { chain, toBlock, toTx, type LiveBlock, type LiveTx } from '$lib/live/chain.svelte';
	import { num, compact, adaCompact } from '$lib/format';
	import Panel from '$lib/ui/Panel.svelte';
	import Stat from '$lib/ui/Stat.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import BlockConveyor from '$lib/dashboard/BlockConveyor.svelte';
	import EpochCard from '$lib/dashboard/EpochCard.svelte';
	import TxFeed from '$lib/dashboard/TxFeed.svelte';
	import SupplyCard from '$lib/dashboard/SupplyCard.svelte';
	import { shell } from '$lib/shell/ui.svelte';
	import Box from '@lucide/svelte/icons/box';
	import ArrowLeftRight from '@lucide/svelte/icons/arrow-left-right';
	import Timer from '@lucide/svelte/icons/timer';
	import Zap from '@lucide/svelte/icons/zap';
	import Activity from '@lucide/svelte/icons/activity';
	import PiggyBank from '@lucide/svelte/icons/piggy-bank';
	import Search from '@lucide/svelte/icons/search';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';
	import Gauge from '@lucide/svelte/icons/gauge';

	let { data } = $props();

	// Server data until the live layer has its own.
	const blocks: LiveBlock[] = $derived(chain.blocks.length ? chain.blocks : data.blocks.map((b: unknown) => toBlock(b, 0)));
	const txs: LiveTx[] = $derived(chain.txs.length ? chain.txs : data.txs.map((t: unknown) => toTx(t, 0)));
	const tip = $derived(blocks[0]);
	const epoch = $derived(data.epoch ? { epoch: data.epoch.epoch, start: data.epoch.start_time, end: data.epoch.end_time } : null);

	// Trends over the recent blocks, oldest first.
	const recent = $derived(blocks.slice(0, 20).reverse());
	const intervals = $derived(recent.slice(1).map((b, i) => b.time - recent[i].time));
	const avgInterval = $derived(intervals.length ? intervals.reduce((a, b) => a + b, 0) / intervals.length : null);
	const txCounts = $derived(recent.map((b) => b.txCount));
	const txTotal = $derived(txCounts.reduce((a, b) => a + b, 0));
	const fullness = $derived(recent.map((b) => (b.size / data.maxBlockSize) * 100));
	const avgFull = $derived(fullness.length ? fullness.reduce((a, b) => a + b, 0) / fullness.length : 0);
	const tps = $derived(recent.length > 1 && recent[recent.length - 1].time > recent[0].time ? txTotal / (recent[recent.length - 1].time - recent[0].time) : 0);
</script>

<svelte:head><title>Dashboard · Yaci Viewer</title></svelte:head>

{#if !data.reachable && !chain.blocks.length}
	<div class="y-panel">
		<ErrorBox
			title="Can't reach Yaci Store"
			message="The viewer reads everything from Yaci Store. Start your devnet with Yaci Store enabled (yaci-devkit up --enable-yaci-store), or point PUBLIC_INDEXER_BASE_URL at a running store."
		/>
	</div>
{:else}
	<!-- Header -->
	<section class="mb-6 flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
		<div>
			<div class="mb-2 flex flex-wrap items-center gap-2 text-xs">
				<span class="y-pill border-brand/25 bg-brand/10 font-semibold text-brand">{chain.network?.name ?? 'Cardano'}</span>
				{#if chain.network?.era}<span class="y-pill border-line bg-surface text-muted">{chain.network.era} era</span>{/if}
				{#if chain.status === 'live'}
					<span class="y-pill border-mint/25 bg-mint/10 text-mint"><span class="h-1.5 w-1.5 rounded-full bg-mint"></span>Live</span>
				{:else if chain.status === 'polling'}
					<span class="y-pill border-blue/25 bg-blue/10 text-blue"><span class="h-1.5 w-1.5 rounded-full bg-blue"></span>Auto-refresh</span>
				{/if}
			</div>
			<h1 class="text-3xl font-semibold sm:text-4xl">
				Chain tip
				{#if tip}
					{#key tip.number}
						<a href="/blocks/{tip.number}" class="y-grad-text inline-block animate-fade-up tabular-nums">#{num(tip.number)}</a>
					{/key}
				{/if}
			</h1>
			<p class="mt-1.5 text-sm text-muted">
				{#if tip}Last block <TimeAgo ts={tip.time} /> · slot {num(tip.slot)} · epoch {tip.epoch}{:else}Waiting for the first block…{/if}
			</p>
		</div>
		<button
			class="group flex h-11 w-full items-center gap-3 rounded-xl border border-line bg-surface px-4 text-sm text-faint shadow-card transition hover:border-brand/40 lg:w-[420px]"
			onclick={() => shell.openPalette()}
		>
			<Search class="h-4 w-4 group-hover:text-brand" />
			<span class="truncate">Paste a tx hash, address, policy id…</span>
			<ArrowRight class="ml-auto h-4 w-4 opacity-0 transition group-hover:translate-x-0.5 group-hover:opacity-100" />
		</button>
	</section>

	<!-- Live blocks -->
	<Panel title="Live blocks" sub="Newest first. Fill shows how full each block is." class="mb-6">
		{#snippet icon()}<Box class="h-4 w-4" />{/snippet}
		{#snippet actions()}<a href="/blocks" class="y-btn h-8 text-xs">All blocks <ArrowRight class="h-3.5 w-3.5" /></a>{/snippet}
		<BlockConveyor {blocks} maxBlockSize={data.maxBlockSize} />
	</Panel>

	<!-- KPIs -->
	<section class="mb-6 grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
		<Stat label="Block time" value={avgInterval ? `${avgInterval.toFixed(avgInterval < 10 ? 2 : 1)}s` : '—'} sub="average, last {recent.length} blocks" trend={intervals} color="rgb(var(--c-violet))">
			{#snippet icon()}<Timer class="h-4 w-4" />{/snippet}
		</Stat>
		<Stat label="Transactions" value={num(txTotal)} sub="in the last {recent.length} blocks · {tps < 1 ? tps.toFixed(2) : compact(tps)} tx/s" trend={txCounts} href="/transactions">
			{#snippet icon()}<Zap class="h-4 w-4" />{/snippet}
		</Stat>
		<Stat label="Block load" value="{avgFull.toFixed(avgFull < 10 ? 2 : 1)}%" sub="average fill of max {compact(data.maxBlockSize / 1024)} KB" trend={fullness} color="rgb(var(--c-cyan))">
			{#snippet icon()}<Gauge class="h-4 w-4" />{/snippet}
		</Stat>
		<Stat
			label="Treasury"
			value={data.network?.supply?.treasury ? `₳${adaCompact(data.network.supply.treasury)}` : '—'}
			sub="reserves ₳{adaCompact(data.network?.supply?.reserves)}"
			href="/adapot/current"
		>
			{#snippet icon()}<PiggyBank class="h-4 w-4" />{/snippet}
		</Stat>
	</section>

	<!-- Transactions + epoch/supply -->
	<section class="grid gap-6 xl:grid-cols-[minmax(0,1.45fr)_minmax(0,1fr)]">
		<Panel title="Latest transactions" flush>
			{#snippet icon()}<ArrowLeftRight class="h-4 w-4" />{/snippet}
			{#snippet actions()}<a href="/transactions" class="y-btn h-8 text-xs">All transactions <ArrowRight class="h-3.5 w-3.5" /></a>{/snippet}
			<TxFeed {txs} />
		</Panel>

		<div class="flex flex-col gap-6">
			<Panel title="Epoch progress">
				{#snippet icon()}<Activity class="h-4 w-4" />{/snippet}
				<EpochCard {epoch} />
			</Panel>
			<Panel title="ADA supply">
				{#snippet icon()}<PiggyBank class="h-4 w-4" />{/snippet}
				<SupplyCard network={data.network} />
			</Panel>
		</div>
	</section>
{/if}
