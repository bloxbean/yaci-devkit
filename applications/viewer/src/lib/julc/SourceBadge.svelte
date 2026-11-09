<script lang="ts">
	import FileCheck from '@lucide/svelte/icons/file-check';
	import Info from '@lucide/svelte/icons/info';

	/*
	 * Where a replayed transaction came from: its original CBOR as Yaci Store saved it (exact), or rebuilt from the
	 * store's JSON because the store doesn't keep transaction CBOR (store.transaction.save-cbor is off).
	 */
	let { source, compact = false }: { source: 'cbor' | 'store' | null | undefined; compact?: boolean } = $props();

	const rebuiltWhy = 'This Yaci Store doesn’t keep the original transaction CBOR (store.transaction.save-cbor), so the transaction was rebuilt from its JSON data. A few details can differ from what the ledger saw.';
</script>

{#if source === 'cbor'}
	<span class="y-pill border-mint/30 bg-mint/10 text-mint" title="Replayed from the transaction’s original bytes, as Yaci Store saved them." data-source="cbor">
		<FileCheck class="h-3.5 w-3.5" />{compact ? 'CBOR' : 'From the transaction’s CBOR'}
	</span>
{:else if source === 'store'}
	{#if compact}
		<span class="y-pill border-line text-muted" title={rebuiltWhy} data-source="store"><Info class="h-3.5 w-3.5" />Rebuilt</span>
	{:else}
		<span class="flex items-start gap-1.5 text-xs text-muted" data-source="store">
			<Info class="mt-0.5 h-3.5 w-3.5 shrink-0 text-blue" />
			<span><span class="font-medium text-ink">Rebuilt from Yaci Store data.</span> {rebuiltWhy}</span>
		</span>
	{/if}
{/if}
