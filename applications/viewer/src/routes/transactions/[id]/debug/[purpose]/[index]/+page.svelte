<script lang="ts">
	import { shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import Debugger from '$lib/julc/debug/Debugger.svelte';
	import { plutusVersion, purposeOf } from '$lib/julc/replay/build';
	import Bug from '@lucide/svelte/icons/bug';
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';

	let { data } = $props();
	const tx = $derived(data.tx);

	// The script run this page debugs, for the header (the debugger finds it again from the rebuilt transaction).
	const script = $derived(
		data.scripts.find((s: { redeemer?: unknown }) => {
			const r = typeof s.redeemer === 'string' ? JSON.parse(s.redeemer) : (s.redeemer as { tag?: string; index?: number } | null);
			return r && purposeOf(r.tag) === data.purpose && Number(r.index ?? 0) === data.index;
		}) as { script_hash: string; type?: string | null } | undefined
	);
</script>

<svelte:head><title>Debug {data.purpose} #{data.index} · Tx {shorten(tx.hash, 8, 6)} · Yaci Viewer</title></svelte:head>

<a href="/transactions/{tx.hash}?tab=scripts" class="mb-3 inline-flex items-center gap-1.5 text-sm text-muted hover:text-ink"><ArrowLeft class="h-4 w-4" />Transaction {shorten(tx.hash, 10, 8)}</a>

<PageHeader kind="Script debugger" title="{data.purpose} #{data.index}">
	{#snippet icon()}<Bug class="h-5 w-5" />{/snippet}
	{#if script}
		<div class="flex flex-wrap items-center gap-2 text-sm text-muted">
			Script <Hash value={script.script_hash} href="/scripts/{script.script_hash}" head={10} tail={8} />
		</div>
	{/if}
	{#snippet actions()}
		{#if script?.type && plutusVersion(script.type)}<Badge tone="violet">Plutus {plutusVersion(script.type)}</Badge>{/if}
		{#if tx.invalid}<Badge tone="bad">Failed on chain</Badge>{/if}
	{/snippet}
</PageHeader>

{#key `${tx.hash}/${data.purpose}/${data.index}`}
	<Debugger
		page={{ hash: tx.hash, tx, scripts: data.scripts, withdrawals: data.withdrawals, mints: data.mints, epoch: data.block?.epoch ?? null }}
		purpose={data.purpose}
		index={data.index}
	/>
{/key}
