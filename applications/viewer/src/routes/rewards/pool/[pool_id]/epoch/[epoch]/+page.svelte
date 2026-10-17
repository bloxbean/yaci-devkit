<script lang="ts">
	import { shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import { poolBech32 } from '$lib/staking/pool';
	import Gift from '@lucide/svelte/icons/gift';
	import Server from '@lucide/svelte/icons/server';
	import ChevronLeft from '@lucide/svelte/icons/chevron-left';
	import ChevronRight from '@lucide/svelte/icons/chevron-right';

	let { data } = $props();
	const id = $derived(poolBech32(data.poolId));
	const pageTotal = $derived(data.rows.reduce((s, r) => s + BigInt(r.reward ?? r.amount ?? 0), 0n));
	const isLast = $derived(data.latest != null && data.epoch >= data.latest);
</script>

<svelte:head><title>Pool rewards · epoch {data.epoch} · Yaci Viewer</title></svelte:head>

<PageHeader kind="Pool rewards" title="Epoch {data.epoch}">
	{#snippet icon()}<Gift class="h-5 w-5" />{/snippet}
	<a class="y-link mono inline-flex items-center gap-1.5 text-[13px]" href="/pools/{id}"><Server class="h-3.5 w-3.5" />{shorten(id, 14, 8)}</a>
	{#snippet actions()}
		<a class="y-btn {data.epoch <= 0 ? 'pointer-events-none opacity-40' : ''}" href="/rewards/pool/{data.poolId}/epoch/{data.epoch - 1}"><ChevronLeft class="h-4 w-4" /> {data.epoch - 1}</a>
		<a class="y-btn {isLast ? 'pointer-events-none opacity-40' : ''}" href="/rewards/pool/{data.poolId}/epoch/{data.epoch + 1}">{data.epoch + 1} <ChevronRight class="h-4 w-4" /></a>
	{/snippet}
</PageHeader>

<section class="y-panel overflow-hidden">
	{#if data.listError}
		<ErrorBox title="Couldn't load rewards" message={data.listError} />
	{:else}
		{#if data.rows.length}
			<div class="flex items-center justify-between border-b border-line px-4 py-3 text-sm">
				<span class="text-muted">{data.rows.length} reward{data.rows.length === 1 ? '' : 's'} on this page</span>
				<span>Total <Ada value={pageTotal} decimals={6} class="font-semibold" /></span>
			</div>
		{/if}
		<DataTable
			rows={data.rows}
			columns={[{ key: 'address', label: 'Stake address' }, { key: 'type', label: 'Type' }, { key: 'earned', label: 'Earned in', minor: true }, { key: 'amount', label: 'Amount', align: 'right' }]}
			key={(r) => `${r.address}-${r.type}-${r.earned_epoch}`}
			emptyTitle="No rewards for this pool in this epoch"
			emptyText="Rewards are paid two epochs after they are earned, and only if the pool made blocks."
		>
			{#snippet cell(r, key)}
				{#if key === 'address'}<a class="mono text-[13px] text-ink hover:text-brand" href="/stake/{r.address}">{shorten(r.address, 14, 8)}</a>
				{:else if key === 'type'}<Badge tone={r.type === 'leader' ? 'violet' : 'ok'}>{r.type}</Badge>
				{:else if key === 'earned'}<span class="text-muted">{r.earned_epoch ?? '—'}</span>
				{:else}<Ada value={r.reward ?? r.amount} decimals={6} class="font-semibold" />{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{/if}
</section>
