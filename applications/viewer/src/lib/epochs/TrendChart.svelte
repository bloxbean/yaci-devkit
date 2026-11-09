<script lang="ts">
	/*
	 * One value over epochs as an area chart, with min/max labels and a hover readout. Points are drawn oldest to
	 * newest, left to right.
	 */
	let {
		points,
		label,
		color = 'rgb(var(--c-brand))',
		format = (v: number) => String(v),
		height = 120
	}: {
		points: { x: number; y: number }[];
		label: string;
		color?: string;
		format?: (v: number) => string;
		height?: number;
	} = $props();

	const W = 400;
	const id = `tc${Math.random().toString(36).slice(2, 8)}`;
	let hover = $state<number | null>(null);

	const sorted = $derived([...points].filter((p) => Number.isFinite(p.y)).sort((a, b) => a.x - b.x));
	const min = $derived(sorted.length ? Math.min(...sorted.map((p) => p.y)) : 0);
	const max = $derived(sorted.length ? Math.max(...sorted.map((p) => p.y)) : 0);
	const span = $derived(max - min || Math.abs(max) || 1);
	const xy = $derived(
		sorted.map((p, i) => [sorted.length > 1 ? (i / (sorted.length - 1)) * W : W / 2, height - 6 - ((p.y - min) / span) * (height - 18)] as const)
	);
	const line = $derived(xy.map(([x, y], i) => `${i ? 'L' : 'M'}${x.toFixed(1)},${y.toFixed(1)}`).join(' '));
	const area = $derived(xy.length ? `${line} L${xy[xy.length - 1][0]},${height} L${xy[0][0]},${height} Z` : '');
	const last = $derived(sorted[sorted.length - 1]);
	const first = $derived(sorted[0]);
	const change = $derived(first && last && first.y ? ((last.y - first.y) / Math.abs(first.y)) * 100 : null);
	const shown = $derived(hover != null ? sorted[hover] : last);

	function onMove(e: PointerEvent) {
		const r = (e.currentTarget as SVGElement).getBoundingClientRect();
		const f = (e.clientX - r.left) / r.width;
		hover = sorted.length ? Math.max(0, Math.min(sorted.length - 1, Math.round(f * (sorted.length - 1)))) : null;
	}
</script>

<div class="min-w-0">
	<div class="mb-2 flex items-baseline justify-between gap-2">
		<span class="y-label">{label}</span>
		{#if change != null}
			<span class="text-[11px] tabular-nums {change >= 0 ? 'text-mint' : 'text-danger'}">{change >= 0 ? '+' : ''}{change.toFixed(2)}%</span>
		{/if}
	</div>
	{#if shown}
		<div class="font-display text-lg font-semibold tabular-nums">{format(shown.y)}</div>
		<div class="mb-2 text-[11px] text-faint">epoch {shown.x}{hover == null ? ' · latest' : ''}</div>
	{/if}
	{#if sorted.length > 1}
		<div class="relative">
		<svg
			viewBox="0 0 {W} {height}"
			preserveAspectRatio="none"
			style="height:{height}px"
			class="block w-full touch-none overflow-visible"
			role="img"
			aria-label="{label} over the last {sorted.length} epochs"
			onpointermove={onMove}
			onpointerleave={() => (hover = null)}
		>
			<defs>
				<linearGradient id={id} x1="0" x2="0" y1="0" y2="1">
					<stop offset="0%" stop-color={color} stop-opacity="0.3" />
					<stop offset="100%" stop-color={color} stop-opacity="0" />
				</linearGradient>
			</defs>
			<path d={area} fill="url(#{id})" />
			<path d={line} fill="none" stroke={color} stroke-width="2" vector-effect="non-scaling-stroke" stroke-linejoin="round" />
			{#if hover != null && xy[hover]}
				<line x1={xy[hover][0]} x2={xy[hover][0]} y1="0" y2={height} stroke="rgb(var(--c-line))" stroke-width="1" vector-effect="non-scaling-stroke" />
			{/if}
		</svg>
		{#if hover != null && xy[hover]}
			<span
				class="pointer-events-none absolute h-2.5 w-2.5 -translate-x-1/2 -translate-y-1/2 rounded-full ring-2 ring-surface"
				style="left:{(xy[hover][0] / W) * 100}%;top:{(xy[hover][1] / height) * 100}%;background:{color}"
			></span>
		{/if}
		</div>
		<div class="mt-1 flex justify-between text-[10px] text-faint">
			<span>epoch {first.x}</span><span>epoch {last.x}</span>
		</div>
	{:else}
		<p class="text-xs text-muted">Needs at least two epochs of data.</p>
	{/if}
</div>
