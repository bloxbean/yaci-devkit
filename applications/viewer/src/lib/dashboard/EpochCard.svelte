<script lang="ts">
	import { chain } from '$lib/live/chain.svelte';
	import { dateTime, duration, num } from '$lib/format';
	import Ring from '$lib/ui/Ring.svelte';

	/* Where we are in the current epoch, with the time left. */
	let { epoch }: { epoch: { epoch: number; start: number; end: number } | null } = $props();

	const now = $derived(chain.now / 1000);
	const e = $derived(chain.epoch ?? epoch);
	const span = $derived(e ? e.end - e.start : 0);
	const progress = $derived(e && span > 0 ? Math.min(1, Math.max(0, (now - e.start) / span)) : 0);
	const left = $derived(e ? Math.max(0, e.end - now) : 0);
</script>

<div class="flex h-full items-center gap-5">
	<Ring value={progress} size={128} stroke={11}>
		<div>
			<div class="font-display text-2xl font-semibold tabular-nums">{(progress * 100).toFixed(1)}<span class="text-sm text-muted">%</span></div>
			<div class="text-[11px] text-faint">of epoch</div>
		</div>
	</Ring>
	<div class="min-w-0 flex-1">
		<div class="y-label">Epoch</div>
		<a href={e ? `/rewards/epoch/${e.epoch}` : undefined} class="font-display text-3xl font-semibold tabular-nums hover:text-brand">{e ? num(e.epoch) : '—'}</a>
		<dl class="mt-3 grid grid-cols-2 gap-x-4 gap-y-2 text-xs">
			<div>
				<dt class="text-faint">Ends in</dt>
				<dd class="font-medium tabular-nums text-ink">{e ? duration(left) : '—'}</dd>
			</div>
			<div>
				<dt class="text-faint">Slot in epoch</dt>
				<dd class="font-medium tabular-nums text-ink">
					{#if chain.tip?.epochSlot != null && span}{num(chain.tip.epochSlot)} / {num(span)}{:else}—{/if}
				</dd>
			</div>
			<div class="col-span-2">
				<dt class="text-faint">Ends</dt>
				<dd class="truncate font-medium text-ink">{e ? dateTime(e.end) : '—'}</dd>
			</div>
		</dl>
	</div>
</div>
