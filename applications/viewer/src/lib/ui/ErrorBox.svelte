<script lang="ts">
	import CircleAlert from '@lucide/svelte/icons/circle-alert';
	import RotateCw from '@lucide/svelte/icons/rotate-cw';
	import { invalidateAll } from '$app/navigation';

	let {
		title = 'Something went wrong',
		message = undefined,
		retry = true
	}: { title?: string; message?: string | null; retry?: boolean } = $props();
</script>

<div class="flex flex-col items-center justify-center gap-3 px-6 py-14 text-center">
	<div class="grid h-12 w-12 place-items-center rounded-2xl border border-danger/25 bg-danger/10 text-danger">
		<CircleAlert class="h-5 w-5" />
	</div>
	<div>
		<p class="font-display text-base font-semibold">{title}</p>
		{#if message}<p class="mx-auto mt-1 max-w-md text-sm text-muted">{message}</p>{/if}
	</div>
	{#if retry}
		<button class="y-btn" onclick={() => invalidateAll()}><RotateCw class="h-4 w-4" /> Try again</button>
	{/if}
</div>
