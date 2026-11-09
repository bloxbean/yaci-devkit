<script lang="ts">
	import { toasts } from './toast.svelte';
	import Check from '@lucide/svelte/icons/check';
	import CircleAlert from '@lucide/svelte/icons/circle-alert';
</script>

<div class="pointer-events-none fixed bottom-5 left-1/2 z-[80] flex -translate-x-1/2 flex-col items-center gap-2" aria-live="polite">
	{#each toasts.items as t (t.id)}
		<div
			class="pointer-events-auto flex animate-fade-up items-center gap-2 rounded-full border border-line bg-surface/95 px-4 py-2 text-sm font-medium text-ink shadow-card backdrop-blur"
		>
			{#if t.tone === 'error'}
				<CircleAlert class="h-4 w-4 text-danger" />
			{:else}
				<Check class="h-4 w-4 text-mint" />
			{/if}
			{t.text}
		</div>
	{/each}
</div>
