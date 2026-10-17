<script lang="ts">
	import { shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import AnchorLink from '$lib/governance/AnchorLink.svelte';
	import { actionHref, voteLabel, voteTone, voterGroup, voterHref, VOTER_LABEL } from '$lib/governance/labels';
	import Vote from '@lucide/svelte/icons/vote';

	let { data } = $props();
	const groupTone = { drep: 'violet', spo: 'cyan', cc: 'brand' } as const;
</script>

<svelte:head><title>Votes · Yaci Viewer</title></svelte:head>

<PageHeader kind="Governance" title="Votes">
	{#snippet icon()}<Vote class="h-5 w-5" />{/snippet}
	Votes cast by DReps, stake pools and the constitutional committee, newest first.
</PageHeader>

<section class="y-panel overflow-hidden">
	<DataTable
		rows={data.votes}
		columns={[
			{ key: 'vote', label: 'Vote' },
			{ key: 'voter', label: 'Voter' },
			{ key: 'action', label: 'On action' },
			{ key: 'rationale', label: 'Rationale', minor: true },
			{ key: 'age', label: 'When', align: 'right' }
		]}
		key={(v) => v.id ?? v.tx_hash + v.voter_hash + v.gov_action_index}
		rowHref={(v) => actionHref(v.gov_action_tx_hash, v.gov_action_index)}
		emptyTitle="No votes yet"
		emptyText="Votes appear once DReps, pools or the committee vote on a governance action."
	>
		{#snippet cell(v, key)}
			{#if key === 'vote'}<Badge tone={voteTone(v.vote)}>{voteLabel(v.vote)}</Badge>
			{:else if key === 'voter'}
				{@const g = voterGroup(v.voter_type)}
				{@const href = voterHref(v)}
				<div class="flex items-center gap-2">
					<Badge tone={groupTone[g]} class="py-0 text-[10px]">{g === 'cc' ? 'CC' : g === 'spo' ? 'SPO' : 'DRep'}</Badge>
					{#if href}<a class="mono text-[13px] text-ink hover:text-brand" {href} title={v.drep_id ?? v.voter_hash}>{shorten(v.drep_id ?? v.voter_hash, 12, 6)}</a>{:else}<span class="mono text-[13px]" title={v.voter_hash}>{shorten(v.voter_hash, 12, 6)}</span>{/if}
				</div>
				<span class="sr-only">{VOTER_LABEL[g]}</span>
			{:else if key === 'action'}<a class="mono text-[13px] text-muted hover:text-brand" href={actionHref(v.gov_action_tx_hash, v.gov_action_index)}>{shorten(v.gov_action_tx_hash, 8, 6)}#{v.gov_action_index}</a>
			{:else if key === 'rationale'}<AnchorLink url={v.anchor_url} hash={v.anchor_hash} />
			{:else}<a class="text-muted hover:text-brand" href="/transactions/{v.tx_hash}"><TimeAgo ts={v.block_time} /></a>{/if}
		{/snippet}
	</DataTable>
	<Pager page={data.page} hasNext={data.votes.length === data.count} />
</section>
