<script lang="ts">
	import type { ScriptInsights } from '$lib/analytics/types';
	import { compact, num } from '$lib/format';
	import AnalyticsNote from '$lib/ui/AnalyticsNote.svelte';
	import Chart from '$lib/ui/Chart.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import { dayLabel } from './labels';

	/* How much a script has been used over the last 90 days: runs, the ex-units they took, and the costliest run each day. */
	let { usage }: { usage: ScriptInsights } = $props();

	const runs = $derived(usage.days.reduce((t, d) => t + d.runs, 0));
	const steps = $derived(usage.days.reduce((t, d) => t + d.steps, 0));
	const peak = $derived(Math.max(0, ...usage.days.map((d) => d.maxSteps)));
	const series = (k: 'runs' | 'steps' | 'maxSteps') => usage.days.map((d) => ({ label: dayLabel(d.date), value: d[k] }));
</script>

<div class="mt-6" data-insights="script">
	<Panel title="Usage over time" sub={usage.days.length ? `${num(runs)} runs since ${dayLabel(usage.from)}, ${compact(steps)} CPU steps in all` : `No runs since ${dayLabel(usage.from)}`}>
		{#snippet actions()}<AnalyticsNote source={usage} compact />{/snippet}
		{#if usage.days.length}
			<div class="grid gap-6 p-1 md:grid-cols-3">
				<div>
					<div class="y-label mb-2">Runs per day</div>
					<Chart points={series('runs')} color="rgb(var(--c-violet))" height={100} label="Runs per day" format={(v) => `${num(v)} runs`} />
				</div>
				<div>
					<div class="y-label mb-2">CPU steps per day</div>
					<Chart points={series('steps')} kind="area" color="rgb(var(--c-cyan))" height={100} label="CPU steps per day" format={(v) => `${compact(v)} steps`} />
				</div>
				<div>
					<div class="y-label mb-2">Costliest run each day <span class="normal-case tracking-normal text-faint">· peak {compact(peak)} steps</span></div>
					<Chart points={series('maxSteps')} kind="area" color="rgb(var(--c-amber))" height={100} label="Costliest run each day" format={(v) => `${compact(v)} steps`} />
				</div>
			</div>
		{/if}
	</Panel>
</div>
