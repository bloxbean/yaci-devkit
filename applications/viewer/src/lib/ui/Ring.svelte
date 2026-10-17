<script lang="ts">
	import type { Snippet } from 'svelte';

	/* A progress ring with whatever you like in the middle. */
	let {
		value,
		size = 120,
		stroke = 10,
		children = undefined
	}: { value: number; size?: number; stroke?: number; children?: Snippet } = $props();

	const id = `rg${Math.random().toString(36).slice(2, 8)}`;
	const r = $derived((size - stroke) / 2);
	const c = $derived(2 * Math.PI * r);
	const clamped = $derived(Math.min(1, Math.max(0, value || 0)));
</script>

<div class="relative shrink-0" style="width:{size}px;height:{size}px">
	<svg viewBox="0 0 {size} {size}" width={size} height={size} class="-rotate-90" aria-hidden="true">
		<defs>
			<linearGradient id={id} x1="0" y1="0" x2="1" y2="1">
				<stop offset="0%" stop-color="#ff4e2d" />
				<stop offset="55%" stop-color="#ff8a3d" />
				<stop offset="100%" stop-color="#fbbf24" />
			</linearGradient>
		</defs>
		<circle cx={size / 2} cy={size / 2} {r} fill="none" stroke="rgb(var(--c-line))" stroke-width={stroke} />
		<circle
			cx={size / 2}
			cy={size / 2}
			{r}
			fill="none"
			stroke="url(#{id})"
			stroke-width={stroke}
			stroke-linecap="round"
			stroke-dasharray={c}
			stroke-dashoffset={c * (1 - clamped)}
			style="transition: stroke-dashoffset 0.9s cubic-bezier(0.2, 0.8, 0.2, 1)"
		/>
	</svg>
	{#if children}<div class="absolute inset-0 grid place-items-center text-center">{@render children()}</div>{/if}
</div>
