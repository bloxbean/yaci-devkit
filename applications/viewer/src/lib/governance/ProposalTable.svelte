<script lang="ts">
	import DataTable from '$lib/ui/DataTable.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import { shorten } from '$lib/format';
	import { actionHref, actionLabel, actionTone, statusLabel, statusTone, type Proposal } from './labels';
	import NameTag from '$lib/names/NameTag.svelte';

	/* Governance actions as a list: type, status, deposit and when it was proposed. */
	let { proposals, compact = false, emptyText = undefined }: { proposals: Proposal[]; compact?: boolean; emptyText?: string } = $props();

	const columns = $derived([
		{ key: 'action', label: 'Action' },
		{ key: 'status', label: 'Status' },
		...(compact ? [] : [{ key: 'deposit', label: 'Deposit', align: 'right' as const, minor: true }, { key: 'epoch', label: 'Epoch', minor: true }]),
		{ key: 'age', label: 'Proposed', align: 'right' as const }
	]);
</script>

<DataTable
	rows={proposals}
	{columns}
	key={(p) => `${p.tx_hash}_${p.index}`}
	rowHref={(p) => actionHref(p.tx_hash, p.index)}
	emptyTitle="No governance actions"
	emptyText={emptyText ?? 'Proposals submitted to this chain show up here.'}
>
	{#snippet cell(p, key)}
		{#if key === 'action'}
			<a href={actionHref(p.tx_hash, p.index)} class="group flex min-w-0 flex-col gap-1">
				<span class="flex items-center gap-2"><Badge tone={actionTone(p.type)}>{actionLabel(p.type)}</Badge></span>
				<NameTag kind="action" id="{p.tx_hash}#{p.index}" block class="max-w-[28rem] font-medium group-hover:text-brand" />
				<span class="mono text-xs text-muted group-hover:text-brand">{shorten(p.tx_hash, 10, 6)}#{p.index}</span>
			</a>
		{:else if key === 'status'}
			{#if p.status}<Badge tone={statusTone(p.status)}>{statusLabel(p.status)}</Badge>{:else}<span class="text-faint">—</span>{/if}
		{:else if key === 'deposit'}
			<Ada value={p.deposit} decimals={0} />
		{:else if key === 'epoch'}
			{p.epoch ?? '—'}
		{:else if key === 'age'}
			{#if p.block_time}<TimeAgo ts={p.block_time} class="text-muted" />{:else}<span class="text-faint">—</span>{/if}
		{/if}
	{/snippet}
</DataTable>
