<script lang="ts">
	import type { PoolInsights } from '$lib/analytics/types';
	import { adaCompact, num } from '$lib/format';
	import AnalyticsNote from '$lib/ui/AnalyticsNote.svelte';
	import Chart from '$lib/ui/Chart.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import { epochLabel } from './labels';

	/* A pool over its recent epochs: blocks made, active stake, and delegators. */
	let { history }: { history: PoolInsights } = $props();

	const e = $derived(history.epochs);
	const first = $derived(e[0]?.epoch);
	const last = $derived(e[e.length - 1]?.epoch);
	const blocks = $derived(e.reduce((t, x) => t + x.blocks, 0));
	const series = (k: 'blocks' | 'stake' | 'delegators') => e.map((x) => ({ label: epochLabel(x.epoch), value: x[k] }));
</script>

{#if e.length}
	<div class="mb-6" data-insights="pool">
		<Panel title="History" sub="Epochs {first} to {last}: {num(blocks)} blocks">
			{#snippet actions()}<AnalyticsNote source={history} compact />{/snippet}
			<div class="grid gap-6 p-1 md:grid-cols-3">
				<div>
					<div class="y-label mb-2">Blocks per epoch</div>
					<Chart points={series('blocks')} height={100} label="Blocks per epoch" format={(v) => `${num(v)} blocks`} />
				</div>
				<div>
					<div class="y-label mb-2">Active stake</div>
					<Chart points={series('stake')} kind="area" color="rgb(var(--c-blue))" height={100} label="Active stake" format={(v) => `₳${adaCompact(Math.round(v))}`} />
				</div>
				<div>
					<div class="y-label mb-2">Delegators</div>
					<Chart points={series('delegators')} kind="area" color="rgb(var(--c-violet))" height={100} label="Delegators" format={(v) => num(v)} />
				</div>
			</div>
		</Panel>
	</div>
{/if}
