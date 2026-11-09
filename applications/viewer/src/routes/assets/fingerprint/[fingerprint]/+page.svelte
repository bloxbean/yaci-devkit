<script lang="ts">
	import { num } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import KV from '$lib/ui/KV.svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import FingerprintPattern from '@lucide/svelte/icons/fingerprint-pattern';

	let { data } = $props();
</script>

<svelte:head><title>{data.fingerprint} · Yaci Viewer</title></svelte:head>

<PageHeader kind="Asset fingerprint" title="{data.fingerprint.slice(0, 14)}…{data.fingerprint.slice(-6)}">
	{#snippet icon()}<FingerprintPattern class="h-5 w-5" />{/snippet}
	The store has no mint history for this asset, so its policy and name aren't known here.
</PageHeader>

<Panel title="What the store knows">
	<dl class="grid gap-x-6 gap-y-5 sm:grid-cols-2">
		<KV label="Fingerprint"><Hash value={data.fingerprint} full /></KV>
		<KV label="Supply">
			{#if data.supply.value != null}<span class="font-display text-xl font-semibold tabular-nums">{num(data.supply.value)}</span>{:else}<span class="text-muted">Not reported</span>{/if}
		</KV>
	</dl>
</Panel>
