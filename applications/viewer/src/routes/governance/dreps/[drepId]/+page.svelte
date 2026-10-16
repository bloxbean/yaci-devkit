<script lang="ts">
	import { shorten, num } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import KV from '$lib/ui/KV.svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Tabs from '$lib/ui/Tabs.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import AnchorLink from '$lib/governance/AnchorLink.svelte';
	import { actionHref, credLabel, statusLabel, statusTone, voteLabel, voteTone } from '$lib/governance/labels';
	import Users from '@lucide/svelte/icons/users';

	let { data } = $props();
	const d = $derived(data.drep);
	const a = $derived(data.activity);
</script>

<svelte:head><title>DRep {shorten(d.drep_id, 10, 6)} · Yaci Viewer</title></svelte:head>

<PageHeader kind="DRep" title={shorten(d.drep_id, 14, 10)}>
	{#snippet icon()}<Users class="h-5 w-5" />{/snippet}
	<div class="flex items-start gap-1.5">
		<span class="mono break-all text-xs text-faint">{d.drep_id}</span>
		<CopyButton value={d.drep_id} label="DRep id copied" />
	</div>
	{#snippet actions()}
		<Badge tone={statusTone(d.status)}>{statusLabel(d.status)}</Badge>
		<Badge tone="neutral">{credLabel(d.drep_type)}</Badge>
	{/snippet}
</PageHeader>

<section class="mb-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
	<div class="y-panel relative overflow-hidden p-5 xl:col-span-2">
		<div class="pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-brand/10 blur-2xl"></div>
		<div class="y-label">Voting power</div>
		<div class="mt-1 font-display text-4xl font-semibold tracking-tight"><Ada value={d.voting_power} decimals={0} /></div>
		<div class="mt-2 text-sm text-muted">stake delegated to this DRep</div>
	</div>
	<div class="y-panel p-5">
		<div class="y-label">Deposit</div>
		<div class="mt-2 font-display text-xl font-semibold"><Ada value={d.deposit} decimals={0} /></div>
		<div class="mt-1 text-xs text-muted">returned when the DRep retires</div>
	</div>
	<div class="y-panel p-5">
		<div class="y-label">Activity</div>
		{#if a}
			<div class="mt-2 text-sm">Last voted in epoch <b>{a.last_active_epoch ?? '—'}</b></div>
			<div class="mt-1 text-xs text-muted">active since epoch {a.active_epoch ?? '—'}{a.expired ? ' · expired' : ''}{a.retired ? ' · retired' : ''}</div>
		{:else}
			<div class="mt-2 text-sm text-muted">Registered at slot {num(d.registration_slot)}</div>
		{/if}
	</div>
</section>

<Panel title="Details" class="mb-6">
	<dl class="grid gap-x-6 gap-y-5 sm:grid-cols-2 lg:grid-cols-3">
		<KV label="Credential hash" wide><Hash value={d.drep_hash} head={16} tail={12} /></KV>
		<KV label="Registered at slot"><span class="mono">{num(d.registration_slot)}</span></KV>
		<KV label="Profile" wide>{#if data.metadata?.url}<AnchorLink url={data.metadata.url} hash={data.metadata.hash} />{:else}<span class="text-muted">None published</span>{/if}</KV>
	</dl>
</Panel>

<section class="y-panel overflow-hidden">
	<Tabs tabs={[{ id: 'delegators', label: 'Delegations' }, { id: 'votes', label: 'Votes cast' }]} active={data.tab} link />
	{#if !data.rowsAvailable}
		<Empty title="Not available" text={data.tab === 'votes' ? "This store doesn't list votes by DRep." : "This store doesn't list delegations by DRep."} />
	{:else if data.tab === 'votes'}
		<DataTable
			rows={data.rows}
			columns={[{ key: 'vote', label: 'Vote' }, { key: 'action', label: 'On action' }, { key: 'tx', label: 'In transaction', align: 'right' }]}
			key={(v) => v.tx_hash + v.cert_index}
			rowHref={(v) => actionHref(v.proposal_tx_hash, v.proposal_cert_index)}
			emptyTitle="No votes yet"
		>
			{#snippet cell(v, key)}
				{#if key === 'vote'}<Badge tone={voteTone(v.vote)}>{voteLabel(v.vote)}</Badge>
				{:else if key === 'action'}<a class="mono text-[13px] text-ink hover:text-brand" href={actionHref(v.proposal_tx_hash, v.proposal_cert_index)}>{shorten(v.proposal_id ?? v.proposal_tx_hash, 14, 8)}</a>
				{:else}<a class="mono text-xs text-muted hover:text-brand" href="/transactions/{v.tx_hash}">{shorten(v.tx_hash, 8, 6)}</a>{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{:else}
		<DataTable
			rows={data.rows}
			columns={[{ key: 'address', label: 'Stake account' }, { key: 'epoch', label: 'Epoch', minor: true }, { key: 'tx', label: 'Transaction', minor: true }, { key: 'age', label: 'When', align: 'right' }]}
			key={(r) => r.tx_hash + r.cert_index}
			emptyTitle="No delegations yet"
			emptyText="Stake accounts that delegate their voting power to this DRep appear here."
		>
			{#snippet cell(r, key)}
				{#if key === 'address'}<Hash value={r.address} href="/stake/{r.address}" head={14} tail={8} />
				{:else if key === 'epoch'}{r.epoch ?? '—'}
				{:else if key === 'tx'}<a class="mono text-xs text-muted hover:text-brand" href="/transactions/{r.tx_hash}">{shorten(r.tx_hash, 8, 6)}</a>
				{:else}<TimeAgo ts={r.block_time} class="text-muted" />{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{/if}
</section>
