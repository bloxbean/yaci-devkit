<script lang="ts">
	import type { Snippet } from 'svelte';
	import Hourglass from '@lucide/svelte/icons/hourglass';

	/*
	 * In place of a list Yaci Store is too slow to return (bloxbean/yaci-store#1269): it timed out, so the viewer
	 * won't ask again for a while ('heavy'), or the viewer is already waiting on other slow calls ('busy'). `bounded`:
	 * the store stops these queries itself (its Blockfrost-compatible API, 15 s), so trying again is fine.
	 */
	let { reason, bounded = false, children = undefined }: { reason: 'heavy' | 'busy'; bounded?: boolean; children?: Snippet } = $props();
</script>

<div class="flex flex-col items-center justify-center gap-3 px-6 py-14 text-center" data-heavy-notice={reason}>
	<div class="grid h-12 w-12 place-items-center rounded-2xl border border-amber/25 bg-amber/10 text-amber"><Hourglass class="h-5 w-5" /></div>
	<div>
		<p class="font-display text-base font-semibold">{reason === 'busy' ? 'Yaci Store is busy' : 'This list is too slow on this Yaci Store'}</p>
		<p class="mx-auto mt-1 max-w-lg text-sm text-muted">
			{#if reason === 'busy'}
				The viewer is already waiting on other slow requests. Try again in a minute.
			{:else if bounded}
				Yaci Store gave up after 15 seconds. A second try is often quicker, once the store has this address’s data at hand.
			{:else}
				Yaci Store didn’t answer in time. With a long history it can take hours
				(<a class="y-link" href="https://github.com/bloxbean/yaci-store/issues/1269" target="_blank" rel="noopener noreferrer">yaci-store#1269</a>), so the viewer won’t ask again for a while.
			{/if}
		</p>
	</div>
	{#if children}{@render children()}{/if}
</div>
