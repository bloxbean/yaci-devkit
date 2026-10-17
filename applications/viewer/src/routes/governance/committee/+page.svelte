<script lang="ts">
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import Ring from '$lib/ui/Ring.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import { credLabel } from '$lib/governance/labels';
	import Gavel from '@lucide/svelte/icons/gavel';

	let { data } = $props();
	const c = $derived(data.committee);
	const active = $derived(c?.members.filter((m) => m.active).length ?? 0);
	const t = $derived(c?.threshold);

	// How far through its term each member is, for the bar.
	const term = (m: { start_epoch: number | null; expired_epoch: number | null }) => {
		if (c?.epoch == null || m.start_epoch == null || m.expired_epoch == null || m.expired_epoch <= m.start_epoch) return null;
		return Math.min(1, Math.max(0, (c.epoch - m.start_epoch) / (m.expired_epoch - m.start_epoch)));
	};
</script>

<svelte:head><title>Constitutional committee · Yaci Viewer</title></svelte:head>

<PageHeader kind="Governance" title="Constitutional committee">
	{#snippet icon()}<Gavel class="h-5 w-5" />{/snippet}
	The committee checks that governance actions follow the constitution{#if c?.epoch != null} · current epoch {c.epoch}{/if}.
</PageHeader>

{#if !c || !c.members.length}
	<div class="y-panel"><Empty title="No committee" text="This chain has no constitutional committee yet, or the store doesn't index it." /></div>
{:else}
	<div class="mb-6 grid gap-6 lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
		<Panel title="Approval threshold">
			<div class="flex items-center gap-5">
				<Ring value={t ? t.numerator / t.denominator : 0} size={112} stroke={10}>
					<span class="font-display text-xl font-semibold">{t ? `${t.numerator}/${t.denominator}` : '—'}</span>
				</Ring>
				<p class="text-sm text-muted">An action passes the committee when {t ? `${Math.round((t.numerator / t.denominator) * 100)}%` : 'the threshold'} of active members vote yes.</p>
			</div>
		</Panel>
		<Panel title="Members">
			<div class="grid grid-cols-3 gap-4 text-center">
				<div><div class="font-display text-3xl font-semibold">{c.members.length}</div><div class="text-xs text-muted">seats</div></div>
				<div><div class="font-display text-3xl font-semibold text-mint">{active}</div><div class="text-xs text-muted">active</div></div>
				<div><div class="font-display text-3xl font-semibold text-muted">{c.members.length - active}</div><div class="text-xs text-muted">term ended</div></div>
			</div>
		</Panel>
	</div>

	<section class="y-panel overflow-hidden">
		<DataTable
			rows={c.members}
			columns={[{ key: 'member', label: 'Cold credential' }, { key: 'status', label: 'Status' }, { key: 'type', label: 'Type', minor: true }, { key: 'term', label: 'Term (epochs)' }]}
			key={(m) => m.hash}
			emptyTitle="No members"
		>
			{#snippet cell(m, key)}
				{#if key === 'member'}<Hash value={m.hash} head={14} tail={10} />
				{:else if key === 'status'}<Badge tone={m.active ? 'ok' : 'neutral'}>{m.active ? 'Active' : 'Term ended'}</Badge>
				{:else if key === 'type'}<span class="text-muted">{credLabel(m.cred_type)}</span>
				{:else}
					{@const p = term(m)}
					<div class="flex w-56 items-center gap-2.5">
						<span class="w-8 text-right text-xs tabular-nums text-muted">{m.start_epoch ?? '—'}</span>
						<div class="h-1.5 flex-1 overflow-hidden rounded-full bg-line">{#if p != null}<div class="h-full rounded-full {m.active ? 'bg-grad' : 'bg-faint'}" style="width:{Math.max(2, p * 100)}%"></div>{/if}</div>
						<span class="w-8 text-xs tabular-nums text-muted">{m.expired_epoch ?? '—'}</span>
					</div>
				{/if}
			{/snippet}
		</DataTable>
	</section>
{/if}
