<script lang="ts">
	import { chain, type LiveBlock } from '$lib/live/chain.svelte';
	import { num, timeAgo } from '$lib/format';
	import Box from '@lucide/svelte/icons/box';

	/*
	 * The newest blocks as a moving chain: a "forging" slot counts down to the expected next block, new blocks slide
	 * in with a glow, and each block's fill shows how full it is.
	 */
	let { blocks, maxBlockSize }: { blocks: LiveBlock[]; maxBlockSize: number } = $props();

	const shown = $derived(blocks.slice(0, 12));
	const tip = $derived(shown[0]);
	const interval = $derived(chain.blockTime ?? 20);
	const elapsed = $derived(tip ? Math.max(0, chain.now / 1000 - tip.time) : 0);
	const progress = $derived(Math.min(1, elapsed / interval));
	const overdue = $derived(elapsed > interval * 1.6);

	const fill = (b: LiveBlock) => Math.max(0.04, Math.min(1, b.size / maxBlockSize));
	// Only the newest block glows, for a moment, so a fast devnet doesn't light up the whole row.
	const isNew = (b: LiveBlock) => b === shown[0] && b.receivedAt > 0 && chain.now - b.receivedAt < Math.min(2500, interval * 900);
	const kb = (bytes: number) => (bytes >= 1024 ? `${(bytes / 1024).toFixed(1)} KB` : `${bytes} B`);
</script>

<div class="relative -mx-1 overflow-x-auto px-1 pb-1 [mask-image:linear-gradient(90deg,#000_88%,transparent)] md:overflow-hidden">
	<ol class="flex items-stretch gap-3">
		{#if tip}
			<!-- The block being forged -->
			<li class="w-[148px] shrink-0">
				<div class="relative flex h-full flex-col justify-between rounded-2xl border-2 border-dashed border-brand/35 bg-brand/[0.04] p-3.5">
					<div class="flex items-center justify-between">
						<span class="y-label text-brand">Next</span>
						<span class="relative flex h-2 w-2"
							><span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-brand opacity-60"></span><span
								class="relative inline-flex h-2 w-2 rounded-full bg-brand"
							></span></span
						>
					</div>
					<div class="mono mt-2 text-[15px] font-semibold tabular-nums text-muted">#{num(tip.number + 1)}</div>
					<div class="mt-3">
						<div class="h-1.5 overflow-hidden rounded-full bg-line">
							<div class="h-full rounded-full bg-grad transition-[width] duration-1000 ease-linear" style="width:{progress * 100}%"></div>
						</div>
						<div class="mt-2 text-[11px] text-faint">
							{#if overdue}waiting for a slot leader…{:else if interval - elapsed < 1}any moment…{:else}~{Math.ceil(interval - elapsed)}s · forging{/if}
						</div>
					</div>
				</div>
			</li>
		{/if}

		{#each shown as b, i (b.number)}
			<li class="relative w-[148px] shrink-0 {isNew(b) ? 'animate-slide-in' : ''}" style="opacity:{1 - i * 0.045}">
				<!-- the link to the previous block -->
				<span class="absolute -left-3 top-1/2 h-px w-3 bg-line" aria-hidden="true"></span>
				<a
					href="/blocks/{b.number}"
					class="group relative flex h-full flex-col overflow-hidden rounded-2xl border bg-surface p-3.5 transition hover:-translate-y-0.5 hover:border-brand/50 {isNew(b)
						? 'border-brand/60 shadow-glow'
						: 'border-line'}"
				>
					<!-- fill level = how full the block is -->
					<span
						class="pointer-events-none absolute inset-x-0 bottom-0 bg-gradient-to-t from-brand/[0.16] to-brand/[0.02] transition-[height] duration-700"
						style="height:{fill(b) * 100}%"
						aria-hidden="true"
					></span>
					<div class="relative flex items-center justify-between">
						<Box class="h-4 w-4 {isNew(b) ? 'text-brand' : 'text-faint group-hover:text-brand'}" />
						<span class="text-[11px] text-faint">{timeAgo(b.time, chain.now)}</span>
					</div>
					<div class="mono relative mt-2 text-[15px] font-semibold tabular-nums">#{num(b.number)}</div>
					<div class="relative mt-3 flex items-center justify-between text-[11.5px]">
						<span class="{b.txCount ? 'font-semibold text-ink' : 'text-faint'}">{b.txCount} tx{b.txCount === 1 ? '' : 's'}</span>
						<span class="text-faint">{kb(b.size)}</span>
					</div>
				</a>
			</li>
		{/each}
	</ol>
</div>
