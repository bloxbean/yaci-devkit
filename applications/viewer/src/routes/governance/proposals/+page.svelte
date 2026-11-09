<script lang="ts">
	import { goto } from '$app/navigation';
	import { page as pageState } from '$app/state';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Tabs from '$lib/ui/Tabs.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import ProposalTable from '$lib/governance/ProposalTable.svelte';
	import { ACTION_LABEL } from '$lib/governance/labels';
	import ScrollText from '@lucide/svelte/icons/scroll-text';

	let { data } = $props();

	function setType(e: Event) {
		const u = new URL(pageState.url);
		const v = (e.target as HTMLSelectElement).value;
		if (v) u.searchParams.set('type', v);
		else u.searchParams.delete('type');
		u.searchParams.delete('page');
		u.searchParams.delete('view');
		goto(u.pathname + u.search, { noScroll: true, keepFocus: true });
	}
</script>

<svelte:head><title>Proposals · Yaci Viewer</title></svelte:head>

<PageHeader kind="Governance" title="Proposals">
	{#snippet icon()}<ScrollText class="h-5 w-5" />{/snippet}
	Governance actions submitted on chain, newest first. Open one to see its details and votes.
</PageHeader>

<section class="y-panel overflow-hidden">
	<div class="flex items-center justify-between gap-2 pr-3">
		<div class="min-w-0 flex-1">
			<Tabs tabs={[{ id: 'all', label: 'All' }, { id: 'live', label: 'Voting now' }]} active={data.view} param="view" link />
		</div>
		{#if data.view === 'all'}
			<select class="y-input h-8 w-auto shrink-0 py-0 text-xs" value={data.type} onchange={setType} aria-label="Filter by action type">
				<option value="">All types</option>
				{#each Object.entries(ACTION_LABEL) as [id, label] (id)}<option value={id}>{label}</option>{/each}
			</select>
		{/if}
	</div>
	<ProposalTable proposals={data.proposals} emptyText={data.view === 'live' ? 'Nothing is being voted on right now.' : undefined} />
	{#if data.paged}<Pager page={data.page} hasNext={data.proposals.length === data.count} />{/if}
</section>
