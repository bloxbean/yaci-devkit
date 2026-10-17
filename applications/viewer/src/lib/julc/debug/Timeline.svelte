<script lang="ts">
	import { num } from '$lib/format';

	/*
	 * The whole run as a bar: trace markers (amber), the failing step (red) and the current step. Click or drag to jump.
	 * Seeking replays from step 0 in the engine, which takes milliseconds, so dragging is live.
	 */
	let {
		total,
		current,
		traces = [],
		error = null,
		onseek
	}: { total: number; current: number; traces?: number[]; error?: number | null; onseek: (step: number) => void } = $props();

	let track = $state<HTMLDivElement>();
	let dragging = $state(false);
	let hover = $state<number | null>(null);

	const at = (step: number) => (total <= 1 ? 0 : (step / (total - 1)) * 100);
	const stepAt = (clientX: number) => {
		const r = track!.getBoundingClientRect();
		return Math.round(Math.min(1, Math.max(0, (clientX - r.left) / r.width)) * Math.max(0, total - 1));
	};

	function down(e: PointerEvent) {
		dragging = true;
		track!.setPointerCapture(e.pointerId);
		onseek(stepAt(e.clientX));
	}
	function move(e: PointerEvent) {
		hover = stepAt(e.clientX);
		if (dragging) onseek(hover);
	}
	// Many traces would draw a solid bar; thin them to one marker per pixel-ish bucket.
	const traceMarks = $derived(traces.length > 400 ? traces.filter((_, i) => i % Math.ceil(traces.length / 400) === 0) : traces);
</script>

<div class="select-none">
	<div
		bind:this={track}
		class="relative h-9 cursor-pointer rounded-xl border border-line bg-bg-2"
		role="slider"
		tabindex="-1"
		aria-label="Step"
		aria-valuemin={0}
		aria-valuemax={Math.max(0, total - 1)}
		aria-valuenow={current}
		onpointerdown={down}
		onpointermove={move}
		onpointerup={() => (dragging = false)}
		onpointerleave={() => (hover = null)}
	>
		<div class="absolute inset-y-0 left-0 rounded-l-xl bg-brand/10" style="width:{at(current)}%"></div>
		{#each traceMarks as t}<span class="absolute top-1 h-2.5 w-0.5 rounded-full bg-amber" style="left:{at(t)}%" title="Trace at step {num(t)}"></span>{/each}
		{#if error != null}<span class="absolute inset-y-1 w-1 -translate-x-1/2 rounded-full bg-danger" style="left:{at(error)}%" title="Failure at step {num(error)}"></span>{/if}
		<span class="absolute inset-y-0 w-0.5 -translate-x-1/2 bg-brand shadow-glow" style="left:{at(current)}%">
			<span class="absolute -top-1 left-1/2 h-2.5 w-2.5 -translate-x-1/2 rounded-full border-2 border-surface bg-brand"></span>
		</span>
		{#if hover != null && !dragging}
			<span class="pointer-events-none absolute -top-7 -translate-x-1/2 whitespace-nowrap rounded-md bg-ink px-1.5 py-0.5 text-[11px] tabular-nums text-bg" style="left:{at(hover)}%">{num(hover)}</span>
		{/if}
	</div>
	<div class="mt-1.5 flex justify-between text-[11px] tabular-nums text-faint">
		<span>0</span>
		<span class="flex gap-3">
			{#if traces.length}<span class="flex items-center gap-1"><span class="h-2 w-0.5 rounded-full bg-amber"></span>{traces.length} trace{traces.length === 1 ? '' : 's'}</span>{/if}
			{#if error != null}<span class="flex items-center gap-1"><span class="h-2 w-1 rounded-full bg-danger"></span>failure at {num(error)}</span>{/if}
		</span>
		<span>{num(Math.max(0, total - 1))}</span>
	</div>
</div>
