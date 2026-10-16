<script lang="ts">
	import { goto } from '$app/navigation';
	import { chain } from '$lib/live/chain.svelte';
	import { num, shorten, dateTime } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import KV from '$lib/ui/KV.svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import Ring from '$lib/ui/Ring.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Box from '@lucide/svelte/icons/box';
	import ChevronLeft from '@lucide/svelte/icons/chevron-left';
	import ChevronRight from '@lucide/svelte/icons/chevron-right';
	import ArrowLeftRight from '@lucide/svelte/icons/arrow-left-right';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import ChevronDown from '@lucide/svelte/icons/chevron-down';

	let { data } = $props();
	const b = $derived(data.block);

	const tip = $derived(chain.tip?.number ?? b.number);
	const confirmations = $derived(Math.max(0, tip - b.number));
	const isTip = $derived(confirmations === 0);
	const fill = $derived(Math.min(1, (Number(b.size) || 0) / data.maxBlockSize));
	let advanced = $state(false);

	function onKey(e: KeyboardEvent) {
		if ((e.target as HTMLElement).closest('input,textarea')) return;
		if (e.key === 'ArrowLeft' && b.number > 0) goto(`/blocks/${b.number - 1}`);
		if (e.key === 'ArrowRight' && !isTip) goto(`/blocks/${b.number + 1}`);
	}

	const columns = [
		{ key: 'hash', label: 'Transaction' },
		{ key: 'to', label: 'Sent to', minor: true },
		{ key: 'fee', label: 'Fee', align: 'right' as const, minor: true },
		{ key: 'out', label: 'Output', align: 'right' as const }
	];
</script>

<svelte:window onkeydown={onKey} />
<svelte:head><title>Block #{num(b.number)} · Yaci Viewer</title></svelte:head>

<PageHeader kind="Block" title="#{num(b.number)}">
	{#snippet icon()}<Box class="h-5 w-5" />{/snippet}
	<TimeAgo ts={b.time} /> · {dateTime(b.time)}
	{#snippet actions()}
		<a class="y-btn {b.number <= 0 ? 'pointer-events-none opacity-40' : ''}" href="/blocks/{b.number - 1}" title="Previous block (←)"><ChevronLeft class="h-4 w-4" /> Prev</a>
		<a class="y-btn {isTip ? 'pointer-events-none opacity-40' : ''}" href="/blocks/{b.number + 1}" title="Next block (→)">Next <ChevronRight class="h-4 w-4" /></a>
	{/snippet}
</PageHeader>

<div class="mb-6 grid gap-6 xl:grid-cols-[minmax(0,1.7fr)_minmax(0,1fr)]">
	<Panel title="Block details">
		<dl class="grid gap-x-6 gap-y-5 sm:grid-cols-2 lg:grid-cols-3">
			<KV label="Hash" wide><Hash value={b.hash} head={16} tail={12} /></KV>
			<KV label="Epoch"><span class="font-medium">{b.epoch}</span></KV>
			<KV label="Slot"><span class="mono">{num(b.slot)}</span>{#if b.epoch_slot != null}<span class="text-faint"> · {num(b.epoch_slot)} in epoch</span>{/if}</KV>
			<KV label="Transactions"><span class="font-semibold">{b.tx_count}</span></KV>
			<KV label="Fees"><Ada value={b.fees} decimals={6} /></KV>
			<KV label="Output"><Ada value={b.output} decimals={2} /></KV>
			<KV label="Previous block" wide>{#if b.previous_block}<Hash value={b.previous_block} href="/blocks/{b.previous_block}" head={16} tail={12} />{:else}—{/if}</KV>
			<KV label="Protocol version"><span class="mono">{b.protocol_version ?? '—'}</span></KV>
			<KV label="Producer" wide><Hash value={b.slot_leader} head={16} tail={10} /></KV>
		</dl>

		<button class="mt-6 flex items-center gap-1.5 text-xs font-medium text-muted hover:text-ink" onclick={() => (advanced = !advanced)}>
			<ChevronDown class="h-3.5 w-3.5 transition {advanced ? 'rotate-180' : ''}" /> Consensus fields
		</button>
		{#if advanced}
			<dl class="mt-4 grid animate-fade-up gap-x-6 gap-y-4 border-t border-line pt-4 sm:grid-cols-2">
				<KV label="Issuer key"><Hash value={b.issuer_vkey} /></KV>
				<KV label="Block body hash"><Hash value={b.block_body_hash} /></KV>
				<KV label="VRF key"><Hash value={b.block_vrf} /></KV>
				<KV label="Operational cert"><Hash value={b.op_cert} /></KV>
				<KV label="Op cert counter / KES period"><span class="mono">{b.op_cert_counter ?? '—'} / {b.op_cert_kes_period ?? '—'}</span></KV>
				<KV label="VRF output"><Hash value={b.vrf_result?.output} /></KV>
			</dl>
		{/if}
	</Panel>

	<div class="flex flex-col gap-6">
		<Panel title="Confirmations">
			{#snippet icon()}<ShieldCheck class="h-4 w-4" />{/snippet}
			<div class="flex items-center gap-5">
				<Ring value={Math.min(1, confirmations / 100)} size={96} stroke={9}>
					<span class="font-display text-xl font-semibold tabular-nums">{confirmations > 999 ? '999+' : confirmations}</span>
				</Ring>
				<p class="text-sm text-muted">
					{#if isTip}This is the newest block. It is the chain tip.{:else if confirmations < 100}{num(confirmations)} block{confirmations === 1 ? '' : 's'} built on top. Settles after a few more.{:else}{num(confirmations)} blocks on top. Final for all practical purposes.{/if}
				</p>
			</div>
		</Panel>
		<Panel title="Block size">
			<div class="flex items-end justify-between">
				<div class="font-display text-2xl font-semibold tabular-nums">{(Number(b.size) / 1024).toFixed(2)} <span class="text-sm text-muted">KB</span></div>
				<div class="text-sm text-muted">{(fill * 100).toFixed(2)}% of {(data.maxBlockSize / 1024).toFixed(0)} KB</div>
			</div>
			<div class="mt-3 h-2.5 overflow-hidden rounded-full bg-line"><div class="h-full rounded-full bg-grad" style="width:{Math.max(1, fill * 100)}%"></div></div>
		</Panel>
	</div>
</div>

<section class="y-panel overflow-hidden">
	<header class="y-panel-head">
		<div class="flex items-center gap-2.5">
			<span class="grid h-7 w-7 place-items-center rounded-lg bg-brand/10 text-brand"><ArrowLeftRight class="h-4 w-4" /></span>
			<h2 class="font-display text-[15px] font-semibold">Transactions <span class="text-muted">· {data.txs.length}</span></h2>
		</div>
	</header>
	<DataTable rows={data.txs} {columns} key={(t) => t.tx_hash} rowHref={(t) => `/transactions/${t.tx_hash}`} emptyTitle="No transactions in this block" emptyText="Empty blocks are normal on a quiet devnet.">
		{#snippet cell(t, key)}
			{#if key === 'hash'}
				<a class="mono font-medium text-ink hover:text-brand" href="/transactions/{t.tx_hash}">{shorten(t.tx_hash, 12, 10)}</a>
			{:else if key === 'to'}
				{@const addrs = t.output_addresses ?? []}
				<span class="mono text-xs text-muted">{shorten(addrs[0], 12, 6)}</span>{#if addrs.length > 1}<span class="ml-1.5 text-xs text-faint">+{addrs.length - 1}</span>{/if}
			{:else if key === 'fee'}
				<Ada value={t.fee} decimals={6} class="text-muted" />
			{:else if key === 'out'}
				<Ada value={t.total_output} class="font-semibold" />
			{/if}
		{/snippet}
	</DataTable>
</section>
