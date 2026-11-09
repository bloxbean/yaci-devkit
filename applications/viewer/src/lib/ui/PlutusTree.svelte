<script lang="ts">
	import type { PlutusData } from '$lib/cbor';
	import { hexToText } from '$lib/format';
	import PlutusTree from './PlutusTree.svelte';

	/* Plutus data as a readable tree: Constr n, integers, bytes (with text when it decodes), lists and maps. */
	let { data, depth = 0 }: { data: PlutusData; depth?: number } = $props();
</script>

{#if 'fields' in data}
	<div>
		<span class="text-violet">Constr</span> <span class="text-brand">{data.constructor}</span>
		{#if data.fields.length === 0}<span class="text-faint">[]</span>{/if}
		{#if data.fields.length}
			<div class="ml-3 border-l border-line pl-3">
				{#each data.fields as f, i (i)}
					<div class="flex gap-2"><span class="select-none text-faint">{i}</span><div class="min-w-0"><PlutusTree data={f} depth={depth + 1} /></div></div>
				{/each}
			</div>
		{/if}
	</div>
{:else if 'int' in data}
	<span class="text-cyan">{data.int}</span>
{:else if 'bytes' in data}
	{@const text = data.bytes.length >= 4 ? hexToText(data.bytes) : null}
	<span class="break-all text-mint">{data.bytes ? `#${data.bytes}` : '#""'}</span>{#if text}<span class="ml-2 text-faint">"{text}"</span>{/if}
{:else if 'list' in data}
	<div>
		<span class="text-faint">List [{data.list.length}]</span>
		{#if data.list.length}
			<div class="ml-3 border-l border-line pl-3">
				{#each data.list as item, i (i)}
					<div class="flex gap-2"><span class="select-none text-faint">{i}</span><div class="min-w-0"><PlutusTree data={item} depth={depth + 1} /></div></div>
				{/each}
			</div>
		{/if}
	</div>
{:else if 'map' in data}
	<div>
		<span class="text-faint">Map {'{'}{data.map.length}{'}'}</span>
		<div class="ml-3 border-l border-line pl-3">
			{#each data.map as kv, i (i)}
				<div class="flex flex-wrap gap-x-2">
					<div class="min-w-0"><PlutusTree data={kv.k} depth={depth + 1} /></div>
					<span class="text-faint">→</span>
					<div class="min-w-0"><PlutusTree data={kv.v} depth={depth + 1} /></div>
				</div>
			{/each}
		</div>
	</div>
{/if}
