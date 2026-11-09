<script lang="ts">
	import type { Snippet } from 'svelte';
	import Sparkline from './Sparkline.svelte';

	/* A KPI tile: label, big value, a small line under it, and an optional trend. */
	let {
		label,
		value = undefined,
		sub = undefined,
		trend = undefined,
		color = 'rgb(var(--c-brand))',
		href = undefined,
		icon = undefined,
		children = undefined
	}: {
		label: string;
		value?: string | number | null;
		sub?: string;
		trend?: number[];
		color?: string;
		href?: string;
		icon?: Snippet;
		children?: Snippet;
	} = $props();
</script>

<svelte:element
	this={href ? 'a' : 'div'}
	{href}
	class="y-panel group relative flex min-w-0 flex-col gap-2 p-4 transition {href ? 'hover:-translate-y-0.5 hover:border-brand/40' : ''}"
>
	<div class="flex items-center justify-between gap-2">
		<span class="y-label">{label}</span>
		{#if icon}<span class="text-faint transition group-hover:text-brand">{@render icon()}</span>{/if}
	</div>
	<div class="flex items-end justify-between gap-3">
		<div class="min-w-0">
			{#if children}
				{@render children()}
			{:else}
				<div class="truncate font-display text-2xl font-semibold tabular-nums tracking-tight">{value ?? '—'}</div>
			{/if}
			{#if sub}<div class="mt-0.5 truncate text-xs text-muted">{sub}</div>{/if}
		</div>
		{#if trend && trend.length > 1}
			<div class="w-24 shrink-0 opacity-90"><Sparkline values={trend} width={96} height={30} {color} /></div>
		{/if}
	</div>
</svelte:element>
