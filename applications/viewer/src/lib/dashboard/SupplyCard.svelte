<script lang="ts">
	import { adaCompact, percent } from '$lib/format';

	/* Where the 45B ADA sits: circulating, reserves and treasury, as one bar. */
	let { network }: { network: { supply?: Record<string, number>; stake?: { active?: number } } | null } = $props();

	const s = $derived(network?.supply);
	const max = $derived(Number(s?.max) || 0);
	const parts = $derived(
		s && max
			? [
					{ label: 'Circulating', value: Number(s.circulating), color: 'bg-brand', dot: 'bg-brand' },
					{ label: 'Reserves', value: Number(s.reserves), color: 'bg-violet', dot: 'bg-violet' },
					{ label: 'Treasury', value: Number(s.treasury), color: 'bg-cyan', dot: 'bg-cyan' }
				]
			: []
	);
	const active = $derived(Number(network?.stake?.active) || 0);
</script>

{#if parts.length}
	<div class="flex h-3 overflow-hidden rounded-full bg-line">
		{#each parts as p (p.label)}
			<div class="{p.color} h-full transition-[width] duration-700" style="width:{(p.value / max) * 100}%" title="{p.label} {percent(p.value, max)}"></div>
		{/each}
	</div>
	<dl class="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
		{#each parts as p (p.label)}
			<div>
				<dt class="flex items-center gap-1.5 text-xs text-muted"><span class="h-2 w-2 rounded-full {p.dot}"></span>{p.label}</dt>
				<dd class="mt-0.5 font-display text-lg font-semibold tabular-nums">₳{adaCompact(p.value)}</dd>
				<dd class="text-[11px] text-faint">{percent(p.value, max)} of max</dd>
			</div>
		{/each}
		<div>
			<dt class="flex items-center gap-1.5 text-xs text-muted"><span class="h-2 w-2 rounded-full bg-mint"></span>Active stake</dt>
			<dd class="mt-0.5 font-display text-lg font-semibold tabular-nums">₳{adaCompact(active)}</dd>
			<dd class="text-[11px] text-faint">{percent(active, Number(s?.circulating) || 1)} of circulating</dd>
		</div>
	</dl>
{:else}
	<p class="text-sm text-muted">Supply figures appear after the first epoch boundary.</p>
{/if}
