<script lang="ts">
	import { tryDecodePlutusData } from '$lib/cbor';
	import PlutusTree from './PlutusTree.svelte';
	import JsonView from './JsonView.svelte';
	import CopyButton from './CopyButton.svelte';
	import TypedTree from '$lib/julc/blueprint/TypedTree.svelte';
	import { typeData, type Schema } from '$lib/julc/blueprint/blueprint';

	/*
	 * Datum or redeemer: decoded tree by default, with the detailed-schema JSON and the raw CBOR a click away. With a
	 * blueprint schema, a Typed view (type, variant and field names) comes first.
	 */
	let {
		cbor,
		json = undefined,
		label = 'Data',
		schema = undefined
	}: { cbor: string | null | undefined; json?: unknown; label?: string; schema?: { schema: Schema | null; definitions: Record<string, Schema>; source: string } | null } = $props();

	const decoded = $derived(tryDecodePlutusData(cbor));
	const typed = $derived(decoded && schema?.schema ? typeData(decoded, schema.schema, schema.definitions) : null);
	let chosen = $state<'typed' | 'tree' | 'json' | 'cbor' | null>(null);
	const view = $derived(chosen ?? (typed ? 'typed' : 'tree'));
	const views = $derived([...(typed ? [['typed', 'Typed']] : []), ['tree', 'Decoded'], ['json', 'JSON'], ['cbor', 'CBOR']] as [typeof view, string][]);
</script>

<div class="overflow-hidden rounded-xl border border-line bg-bg-2">
	<div class="flex items-center justify-between gap-2 border-b border-line px-3 py-1.5">
		<span class="y-label">{label}</span>
		<div class="flex items-center gap-0.5 rounded-lg bg-surface-2 p-0.5 text-xs">
			{#each views as [id, text] (id)}
				<button class="rounded-md px-2 py-0.5 transition {view === id ? 'bg-surface text-ink shadow-sm' : 'text-muted hover:text-ink'}" onclick={() => (chosen = id)}>{text}</button>
			{/each}
		</div>
	</div>
	{#if view === 'typed' && typed}
		<div class="max-h-[380px] overflow-auto p-4 font-mono text-[12.5px] leading-relaxed">
			<TypedTree node={typed} />
			<div class="mt-3 font-sans text-[11px] text-faint">Typed by the blueprint “{schema?.source}”.</div>
		</div>
	{:else if view === 'tree'}
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
