<script lang="ts">
	import { goto } from '$app/navigation';
	import { num, shorten, adaCompact } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import { credLabel, statusLabel, statusTone } from '$lib/governance/labels';
	import { drepIdCandidates } from '$lib/governance/drep';
	import Users from '@lucide/svelte/icons/users';
	import Search from '@lucide/svelte/icons/search';

	let { data } = $props();

	let query = $state('');
	let problem = $state<string | null>(null);
	function find(e: Event) {
		e.preventDefault();
		const q = query.trim();
		if (!q) return;
		if (!drepIdCandidates(q).length) {
			problem = 'That is not a DRep id. Use drep1… or the 56-character hash.';
			return;
		}
		goto(`/governance/dreps/${q}`);
	}

	const abstain = $derived(data.special.find((s) => s.drep_type === 'ABSTAIN')?.voting_power);
	const noConfidence = $derived(data.special.find((s) => s.drep_type === 'NO_CONFIDENCE')?.voting_power);
</script>

<svelte:head><title>DReps · Yaci Viewer</title></svelte:head>

<PageHeader kind="Governance" title="DReps">
	{#snippet icon()}<Users class="h-5 w-5" />{/snippet}
	Delegated representatives vote on governance actions with the stake delegated to them.
	{#snippet actions()}
		<form class="flex w-full gap-2 sm:w-96" onsubmit={find}>
			<input class="y-input h-9 font-mono text-xs placeholder:font-sans" placeholder="drep1… or hash" bind:value={query} oninput={() => (problem = null)} aria-label="Find a DRep" />
			<button class="y-btn h-9" type="submit" aria-label="Find"><Search class="h-4 w-4" /></button>
		</form>
	{/snippet}
</PageHeader>

{#if problem}<p class="-mt-3 mb-4 text-right text-sm text-danger">{problem}</p>{/if}

{#if abstain != null || noConfidence != null}
	<section class="mb-6 grid gap-4 sm:grid-cols-2">
		<div class="y-panel p-4">
			<div class="y-label">Always abstain</div>
			<div class="mt-1 font-display text-2xl font-semibold">₳{adaCompact(abstain)}</div>
			<div class="text-xs text-muted">stake that counts as abstaining on every action</div>
		</div>
		<div class="y-panel p-4">
			<div class="y-label">Always no confidence</div>
			<div class="mt-1 font-display text-2xl font-semibold">₳{adaCompact(noConfidence)}</div>
			<div class="text-xs text-muted">stake that votes no confidence in the committee</div>
		</div>
	</section>
{/if}

<section class="y-panel overflow-hidden">
	<DataTable
		rows={data.dreps}
		columns={[
			{ key: 'drep', label: 'DRep' },
			{ key: 'status', label: 'Status' },
			{ key: 'power', label: 'Voting power', align: 'right' },
			{ key: 'deposit', label: 'Deposit', align: 'right', minor: true },
			{ key: 'type', label: 'Credential', minor: true },
			{ key: 'slot', label: 'Registered at slot', align: 'right', minor: true }
		]}
		key={(d) => d.drep_id}
		rowHref={(d) => `/governance/dreps/${d.drep_id}`}
		emptyTitle="No DReps registered"
		emptyText="Register a DRep on your devnet and it appears here."
	>
		{#snippet cell(d, key)}
			{#if key === 'drep'}
				<a class="group flex items-center gap-2.5" href="/governance/dreps/{d.drep_id}">
					<span class="grid h-8 w-8 place-items-center rounded-lg border border-line bg-surface-2 text-faint group-hover:border-brand/40 group-hover:text-brand"><Users class="h-4 w-4" /></span>
					<span class="mono text-[13px] font-medium text-ink group-hover:text-brand">{shorten(d.drep_id, 14, 8)}</span>
				</a>
			{:else if key === 'status'}<Badge tone={statusTone(d.status)}>{statusLabel(d.status)}</Badge>
			{:else if key === 'power'}<Ada value={d.voting_power} decimals={0} class="font-semibold" />
			{:else if key === 'deposit'}<Ada value={d.deposit} decimals={0} class="text-muted" />
			{:else if key === 'type'}<span class="text-muted">{credLabel(d.drep_type)}</span>
			{:else if key === 'slot'}<span class="mono text-xs text-muted">{num(d.registration_slot)}</span>{/if}
		{/snippet}
	</DataTable>
	<Pager page={data.page} hasNext={data.dreps.length === data.count} />
</section>
