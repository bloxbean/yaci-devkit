<script lang="ts">
	import { tryDecodePlutusData } from '$lib/cbor';
	import PlutusTree from './PlutusTree.svelte';
	import JsonView from './JsonView.svelte';
	import CopyButton from './CopyButton.svelte';

	/* Datum or redeemer: decoded tree by default, with the detailed-schema JSON and the raw CBOR a click away. */
	let { cbor, json = undefined, label = 'Data' }: { cbor: string | null | undefined; json?: unknown; label?: string } = $props();

	const decoded = $derived(tryDecodePlutusData(cbor));
	let view = $state<'tree' | 'json' | 'cbor'>('tree');
</script>

<div class="overflow-hidden rounded-xl border border-line bg-bg-2">
	<div class="flex items-center justify-between gap-2 border-b border-line px-3 py-1.5">
		<span class="y-label">{label}</span>
		<div class="flex items-center gap-0.5 rounded-lg bg-surface-2 p-0.5 text-xs">
			{#each [['tree', 'Decoded'], ['json', 'JSON'], ['cbor', 'CBOR']] as [id, text] (id)}
				<button
					class="rounded-md px-2 py-0.5 transition {view === id ? 'bg-surface text-ink shadow-sm' : 'text-muted hover:text-ink'}"
					onclick={() => (view = id as typeof view)}>{text}</button
				>
			{/each}
		</div>
	</div>
	{#if view === 'tree'}
		<div class="max-h-[380px] overflow-auto p-4 font-mono text-[12.5px] leading-relaxed">
			{#if decoded}<PlutusTree data={decoded} />{:else}<span class="text-faint">Could not decode this CBOR as Plutus data.</span>{/if}
		</div>
	{:else if view === 'json'}
		<div class="p-2"><JsonView value={json ?? decoded} label="Detailed schema" maxHeight={340} /></div>
	{:else}
		<div class="flex items-start gap-2 p-4">
			<code class="max-h-[340px] min-w-0 flex-1 overflow-auto break-all text-[12px] text-muted">{cbor}</code>
			{#if cbor}<CopyButton value={cbor} label="CBOR copied" />{/if}
		</div>
	{/if}
</div>
