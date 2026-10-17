<script lang="ts">
	import { adaParts, type Amountish } from '$lib/format';

	/* Lovelace shown as ADA, with the decimals dimmed so the whole part reads first. */
	let {
		value,
		decimals = 2,
		symbol = true,
		class: klass = ''
	}: { value: Amountish; decimals?: number; symbol?: boolean; class?: string } = $props();

	const p = $derived(adaParts(value));
	const frac = $derived(decimals > 0 ? p.frac.slice(0, decimals) : '');
</script>

<span class="whitespace-nowrap tabular-nums {klass}" title="{p.neg ? '−' : ''}{p.whole}.{p.frac} ADA"
	>{#if symbol}<span class="mr-0.5 text-faint">₳</span>{/if}{p.neg ? '−' : ''}{p.whole}{#if frac}<span class="text-faint"
			>.{frac}</span
		>{/if}</span
>
