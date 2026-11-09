<script lang="ts">
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import KV from '$lib/ui/KV.svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import { anchorHref } from '$lib/governance/labels';
	import Scale from '@lucide/svelte/icons/scale';
	import FileText from '@lucide/svelte/icons/file-text';
	import FileCode from '@lucide/svelte/icons/file-code';

	let { data } = $props();
	const c = $derived(data.constitution);
</script>

<svelte:head><title>Constitution · Yaci Viewer</title></svelte:head>

<PageHeader kind="Governance" title="Constitution">
	{#snippet icon()}<Scale class="h-5 w-5" />{/snippet}
	The document every governance action is measured against, and the guardrail script that enforces parts of it.
</PageHeader>

{#if !c}
	<div class="y-panel"><Empty title="No constitution" text="No constitution has been enacted on this chain, or the store doesn't index it." /></div>
{:else}
	<div class="grid gap-6 lg:grid-cols-2">
		<Panel title="Document">
			{#snippet icon()}<FileText class="h-4 w-4" />{/snippet}
			<dl class="grid gap-5">
				<KV label="Anchor URL"><span class="mono break-all text-xs">{c.anchor_url ?? '—'}</span></KV>
				<KV label="Anchor hash"><Hash value={c.anchor_hash} head={16} tail={12} /></KV>
				<KV label="In force since">Epoch {c.active_epoch ?? '—'}</KV>
			</dl>
			{#if c.anchor_url}<a class="y-btn y-btn-primary mt-5" href={anchorHref(c.anchor_url, c.anchor_hash)}><FileText class="h-4 w-4" /> Read the constitution</a>{/if}
		</Panel>
		<Panel title="Guardrail script">
			{#snippet icon()}<FileCode class="h-4 w-4" />{/snippet}
			{#if c.script}
				<p class="mb-4 text-sm text-muted">Parameter changes and treasury withdrawals must pass this script.</p>
				<Hash value={c.script} href="/scripts/{c.script}" full />
			{:else}
				<p class="text-sm text-muted">No guardrail script. Proposals are not checked by a script.</p>
			{/if}
		</Panel>
	</div>
{/if}
