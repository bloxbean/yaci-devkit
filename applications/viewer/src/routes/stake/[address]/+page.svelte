<script lang="ts">
	import { num, shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Tabs from '$lib/ui/Tabs.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import Skeleton from '$lib/ui/Skeleton.svelte';
	import SlowNotice from '$lib/ui/SlowNotice.svelte';
	import AnalyticsNote from '$lib/ui/AnalyticsNote.svelte';
	import StakeHistory from '$lib/insights/StakeHistory.svelte';
	import { resolved } from '$lib/ui/resolved.svelte';
	import KeyRound from '@lucide/svelte/icons/key-round';
	import Server from '@lucide/svelte/icons/server';
	import Users from '@lucide/svelte/icons/users';
	import Gift from '@lucide/svelte/icons/gift';

	let { data } = $props();

	// Streamed in after the page renders: undefined while loading.
	const accountResult = resolved(() => data.account);
	const list = resolved(() => data.list);
	const account = $derived(accountResult.value?.ok ? accountResult.value.value : null);
	const rows = $derived(list.value?.ok ? list.value.value.rows : []);
	// Set when the rewards came from analytics (20 epochs a page) because the store's own list is too slow.
	const listAnalytics = $derived(list.value?.ok ? list.value.value.analytics : null);
	const history = resolved(() => data.history);

	const drepLabel = $derived.by(() => {
		const d = data.drep;
		if (!d) return null;
		if (d.drep_type === 'ABSTAIN' || d.drep_id === 'drep_always_abstain') return 'Always abstain';
		if (d.drep_type === 'NO_CONFIDENCE' || d.drep_id === 'drep_always_no_confidence') return 'Always no confidence';
		return null;
	});
</script>

<svelte:head><title>Stake {shorten(data.address, 10, 6)} · Yaci Viewer</title></svelte:head>

<PageHeader kind="Stake account" title={shorten(data.address, 16, 10)}>
	{#snippet icon()}<KeyRound class="h-5 w-5" />{/snippet}
	<div class="flex items-start gap-1.5">
		<span class="mono break-all text-xs text-faint">{data.address}</span>
		<CopyButton value={data.address} label="Stake address copied" />
	</div>
	{#snippet actions()}
		{#if account?.pool_id}<Badge tone="ok">Delegating</Badge>{:else if account}<Badge tone="neutral">Not delegated</Badge>{/if}
	{/snippet}
</PageHeader>

<section class="mb-6 grid gap-4 md:grid-cols-2 xl:grid-cols-4">
	<div class="y-panel relative overflow-hidden p-5 xl:col-span-2">
		<div class="pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-brand/10 blur-2xl"></div>
		<div class="y-label">Controlled stake</div>
		<div class="mt-1 font-display text-4xl font-semibold tracking-tight">
			{#if accountResult.value === undefined}<span class="y-skel inline-block h-9 w-56 align-middle" aria-label="Loading the stake account"></span>
			{:else if account}<Ada value={account.controlled_amount} decimals={6} />{:else}<span class="text-muted">—</span>{/if}
		</div>
		<div class="mt-2 text-sm text-muted">
			{#if accountResult.value && !accountResult.value.ok}
				<span title={accountResult.value.message}>{accountResult.value.reason === 'error' ? 'Couldn’t load this stake account.' : 'This stake account is too slow to load on this Yaci Store right now (yaci-store#1269).'}</span>
			{:else if account?.analytics}
				<AnalyticsNote source={account.analytics} compact />
			{:else}
				Withdrawable rewards {#if account}<Ada value={account.withdrawable_amount} decimals={6} class="font-semibold text-ink" />{:else}—{/if}
			{/if}
		</div>
	</div>
	<div class="y-panel p-5">
		<div class="y-label flex items-center gap-1.5"><Server class="h-3.5 w-3.5" />Stake pool</div>
		{#if account?.pool_id}
			<a href="/pools/{account.pool_id}" class="mono mt-2 block truncate text-sm text-brand hover:underline" title={account.pool_id}>{shorten(account.pool_id, 12, 8)}</a>
			<div class="mt-1 text-xs text-muted">earns staking rewards</div>
		{:else if accountResult.value === undefined}
			<div class="y-skel mt-3 h-4 w-40"></div>
		{:else if !account}
			<div class="mt-2 text-sm text-muted">—</div>
		{:else}
			<div class="mt-2 text-sm text-muted">Not delegated to a pool</div>
		{/if}
	</div>
	<div class="y-panel p-5">
		<div class="y-label flex items-center gap-1.5"><Users class="h-3.5 w-3.5" />Voting power (DRep)</div>
		{#if drepLabel}
			<div class="mt-2 text-sm font-medium">{drepLabel}</div>
		{:else if data.drep?.drep_id}
			<a href="/governance/dreps/{data.drep.drep_id}" class="mono mt-2 block truncate text-sm text-brand hover:underline">{shorten(data.drep.drep_id, 12, 8)}</a>
			<div class="mt-1 text-xs text-muted">since <TimeAgo ts={data.drep.block_time} /></div>
		{:else}
			<div class="mt-2 text-sm text-muted">Not delegated to a DRep</div>
		{/if}
	</div>
</section>

{#if history.value}<StakeHistory history={history.value} />{/if}

<section class="y-panel overflow-hidden">
	<Tabs tabs={[{ id: 'rewards', label: 'Rewards' }, { id: 'withdrawals', label: 'Withdrawals' }, { id: 'other', label: 'Other rewards' }]} active={data.tab} link />
	{#if list.value === undefined}
		<Skeleton rows={8} />
	{:else if !list.value.ok}
		{#if list.value.reason === 'error'}
			<ErrorBox title="Couldn't load this list" message={list.value.message} />
		{:else}
			<SlowNotice reason={list.value.reason} />
		{/if}
	{:else if data.tab === 'withdrawals'}
		<DataTable
			{rows}
			columns={[{ key: 'tx', label: 'Transaction' }, { key: 'epoch', label: 'Epoch' }, { key: 'age', label: 'Age', minor: true }, { key: 'amount', label: 'Amount', align: 'right' }]}
			key={(r) => r.tx_hash}
			rowHref={(r) => `/transactions/${r.tx_hash}`}
			emptyTitle="No withdrawals"
		>
			{#snippet cell(r, key)}
				{#if key === 'tx'}<a class="mono text-ink hover:text-brand" href="/transactions/{r.tx_hash}">{shorten(r.tx_hash, 12, 8)}</a>
				{:else if key === 'epoch'}{r.epoch}
				{:else if key === 'age'}<TimeAgo ts={r.block_time} class="text-muted" />
				{:else}<Ada value={r.amount} decimals={6} class="font-semibold" />{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={rows.length === data.count} />
	{:else}
		{#if listAnalytics}
			<AnalyticsNote source={listAnalytics}>Epochs {listAnalytics.fromEpoch} to {listAnalytics.toEpoch}, {data.rewardEpochs} epochs a page.</AnalyticsNote>
		{/if}
		<DataTable
			{rows}
			columns={[{ key: 'epoch', label: 'Epoch' }, { key: 'type', label: 'Type' }, { key: 'pool', label: 'Pool', minor: true }, { key: 'amount', label: 'Amount', align: 'right' }]}
			key={(r) => `${r.epoch}-${r.type}-${r.pool_id ?? ''}-${r.amount}`}
			emptyTitle={data.tab === 'other' ? 'No other rewards' : 'No rewards yet'}
			emptyText={data.tab === 'other' ? 'Refunds, treasury and reserve payouts show up here.' : 'Rewards arrive two epochs after delegating.'}
		>
			{#snippet cell(r, key)}
				{#if key === 'epoch'}<a class="y-link" href="/rewards/epoch/{r.epoch ?? r.earned_epoch}">{r.epoch ?? r.earned_epoch}</a>
				{:else if key === 'type'}<Badge tone={r.type === 'leader' ? 'violet' : r.type === 'member' ? 'ok' : 'neutral'}><Gift class="h-3 w-3" />{r.type}</Badge>
				{:else if key === 'pool'}{#if r.pool_id}<a class="mono text-xs text-muted hover:text-brand" href="/pools/{r.pool_id}">{shorten(r.pool_id, 10, 6)}</a>{:else}—{/if}
				{:else}<Ada value={r.amount} decimals={6} class="font-semibold" />{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={listAnalytics ? listAnalytics.fromEpoch > 0 : rows.length === data.count} />
	{/if}
</section>
