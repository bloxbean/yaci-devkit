<script lang="ts">
	import { num, shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import BadgeCheck from '@lucide/svelte/icons/badge-check';
	import { CERTS, CERT_ORDER, type CertKind } from './certs';
	import { isGenesisTx, marginPct, poolBech32 } from './pool';
	import NameTag from '$lib/names/NameTag.svelte';

	/* One page for all five certificate lists, with a switcher between them. */
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	let { data }: { data: { kind: CertKind; rows: any[]; page: number; count: number; error: string | null } } = $props();

	const meta = $derived(CERTS[data.kind]);
	const isPool = $derived(data.kind === 'pool-registrations' || data.kind === 'pool-retirements');

	const columns = $derived.by(() => {
		const subject = { key: 'subject', label: isPool ? 'Pool' : 'Stake address' };
		const tail = [
			{ key: 'tx', label: 'Transaction', minor: true },
			{ key: 'block', label: 'Block', minor: true },
			{ key: 'age', label: 'Age', align: 'right' as const }
		];
		switch (data.kind) {
			case 'stakekey-registrations':
				return [subject, { key: 'cred', label: 'Credential', minor: true }, { key: 'deposit', label: 'Deposit', align: 'right' as const }, ...tail];
			case 'stakekey-deregistrations':
				return [subject, { key: 'cred', label: 'Credential', minor: true }, ...tail];
			case 'delegations':
				return [subject, { key: 'pool', label: 'Delegated to' }, ...tail];
			case 'pool-registrations':
				return [subject, { key: 'pledge', label: 'Pledge', align: 'right' as const }, { key: 'cost', label: 'Fixed cost', align: 'right' as const, minor: true }, { key: 'margin', label: 'Margin', align: 'right' as const }, ...tail];
			default:
				return [subject, { key: 'retire', label: 'Retires in epoch' }, { key: 'epoch', label: 'Announced in', minor: true }, ...tail];
		}
	});
</script>

<svelte:head><title>{meta.title} · Yaci Viewer</title></svelte:head>

<PageHeader kind="Certificates" title={meta.title}>
	{#snippet icon()}<BadgeCheck class="h-5 w-5" />{/snippet}
	{meta.text}
</PageHeader>

<nav class="mb-5 flex gap-1 overflow-x-auto rounded-2xl border border-line bg-surface p-1 [scrollbar-width:none]" aria-label="Certificate types">
	{#each CERT_ORDER as k (k)}
		<a
			href="/certificates/{k}"
			class="shrink-0 whitespace-nowrap rounded-xl px-3.5 py-2 text-sm font-medium transition-colors {k === data.kind ? 'bg-brand/10 text-brand' : 'text-muted hover:bg-surface-2 hover:text-ink'}"
			aria-current={k === data.kind ? 'page' : undefined}>{CERTS[k].label}</a
		>
	{/each}
</nav>

<section class="y-panel overflow-hidden">
	{#if data.error}
		<ErrorBox title="Couldn't load certificates" message={data.error} />
	{:else}
		<DataTable
			rows={data.rows}
			{columns}
			key={(r) => `${r.tx_hash}-${r.cert_index}-${r.address ?? r.pool_id}`}
			emptyTitle="No certificates yet"
			emptyText="They appear here as transactions carrying them land on chain."
		>
			{#snippet cell(r, key)}
				{#if key === 'subject'}
					{#if isPool}
						{@const id = r.pool_id_bech32 ?? poolBech32(r.pool_id)}
						<a class="mono text-[13px] font-medium text-ink hover:text-brand" href="/pools/{id}" title={id}>{shorten(id, 12, 8)}</a> <NameTag kind="pool" {id} />
					{:else}
						<a class="mono text-[13px] font-medium text-ink hover:text-brand" href="/stake/{r.address}" title={r.address}>{shorten(r.address, 14, 8)}</a>
					{/if}
				{:else if key === 'cred'}
					<Badge tone={r.credential_type === 'SCRIPTHASH' ? 'violet' : 'neutral'}>{r.credential_type === 'SCRIPTHASH' ? 'Script' : 'Key'}</Badge>
				{:else if key === 'deposit'}
					{#if r.deposit != null}<Ada value={r.deposit} />{:else}<span class="text-faint">—</span>{/if}
				{:else if key === 'pool'}
					{@const id = poolBech32(r.pool_id)}
					<a class="mono text-xs text-brand hover:underline" href="/pools/{id}" title={id}>{shorten(id, 10, 6)}</a> <NameTag kind="pool" {id} />
				{:else if key === 'pledge'}
					<Ada value={r.pledge} decimals={0} class="font-semibold" />
				{:else if key === 'cost'}
					<Ada value={r.cost} decimals={0} class="text-muted" />
				{:else if key === 'margin'}
					<span class="tabular-nums">{marginPct(r.margin)}</span>
				{:else if key === 'retire'}
					<a class="y-link font-semibold" href="/epochs/{r.retirement_epoch}">{r.retirement_epoch}</a>
				{:else if key === 'epoch'}
					<span class="text-muted">{r.epoch}</span>
				{:else if key === 'tx'}
					{#if isGenesisTx(r.tx_hash)}<span class="text-muted">Genesis</span>{:else}<a class="mono text-xs text-muted hover:text-brand" href="/transactions/{r.tx_hash}">{shorten(r.tx_hash, 8, 6)}</a>{/if}
				{:else if key === 'block'}
					{#if r.block_number >= 0}<a class="y-link mono text-xs" href="/blocks/{r.block_number}">#{num(r.block_number)}</a>{:else}<span class="text-faint">—</span>{/if}
				{:else if key === 'age'}
					<TimeAgo ts={r.block_time} class="text-muted" />
				{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{/if}
</section>
