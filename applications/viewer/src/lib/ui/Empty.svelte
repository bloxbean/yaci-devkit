<script lang="ts">
	import type { Snippet } from 'svelte';
	import Inbox from '@lucide/svelte/icons/inbox';

	let {
		title = 'Nothing here yet',
		text = undefined,
		children = undefined
	}: { title?: string; text?: string; children?: Snippet } = $props();
</script>

<div class="flex flex-col items-center justify-center gap-3 px-6 py-14 text-center">
	<div class="grid h-12 w-12 place-items-center rounded-2xl border border-line bg-surface-2 text-faint">
		<Inbox class="h-5 w-5" />
	</div>
	<div>
		<p class="font-display text-base font-semibold">{title}</p>
		{#if text}<p class="mx-auto mt-1 max-w-md text-sm text-muted">{text}</p>{/if}
	</div>
	{#if children}{@render children()}{/if}
</div>
