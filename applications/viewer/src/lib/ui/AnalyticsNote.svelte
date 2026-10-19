<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { AnalyticsSource } from '$lib/analytics/types';
	import ChartColumn from '@lucide/svelte/icons/chart-column';

	/*
	 * Marks a section answered from Yaci Store analytics (its daily exports, plus live data where the store joins it)
	 * because the regular endpoint was too slow. Says how fresh it is; extra detail goes in `children`.
	 */
	let { source, compact = false, children = undefined }: { source: AnalyticsSource; compact?: boolean; children?: Snippet } = $props();

	const day = (d: string) => new Date(`${d}T00:00:00Z`).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric', timeZone: 'UTC' });
	const freshness = $derived(source.live ? 'live data' : source.exportedTo ? `data up to ${day(source.exportedTo)}` : 'daily exports');
</script>

{#if compact}
	<span class="y-pill border-violet/25 bg-violet/10 text-violet" title="From Yaci Store analytics ({freshness}): the regular endpoint is too slow for this account." data-source="analytics">
		<ChartColumn class="h-3.5 w-3.5" />Analytics · {freshness}
	</span>
{:else}
	<div class="flex items-start gap-2 border-b border-line bg-violet/5 px-4 py-2.5 text-xs text-muted" data-source="analytics">
		<ChartColumn class="mt-0.5 h-3.5 w-3.5 shrink-0 text-violet" />
		<span>
			<span class="font-medium text-ink">From Yaci Store analytics</span> ({freshness}), because the regular list is too slow here on this store
			(<a class="y-link" href="https://github.com/bloxbean/yaci-store/issues/1269" target="_blank" rel="noopener noreferrer">yaci-store#1269</a>).
			{#if children}{@render children()}{/if}
		</span>
	</div>
{/if}
