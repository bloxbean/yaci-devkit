<script lang="ts">
	import { goto } from '$app/navigation';
	import { percent, shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import { isPoolId, poolBech32 } from '$lib/staking/pool';
	import Server from '@lucide/svelte/icons/server';
	import Search from '@lucide/svelte/icons/search';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';
	import ChevronLeft from '@lucide/svelte/icons/chevron-left';
	import ChevronRight from '@lucide/svelte/icons/chevron-right';
	import NameTag from '$lib/names/NameTag.svelte';

	let { data } = $props();

	let poolInput = $state('');
	let epochInput = $state('');
	let problem = $state<string | null>(null);
	function find(e: Event) {
		e.preventDefault();
		problem = null;
		const id = poolInput.trim() || data.id || '';
		const ep = epochInput.trim() || String(data.epoch);
		if (!isPoolId(id)) return (problem = 'Pool ids start with pool1, or are a 56-character hex hash.');
		if (!/^\d+$/.test(ep)) return (problem = 'Enter an epoch number.');
		goto(`/epoch-stakes/pools?id=${id}&epoch=${ep}`);
	}

	const poolId = $derived(data.stake?.poolId ?? (data.id ? poolBech32(data.id) : ''));
	const qs = (e: number) => `?id=${data.id}&epoch=${e}`;
</script>

<svelte:head><title>Pool stake · epoch {data.epoch} · Yaci Viewer</title></svelte:head>

<PageHeader kind="Epoch stakes" title={data.id && data.valid ? `Pool stake in epoch ${data.epoch}` : 'Pool stake by epoch'}>
	{#snippet icon()}<Server class="h-5 w-5" />{/snippet}
	{#if data.id && data.valid}<a class="y-link mono text-[13px]" href="/pools/{poolId}">{shorten(poolId, 14, 8)}</a> <NameTag kind="pool" id={poolId} />{:else}A pool's stake and delegators in one epoch's snapshot.{/if}
	{#snippet actions()}
		{#if data.id && data.valid}
			<a class="y-btn {data.epoch <= 0 ? 'pointer-events-none opacity-40' : ''}" href={qs(data.epoch - 1)}><ChevronLeft class="h-4 w-4" /> {data.epoch - 1}</a>
			<a class="y-btn {data.epoch >= data.current ? 'pointer-events-none opacity-40' : ''}" href={qs(data.epoch + 1)}>{data.epoch + 1} <ChevronRight class="h-4 w-4" /></a>
		{/if}
	{/snippet}
</PageHeader>

<form class="y-panel mb-6 flex flex-col gap-2 p-4 sm:flex-row sm:items-center" onsubmit={find}>
	<input class="y-input flex-1 font-mono text-[13px] placeholder:font-sans" placeholder={data.id ?? 'pool1… or pool hash'} bind:value={poolInput} spellcheck="false" aria-label="Pool id" />
	<input class="y-input sm:w-32" inputmode="numeric" placeholder="Epoch {data.epoch}" bind:value={epochInput} aria-label="Epoch" />
	<button class="y-btn y-btn-primary" type="submit"><Search class="h-4 w-4" /> Show</button>
</form>
{#if problem || !data.valid}<p class="-mt-4 mb-6 text-sm text-danger">{problem ?? 'That is not a pool id.'}</p>{/if}

{#if data.id && data.valid}
	<section class="mb-6 grid gap-4 sm:grid-cols-2">
		<div class="y-panel p-5">
			<div class="y-label">Active stake</div>
			<div class="mt-1 font-display text-3xl font-semibold">{#if data.stake}<Ada value={data.stake.amount} decimals={0} />{:else}<span class="text-muted">—</span>{/if}</div>
			{#if data.stake && data.total}<div class="mt-1 text-sm text-muted">{percent(data.stake.amount, data.total, 3)} of all active stake</div>{/if}
		</div>
		<a class="y-panel group flex items-center justify-between p-5 transition hover:border-brand/40" href="/pools/{poolId}">
			<div>
				<div class="y-label">Pool details</div>
				<div class="mt-1 text-sm text-muted">Pledge, fees, owners, relays and blocks</div>
			</div>
			<ArrowRight class="h-5 w-5 text-faint transition group-hover:translate-x-0.5 group-hover:text-brand" />
		</a>
	</section>

	<section class="y-panel overflow-hidden">
		<header class="y-panel-head"><h2 class="font-display text-[15px] font-semibold">Delegators</h2></header>
		{#if data.listError}
			<ErrorBox title="Couldn't load delegators" message={data.listError} />
		{:else}
			<DataTable
				rows={data.rows}
				columns={[{ key: 'address', label: 'Stake address' }, { key: 'since', label: 'Delegating since', minor: true }, { key: 'amount', label: 'Stake', align: 'right' }, { key: 'share', label: 'Share of pool', align: 'right', minor: true }]}
				key={(r) => r.address}
				emptyTitle="No delegators in this epoch"
			>
				{#snippet cell(r, key)}
					{#if key === 'address'}<a class="mono text-[13px] text-ink hover:text-brand" href="/epoch-stakes/accounts?address={r.address}&epoch={data.epoch}">{shorten(r.address, 14, 8)}</a>
					{:else if key === 'since'}<span class="text-muted">epoch {r.delegation_epoch ?? '—'}</span>
					{:else if key === 'amount'}<Ada value={r.amount} class="font-semibold" />
					{:else}<span class="tabular-nums text-muted">{data.stake ? percent(Number(r.amount), data.stake.amount, 2) : '—'}</span>{/if}
				{/snippet}
			</DataTable>
			<Pager page={data.page} hasNext={data.rows.length === data.count} />
		{/if}
	</section>
{:else if !data.id}
	<div class="y-panel"><Empty title="Pick a pool" text="Enter a pool id above, or choose one from the epoch stake overview." /></div>
{/if}
