<script lang="ts">
	import type { Snippet } from 'svelte';
	import Chart from '$lib/ui/Chart.svelte';

	/* A panel with a title, a headline number, a line under it, and a chart. */
	let {
		title,
		value,
		sub = undefined,
		points,
		kind = 'bar',
		color = 'rgb(var(--c-brand))',
		format = undefined,
		height = 110,
		children = undefined
	}: {
		title: string;
		value: string;
		sub?: string;
		points: { label: string; value: number }[];
		kind?: 'bar' | 'area';
		color?: string;
		format?: (v: number) => string;
		height?: number;
		children?: Snippet;
	} = $props();
</script>

<div class="y-panel flex min-w-0 flex-col gap-3 p-4">
	<div>
		<div class="y-label">{title}</div>
		<div class="mt-1 font-display text-2xl font-semibold tabular-nums tracking-tight">{value}</div>
		{#if sub}<div class="mt-0.5 text-xs text-muted">{sub}</div>{/if}
	</div>
	<Chart {points} {kind} {color} {format} {height} label={title} />
	{#if children}{@render children()}{/if}
</div>
