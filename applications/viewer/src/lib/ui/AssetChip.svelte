<script lang="ts">
	import { num, parseUnit, tokenAmount } from '$lib/format';
	import { tokenName } from '$lib/names/names.svelte';

	/*
	 * A native asset: coloured by policy, readable name when the hex decodes, links to the asset page. On Mainnet a
	 * token in the Cardano token registry shows its ticker, and its quantity with the registered decimals.
	 */
	let { unit, quantity = undefined, link = true }: { unit: string; quantity?: string | number; link?: boolean } = $props();

	const registered = $derived(tokenName(unit));
	const parsed = $derived(parseUnit(unit));
	const info = $derived(registered?.ticker ? { ...parsed, name: registered.ticker } : parsed);
	const decimals = $derived(registered?.decimals ?? 0);
	const amount = (q: string) => (decimals ? tokenAmount(q, decimals) : num(q));
	const hue = $derived(parseInt(info.policy.slice(0, 6) || '0', 16) % 360);
</script>

<svelte:element
	this={link ? 'a' : 'span'}
	href={link ? `/assets/unit/${unit}` : undefined}
	class="inline-flex max-w-full items-center gap-1.5 rounded-lg border border-line bg-surface-2/70 py-0.5 pl-1 pr-2 text-xs transition hover:border-brand/40"
	title="{registered?.name ? `${registered.name} (${registered.ticker ?? parsed.name}) · ` : `${info.name} · `}{unit}"
>
	<span class="grid h-4 w-4 shrink-0 place-items-center rounded-md text-[9px] font-bold text-white" style="background:hsl({hue} 70% 50%)"
		>{info.name.slice(0, 1).toUpperCase()}</span
	>
	<span class="truncate font-medium">{info.name}</span>
	{#if info.label}<span class="shrink-0 rounded bg-surface px-1 text-[9px] font-semibold uppercase text-faint" title="CIP-68 {info.label}">{info.label}</span>{/if}
	{#if quantity != null}
		{@const q = String(quantity)}
		<span class="mono shrink-0 {q.startsWith('-') ? 'text-danger' : q.startsWith('+') ? 'text-mint' : 'text-muted'}"
			>{q.startsWith('-') || q.startsWith('+') ? q[0] + amount(q.slice(1)) : '×' + amount(q)}</span
		>
	{/if}
</svelte:element>
