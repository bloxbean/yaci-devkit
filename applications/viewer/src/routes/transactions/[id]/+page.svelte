<script lang="ts">
	import { page } from '$app/state';
	import { num, shorten, parseUnit } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import KV from '$lib/ui/KV.svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import Tabs from '$lib/ui/Tabs.svelte';
	import JsonView from '$lib/ui/JsonView.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import AssetChip from '$lib/ui/AssetChip.svelte';
	import UtxoFlow from '$lib/tx/UtxoFlow.svelte';
	import UtxoCard from '$lib/tx/UtxoCard.svelte';
	import BalanceBar from '$lib/tx/BalanceBar.svelte';
	import ScriptCard from '$lib/tx/ScriptCard.svelte';
	import DatumDialog from '$lib/tx/DatumDialog.svelte';
	import { sumLovelace, type Utxo } from '$lib/tx/types';
	import ArrowLeftRight from '@lucide/svelte/icons/arrow-left-right';
	import CircleCheck from '@lucide/svelte/icons/circle-check';
	import CircleX from '@lucide/svelte/icons/circle-x';
	import FileCode from '@lucide/svelte/icons/file-code';
	import Sparkles from '@lucide/svelte/icons/sparkles';
	import MessageSquare from '@lucide/svelte/icons/message-square';
	import Flame from '@lucide/svelte/icons/flame';

	let { data } = $props();
	const tx = $derived(data.tx);

	const METADATA_LABELS: Record<string, string> = {
		'674': 'Message (CIP-20)',
		'721': 'NFT metadata (CIP-25)',
		'20': 'Token metadata',
		'61284': 'Catalyst registration (CIP-15)',
		'61285': 'Catalyst signature (CIP-15)'
	};

	const withdrawn = $derived(data.withdrawals.reduce((s: bigint, w: { amount: number | string }) => s + BigInt(w.amount ?? 0), 0n));
	const minted = $derived(data.mints.filter((m: { quantity: number }) => Number(m.quantity) > 0));
	const burned = $derived(data.mints.filter((m: { quantity: number }) => Number(m.quantity) < 0));

	const tabs = $derived(
		[
			{ id: 'utxos', label: 'Inputs & outputs', count: tx.inputs.length + tx.outputs.length },
			data.scripts.length && { id: 'scripts', label: 'Scripts', count: data.scripts.length },
			data.mints.length && { id: 'mints', label: 'Mint & burn', count: data.mints.length },
			data.metadata.length && { id: 'metadata', label: 'Metadata', count: data.metadata.length },
			data.withdrawals.length && { id: 'withdrawals', label: 'Withdrawals', count: data.withdrawals.length },
			(tx.collateral_inputs?.length || tx.collateral_return) && { id: 'collateral', label: 'Collateral', count: tx.collateral_inputs?.length ?? 0 },
			tx.reference_inputs?.length && { id: 'references', label: 'Reference inputs', count: tx.reference_inputs.length },
			tx.required_signers?.length && { id: 'signers', label: 'Signers', count: tx.required_signers.length },
			{ id: 'raw', label: 'Raw' }
		].filter(Boolean) as { id: string; label: string; count?: number }[]
	);

	let active = $state(page.url.searchParams.get('tab') ?? 'utxos');
	$effect(() => {
		if (!tabs.some((t) => t.id === active)) active = 'utxos';
	});

	let datumOf = $state<Utxo | null>(null);
</script>

<svelte:head><title>Tx {shorten(tx.hash, 8, 6)} · Yaci Viewer</title></svelte:head>

<PageHeader kind="Transaction" title={shorten(tx.hash, 14, 10)}>
	{#snippet icon()}<ArrowLeftRight class="h-5 w-5" />{/snippet}
	<div class="flex flex-wrap items-center gap-2">
		<span class="mono hidden break-all text-xs text-faint md:inline">{tx.hash}</span>
		<CopyButton value={tx.hash} label="Transaction hash copied" />
	</div>
	{#snippet actions()}
		{#if tx.invalid}
			<Badge tone="bad"><CircleX class="h-3.5 w-3.5" />Script failed · collateral taken</Badge>
		{:else}
			<Badge tone="ok"><CircleCheck class="h-3.5 w-3.5" />Success</Badge>
		{/if}
		{#if data.scripts.length}<Badge tone="violet"><FileCode class="h-3.5 w-3.5" />Smart contract</Badge>{/if}
		{#if minted.length}<Badge tone="brand"><Sparkles class="h-3.5 w-3.5" />Mint</Badge>{/if}
		{#if burned.length}<Badge tone="warn"><Flame class="h-3.5 w-3.5" />Burn</Badge>{/if}
		{#if data.metadata.length}<Badge tone="cyan"><MessageSquare class="h-3.5 w-3.5" />Metadata</Badge>{/if}
	{/snippet}
</PageHeader>

<div class="mb-6 grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
	<Panel title="Overview">
		<dl class="grid grid-cols-2 gap-x-6 gap-y-5 lg:grid-cols-3">
			<KV label="Block">
				{#if tx.block_height >= 0}
					<a class="y-link mono font-medium" href="/blocks/{tx.block_height}">#{num(tx.block_height)}</a>
				{:else}<span class="text-muted">Genesis</span>{/if}
			</KV>
			<KV label="Time">{#if data.block}<TimeAgo ts={data.block.time} />{:else}—{/if}</KV>
			<KV label="Epoch / slot">
				{#if data.block}<span>{data.block.epoch}</span><span class="text-faint"> / </span>{/if}<span class="mono">{num(tx.slot)}</span>
			</KV>
			<KV label="Fee"><Ada value={tx.fees} decimals={6} class="font-semibold" /></KV>
			<KV label="Total output"><Ada value={tx.total_output} decimals={6} /></KV>
			<KV label="Validity">
				{#if tx.validity_interval_start || tx.ttl}
					<span class="mono text-[13px]">{tx.validity_interval_start ? num(tx.validity_interval_start) : '…'} → {tx.ttl ? num(tx.ttl) : '∞'}</span>
				{:else}<span class="text-muted">No limit</span>{/if}
			</KV>
			{#if tx.script_data_hash}<KV label="Script data hash"><Hash value={tx.script_data_hash} /></KV>{/if}
			{#if tx.auxiliary_data_hash}<KV label="Metadata hash"><Hash value={tx.auxiliary_data_hash} /></KV>{/if}
			{#if tx.total_collateral}<KV label="Collateral"><Ada value={tx.total_collateral} decimals={6} /></KV>{/if}
		</dl>
	</Panel>

	<Panel title="Where the ADA went">
		<BalanceBar inputs={sumLovelace(tx.inputs)} outputs={sumLovelace(tx.outputs)} fee={BigInt(tx.fees ?? 0)} withdrawals={withdrawn} />
		{#if data.mints.length}
			<div class="mt-5 border-t border-line pt-4">
				<div class="y-label mb-2">Tokens {minted.length && burned.length ? 'minted and burned' : minted.length ? 'minted' : 'burned'}</div>
				<div class="flex flex-wrap gap-1.5">
					{#each data.mints.slice(0, 8) as m (m.unit)}
						<AssetChip unit={m.unit} quantity={(Number(m.quantity) > 0 ? '+' : '') + m.quantity} />
					{/each}
					{#if data.mints.length > 8}<button class="text-xs text-brand hover:underline" onclick={() => (active = 'mints')}>+{data.mints.length - 8} more</button>{/if}
				</div>
			</div>
		{/if}
	</Panel>
</div>

<section class="y-panel overflow-hidden">
	<Tabs {tabs} bind:active param="tab" />
	<div class="p-4 sm:p-5">
		{#if active === 'utxos'}
			<UtxoFlow inputs={tx.inputs} outputs={tx.outputs} fee={tx.fees} ondatum={(u) => (datumOf = u)} />
		{:else if active === 'scripts'}
			<div class="grid gap-4">
				{#each data.scripts as s, i (s.script_hash + i)}
					<ScriptCard script={s} maxMem={data.limits.mem} maxSteps={data.limits.steps} />
				{/each}
			</div>
		{:else if active === 'mints'}
			<div class="overflow-x-auto">
				<table class="w-full text-sm">
					<thead><tr class="border-b border-line text-left"><th class="y-label py-2 pr-4">Asset</th><th class="y-label py-2 pr-4">Policy</th><th class="y-label py-2 pr-4">Fingerprint</th><th class="y-label py-2 text-right">Quantity</th></tr></thead>
					<tbody>
						{#each data.mints as m (m.unit)}
							<tr class="border-b border-line/60 last:border-0">
								<td class="py-2.5 pr-4"><a class="y-link font-medium" href="/assets/unit/{m.unit}">{parseUnit(m.unit).name}</a></td>
								<td class="py-2.5 pr-4"><Hash value={m.policy} href="/assets/policy/{m.policy}" head={8} tail={6} /></td>
								<td class="py-2.5 pr-4"><Hash value={m.fingerprint ?? m.asset_name} href="/assets/fingerprint/{m.fingerprint ?? m.asset_name}" head={10} tail={6} /></td>
								<td class="py-2.5 text-right font-semibold tabular-nums {Number(m.quantity) > 0 ? 'text-mint' : 'text-danger'}">{Number(m.quantity) > 0 ? '+' : ''}{num(m.quantity)}</td>
							</tr>
						{/each}
					</tbody>
				</table>
			</div>
		{:else if active === 'metadata'}
			<div class="grid gap-4">
				{#each data.metadata as m (m.label)}
					<div>
						<div class="mb-2 flex items-center gap-2">
							<Badge tone="cyan">Label {m.label}</Badge>
							{#if METADATA_LABELS[m.label]}<span class="text-sm text-muted">{METADATA_LABELS[m.label]}</span>{/if}
						</div>
						<JsonView value={m.json_metadata ?? m.body} label="Label {m.label}" />
					</div>
				{/each}
			</div>
		{:else if active === 'withdrawals'}
			<ul class="divide-y divide-line">
				{#each data.withdrawals as w (w.address)}
					<li class="flex items-center justify-between gap-3 py-3">
						<Hash value={w.address} href="/stake/{w.address}" head={14} tail={8} />
						<Ada value={w.amount} decimals={6} class="font-semibold" />
					</li>
				{/each}
			</ul>
		{:else if active === 'collateral'}
			<div class="grid gap-6 lg:grid-cols-2">
				<div>
					<div class="y-label mb-2">Collateral inputs</div>
					<div class="flex flex-col gap-2.5">
						{#each tx.collateral_inputs ?? [] as u (u.tx_hash + u.output_index)}<UtxoCard utxo={u} side="in" />{/each}
					</div>
				</div>
				<div>
					<div class="y-label mb-2">Collateral return</div>
					{#if tx.collateral_return}<UtxoCard utxo={tx.collateral_return} side="out" />{:else}<p class="text-sm text-muted">None</p>{/if}
					<p class="mt-3 text-xs text-muted">Collateral is only taken if a script fails. {tx.invalid ? 'It failed, so it was.' : 'This transaction succeeded, so it was not.'}</p>
				</div>
			</div>
		{:else if active === 'references'}
			<p class="mb-3 text-sm text-muted">Read but not spent: these outputs supply reference scripts or datums.</p>
			<div class="grid gap-2.5 lg:grid-cols-2">
				{#each tx.reference_inputs ?? [] as u (u.tx_hash + u.output_index)}<UtxoCard utxo={u} side="in" ondatum={(x) => (datumOf = x)} />{/each}
			</div>
		{:else if active === 'signers'}
			<p class="mb-3 text-sm text-muted">Key hashes that must sign this transaction (for scripts that check signatures).</p>
			<ul class="flex flex-col gap-2">
				{#each tx.required_signers ?? [] as s (s)}<li class="rounded-xl border border-line bg-surface-2/60 px-3 py-2"><Hash value={s} full /></li>{/each}
			</ul>
		{:else if active === 'raw'}
			<JsonView value={data.tx} label="Transaction" maxHeight={560} />
		{:else}
			<Empty />
		{/if}
	</div>
</section>

<DatumDialog bind:utxo={datumOf} />
