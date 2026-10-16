<script lang="ts">
	import type { Snippet } from 'svelte';

	/* One labelled fact in a details grid. */
	let { label, children, wide = false }: { label: string; children: Snippet; wide?: boolean } = $props();
</script>

<div class="min-w-0 {wide ? 'sm:col-span-2' : ''}">
	<dt class="y-label mb-1">{label}</dt>
	<dd class="min-w-0 break-words text-sm text-ink">{@render children()}</dd>
</div>
