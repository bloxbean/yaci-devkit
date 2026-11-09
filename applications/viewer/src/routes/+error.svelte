<script lang="ts">
	import { page } from '$app/state';
	import { shell } from '$lib/shell/ui.svelte';
	import Search from '@lucide/svelte/icons/search';
	import House from '@lucide/svelte/icons/house';
	import CircleAlert from '@lucide/svelte/icons/circle-alert';
</script>

<svelte:head><title>{page.status === 404 ? 'Not found' : 'Error'} · Yaci Viewer</title></svelte:head>

<div class="mx-auto flex max-w-xl flex-col items-center py-20 text-center">
	<div class="mb-6 grid h-16 w-16 place-items-center rounded-3xl border border-line bg-surface text-brand shadow-card">
		<CircleAlert class="h-7 w-7" />
	</div>
	<div class="y-label mb-2 text-brand">{page.status}</div>
	<h1 class="text-3xl font-semibold">{page.status === 404 ? "We couldn't find that" : 'Something went wrong'}</h1>
	<p class="mt-3 text-muted">
		{page.error?.message ??
			(page.status === 404 ? 'It may not be on this chain yet, or the store has not indexed it.' : 'Yaci Store returned an error.')}
	</p>
	<div class="mt-8 flex flex-wrap justify-center gap-3">
		<button class="y-btn y-btn-primary" onclick={() => shell.openPalette()}><Search class="h-4 w-4" /> Search again</button>
		<a class="y-btn" href="/"><House class="h-4 w-4" /> Dashboard</a>
	</div>
</div>
