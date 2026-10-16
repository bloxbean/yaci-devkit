<script lang="ts">
	import Ada from '$lib/ui/Ada.svelte';

	/*
	 * Value conservation at a glance: what went in equals what came out plus the fee, and anything left over is
	 * deposits (or refunds, when negative).
	 */
	let {
		inputs,
		outputs,
		fee,
		withdrawals = 0n
	}: { inputs: bigint; outputs: bigint; fee: bigint; withdrawals?: bigint } = $props();

	const other = $derived(inputs + withdrawals - outputs - fee);
	const total = $derived(inputs + withdrawals > 0n ? inputs + withdrawals : 1n);
	const pct = (v: bigint) => Math.max(0, Number((v * 10000n) / total) / 100);
</script>

<div>
	<div class="flex h-2.5 overflow-hidden rounded-full bg-line">
		<div class="h-full bg-brand" style="width:{pct(outputs)}%" title="Outputs"></div>
		<div class="h-full bg-amber" style="width:{Math.max(pct(fee), 0.8)}%" title="Fee"></div>
		{#if other > 0n}<div class="h-full bg-violet" style="width:{pct(other)}%" title="Deposits"></div>{/if}
	</div>
	<dl class="mt-4 grid grid-cols-[1fr_auto] gap-x-4 gap-y-2 text-[13px]">
		<dt class="text-muted">In{withdrawals > 0n ? ' (inputs + withdrawals)' : ''}</dt>
		<dd class="text-right"><Ada value={inputs + withdrawals} decimals={6} class="font-semibold" /></dd>
		<dt class="flex items-center gap-2 text-muted"><span class="h-2 w-2 rounded-full bg-brand"></span>To outputs</dt>
		<dd class="text-right"><Ada value={outputs} decimals={6} /></dd>
		<dt class="flex items-center gap-2 text-muted"><span class="h-2 w-2 rounded-full bg-amber"></span>Fee</dt>
		<dd class="text-right"><Ada value={fee} decimals={6} /></dd>
		{#if other !== 0n}
			<dt class="flex items-center gap-2 text-muted"><span class="h-2 w-2 rounded-full bg-violet"></span>{other > 0n ? 'Deposits' : 'Deposit refunds'}</dt>
			<dd class="text-right"><Ada value={other > 0n ? other : -other} decimals={6} /></dd>
		{/if}
	</dl>
</div>
