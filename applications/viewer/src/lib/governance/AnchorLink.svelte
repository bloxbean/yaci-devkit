<script lang="ts">
	import { shorten } from '$lib/format';
	import FileText from '@lucide/svelte/icons/file-text';
	import { anchorHref } from './labels';

	/* An on-chain anchor: open the document in the viewer, with the raw URL on hover. */
	let { url, hash = undefined }: { url: string | null | undefined; hash?: string | null } = $props();
</script>

{#if url}
	<a href={anchorHref(url, hash)} class="inline-flex min-w-0 max-w-full items-center gap-1.5 text-brand hover:underline" title={url}>
		<FileText class="h-3.5 w-3.5 shrink-0" />
		<span class="truncate">{shorten(url.replace(/^https?:\/\//, ''), 26, 16)}</span>
	</a>
{:else}
	<span class="text-faint">None</span>
{/if}
