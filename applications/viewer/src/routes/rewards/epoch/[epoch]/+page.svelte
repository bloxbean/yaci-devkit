<script lang="ts">
	import { shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Stat from '$lib/ui/Stat.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Tabs from '$lib/ui/Tabs.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import { poolBech32 } from '$lib/staking/pool';
	import Gift from '@lucide/svelte/icons/gift';
	import ChevronLeft from '@lucide/svelte/icons/chevron-left';
	import ChevronRight from '@lucide/svelte/icons/chevron-right';
	import NameTag from '$lib/names/NameTag.svelte';

	let { data } = $props();

	const tone = (t: string) => (t === 'leader' ? 'violet' : t === 'member' ? 'ok' : t === 'refund' ? 'cyan' : 'neutral') as 'violet' | 'ok' | 'cyan' | 'neutral';
	const isLast = $derived(data.latest != null && data.epoch >= data.latest);
	const empty = $derived(
		data.tab === 'rewards'
			? { title: 'No rewards paid in this epoch', text: 'On a new devnet rewards start a few epochs in.' }
			: data.tab === 'other'
				? { title: 'No other rewards', text: 'Treasury and reserve payouts and proposal refunds show up here.' }
				: { title: 'No unclaimed rewards', text: 'Rewards for accounts that were deregistered before payout.' }
	);
</script>

<svelte:head><title>Rewards in epoch {data.epoch} · Yaci Viewer</title></svelte:head>

<PageHeader kind="Rewards" title="Epoch {data.epoch}">
	{#snippet icon()}<Gift class="h-5 w-5" />{/snippet}
	Rewards that became spendable at the start of this epoch.
	{#snippet actions()}
		<a class="y-btn {data.epoch <= 0 ? 'pointer-events-none opacity-40' : ''}" href="/rewards/epoch/{data.epoch - 1}"><ChevronLeft class="h-4 w-4" /> {data.epoch - 1}</a>
		<a class="y-btn {isLast ? 'pointer-events-none opacity-40' : ''}" href="/rewards/epoch/{data.epoch + 1}">{data.epoch + 1} <ChevronRight class="h-4 w-4" /></a>
	{/snippet}
</PageHeader>

{#if data.pots}
	<section class="mb-6 grid grid-cols-2 gap-3 sm:gap-4 xl:grid-cols-4">
		<Stat label="Rewards pot" sub="for pools and delegators">
			{#snippet children()}<div class="font-display text-2xl font-semibold"><Ada value={data.pots?.rewards_pot} decimals={0} /></div>{/snippet}
		</Stat>
		<Stat label="Distributed" sub="paid out to accounts">
			{#snippet children()}<div class="font-display text-2xl font-semibold"><Ada value={data.pots?.distributed_rewards} decimals={0} /></div>{/snippet}
		</Stat>
		<Stat label="Undistributed" sub="returned to the reserves">
			{#snippet children()}<div class="font-display text-2xl font-semibold"><Ada value={data.pots?.undistributed_rewards} decimals={0} /></div>{/snippet}
		</Stat>
		<Stat label="Fees collected" sub="in the previous epoch">
			{#snippet children()}<div class="font-display text-2xl font-semibold"><Ada value={data.pots?.fees} decimals={0} /></div>{/snippet}
		</Stat>
	</section>
{/if}

<section class="y-panel overflow-hidden">
	<Tabs tabs={[{ id: 'rewards', label: 'Rewards' }, { id: 'other', label: 'Other rewards' }, { id: 'unclaimed', label: 'Unclaimed' }]} active={data.tab} link />
	{#if data.listError}
		<ErrorBox title="Couldn't load rewards" message={data.listError} />
	{:else}
		<DataTable
			rows={data.rows}
			columns={[
				{ key: 'address', label: 'Stake address' },
				{ key: 'type', label: 'Type' },
				{ key: 'pool', label: 'Pool', minor: true },
				{ key: 'earned', label: 'Earned in', minor: true },
				{ key: 'amount', label: 'Amount', align: 'right' }
			]}
			key={(r) => `${r.address}-${r.type}-${r.pool_id ?? ''}-${r.earned_epoch}-${r.amount}`}
			emptyTitle={empty.title}
			emptyText={empty.text}
		>
			{#snippet cell(r, key)}
				{#if key === 'address'}<a class="mono text-[13px] text-ink hover:text-brand" href="/stake/{r.address}">{shorten(r.address, 14, 8)}</a>
				{:else if key === 'type'}<Badge tone={tone(r.type)}>{r.type}</Badge>
				{:else if key === 'pool'}{#if r.pool_id}{@const id = poolBech32(r.pool_id)}<a class="mono text-xs text-muted hover:text-brand" href="/pools/{id}">{shorten(id, 10, 6)}</a> <NameTag kind="pool" {id} />{:else}<span class="text-faint">—</span>{/if}
				{:else if key === 'earned'}<span class="text-muted">{r.earned_epoch ?? '—'}</span>
				{:else}<Ada value={r.amount} decimals={6} class="font-semibold" />{/if}
			{/snippet}
		</DataTable>
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{/if}
</section>
