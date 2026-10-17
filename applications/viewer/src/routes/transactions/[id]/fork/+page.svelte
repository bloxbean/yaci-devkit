<script lang="ts">
	import { shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import ForkLab from '$lib/julc/fork/ForkLab.svelte';
	import GitFork from '@lucide/svelte/icons/git-fork';
	import ArrowLeft from '@lucide/svelte/icons/arrow-left';

	let { data } = $props();
	const tx = $derived(data.tx);
</script>

<svelte:head><title>Fork Tx {shorten(tx.hash, 8, 6)} · Yaci Viewer</title></svelte:head>

<a href="/transactions/{tx.hash}?tab=scripts" class="mb-3 inline-flex items-center gap-1.5 text-sm text-muted hover:text-ink"><ArrowLeft class="h-4 w-4" />Transaction {shorten(tx.hash, 10, 8)}</a>

<PageHeader kind="What-if lab" title="Fork this transaction">
	{#snippet icon()}<GitFork class="h-5 w-5" />{/snippet}
	<p class="max-w-2xl text-sm text-muted">
		Change redeemers, datums, signers, the validity range, outputs, mint or the rules, and every script runs again as you type. Nothing is submitted: the fork exists
		only in your browser.
	</p>
	{#snippet actions()}
		{#if tx.invalid}<Badge tone="bad">Failed on chain</Badge>{/if}
	{/snippet}
</PageHeader>

{#key tx.hash}
	<ForkLab page={{ hash: tx.hash, tx, scripts: data.scripts, withdrawals: data.withdrawals, mints: data.mints, epoch: data.block?.epoch ?? null }} />
{/key}
