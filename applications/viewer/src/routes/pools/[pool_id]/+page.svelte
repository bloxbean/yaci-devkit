<script lang="ts">
	import { adaCompact, num, shorten, percent } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import KV from '$lib/ui/KV.svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Stat from '$lib/ui/Stat.svelte';
	import Tabs from '$lib/ui/Tabs.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import { marginPct, poolStatus } from '$lib/staking/pool';
	import Server from '@lucide/svelte/icons/server';
	import Globe from '@lucide/svelte/icons/globe';
	import Layers from '@lucide/svelte/icons/layers';
	import Box from '@lucide/svelte/icons/box';
	import Users from '@lucide/svelte/icons/users';
	import ExternalLink from '@lucide/svelte/icons/external-link';
	import ChevronLeft from '@lucide/svelte/icons/chevron-left';
	import ChevronRight from '@lucide/svelte/icons/chevron-right';

	let { data } = $props();
	const p = $derived(data.pool);
	const m = $derived(data.metadata);
	const status = $derived(poolStatus(p.retire_epoch, data.epoch));
	const smallShare = (part: number, whole: number) => (part > 0 && part / whole < 0.0001 ? '<0.01%' : percent(part, whole, 2));
	const share = $derived(data.stake != null && data.totalStake ? percent(data.stake, data.totalStake, 3) : null);
	const title = $derived(m?.ticker ? `[${m.ticker}] ${m.name ?? ''}`.trim() : shorten(p.pool_id, 14, 8));
	const relays = $derived((p.relays ?? []) as { dnsName?: string | null; ipv4?: string | null; ipv6?: string | null; port?: number | null }[]);
	const shownBlocks = $derived((data.blocks ?? []).slice(0, 50));
</script>

<svelte:head><title>{m?.ticker ?? shorten(p.pool_id, 10, 6)} · Stake pool · Yaci Viewer</title></svelte:head>

<PageHeader kind="Stake pool" {title}>
	{#snippet icon()}<Server class="h-5 w-5" />{/snippet}
	<div class="flex flex-col gap-1">
		<div class="flex items-center gap-1.5"><span class="mono break-all text-xs text-faint">{p.pool_id}</span><CopyButton value={p.pool_id} label="Pool id copied" /></div>
		{#if m?.description}<p class="max-w-3xl text-sm">{m.description}</p>{/if}
	</div>
	{#snippet actions()}
		{#if status === 'active'}<Badge tone="ok">Active</Badge>{:else if status === 'retiring'}<Badge tone="warn">Retiring in epoch {p.retire_epoch}</Badge>{:else}<Badge tone="bad">Retired in epoch {p.retire_epoch}</Badge>{/if}
		{#if m?.homepage}<a class="y-btn h-8 text-xs" href={m.homepage} target="_blank" rel="noopener noreferrer nofollow"><Globe class="h-3.5 w-3.5" />Website</a>{/if}
	{/snippet}
</PageHeader>

<section class="mb-6 grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
	<Stat
		label="Active stake"
		value={data.stake != null ? `₳${adaCompact(data.stake)}` : '—'}
		sub={share ? `${share} of all active stake · epoch ${data.epoch}` : `epoch ${data.epoch}`}
		trend={data.stakeTrend.map((t) => t.stake)}
	>
		{#snippet icon()}<Layers class="h-4 w-4" />{/snippet}
	</Stat>
	<Stat label="Pledge" sub="committed by the owners">
		{#snippet children()}<div class="font-display text-2xl font-semibold"><Ada value={p.pledge} decimals={0} /></div>{/snippet}
	</Stat>
	<Stat label="Fees" sub="fixed cost per epoch + margin">
		{#snippet children()}<div class="font-display text-2xl font-semibold"><Ada value={p.cost} decimals={0} /> <span class="text-base text-muted">+ {marginPct(p.margin)}</span></div>{/snippet}
	</Stat>
	<Stat label="Blocks this epoch" value={data.blocks ? num(data.blocks.length) : '—'} sub="epoch {data.epoch}">
		{#snippet icon()}<Box class="h-4 w-4" />{/snippet}
	</Stat>
</section>

<div class="mb-6 grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
	<Panel title="Registration">
		<dl class="grid gap-x-6 gap-y-5 sm:grid-cols-2">
			<KV label="Pool hash" wide><Hash value={p.pool_hash} head={16} tail={12} /></KV>
			<KV label="Reward account" wide><Hash value={p.reward_account} href="/stake/{p.reward_account}" head={16} tail={10} /></KV>
			<KV label="VRF key hash" wide><Hash value={p.vrf_key_hash} head={16} tail={12} /></KV>
			<KV label="Owners" wide>
				{#if p.pool_owners?.length}
					<ul class="flex flex-col gap-1">{#each p.pool_owners as o (o)}<li><Hash value={o} href="/stake/{o}" head={14} tail={8} /></li>{/each}</ul>
				{:else}<span class="text-muted">None listed</span>{/if}
			</KV>
			<KV label="Last certificate">
				{#if p.tx_hash && p.tx_hash !== 'Genesis'}<a class="y-link mono text-[13px]" href="/transactions/{p.tx_hash}">{shorten(p.tx_hash, 8, 6)}</a>{:else}<span class="text-muted">Genesis</span>{/if}
				<span class="text-faint"> · epoch {p.epoch}</span>
			</KV>
			<KV label="Type"><span class="text-muted">{(p.status ?? 'registration').toLowerCase()}</span></KV>
		</dl>
	</Panel>

	<div class="flex flex-col gap-6">
		<Panel title="Relays">
			{#if relays.length}
				<ul class="flex flex-col gap-2">
					{#each relays as r, i (i)}
						<li class="flex items-center justify-between gap-3 rounded-xl border border-line bg-surface-2/60 px-3 py-2 text-sm">
							<span class="mono min-w-0 truncate">{r.dnsName ?? r.ipv4 ?? r.ipv6 ?? '—'}</span>
							{#if r.port}<span class="mono shrink-0 text-xs text-muted">:{r.port}</span>{/if}
						</li>
					{/each}
				</ul>
			{:else}
				<p class="text-sm text-muted">No relays registered. Devnet pools often have none.</p>
			{/if}
		</Panel>
		<Panel title="Metadata">
			{#if p.metadata_url}
				<a class="y-link flex items-center gap-1.5 break-all text-sm" href={p.metadata_url} target="_blank" rel="noopener noreferrer nofollow">{p.metadata_url}<ExternalLink class="h-3.5 w-3.5 shrink-0" /></a>
				<div class="mt-2"><Hash value={p.metadata_hash} head={12} tail={8} /></div>
				{#if !m}<p class="mt-2 text-xs text-faint">Couldn't read the metadata file, so the name and ticker aren't shown.</p>{/if}
			{:else}
				<p class="text-sm text-muted">No metadata registered.</p>
			{/if}
		</Panel>
	</div>
</div>

<section class="y-panel overflow-hidden">
	<Tabs
		tabs={[{ id: 'delegators', label: 'Delegators' }, { id: 'blocks', label: 'Blocks this epoch', count: data.blocks?.length ?? null }, { id: 'rewards', label: 'Rewards' }]}
		active={data.tab}
		link
	/>
	{#if data.listError}
		<ErrorBox title="Couldn't load this list" message={data.listError} />
	{:else if data.tab === 'delegators'}
		<DataTable
			rows={data.rows}
			columns={[{ key: 'address', label: 'Stake address' }, { key: 'since', label: 'Delegating since', minor: true }, { key: 'amount', label: 'Stake', align: 'right' }, { key: 'share', label: 'Share of pool', align: 'right', minor: true }]}
			key={(r) => r.address}
			emptyTitle="No delegators this epoch"
			emptyText="Delegations count from the stake snapshot, two epochs after they are made."
		>
			{#snippet cell(r, key)}
				{#if key === 'address'}<a class="mono text-[13px] text-ink hover:text-brand" href="/stake/{r.address}">{shorten(r.address, 14, 8)}</a>
				{:else if key === 'since'}<span class="text-muted">epoch {r.delegation_epoch ?? '—'}</span>
				{:else if key === 'amount'}<Ada value={r.amount} class="font-semibold" />
				{:else if key === 'share'}<span class="tabular-nums text-muted">{data.stake ? smallShare(Number(r.amount), data.stake) : '—'}</span>{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{:else if data.tab === 'blocks'}
		{#if shownBlocks.length}
			<DataTable rows={shownBlocks} columns={[{ key: 'number', label: 'Block' }, { key: 'hash', label: 'Hash' }]} key={(b) => b.number} rowHref={(b) => `/blocks/${b.number}`}>
				{#snippet cell(b, key)}
					{#if key === 'number'}<a class="y-link mono font-medium" href="/blocks/{b.number}">#{num(b.number)}</a>{:else}<span class="mono text-xs text-muted">{shorten(b.hash, 16, 12)}</span>{/if}
				{/snippet}
			</DataTable>
			{#if (data.blocks?.length ?? 0) > shownBlocks.length}<p class="border-t border-line px-4 py-3 text-xs text-muted">Showing the latest {shownBlocks.length} of {num(data.blocks?.length ?? 0)} blocks.</p>{/if}
		{:else}
			<Empty title="No blocks yet this epoch" text={data.blocks ? 'This pool has not made a block in the current epoch.' : 'Block data is not available from this store.'} />
		{/if}
	{:else}
		<div class="flex items-center justify-between gap-3 border-b border-line px-4 py-2.5 text-sm">
			<span class="text-muted">Rewards paid out in epoch <b class="text-ink">{data.rewardEpoch}</b></span>
			<div class="flex gap-1.5">
				<a class="y-btn h-8 px-2.5 {data.rewardEpoch <= 0 ? 'pointer-events-none opacity-40' : ''}" href="?tab=rewards&epoch={data.rewardEpoch - 1}" data-sveltekit-noscroll><ChevronLeft class="h-4 w-4" /></a>
				<a class="y-btn h-8 px-2.5 {data.rewardEpoch >= data.epoch ? 'pointer-events-none opacity-40' : ''}" href="?tab=rewards&epoch={data.rewardEpoch + 1}" data-sveltekit-noscroll><ChevronRight class="h-4 w-4" /></a>
			</div>
		</div>
		<DataTable
			rows={data.rows}
			columns={[{ key: 'address', label: 'Stake address' }, { key: 'type', label: 'Type' }, { key: 'earned', label: 'Earned in', minor: true }, { key: 'amount', label: 'Amount', align: 'right' }]}
			key={(r) => `${r.address}-${r.type}-${r.earned_epoch}`}
			emptyTitle="No rewards in this epoch"
			emptyText="Rewards are paid two epochs after the epoch they are earned in."
		>
			{#snippet cell(r, key)}
				{#if key === 'address'}<a class="mono text-[13px] text-ink hover:text-brand" href="/stake/{r.address}">{shorten(r.address, 14, 8)}</a>
				{:else if key === 'type'}<Badge tone={r.type === 'leader' ? 'violet' : 'ok'}>{r.type}</Badge>
				{:else if key === 'earned'}<span class="text-muted">epoch {r.earned_epoch}</span>
				{:else}<Ada value={r.reward ?? r.amount} decimals={6} class="font-semibold" />{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{/if}
</section>

<p class="mt-4 flex items-center gap-1.5 text-xs text-faint"><Users class="h-3.5 w-3.5" />Stake and delegators come from the epoch {data.epoch} snapshot.</p>
