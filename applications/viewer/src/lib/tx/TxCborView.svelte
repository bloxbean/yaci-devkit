<script lang="ts">
	import { num } from '$lib/format';
	import { copyText } from '$lib/ui/toast.svelte';
	import CborTree from './CborTree.svelte';
	import { decodeTransaction } from './decode';
	import { hexToBytes } from './cbor-item';
	import BadgeCheck from '@lucide/svelte/icons/badge-check';
	import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
	import Copy from '@lucide/svelte/icons/copy';
	import Download from '@lucide/svelte/icons/download';

	/*
	 * A transaction's original CBOR, as Yaci Store saved it: checked against the transaction id (the hash of the body
	 * bytes), and shown as a tree with the ledger's field names and each item's byte range.
	 */
	let { cbor, hash }: { cbor: string; hash: string } = $props();

	const decoded = $derived.by(() => {
		try {
			return { tx: decodeTransaction(cbor), error: null };
		} catch (e) {
			return { tx: null, error: (e as Error).message };
		}
	});
	const t = $derived(decoded.tx);

	function download() {
		const blob = new Blob([hexToBytes(cbor)], { type: 'application/cbor' });
		const a = document.createElement('a');
		a.href = URL.createObjectURL(blob);
		a.download = `${hash}.cbor`;
		a.click();
		URL.revokeObjectURL(a.href);
	}
</script>

<div class="space-y-4">
	<div class="flex flex-wrap items-center gap-3">
		{#if t}
			{#if t.hash === hash}
				<span class="y-pill border-mint/30 bg-mint/10 text-mint" data-hash-check="ok"><BadgeCheck class="h-3.5 w-3.5" />Hashes to this transaction’s id</span>
			{:else}
				<span class="y-pill border-danger/30 bg-danger/10 text-danger" data-hash-check="mismatch"><TriangleAlert class="h-3.5 w-3.5" />Hashes to {t.hash.slice(0, 12)}…, not this transaction</span>
			{/if}
			<span class="text-sm text-muted">{num(t.size)} bytes · {t.witnesses.vkeys.length} signature{t.witnesses.vkeys.length === 1 ? '' : 's'} · {t.witnesses.redeemers.length} redeemer{t.witnesses.redeemers.length === 1 ? '' : 's'} · {t.witnesses.scripts.length} witnessed script{t.witnesses.scripts.length === 1 ? '' : 's'}{t.isValid ? '' : ' · marked invalid'}</span>
		{:else}
			<span class="y-pill border-danger/30 bg-danger/10 text-danger"><TriangleAlert class="h-3.5 w-3.5" />Couldn’t decode: {decoded.error}</span>
		{/if}
		<span class="ml-auto flex gap-2">
			<button class="y-btn h-8 text-xs" onclick={() => copyText(cbor, 'Transaction CBOR copied')}><Copy class="h-3.5 w-3.5" />Copy hex</button>
			<button class="y-btn h-8 text-xs" onclick={download}><Download class="h-3.5 w-3.5" />.cbor</button>
		</span>
	</div>
	{#if t}
		<div class="overflow-x-auto rounded-xl border border-line bg-bg-2 p-3">
			<CborTree item={t.root} label="transaction" context="tx" />
		</div>
	{/if}
	<details class="text-sm">
		<summary class="cursor-pointer text-xs font-medium text-muted hover:text-ink">Hex</summary>
		<pre class="mono mt-2 max-h-72 overflow-auto rounded-xl border border-line bg-bg-2 p-3 text-[11px] leading-5 break-all whitespace-pre-wrap">{cbor}</pre>
	</details>
</div>
