<script lang="ts">
	import { VOTER_LABEL, type Tally, type VoterGroup } from './labels';

	/*
	 * Votes cast by each body, as one stacked bar per body (yes / no / abstain), with the store's approval ratio when
	 * it has one. Counts are votes cast, not stake.
	 */
	let {
		tally,
		ratios = {},
		partial = false
	}: { tally: Record<VoterGroup, Tally>; ratios?: Partial<Record<VoterGroup, number | null>>; partial?: boolean } = $props();

	const groups: VoterGroup[] = ['drep', 'spo', 'cc'];
	const total = (t: Tally) => t.yes + t.no + t.abstain;
	const pct = (n: number, t: Tally) => (total(t) ? (n / total(t)) * 100 : 0);
</script>

<div class="flex flex-col gap-5">
	{#each groups as g (g)}
		{@const t = tally[g]}
		<div>
			<div class="mb-1.5 flex items-baseline justify-between gap-3 text-sm">
				<span class="font-medium">{VOTER_LABEL[g]}</span>
				<span class="text-xs text-muted">
					{#if ratios[g] != null}<span class="font-semibold text-ink">{(Number(ratios[g]) * 100).toFixed(1)}%</span> approval ·{/if}
					{total(t)} vote{total(t) === 1 ? '' : 's'}
				</span>
			</div>
			<div class="flex h-2.5 overflow-hidden rounded-full bg-line" role="img" aria-label="{VOTER_LABEL[g]}: {t.yes} yes, {t.no} no, {t.abstain} abstain">
				{#if total(t)}
					<div class="h-full bg-mint transition-[width] duration-700" style="width:{pct(t.yes, t)}%"></div>
					<div class="h-full bg-danger transition-[width] duration-700" style="width:{pct(t.no, t)}%"></div>
					<div class="h-full bg-faint/60 transition-[width] duration-700" style="width:{pct(t.abstain, t)}%"></div>
				{/if}
			</div>
			<div class="mt-1.5 flex gap-4 text-[11px] text-muted">
				<span class="flex items-center gap-1"><span class="h-1.5 w-1.5 rounded-full bg-mint"></span>Yes {t.yes}</span>
				<span class="flex items-center gap-1"><span class="h-1.5 w-1.5 rounded-full bg-danger"></span>No {t.no}</span>
				<span class="flex items-center gap-1"><span class="h-1.5 w-1.5 rounded-full bg-faint/60"></span>Abstain {t.abstain}</span>
			</div>
		</div>
	{/each}
	{#if partial}<p class="text-xs text-faint">Counted from the most recent votes only; this action has more.</p>{/if}
</div>
