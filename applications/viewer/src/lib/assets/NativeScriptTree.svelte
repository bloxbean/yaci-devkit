<script lang="ts">
	import { num, shorten } from '$lib/format';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import NativeScriptTree from './NativeScriptTree.svelte';
	import KeyRound from '@lucide/svelte/icons/key-round';
	import Clock from '@lucide/svelte/icons/clock';

	/*
	 * A native (timelock) script as rules: who must sign and when it is valid. Accepts the JSON Yaci Store returns
	 * ({ type: sig | all | any | atLeast | before | after, ... }).
	 */
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	let { script }: { script: any } = $props();

	const children = $derived((script?.scripts ?? []) as unknown[]);
	const type = $derived(String(script?.type ?? '').toLowerCase());
</script>

{#if type === 'sig'}
	<div class="flex items-center gap-2 rounded-lg border border-line bg-surface px-2.5 py-1.5">
		<KeyRound class="h-3.5 w-3.5 shrink-0 text-brand" />
		<span class="text-xs text-muted">Signed by</span>
		<span class="mono truncate text-[12.5px]" title={script.keyHash}>{shorten(script.keyHash, 12, 8)}</span>
		<CopyButton value={script.keyHash} size="xs" />
	</div>
{:else if type === 'before' || type === 'after'}
	<div class="flex items-center gap-2 rounded-lg border border-line bg-surface px-2.5 py-1.5">
		<Clock class="h-3.5 w-3.5 shrink-0 text-amber" />
		<span class="text-xs text-muted">{type === 'before' ? 'Valid before slot' : 'Valid from slot'}</span>
		<span class="mono text-[12.5px]">{num(script.slot)}</span>
	</div>
{:else if type === 'all' || type === 'any' || type === 'atleast'}
	<div>
		<div class="mb-2 text-sm font-medium">
			{#if type === 'all'}All of these{:else if type === 'any'}Any one of these{:else}At least {script.required} of these{/if}
			<span class="text-faint">· {children.length}</span>
		</div>
		<div class="flex flex-col gap-2 border-l-2 border-brand/30 pl-4">
			{#each children as child, i (i)}<NativeScriptTree script={child} />{/each}
		</div>
	</div>
{:else}
	<pre class="text-xs text-muted">{JSON.stringify(script, null, 2)}</pre>
{/if}
