<script lang="ts">
	import { adaCompact, num, percent } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import TrendChart from '$lib/epochs/TrendChart.svelte';
	import { POTS, toAda } from '$lib/epochs/pots';
	import PiggyBank from '@lucide/svelte/icons/piggy-bank';
	import ChevronLeft from '@lucide/svelte/icons/chevron-left';
	import ChevronRight from '@lucide/svelte/icons/chevron-right';
	import History from '@lucide/svelte/icons/list-clock';
	import TrendingUp from '@lucide/svelte/icons/trending-up';

	let { data } = $props();
	const pot = $derived(data.pot);
	const MAX = 45_000_000_000_000_000;

	const bar = $derived(
		pot
			? [
					{ label: 'Circulation', value: Number(pot.circulation ?? 0), cls: 'bg-brand' },
					{ label: 'Reserves', value: Number(pot.reserves ?? 0), cls: 'bg-violet' },
					{ label: 'Treasury', value: Number(pot.treasury ?? 0), cls: 'bg-cyan' },
					{ label: 'Deposits', value: Number(pot.deposits_stake ?? 0), cls: 'bg-blue' }
				]
			: []
	);
	const series = (key: (typeof POTS)[number]['key']) => data.history.map((h) => ({ x: h.epoch, y: toAda(h[key]) }));
	const fmt = (v: number) => `₳${adaCompact(v * 1_000_000)}`;
	const tone: Record<string, string> = { cyan: 'bg-cyan', violet: 'bg-violet', brand: 'bg-brand', mint: 'bg-mint', amber: 'bg-amber', blue: 'bg-blue' };
	const isLatest = $derived(pot && data.latest != null && pot.epoch === data.latest);
</script>

<svelte:head><title>Ada pots · Yaci Viewer</title></svelte:head>

<PageHeader kind="Staking" title="Ada pots">
	{#snippet icon()}<PiggyBank class="h-5 w-5" />{/snippet}
	Where all ADA sits at an epoch boundary: treasury, reserves, circulation and the rewards pot.
	{#snippet actions()}
		{#if pot}
			{#if isLatest}<Badge tone="brand">Latest</Badge>{/if}
			<a class="y-btn {pot.epoch <= 0 ? 'pointer-events-none opacity-40' : ''}" href="?epoch={pot.epoch - 1}"><ChevronLeft class="h-4 w-4" /> Epoch {pot.epoch - 1}</a>
			<a class="y-btn {isLatest ? 'pointer-events-none opacity-40' : ''}" href="?epoch={pot.epoch + 1}">Epoch {pot.epoch + 1} <ChevronRight class="h-4 w-4" /></a>
		{/if}
		<a class="y-btn" href="/adapot/list"><History class="h-4 w-4" /> History</a>
	{/snippet}
</PageHeader>

{#if data.error}
	<div class="y-panel"><ErrorBox title="Couldn't load the ada pots" message={data.error} /></div>
{:else if !pot}
	<div class="y-panel">
		<Empty
			title={data.requested != null ? `No ada pots for epoch ${data.requested}` : 'No ada pots yet'}
			text={data.requested != null
				? 'Yaci Store has no snapshot for that epoch.'
				: 'Ada pots are calculated at epoch boundaries. On a new devnet, wait for the first epoch to finish.'}
		>
			{#if data.requested != null && data.latest != null}<a class="y-btn" href="/adapot/current">Show the latest</a>{/if}
		</Empty>
	</div>
{:else}
	<Panel title="Epoch {num(pot.epoch)}" sub="Share of the 45B ADA maximum supply" class="mb-6">
		{#snippet actions()}<a class="y-btn h-8 text-xs" href="/epochs/{pot.epoch}">Epoch details</a>{/snippet}
		<div class="flex h-3.5 overflow-hidden rounded-full bg-line">
			{#each bar as b (b.label)}
				<div class="{b.cls} h-full transition-[width] duration-700" style="width:{(b.value / MAX) * 100}%" title="{b.label} {percent(b.value, MAX)}"></div>
			{/each}
		</div>
		<div class="mt-3 flex flex-wrap gap-x-5 gap-y-1 text-xs text-muted">
			{#each bar as b (b.label)}
				<span class="flex items-center gap-1.5"><span class="h-2 w-2 rounded-full {b.cls}"></span>{b.label} <b class="font-semibold text-ink">{percent(b.value, MAX, 2)}</b></span>
			{/each}
		</div>
	</Panel>

	<section class="mb-6 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
		{#each POTS as p (p.key)}
			{#if pot[p.key] != null}
				<div class="y-panel p-4">
					<div class="flex items-center gap-2"><span class="h-2 w-2 rounded-full {tone[p.color]}"></span><span class="y-label">{p.label}</span></div>
					<div class="mt-2 font-display text-2xl font-semibold tracking-tight"><Ada value={pot[p.key]} decimals={0} /></div>
					<div class="mt-0.5 text-xs text-muted">{p.hint}</div>
				</div>
			{/if}
		{/each}
	</section>

	{#if data.history.length > 1}
		<Panel title="Trends" sub="Last {data.history.length} epochs. Hover a chart to read a value.">
			{#snippet icon()}<TrendingUp class="h-4 w-4" />{/snippet}
			<div class="grid gap-6 md:grid-cols-2 xl:grid-cols-4">
				<TrendChart label="Treasury" points={series('treasury')} color="rgb(var(--c-cyan))" format={fmt} />
				<TrendChart label="Reserves" points={series('reserves')} color="rgb(var(--c-violet))" format={fmt} />
				<TrendChart label="Rewards pot" points={series('rewards_pot')} color="rgb(var(--c-mint))" format={fmt} />
				<TrendChart label="Fees" points={series('fees')} color="rgb(var(--c-amber))" format={fmt} />
			</div>
		</Panel>
	{/if}
{/if}
