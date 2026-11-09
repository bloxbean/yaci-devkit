<script lang="ts">
	import Ada from '$lib/ui/Ada.svelte';
	import UtxoCard from './UtxoCard.svelte';
	import { sumLovelace, type Utxo } from './types';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';
	import ArrowDown from '@lucide/svelte/icons/arrow-down';

	/*
	 * Inputs flow through the transaction into outputs. Each side hangs off a bus line into the centre node, which
	 * carries the fee. Outputs going back to an input address are marked as change.
	 */
	let {
		inputs,
		outputs,
		fee,
		ondatum = undefined
	}: { inputs: Utxo[]; outputs: Utxo[]; fee: number | string; ondatum?: (u: Utxo) => void } = $props();

	const inAddrs = $derived(new Set(inputs.map((i) => i.address)));
	const inTotal = $derived(sumLovelace(inputs));
	const outTotal = $derived(sumLovelace(outputs));
</script>

<div class="grid items-stretch gap-4 lg:grid-cols-[minmax(0,1fr)_92px_minmax(0,1fr)] lg:gap-0">
	<!-- Inputs -->
	<div class="min-w-0">
		<div class="mb-2.5 flex items-baseline justify-between px-1">
			<span class="y-label">Inputs · {inputs.length}</span>
			<Ada value={inTotal} class="text-xs text-muted" />
		</div>
		<ul class="flex flex-col gap-2.5">
			{#each inputs as u (u.tx_hash + u.output_index)}
				<li class="flow-in relative"><UtxoCard utxo={u} side="in" {ondatum} /></li>
			{/each}
		</ul>
	</div>

	<!-- The transaction -->
	<div class="relative flex items-center justify-center py-1 lg:py-0">
		<div class="absolute inset-y-6 left-0 hidden w-px bg-gradient-to-b from-transparent via-brand/40 to-transparent lg:block"></div>
		<div class="absolute inset-y-6 right-0 hidden w-px bg-gradient-to-b from-transparent via-brand/40 to-transparent lg:block"></div>
		<div class="absolute left-0 right-0 top-1/2 hidden h-px bg-brand/40 lg:block"></div>
		<div class="relative z-10 flex flex-col items-center gap-1 rounded-2xl border border-brand/40 bg-surface px-2.5 py-3 shadow-glow lg:sticky lg:top-24">
			<span class="grid h-8 w-8 place-items-center rounded-xl bg-grad text-white">
				<ArrowRight class="hidden h-4 w-4 lg:block" /><ArrowDown class="h-4 w-4 lg:hidden" />
			</span>
			<span class="y-label">Fee</span>
			<Ada value={fee} decimals={4} symbol={false} class="text-[11px] font-semibold" />
		</div>
	</div>

	<!-- Outputs -->
	<div class="min-w-0">
		<div class="mb-2.5 flex items-baseline justify-between px-1">
			<span class="y-label">Outputs · {outputs.length}</span>
			<Ada value={outTotal} class="text-xs text-muted" />
		</div>
		<ul class="flex flex-col gap-2.5">
			{#each outputs as u (u.output_index)}
				<li class="flow-out relative"><UtxoCard utxo={u} side="out" tag={inAddrs.has(u.address) ? 'change' : undefined} {ondatum} /></li>
			{/each}
		</ul>
	</div>
</div>

<style>
	/* Stubs from each card to the bus line on its side (wide screens only). */
	@media (min-width: 1024px) {
		.flow-in::after,
		.flow-out::before {
			content: '';
			position: absolute;
			top: 26px;
			width: 18px;
			height: 1px;
			background: rgb(var(--c-brand) / 0.4);
		}
		.flow-in {
			margin-right: 18px;
		}
		.flow-in::after {
			right: -18px;
		}
		.flow-out {
			margin-left: 18px;
		}
		.flow-out::before {
			left: -18px;
		}
	}
</style>
