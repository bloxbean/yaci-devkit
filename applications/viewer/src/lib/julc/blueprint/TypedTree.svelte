<script lang="ts">
	import { hexToText } from '$lib/format';
	import Self from './TypedTree.svelte';
	import type { TypedNode } from './blueprint';

	/* Plutus data typed by a blueprint schema: field: Type.Variant, with values coloured like the plain tree. */
	let { node, depth = 0 }: { node: TypedNode; depth?: number } = $props();

	const text = $derived(node.kind === 'bytes' && node.value ? hexToText(node.value) : null);
</script>

<div class={depth ? 'border-l border-line pl-3' : ''}>
	<div class="flex flex-wrap items-baseline gap-x-1.5">
		{#if node.field}<span class="text-ink">{node.field}</span><span class="text-faint">:</span>{/if}
		{#if node.kind === 'constructor'}
			<span>{#if node.type && node.type !== node.variant}<span class="text-faint">{node.type}.</span>{/if}<span class="font-semibold text-violet">{node.variant}</span></span>
		{:else if node.kind === 'int'}
			<span class="text-mint">{node.value}</span>
		{:else if node.kind === 'bytes'}
			<span class="break-all text-cyan">#{node.value}</span>{#if text}<span class="text-faint">“{text}”</span>{/if}
		{:else if node.kind === 'list'}
			<span class="text-faint">{node.type ? `${node.type} ` : ''}[{node.children?.length ?? 0}]</span>
		{:else if node.kind === 'map'}
			<span class="text-faint">{node.type ? `${node.type} ` : ''}{'{'}{node.children?.length ?? 0}{'}'}</span>
		{/if}
		{#if node.type && node.kind !== 'constructor' && node.kind !== 'list' && node.kind !== 'map'}<span class="text-[11px] text-faint">{node.type}</span>{/if}
		{#if node.mismatch}<span class="rounded bg-amber/15 px-1 text-[10px] font-semibold uppercase text-amber" title="This part of the data doesn't match the blueprint">not as in blueprint</span>{/if}
	</div>
	{#if node.children?.length}
		<div class="mt-0.5 space-y-0.5">
			{#each node.children as child}<Self node={child} depth={depth + 1} />{/each}
		</div>
	{/if}
</div>
