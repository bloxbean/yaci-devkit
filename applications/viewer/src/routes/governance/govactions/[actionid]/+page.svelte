<script lang="ts">
	import { shorten, num } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import KV from '$lib/ui/KV.svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import JsonView from '$lib/ui/JsonView.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import VoteTally from '$lib/governance/VoteTally.svelte';
	import AnchorLink from '$lib/governance/AnchorLink.svelte';
	import GuardrailPanel from '$lib/julc/governance/GuardrailPanel.svelte';
	import { rewardHexToStake } from '$lib/governance/reward';
	import { actionLabel, actionTone, statusLabel, statusTone, tallyVotes, voteLabel, voteTone, voterGroup, voterHref, VOTER_LABEL } from '$lib/governance/labels';
	import ScrollText from '@lucide/svelte/icons/scroll-text';
	import Vote from '@lucide/svelte/icons/vote';
	import Coins from '@lucide/svelte/icons/coins';

	let { data } = $props();
	const p = $derived(data.proposal);
	const tally = $derived(tallyVotes(data.votes));
	const stats = $derived(p.voting_stats ?? {});
	const ratios = $derived({ drep: stats.drep_approval_ratio ?? null, spo: stats.spo_approval_ratio ?? null, cc: stats.cc_approval_ratio ?? null });

	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const details = $derived((p.details ?? {}) as Record<string, any>);
	const withdrawals = $derived(
		Object.entries((details.withdrawals ?? {}) as Record<string, number | string>).map(([hex, amount]) => ({ hex, stake: rewardHexToStake(hex), amount }))
	);
	const withdrawTotal = $derived(withdrawals.reduce((s, w) => s + BigInt(w.amount), 0n));

	let shown = $state(20);
	const voteRows = $derived(data.votes.slice(0, shown));
</script>

<svelte:head><title>{actionLabel(p.type)} · Governance · Yaci Viewer</title></svelte:head>

<PageHeader kind="Governance action" title={actionLabel(p.type)}>
	{#snippet icon()}<ScrollText class="h-5 w-5" />{/snippet}
	<div class="flex flex-wrap items-center gap-1.5">
		<span class="mono text-xs text-faint">{p.gov_action_id ?? `${p.tx_hash}#${p.index}`}</span>
		<CopyButton value={p.gov_action_id ?? `${p.tx_hash}#${p.index}`} label="Action id copied" />
	</div>
	{#snippet actions()}
		<Badge tone={actionTone(p.type)}>{actionLabel(p.type)}</Badge>
		{#if p.status}<Badge tone={statusTone(p.status)}>{statusLabel(p.status)}</Badge>{/if}
	{/snippet}
</PageHeader>

<div class="mb-6 grid gap-6 xl:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)]">
	<Panel title="Overview">
		<dl class="grid gap-x-6 gap-y-5 sm:grid-cols-2 lg:grid-cols-3">
			<KV label="Proposed in"><a class="y-link mono" href="/transactions/{p.tx_hash}">{shorten(p.tx_hash, 10, 6)}</a><span class="text-faint">#{p.index}</span></KV>
			<KV label="When">{#if p.block_time}<TimeAgo ts={p.block_time} />{:else}—{/if}{#if p.epoch != null}<span class="text-faint"> · epoch {p.epoch}</span>{/if}</KV>
			<KV label="Deposit"><Ada value={p.deposit} decimals={0} class="font-semibold" /></KV>
			<KV label="Deposit returns to" wide>{#if p.return_address}<Hash value={p.return_address} href="/stake/{p.return_address}" head={14} tail={8} />{:else}—{/if}</KV>
			<KV label="Block">{#if p.block_number != null}<a class="y-link mono" href="/blocks/{p.block_number}">#{num(p.block_number)}</a>{:else}—{/if}</KV>
			<KV label="Rationale" wide><AnchorLink url={p.anchor_url} hash={p.anchor_hash} /></KV>
		</dl>
	</Panel>

	<Panel title="Votes">
		{#snippet icon()}<Vote class="h-4 w-4" />{/snippet}
		{#if data.votesAvailable}
			<VoteTally {tally} {ratios} partial={data.votesPartial} />
		{:else}
			<p class="text-sm text-muted">This store doesn't return votes for this action.</p>
		{/if}
	</Panel>
</div>

<GuardrailPanel proposal={p} />

{#if withdrawals.length}
	<Panel title="Treasury withdrawal" sub="Total {withdrawals.length > 1 ? `to ${withdrawals.length} accounts` : ''}" class="mb-6">
		{#snippet icon()}<Coins class="h-4 w-4" />{/snippet}
		{#snippet actions()}<Ada value={withdrawTotal} decimals={0} class="font-display text-lg font-semibold" />{/snippet}
		<ul class="divide-y divide-line">
			{#each withdrawals as w (w.hex)}
				<li class="flex items-center justify-between gap-3 py-2.5">
					{#if w.stake}<Hash value={w.stake} href="/stake/{w.stake}" head={16} tail={8} />{:else}<Hash value={w.hex} />{/if}
					<Ada value={w.amount} decimals={0} class="font-semibold" />
				</li>
			{/each}
		</ul>
		{#if details.policyHash}<p class="mt-3 text-xs text-muted">Guarded by script <a class="y-link mono" href="/scripts/{details.policyHash}">{shorten(details.policyHash, 8, 6)}</a></p>{/if}
	</Panel>
{/if}

<div class="mb-6"><JsonView value={p.details} label="Action details" maxHeight={360} /></div>

<section class="y-panel overflow-hidden">
	<header class="y-panel-head">
		<div class="flex items-center gap-2.5">
			<span class="grid h-7 w-7 place-items-center rounded-lg bg-brand/10 text-brand"><Vote class="h-4 w-4" /></span>
			<h2 class="font-display text-[15px] font-semibold">Votes cast <span class="text-muted">· {data.votes.length}{data.votesPartial ? '+' : ''}</span></h2>
		</div>
	</header>
	<DataTable
		rows={voteRows}
		columns={[{ key: 'vote', label: 'Vote' }, { key: 'voter', label: 'Voter' }, { key: 'rationale', label: 'Rationale', minor: true }, { key: 'age', label: 'When', align: 'right' }]}
		key={(v) => v.id ?? v.tx_hash + v.voter_hash}
		emptyTitle="No votes yet"
		emptyText="DReps, stake pools and the committee vote on actions while they are live."
	>
		{#snippet cell(v, key)}
			{#if key === 'vote'}<Badge tone={voteTone(v.vote)}>{voteLabel(v.vote)}</Badge>
			{:else if key === 'voter'}
				{@const href = voterHref(v)}
				<div class="flex flex-col">
					<span class="text-xs text-muted">{VOTER_LABEL[voterGroup(v.voter_type)].replace(/s$/, '')}</span>
					{#if href}<a class="mono text-[13px] text-ink hover:text-brand" {href}>{shorten(v.drep_id ?? v.voter_hash, 12, 6)}</a>{:else}<span class="mono text-[13px]" title={v.voter_hash}>{shorten(v.voter_hash, 12, 6)}</span>{/if}
				</div>
			{:else if key === 'rationale'}<AnchorLink url={v.anchor_url} hash={v.anchor_hash} />
			{:else if key === 'age'}<a class="text-muted hover:text-brand" href="/transactions/{v.tx_hash}"><TimeAgo ts={v.block_time} /></a>{/if}
		{/snippet}
	</DataTable>
	{#if data.votes.length > shown}
		<div class="border-t border-line p-3 text-center">
			<button class="y-btn h-8 text-xs" onclick={() => (shown += 40)}>Show more ({data.votes.length - shown} left)</button>
		</div>
	{/if}
</section>
