<script lang="ts">
	import { shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import AnchorLink from './AnchorLink.svelte';
	import { CERT_KINDS, type CertKind } from './certs';
	import { credLabel } from './labels';
	import { drepIdFromHash } from './drep';
	import UserPlus from '@lucide/svelte/icons/user-plus';

	/* DRep registrations, updates and retirements: one list, with a switch between the three. */
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	let { kind, rows, page, count }: { kind: CertKind; rows: any[]; page: number; count: number } = $props();

	const TEXT: Record<CertKind, { empty: string; sub: string }> = {
		registrations: { sub: 'New DReps and the deposit they locked.', empty: 'No DRep has registered on this chain yet.' },
		updates: { sub: 'DReps that changed their metadata anchor.', empty: 'No DRep has updated its metadata yet.' },
		deregistrations: { sub: 'DReps that retired and got their deposit back.', empty: 'No DRep has retired yet.' }
	};
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	const drepId = (r: any) => r.drep_id ?? drepIdFromHash(r.drep_hash, r.cred_type === 'SCRIPTHASH') ?? r.drep_hash;
</script>

<PageHeader kind="Governance" title="DRep activity">
	{#snippet icon()}<UserPlus class="h-5 w-5" />{/snippet}
	{TEXT[kind].sub}
	{#snippet actions()}
		<nav class="flex rounded-xl border border-line bg-surface p-1 text-sm" aria-label="Certificate type">
			{#each CERT_KINDS as k (k.id)}
				<a
					href={k.href}
					class="rounded-lg px-3 py-1.5 font-medium transition {k.id === kind ? 'bg-brand/10 text-brand' : 'text-muted hover:text-ink'}"
					aria-current={k.id === kind ? 'page' : undefined}>{k.label}</a
				>
			{/each}
		</nav>
	{/snippet}
</PageHeader>

<section class="y-panel overflow-hidden">
	<DataTable
		{rows}
		columns={[
			{ key: 'drep', label: 'DRep' },
			...(kind === 'updates' ? [] : [{ key: 'deposit', label: kind === 'deregistrations' ? 'Refund' : 'Deposit', align: 'right' as const }]),
			...(kind === 'deregistrations' ? [] : [{ key: 'anchor', label: 'Metadata', minor: true }]),
			{ key: 'type', label: 'Credential', minor: true },
			{ key: 'tx', label: 'Transaction', minor: true },
			{ key: 'age', label: 'When', align: 'right' }
		]}
		key={(r) => r.tx_hash + r.cert_index}
		rowHref={(r) => `/governance/dreps/${drepId(r)}`}
		emptyTitle="Nothing yet"
		emptyText={TEXT[kind].empty}
	>
		{#snippet cell(r, key)}
			{#if key === 'drep'}<a class="mono text-[13px] font-medium text-ink hover:text-brand" href="/governance/dreps/{drepId(r)}">{shorten(drepId(r), 14, 8)}</a>
			{:else if key === 'deposit'}<Ada value={r.deposit} decimals={0} />
			{:else if key === 'anchor'}<AnchorLink url={r.anchor_url} hash={r.anchor_hash} />
			{:else if key === 'type'}<span class="text-muted">{credLabel(r.cred_type)}</span>
			{:else if key === 'tx'}<a class="mono text-xs text-muted hover:text-brand" href="/transactions/{r.tx_hash}">{shorten(r.tx_hash, 8, 6)}</a>
			{:else}<TimeAgo ts={r.block_time} class="text-muted" />{/if}
		{/snippet}
	</DataTable>
	<Pager {page} hasNext={rows.length === count} />
</section>
