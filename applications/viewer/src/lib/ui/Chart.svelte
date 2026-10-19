<script lang="ts">
	/*
	 * A bar or area chart over a series (days, epochs), drawn left (oldest) to right (newest), with the largest value on
	 * the axis and the hovered point's label and value in a tooltip. Plain SVG that stretches to its container's width.
	 */
	let {
		points,
		kind = 'bar',
		color = 'rgb(var(--c-brand))',
		height = 120,
		format = (v: number) => v.toLocaleString('en-US'),
		label = 'chart'
	}: {
		points: { label: string; value: number }[];
		kind?: 'bar' | 'area';
		color?: string;
		height?: number;
		format?: (v: number) => string;
		/** What the chart shows, for screen readers. */
		label?: string;
	} = $props();

	const W = 600;
	const id = `ch${Math.random().toString(36).slice(2, 8)}`;
	const max = $derived(Math.max(0, ...points.map((p) => (Number.isFinite(p.value) ? p.value : 0))) || 1);
	const y = (v: number) => height - 2 - (Math.max(0, v) / max) * (height - 6);
	const step = $derived(W / Math.max(1, points.length));
	const line = $derived(
		points.map((p, i) => `${i ? 'L' : 'M'}${(points.length === 1 ? W / 2 : (i * W) / (points.length - 1)).toFixed(1)},${y(p.value).toFixed(1)}`).join(' ')
	);

	let hover = $state<number | null>(null);
	function move(e: PointerEvent) {
		const r = (e.currentTarget as SVGElement).getBoundingClientRect();
		hover = Math.min(points.length - 1, Math.max(0, Math.floor(((e.clientX - r.left) / r.width) * points.length)));
	}
	const tipLeft = $derived(hover == null ? 0 : ((hover + 0.5) / points.length) * 100);
</script>

{#if points.length}
	<div class="relative select-none pt-4">
		<div class="mono pointer-events-none absolute left-0 top-0 text-[10px] leading-4 text-faint tabular-nums">{format(max)}</div>
		<svg
			viewBox="0 0 {W} {height}"
			preserveAspectRatio="none"
			class="block w-full"
			style="height:{height}px"
			role="img"
			aria-label={label}
			onpointermove={move}
			onpointerleave={() => (hover = null)}
		>
			<defs>
				<linearGradient id={id} x1="0" x2="0" y1="0" y2="1">
					<stop offset="0%" stop-color={color} stop-opacity="0.3" />
					<stop offset="100%" stop-color={color} stop-opacity="0" />
				</linearGradient>
			</defs>
			<line x1="0" x2={W} y1={y(max)} y2={y(max)} stroke="currentColor" class="text-line" stroke-dasharray="3 4" vector-effect="non-scaling-stroke" />
			{#if kind === 'bar'}
				{#each points as p, i (i)}
					<rect
						x={i * step + step * 0.12}
						y={y(p.value)}
						width={step * 0.76}
						height={Math.max(0, height - 2 - y(p.value))}
						fill={color}
						opacity={hover == null || hover === i ? 0.85 : 0.35}
						rx="1"
					/>
				{/each}
			{:else}
				<path d="{line} L{W},{height} L0,{height} Z" fill="url(#{id})" />
				<path d={line} fill="none" stroke={color} stroke-width="1.75" vector-effect="non-scaling-stroke" stroke-linejoin="round" />
				{#if hover != null}
					<line
						x1={points.length === 1 ? W / 2 : (hover * W) / (points.length - 1)}
						x2={points.length === 1 ? W / 2 : (hover * W) / (points.length - 1)}
						y1="0"
						y2={height}
						stroke={color}
						stroke-opacity="0.4"
						vector-effect="non-scaling-stroke"
					/>
				{/if}
			{/if}
		</svg>
		<div class="mono mt-1 flex justify-between text-[10px] text-faint">
			<span>{points[0].label}</span>
			<span>{points[points.length - 1].label}</span>
		</div>
		{#if hover != null}
			<div
				class="pointer-events-none absolute -top-2 z-10 -translate-x-1/2 -translate-y-full whitespace-nowrap rounded-lg border border-line bg-surface px-2 py-1 text-xs shadow-sm"
				style="left:clamp(3rem, {tipLeft}%, calc(100% - 3rem))"
			>
				<span class="text-muted">{points[hover].label}</span>
				<span class="mono ml-1.5 font-semibold tabular-nums">{format(points[hover].value)}</span>
			</div>
		{/if}
	</div>
{/if}
