<script lang="ts">
	import { num, adaCompact, shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import Stat from '$lib/ui/Stat.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Ring from '$lib/ui/Ring.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import ProposalTable from '$lib/governance/ProposalTable.svelte';
	import AnchorLink from '$lib/governance/AnchorLink.svelte';
	import DRepPower from '$lib/insights/DRepPower.svelte';
	import { resolved } from '$lib/ui/resolved.svelte';
	import { actionHref, voteLabel, voteTone, voterGroup, VOTER_LABEL } from '$lib/governance/labels';
	import Landmark from '@lucide/svelte/icons/landmark';
	import ScrollText from '@lucide/svelte/icons/scroll-text';
	import Users from '@lucide/svelte/icons/users';
	import Gavel from '@lucide/svelte/icons/gavel';
	import Scale from '@lucide/svelte/icons/scale';
	import Vote from '@lucide/svelte/icons/vote';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';
	import UserPlus from '@lucide/svelte/icons/user-plus';
	import NameTag from '$lib/names/NameTag.svelte';

	let { data } = $props();
	// Streamed, and only where the store has Yaci Store analytics (null otherwise).
	const power = resolved(() => data.drepPower);

	const d = $derived(data.stats?.drep_stats);
	const p = $derived(data.stats?.proposal_stats);
	const activeMembers = $derived(data.committee?.members.filter((m) => m.active).length ?? 0);
	const threshold = $derived(data.committee?.threshold);
	const abstain = $derived(data.special.find((s) => s.drep_type === 'ABSTAIN')?.voting_power);
	const noConfidence = $derived(data.special.find((s) => s.drep_type === 'NO_CONFIDENCE')?.voting_power);

	const links = [
		{ href: '/governance/proposals', label: 'Proposals', text: 'Every governance action and how it was voted', icon: ScrollText },
		{ href: '/governance/dreps', label: 'DReps', text: 'Delegated representatives and their voting power', icon: Users },
		{ href: '/governance/votes', label: 'Votes', text: 'Every vote cast by DReps, pools and the committee', icon: Vote },
		{ href: '/governance/drep-registrations', label: 'DRep activity', text: 'Registrations, updates and retirements', icon: UserPlus }
	];
</script>

<svelte:head><title>Governance · Yaci Viewer</title></svelte:head>

<PageHeader kind="Governance" title="Governance">
	{#snippet icon()}<Landmark class="h-5 w-5" />{/snippet}
	Conway on-chain governance: proposals, DReps, the constitutional committee and the constitution{#if data.stats?.epoch != null} · epoch {data.stats.epoch}{/if}.
</PageHeader>

<section class="mb-6 grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
	<Stat label="Active proposals" value={p ? num(p.active_proposals) : data.activeCount != null ? num(data.activeCount) : '—'} sub={p ? `${num(p.ratified_proposals)} ratified · ${num(p.expired_proposals)} expired` : 'being voted on now'} href="/governance/proposals">
		{#snippet icon()}<ScrollText class="h-4 w-4" />{/snippet}
	</Stat>
	<Stat label="Active DReps" value={d ? num(d.active_dreps) : '—'} sub={d ? `${num(d.total_dreps)} registered · ${num(d.retired_dreps)} retired` : 'delegated representatives'} href="/governance/dreps">
		{#snippet icon()}<Users class="h-4 w-4" />{/snippet}
	</Stat>
	<Stat label="DRep voting power" value={d ? `₳${adaCompact(d.total_voting_power)}` : '—'} sub={abstain != null ? `₳${adaCompact(abstain)} always abstain` : 'delegated to DReps'}>
		{#snippet icon()}<Vote class="h-4 w-4" />{/snippet}
	</Stat>
	<Stat label="Committee" value={data.committee ? `${activeMembers} active` : '—'} sub={threshold ? `${threshold.numerator}/${threshold.denominator} needed to approve` : 'constitutional committee'} href="/governance/committee">
		{#snippet icon()}<Gavel class="h-4 w-4" />{/snippet}
	</Stat>
</section>

<div class="mb-6 grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
	<Panel title="Latest proposals" flush>
		{#snippet icon()}<ScrollText class="h-4 w-4" />{/snippet}
		{#snippet actions()}<a href="/governance/proposals" class="y-btn h-8 text-xs">All proposals <ArrowRight class="h-3.5 w-3.5" /></a>{/snippet}
		<ProposalTable proposals={data.proposals} compact emptyText="Submit a governance action on your devnet and it shows up here." />
	</Panel>

	<div class="flex flex-col gap-6">
		<Panel title="Constitutional committee">
			{#snippet icon()}<Gavel class="h-4 w-4" />{/snippet}
			{#if data.committee && data.committee.members.length}
				<div class="flex items-center gap-5">
					<Ring value={threshold ? threshold.numerator / threshold.denominator : 0} size={96} stroke={9}>
						<span class="font-display text-lg font-semibold tabular-nums">{threshold ? `${threshold.numerator}/${threshold.denominator}` : '—'}</span>
					</Ring>
					<div class="text-sm text-muted">
						<p><b class="text-ink">{activeMembers}</b> of {data.committee.members.length} members active.</p>
						<p class="mt-1">An action needs {threshold ? `${Math.round((threshold.numerator / threshold.denominator) * 100)}%` : 'a threshold'} of the committee's yes votes.</p>
						<a class="y-link mt-2 inline-block text-xs" href="/governance/committee">See members</a>
					</div>
				</div>
			{:else}
				<p class="text-sm text-muted">No committee on this chain{data.committee ? ' yet' : ', or the store does not index it'}.</p>
			{/if}
		</Panel>

		<Panel title="Constitution">
			{#snippet icon()}<Scale class="h-4 w-4" />{/snippet}
			{#if data.constitution}
				<dl class="grid gap-3 text-sm">
					<div><dt class="y-label mb-1">Document</dt><dd><AnchorLink url={data.constitution.anchor_url} hash={data.constitution.anchor_hash} /></dd></div>
					<div class="flex gap-6">
						<div><dt class="y-label mb-1">Since epoch</dt><dd>{data.constitution.active_epoch ?? '—'}</dd></div>
						<div class="min-w-0"><dt class="y-label mb-1">Guardrail script</dt><dd>{#if data.constitution.script}<a class="y-link mono text-xs" href="/scripts/{data.constitution.script}">{shorten(data.constitution.script, 8, 6)}</a>{:else}<span class="text-muted">None</span>{/if}</dd></div>
					</div>
				</dl>
			{:else}
				<p class="text-sm text-muted">No constitution has been enacted on this chain.</p>
			{/if}
		</Panel>
	</div>
</div>

<div class="grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
	<Panel title="Latest votes" flush>
		{#snippet icon()}<Vote class="h-4 w-4" />{/snippet}
		{#snippet actions()}<a href="/governance/votes" class="y-btn h-8 text-xs">All votes <ArrowRight class="h-3.5 w-3.5" /></a>{/snippet}
		{#if data.votes.length}
			<ul class="divide-y divide-line/70">
				{#each data.votes as v (v.id ?? v.tx_hash + v.voter_hash)}
					<li>
						<a href={actionHref(v.gov_action_tx_hash, v.gov_action_index)} class="flex items-center gap-3 px-5 py-3 transition-colors hover:bg-surface-2/70">
							<Badge tone={voteTone(v.vote)}>{voteLabel(v.vote)}</Badge>
							<span class="min-w-0 flex-1">
								<span class="block truncate text-sm">{VOTER_LABEL[voterGroup(v.voter_type)].replace(/s$/, '')} <span class="mono text-muted">{shorten(v.drep_id ?? v.voter_hash, 10, 6)}</span></span>
								<span class="mono block truncate text-xs text-faint">on {shorten(v.gov_action_tx_hash, 8, 6)}#{v.gov_action_index}</span>
								<NameTag kind="action" id="{v.gov_action_tx_hash}#{v.gov_action_index}" block class="text-xs text-muted" />
							</span>
							<TimeAgo ts={v.block_time} class="shrink-0 text-xs text-muted" />
						</a>
					</li>
				{/each}
			</ul>
		{:else}
			<Empty title="No votes yet" text="Votes by DReps, stake pools and the committee appear here." />
		{/if}
	</Panel>

	<div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-1">
		{#each links as l (l.href)}
			<a href={l.href} class="y-panel group flex items-center gap-3 p-4 transition hover:-translate-y-0.5 hover:border-brand/40">
				<span class="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-brand/10 text-brand"><l.icon class="h-5 w-5" /></span>
				<span class="min-w-0 flex-1">
					<span class="block font-medium">{l.label}</span>
					<span class="block truncate text-xs text-muted">{l.text}</span>
				</span>
				<ArrowRight class="h-4 w-4 shrink-0 text-faint transition group-hover:translate-x-0.5 group-hover:text-brand" />
			</a>
		{/each}
	</div>
</div>

{#if power.value}<DRepPower power={power.value} />{/if}

{#if noConfidence != null}
	<p class="mt-6 text-xs text-faint">Also delegated: ₳{adaCompact(noConfidence)} to "always no confidence".</p>
{/if}
