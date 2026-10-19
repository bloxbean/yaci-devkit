<script lang="ts">
	import type { NetworkInsights } from '$lib/analytics/types';
	import { adaCompact, compact, num, percent, shorten } from '$lib/format';
	import AnalyticsNote from '$lib/ui/AnalyticsNote.svelte';
	import Stat from '$lib/ui/Stat.svelte';
	import ChartCard from './ChartCard.svelte';
	import { dayLabel, epochLabel } from './labels';

	/*
	 * The dashboard's analytics section, where the store has Yaci Store analytics: daily transactions, script use and
	 * fees over 90 days, the ada pots, stake concentration, block production, and the scripts run most often.
	 */
	let { insights: s }: { insights: NetworkInsights } = $props();

	const days = $derived(s.days ?? []);
	const lastDay = $derived(days[days.length - 1]);
	const avg = (k: 'txs' | 'runs' | 'fees' | 'steps') => (days.length ? days.reduce((t, d) => t + d[k], 0) / days.length : 0);
	const series = (k: 'txs' | 'runs' | 'fees' | 'steps') => days.map((d) => ({ label: dayLabel(d.date), value: d[k] }));
	const pots = $derived(s.pots ?? []);
	const lastPot = $derived(pots[pots.length - 1]);
	// The last epoch with blocks may still be in progress; the one before is the last whole one.
	const epochs = $derived(s.epochs ?? []);
	const lastEpoch = $derived(epochs.length > 1 ? epochs[epochs.length - 2] : epochs[0]);
	const maxTopRuns = $derived(Math.max(1, ...(s.topScripts ?? []).map((t) => t.runs)));
	const ada = (v: number) => `₳${adaCompact(Math.round(v))}`;
</script>

<section class="mt-8" data-insights="network">
	<div class="mb-3 flex flex-wrap items-center justify-between gap-2">
		<div>
			<h2 class="font-display text-lg font-semibold">Network insights</h2>
			<p class="text-sm text-muted">The last {days.length || 90} days and recent epochs, from Yaci Store analytics.</p>
		</div>
		<AnalyticsNote source={s} compact />
	</div>

	{#if days.length && lastDay}
		<div class="grid gap-4 md:grid-cols-2">
			<ChartCard
				title="Transactions per day"
				value={num(lastDay.txs)}
				sub="{dayLabel(lastDay.date)} · {num(Math.round(avg('txs')))} a day on average"
				points={series('txs')}
				format={(v) => num(Math.round(v))}
			/>
			<ChartCard
				title="Script runs per day"
				value={num(lastDay.runs)}
				sub="{dayLabel(lastDay.date)} · in {num(lastDay.scriptTxs)} transactions ({percent(lastDay.scriptTxs, lastDay.txs, 0)} of all)"
				points={series('runs')}
				color="rgb(var(--c-violet))"
				format={(v) => num(Math.round(v))}
			/>
			<ChartCard
				title="Script CPU per day"
				value="{compact(lastDay.steps)} steps"
				sub="{dayLabel(lastDay.date)} · {compact(lastDay.mem)} memory units"
				points={series('steps')}
				kind="area"
				color="rgb(var(--c-cyan))"
				format={(v) => `${compact(v)} steps`}
			/>
			<ChartCard
				title="Fees per day"
				value={ada(lastDay.fees)}
				sub="{dayLabel(lastDay.date)} · {ada(avg('fees'))} a day on average"
				points={series('fees')}
				kind="area"
				color="rgb(var(--c-amber))"
				format={ada}
			/>
		</div>
	{/if}

	<div class="mt-4 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
		{#if lastPot}
			<Stat label="Treasury" value={ada(lastPot.treasury)} sub="epoch {lastPot.epoch}" trend={pots.map((p) => p.treasury)} color="rgb(var(--c-mint))" href="/adapot" />
			<Stat label="Reserves" value={ada(lastPot.reserves)} sub="epoch {lastPot.epoch}" trend={pots.map((p) => p.reserves)} color="rgb(var(--c-blue))" href="/adapot" />
		{/if}
		{#if s.concentration}
			<Stat
				label="Nakamoto coefficient"
				value={num(s.concentration.nakamoto)}
				sub="of {num(s.concentration.pools)} pools hold half the stake"
				href="/pools"
			/>
		{/if}
		{#if lastEpoch}
			<Stat
				label="Pools making blocks"
				value={num(lastEpoch.pools)}
				sub="in epoch {lastEpoch.epoch}"
				trend={epochs.slice(0, -1).map((e) => e.pools)}
				color="rgb(var(--c-violet))"
				href="/epochs/{lastEpoch.epoch}"
			/>
		{/if}
	</div>

	{#if s.topScripts?.length}
		<div class="y-panel mt-4 overflow-hidden">
			<div class="flex items-baseline justify-between gap-2 border-b border-line px-4 py-3">
				<h3 class="font-medium">Most-run scripts</h3>
				<span class="text-xs text-muted">last {s.topScriptDays} days</span>
			</div>
			<ol class="divide-y divide-line">
				{#each s.topScripts as t, i (t.script_hash)}
					<li class="flex items-center gap-3 px-4 py-2.5 text-sm">
						<span class="w-5 shrink-0 text-right text-xs text-faint tabular-nums">{i + 1}</span>
						<a class="mono w-44 shrink-0 truncate text-ink hover:text-brand" href="/scripts/{t.script_hash}" title={t.script_hash}>{shorten(t.script_hash, 10, 6)}</a>
						<span class="hidden h-1.5 min-w-0 flex-1 overflow-hidden rounded-full bg-bg-2 sm:block">
							<span class="block h-full rounded-full bg-violet/70" style="width:{(t.runs / maxTopRuns) * 100}%"></span>
						</span>
						<span class="ml-auto shrink-0 tabular-nums"><span class="font-semibold">{num(t.runs)}</span> <span class="text-xs text-muted">runs</span></span>
						<span class="hidden w-28 shrink-0 text-right text-xs text-muted tabular-nums md:block">{num(t.txs)} txs</span>
						<span class="hidden w-28 shrink-0 text-right text-xs text-muted tabular-nums lg:block">{compact(t.steps)} steps</span>
					</li>
				{/each}
			</ol>
		</div>
	{/if}
</section>
