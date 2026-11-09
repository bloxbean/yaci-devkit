<script lang="ts">
	import { num, shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import ArrowDownLeft from '@lucide/svelte/icons/arrow-down-left';
	import KeyRound from '@lucide/svelte/icons/key-round';

	let { data } = $props();

	const columns = [
		{ key: 'account', label: 'Stake account' },
		{ key: 'tx', label: 'Transaction' },
		{ key: 'epoch', label: 'Epoch', minor: true },
		{ key: 'age', label: 'Age', minor: true },
		{ key: 'amount', label: 'Amount', align: 'right' as const }
	];
</script>

<svelte:head><title>Withdrawals · Yaci Viewer</title></svelte:head>

<PageHeader kind="Staking" title="Withdrawals">
	{#snippet icon()}<ArrowDownLeft class="h-5 w-5" />{/snippet}
	Staking rewards moved from stake accounts into wallets, newest first.
</PageHeader>

<section class="y-panel overflow-hidden">
	<DataTable
		rows={data.rows}
		{columns}
		key={(r) => `${r.tx_hash}-${r.address}`}
		rowHref={(r) => `/transactions/${r.tx_hash}`}
		emptyTitle="No withdrawals yet"
		emptyText="A withdrawal appears once a stake account claims its rewards in a transaction."
	>
		{#snippet cell(r, key)}
			{#if key === 'account'}
				<span class="flex items-center gap-2.5">
					<span class="grid h-8 w-8 shrink-0 place-items-center rounded-lg border border-line bg-surface-2 text-faint"><KeyRound class="h-4 w-4" /></span>
					<a class="mono text-[13px] font-medium text-ink hover:text-brand" href="/stake/{r.address}" title={r.address}>{shorten(r.address, 12, 8)}</a>
					<CopyButton value={r.address} size="xs" label="Stake address copied" />
				</span>
			{:else if key === 'tx'}
				<a class="mono text-[13px] text-muted hover:text-brand" href="/transactions/{r.tx_hash}">{shorten(r.tx_hash, 10, 8)}</a>
			{:else if key === 'epoch'}
				{#if r.epoch != null}<a class="y-link tabular-nums" href="/epochs/{r.epoch}">{num(r.epoch)}</a>{:else}—{/if}
			{:else if key === 'age'}
				<TimeAgo ts={r.block_time} class="text-muted" />
			{:else if key === 'amount'}
				{#if Number(r.amount) === 0}<span class="text-faint" title="A zero withdrawal, often used to prove control of a script stake account">₳0</span>{:else}<Ada value={r.amount} decimals={6} class="font-semibold" />{/if}
			{/if}
		{/snippet}
	</DataTable>
	<Pager page={data.page} hasNext={data.rows.length === data.count} />
</section>
