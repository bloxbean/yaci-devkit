<script lang="ts">
	import { num, parseUnit } from '$lib/format';

	/* A native asset: coloured by policy, readable name when the hex decodes, links to the asset page. */
	let { unit, quantity = undefined, link = true }: { unit: string; quantity?: string | number; link?: boolean } = $props();

	const info = $derived(parseUnit(unit));
	const hue = $derived(parseInt(info.policy.slice(0, 6) || '0', 16) % 360);
</script>

<svelte:element
	this={link ? 'a' : 'span'}
	href={link ? `/assets/unit/${unit}` : undefined}
	class="inline-flex max-w-full items-center gap-1.5 rounded-lg border border-line bg-surface-2/70 py-0.5 pl-1 pr-2 text-xs transition hover:border-brand/40"
	title="{info.name} · {unit}"
>
	<span class="grid h-4 w-4 shrink-0 place-items-center rounded-md text-[9px] font-bold text-white" style="background:hsl({hue} 70% 50%)"
		>{info.name.slice(0, 1).toUpperCase()}</span
	>
	<span class="truncate font-medium">{info.name}</span>
	{#if quantity != null}<span class="mono shrink-0 text-muted">×{num(quantity)}</span>{/if}
</svelte:element>
