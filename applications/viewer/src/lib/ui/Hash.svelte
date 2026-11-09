<script lang="ts">
	import { shorten } from '$lib/format';
	import CopyButton from './CopyButton.svelte';

	/*
	 * A hash, address or id: middle-truncated, monospace, with the full value on hover, an optional link and a copy
	 * button. `full` shows it whole and lets it wrap.
	 */
	let {
		value,
		href = undefined,
		head = 10,
		tail = 8,
		full = false,
		copy = true,
		class: klass = ''
	}: {
		value: string | null | undefined;
		href?: string;
		head?: number;
		tail?: number;
		full?: boolean;
		copy?: boolean;
		class?: string;
	} = $props();

	const text = $derived(full ? (value ?? '') : shorten(value, head, tail));
</script>

{#if value}
	<span class="inline-flex min-w-0 max-w-full items-center gap-1 align-middle {klass}">
		{#if href}
			<a {href} class="mono min-w-0 text-[0.92em] text-brand hover:underline {full ? 'break-all' : 'truncate'}" title={value}>{text}</a>
		{:else}
			<span class="mono min-w-0 text-[0.92em] {full ? 'break-all' : 'truncate'}" title={value}>{text}</span>
		{/if}
		{#if copy}<CopyButton {value} size="xs" />{/if}
	</span>
{:else}
	<span class="text-faint">—</span>
{/if}
