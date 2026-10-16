<script lang="ts">
	import { shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import PlutusView from '$lib/ui/PlutusView.svelte';
	import JsonView from '$lib/ui/JsonView.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import Braces from '@lucide/svelte/icons/braces';

	let { data } = $props();
</script>

<svelte:head><title>Datum {shorten(data.hash, 8, 6)} · Yaci Viewer</title></svelte:head>

<PageHeader kind="Datum" title={shorten(data.hash, 14, 10)}>
	{#snippet icon()}<Braces class="h-5 w-5" />{/snippet}
	<div class="flex items-start gap-1.5">
		<span class="mono break-all text-xs text-faint">{data.hash}</span>
		<CopyButton value={data.hash} label="Datum hash copied" />
	</div>
</PageHeader>

{#if data.cbor}
	<PlutusView cbor={data.cbor} json={data.json ?? undefined} label="Datum" />
{:else}
	<JsonView value={data.json} label="Datum JSON" />
{/if}
