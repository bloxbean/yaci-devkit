<script lang="ts">
	import Copy from '@lucide/svelte/icons/copy';
	import Check from '@lucide/svelte/icons/check';
	import { copyText } from './toast.svelte';

	let { value, label = 'Copied', size = 'sm' }: { value: string; label?: string; size?: 'xs' | 'sm' } = $props();
	let done = $state(false);

	async function copy(e: MouseEvent) {
		e.preventDefault();
		e.stopPropagation();
		if (await copyText(value, label)) {
			done = true;
			setTimeout(() => (done = false), 1400);
		}
	}
</script>

<button
	type="button"
	onclick={copy}
	class="inline-grid shrink-0 place-items-center rounded-md text-faint transition hover:bg-surface-2 hover:text-ink {size === 'xs' ? 'h-5 w-5' : 'h-6 w-6'}"
	aria-label="Copy {value}"
	title="Copy"
>
	{#if done}
		<Check class="{size === 'xs' ? 'h-3 w-3' : 'h-3.5 w-3.5'} text-mint" />
	{:else}
		<Copy class={size === 'xs' ? 'h-3 w-3' : 'h-3.5 w-3.5'} />
	{/if}
</button>
