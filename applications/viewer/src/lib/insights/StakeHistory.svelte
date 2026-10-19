<script lang="ts">
	import type { StakeInsights } from '$lib/analytics/types';
	import { adaCompact } from '$lib/format';
	import AnalyticsNote from '$lib/ui/AnalyticsNote.svelte';
	import Chart from '$lib/ui/Chart.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import { epochLabel } from './labels';

	/* A stake account over its recent epochs: rewards earned and active stake. */
	let { history }: { history: StakeInsights } = $props();

	const e = $derived(history.epochs);
	const total = $derived(e.reduce((t, x) => t + x.rewards, 0));
	const pools = $derived(new Set(e.map((x) => x.pool_id).filter(Boolean)).size);
	const ada = (v: number) => `₳${adaCompact(Math.round(v))}`;
</script>

{#if e.length}
	<div class="mb-6" data-insights="stake">
		<Panel title="History" sub="Epochs {e[0].epoch} to {e[e.length - 1].epoch}: {ada(total)} in rewards{pools > 1 ? `, with ${pools} pools` : ''}">
			{#snippet actions()}<AnalyticsNote source={history} compact />{/snippet}
			<div class="grid gap-6 p-1 md:grid-cols-2">
				<div>
					<div class="y-label mb-2">Rewards per epoch</div>
					<Chart points={e.map((x) => ({ label: epochLabel(x.epoch), value: x.rewards }))} color="rgb(var(--c-mint))" height={100} label="Rewards per epoch" format={ada} />
				</div>
				<div>
					<div class="y-label mb-2">Active stake</div>
					<Chart points={e.map((x) => ({ label: epochLabel(x.epoch), value: x.stake ?? 0 }))} kind="area" color="rgb(var(--c-blue))" height={100} label="Active stake" format={ada} />
				</div>
			</div>
		</Panel>
	</div>
{/if}
