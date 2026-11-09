<script lang="ts">
	/* A tiny trend line. Values are drawn left (oldest) to right (newest). */
	let {
		values,
		width = 120,
		height = 32,
		color = 'rgb(var(--c-brand))',
		fill = true
	}: { values: number[]; width?: number; height?: number; color?: string; fill?: boolean } = $props();

	const id = `sp${Math.random().toString(36).slice(2, 8)}`;

	const pts = $derived.by(() => {
		const v = values.filter((x) => Number.isFinite(x));
		if (v.length < 2) return null;
		const min = Math.min(...v);
		const max = Math.max(...v);
		const span = max - min || 1;
		const step = width / (v.length - 1);
		return v.map((x, i) => [i * step, height - 3 - ((x - min) / span) * (height - 6)] as const);
	});
	const line = $derived(pts ? pts.map(([x, y], i) => `${i ? 'L' : 'M'}${x.toFixed(1)},${y.toFixed(1)}`).join(' ') : '');
	const area = $derived(pts ? `${line} L${width},${height} L0,${height} Z` : '');
</script>

{#if pts}
	<svg viewBox="0 0 {width} {height}" {width} {height} class="block max-w-full overflow-visible" aria-hidden="true">
		<defs>
			<linearGradient id={id} x1="0" x2="0" y1="0" y2="1">
				<stop offset="0%" stop-color={color} stop-opacity="0.28" />
				<stop offset="100%" stop-color={color} stop-opacity="0" />
			</linearGradient>
		</defs>
		{#if fill}<path d={area} fill="url(#{id})" />{/if}
		<path d={line} fill="none" stroke={color} stroke-width="1.75" stroke-linejoin="round" stroke-linecap="round" />
		<circle cx={pts[pts.length - 1][0]} cy={pts[pts.length - 1][1]} r="2.5" fill={color} />
	</svg>
{/if}
