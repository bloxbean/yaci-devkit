<script lang="ts">
	import { page as pageState } from '$app/state';
	import ChevronLeft from '@lucide/svelte/icons/chevron-left';
	import ChevronRight from '@lucide/svelte/icons/chevron-right';
	import ChevronsLeft from '@lucide/svelte/icons/chevrons-left';

	/*
	 * Page links that keep the rest of the query string. Yaci Store rarely returns totals, so "next" is enabled when
	 * the page came back full.
	 */
	let {
		page,
		hasNext,
		totalPages = undefined,
		param = 'page'
	}: { page: number; hasNext: boolean; totalPages?: number | null; param?: string } = $props();

	function href(p: number) {
		const u = new URL(pageState.url);
		if (p <= 1) u.searchParams.delete(param);
		else u.searchParams.set(param, String(p));
		return u.pathname + u.search;
	}
</script>

<nav class="flex items-center justify-between gap-3 border-t border-line px-4 py-3 text-sm" aria-label="Pagination">
	<span class="text-muted">
		Page <b class="font-semibold text-ink tabular-nums">{page.toLocaleString()}</b>{#if totalPages}{' '}of {totalPages.toLocaleString()}{/if}
	</span>
	<div class="flex items-center gap-1.5">
		<a class="y-icon-btn h-8 w-8 {page <= 1 ? 'pointer-events-none opacity-35' : ''}" href={href(1)} aria-label="First page" data-sveltekit-noscroll
			><ChevronsLeft class="h-4 w-4" /></a
		>
		<a class="y-btn h-8 px-2.5 {page <= 1 ? 'pointer-events-none opacity-35' : ''}" href={href(page - 1)} data-sveltekit-noscroll
			><ChevronLeft class="h-4 w-4" /> Prev</a
		>
		<a class="y-btn h-8 px-2.5 {hasNext ? '' : 'pointer-events-none opacity-35'}" href={href(page + 1)} data-sveltekit-noscroll
			>Next <ChevronRight class="h-4 w-4" /></a
		>
	</div>
</nav>
