<script lang="ts">
	import { chain, type LiveTx } from '$lib/live/chain.svelte';
	import { shorten, num } from '$lib/format';
	import Ada from '$lib/ui/Ada.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import ArrowLeftRight from '@lucide/svelte/icons/arrow-left-right';

	/* Latest transactions; new arrivals flash. Time comes from the block they landed in. */
	let { txs }: { txs: LiveTx[] } = $props();

	const blockTime = $derived(new Map(chain.blocks.map((b) => [b.number, b.time])));
	const isNew = (t: LiveTx) => t.receivedAt > 0 && chain.now - t.receivedAt < 3000;
</script>

{#if txs.length === 0}
	<Empty title="No transactions yet" text="Submit one to your devnet and it shows up here as soon as it lands in a block." />
{:else}
	<ul class="divide-y divide-line/70">
		{#each txs.slice(0, 10) as t (t.hash)}
			<li class={isNew(t) ? 'animate-flash' : ''}>
				<a href="/transactions/{t.hash}" class="group flex items-center gap-3 px-5 py-3 transition-colors hover:bg-surface-2/70">
					<span class="grid h-9 w-9 shrink-0 place-items-center rounded-xl border border-line bg-surface-2 text-faint transition group-hover:border-brand/40 group-hover:text-brand">
						<ArrowLeftRight class="h-4 w-4" />
					</span>
					<span class="min-w-0 flex-1">
						<span class="mono block truncate text-[13px] font-medium text-ink group-hover:text-brand">{shorten(t.hash, 12, 10)}</span>
						<span class="block truncate text-xs text-muted">
							{t.block < 0 ? 'Genesis' : `Block #${num(t.block)}`} · {t.outputAddresses.length} output address{t.outputAddresses.length === 1 ? '' : 'es'}
						</span>
					</span>
					<span class="shrink-0 text-right">
						<Ada value={t.output} class="block text-[13px] font-semibold" />
						<span class="block text-[11px] text-faint">
							{#if blockTime.get(t.block)}<TimeAgo ts={blockTime.get(t.block)} />{:else if t.slot >= 0}slot {num(t.slot)}{:else}genesis{/if}
						</span>
					</span>
				</a>
			</li>
		{/each}
	</ul>
{/if}
