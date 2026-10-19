<script lang="ts">
	import type { DRepPower } from '$lib/analytics/types';
	import { adaCompact, num, percent, shorten } from '$lib/format';
	import AnalyticsNote from '$lib/ui/AnalyticsNote.svelte';

	/* Who holds the voting power: the DReps (and the abstain / no-confidence options) with the most delegated stake. */
	let { power }: { power: DRepPower } = $props();

	const special: Record<string, string> = { ABSTAIN: 'Always abstain', NO_CONFIDENCE: 'Always no confidence' };
	const max = $derived(Math.max(1, ...power.top.map((d) => d.amount)));
	const shown = $derived(power.top.reduce((t, d) => t + d.amount, 0));
</script>

<section class="y-panel mt-6 overflow-hidden" data-insights="drep-power">
	<div class="flex flex-wrap items-center justify-between gap-2 border-b border-line px-5 py-3">
		<div>
			<h2 class="font-medium">Voting power</h2>
			<p class="text-xs text-muted">
				The 10 largest of {num(power.dreps)} DReps and options in epoch {power.epoch}: {percent(shown, power.total, 0)} of ₳{adaCompact(power.total)} delegated.
			</p>
		</div>
		<AnalyticsNote source={power} compact />
	</div>
	<ol class="divide-y divide-line/70">
		{#each power.top as d, i (d.drep_id ?? d.drep_type)}
			<li class="flex items-center gap-3 px-5 py-2.5 text-sm">
				<span class="w-5 shrink-0 text-right text-xs text-faint tabular-nums">{i + 1}</span>
				{#if d.drep_id}
					<a class="mono w-48 shrink-0 truncate text-ink hover:text-brand" href="/governance/dreps/{d.drep_id}" title={d.drep_id}>{shorten(d.drep_id, 12, 6)}</a>
				{:else}
					<span class="w-48 shrink-0 truncate font-medium">{special[d.drep_type] ?? d.drep_type}</span>
				{/if}
				<span class="hidden h-1.5 min-w-0 flex-1 overflow-hidden rounded-full bg-bg-2 sm:block">
					<span class="block h-full rounded-full {d.drep_id ? 'bg-brand/70' : 'bg-faint/60'}" style="width:{(d.amount / max) * 100}%"></span>
				</span>
				<span class="ml-auto shrink-0 font-semibold tabular-nums">₳{adaCompact(d.amount)}</span>
				<span class="w-14 shrink-0 text-right text-xs text-muted tabular-nums">{percent(d.amount, power.total)}</span>
			</li>
		{/each}
	</ol>
</section>
