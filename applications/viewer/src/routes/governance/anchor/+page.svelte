<script lang="ts">
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import JsonView from '$lib/ui/JsonView.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import AnchorDoc from '$lib/governance/AnchorDoc.svelte';
	import FileText from '@lucide/svelte/icons/file-text';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import ShieldAlert from '@lucide/svelte/icons/shield-alert';
	import ExternalLink from '@lucide/svelte/icons/external-link';

	let { data } = $props();

	const verified = $derived(data.result && data.expected ? data.result.hash === data.expected : null);
	const openHref = $derived(/^https?:/i.test(data.target) ? data.target : data.result?.fetchedFrom);
</script>

<svelte:head><title>Anchor document · Yaci Viewer</title></svelte:head>

<PageHeader kind="Governance" title="Anchor document">
	{#snippet icon()}<FileText class="h-5 w-5" />{/snippet}
	<span class="mono break-all text-xs text-faint">{data.target || '—'}</span>
	{#snippet actions()}
		{#if verified === true}<Badge tone="ok"><ShieldCheck class="h-3.5 w-3.5" />Hash matches the chain</Badge>{/if}
		{#if verified === false}<Badge tone="bad"><ShieldAlert class="h-3.5 w-3.5" />Hash does not match</Badge>{/if}
		{#if openHref}<a class="y-btn h-8 text-xs" href={openHref} target="_blank" rel="noopener noreferrer nofollow">Open original <ExternalLink class="h-3.5 w-3.5" /></a>{/if}
	{/snippet}
</PageHeader>

{#if data.error}
	<div class="y-panel"><ErrorBox title="Couldn't open this anchor" message={data.error} retry={false} /></div>
{:else if data.result}
	{#if verified === false}
		<div class="mb-6 flex items-start gap-3 rounded-xl border border-danger/30 bg-danger/10 px-4 py-3 text-sm">
			<ShieldAlert class="mt-0.5 h-4 w-4 shrink-0 text-danger" />
			<div>The document at this URL has changed since it was anchored on chain. Its BLAKE2b-256 hash is <span class="mono break-all">{data.result.hash}</span>, the chain expects <span class="mono break-all">{data.expected}</span>.</div>
		</div>
	{/if}
	<div class="grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
		<Panel>
			<AnchorDoc doc={data.result.json} />
		</Panel>
		<div class="flex flex-col gap-6">
			<Panel title="Document">
				<dl class="grid gap-3 text-sm">
					<div><dt class="y-label mb-1">Hash (BLAKE2b-256)</dt><dd class="flex items-start gap-1"><span class="mono break-all text-xs">{data.result.hash}</span><CopyButton value={data.result.hash} /></dd></div>
					{#if data.expected}<div><dt class="y-label mb-1">Hash on chain</dt><dd class="mono break-all text-xs">{data.expected}</dd></div>{/if}
					{#if data.result.fetchedFrom !== data.target}<div><dt class="y-label mb-1">Fetched from</dt><dd class="mono break-all text-xs text-muted">{data.result.fetchedFrom}</dd></div>{/if}
				</dl>
			</Panel>
			<JsonView value={data.result.json} label="Raw JSON-LD" maxHeight={520} />
		</div>
	</div>
{/if}
