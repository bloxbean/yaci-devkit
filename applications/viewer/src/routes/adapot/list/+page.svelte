<script lang="ts">
	import { adaCompact, num } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import TrendChart from '$lib/epochs/TrendChart.svelte';
	import { toAda, type AdaPot } from '$lib/epochs/pots';
	import PiggyBank from '@lucide/svelte/icons/piggy-bank';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';

	let { data } = $props();

	const fmt = (v: number) => `₳${adaCompact(v * 1_000_000)}`;
	const series = (key: keyof AdaPot) => data.rows.map((r) => ({ x: r.epoch, y: toAda(r[key] as number) }));
	const cols: { key: keyof AdaPot | 'epoch'; label: string; minor?: boolean }[] = [
		{ key: 'treasury', label: 'Treasury' },
		{ key: 'reserves', label: 'Reserves' },
		{ key: 'circulation', label: 'Circulation', minor: true },
		{ key: 'rewards_pot', label: 'Rewards pot', minor: true },
		{ key: 'distributed_rewards', label: 'Distributed', minor: true },
		{ key: 'fees', label: 'Fees', minor: true },
		{ key: 'deposits_stake', label: 'Deposits', minor: true }
	];
	const columns = [{ key: 'epoch', label: 'Epoch' }, ...cols.map((c) => ({ key: c.key as string, label: c.label, align: 'right' as const, minor: c.minor }))];
	// Change from the previous (older) row, to show which way the treasury and reserves are moving.
	const delta = (i: number, key: keyof AdaPot) => {
		const cur = Number(data.rows[i]?.[key]);
		const prev = Number(data.rows[i + 1]?.[key]);
		return Number.isFinite(cur) && Number.isFinite(prev) && prev ? cur - prev : null;
	};
</script>

<svelte:head><title>Ada pot history · Yaci Viewer</title></svelte:head>

<PageHeader kind="Staking" title="Ada pot history">
	{#snippet icon()}<PiggyBank class="h-5 w-5" />{/snippet}
	Treasury, reserves and rewards at each epoch boundary.
	{#snippet actions()}<a class="y-btn" href="/adapot/current">Latest pots <ArrowRight class="h-4 w-4" /></a>{/snippet}
</PageHeader>

{#if data.rows.length > 1}
	<Panel title="This page" class="mb-6">
		<div class="grid gap-6 md:grid-cols-2">
			<TrendChart label="Treasury" points={series('treasury')} color="rgb(var(--c-cyan))" format={fmt} />
			<TrendChart label="Reserves" points={series('reserves')} color="rgb(var(--c-violet))" format={fmt} />
		</div>
	</Panel>
{/if}

<section class="y-panel overflow-hidden">
	<DataTable
		rows={data.rows}
		{columns}
		key={(r) => r.epoch}
		rowHref={(r) => `/adapot/current?epoch=${r.epoch}`}
		emptyTitle="No ada pots yet"
		emptyText="Ada pots are calculated at epoch boundaries. On a new devnet, wait for the first epoch to finish."
	>
		{#snippet cell(r, key)}
			{#if key === 'epoch'}
				<a class="y-link font-semibold tabular-nums" href="/adapot/current?epoch={r.epoch}">{num(r.epoch)}</a>
			{:else}
				{@const v = r[key as keyof AdaPot]}
				{@const i = data.rows.indexOf(r)}
				{@const d = key === 'treasury' || key === 'reserves' ? delta(i, key) : null}
				<span class="tabular-nums">{v != null ? `₳${adaCompact(v)}` : '—'}</span>
				{#if d}<div class="text-[11px] tabular-nums {d > 0 ? 'text-mint' : 'text-danger'}">{d > 0 ? '+' : '−'}₳{adaCompact(Math.abs(d))}</div>{/if}
			{/if}
		{/snippet}
	</DataTable>
	<Pager page={data.page} hasNext={data.rows.length === data.count} />
</section>
