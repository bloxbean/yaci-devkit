<script lang="ts">
	import { goto } from '$app/navigation';
	import { adaCompact, num, percent, shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import Stat from '$lib/ui/Stat.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import DataTable from '$lib/ui/DataTable.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import { isPoolId, poolBech32 } from '$lib/staking/pool';
	import Layers from '@lucide/svelte/icons/layers';
	import ChartColumn from '@lucide/svelte/icons/chart-column';
	import Search from '@lucide/svelte/icons/search';
	import ChevronLeft from '@lucide/svelte/icons/chevron-left';
	import ChevronRight from '@lucide/svelte/icons/chevron-right';

	let { data } = $props();

	const top = $derived(data.distribution.pools.slice(0, 12));
	const max = $derived(top[0]?.stake ?? 1);
	const covered = $derived(data.distribution.pools.reduce((s, p) => s + p.stake, 0));
	const change = $derived.by(() => {
		const t = data.trend;
		if (t.length < 2) return null;
		const prev = t[t.length - 2].stake;
		return prev ? ((t[t.length - 1].stake - prev) / prev) * 100 : null;
	});

	let lookup = $state('');
	let problem = $state<string | null>(null);
	function find(e: Event) {
		e.preventDefault();
		const v = lookup.trim();
		problem = null;
		if (isPoolId(v)) goto(`/epoch-stakes/pools?id=${v}&epoch=${data.epoch}`);
		else if (/^(stake|stake_test)1/.test(v)) goto(`/epoch-stakes/accounts?address=${v}&epoch=${data.epoch}`);
		else problem = 'Enter a pool id or a stake address.';
	}
</script>

<svelte:head><title>Epoch stakes · epoch {data.epoch} · Yaci Viewer</title></svelte:head>

<PageHeader kind="Staking" title="Stake in epoch {data.epoch}">
	{#snippet icon()}<Layers class="h-5 w-5" />{/snippet}
	The stake snapshot used for leader election and rewards{data.epoch === data.current ? ' in the current epoch' : ''}.
	{#snippet actions()}
		<a class="y-btn {data.epoch <= 0 ? 'pointer-events-none opacity-40' : ''}" href="?epoch={data.epoch - 1}"><ChevronLeft class="h-4 w-4" /> {data.epoch - 1}</a>
		<a class="y-btn {data.epoch >= data.current ? 'pointer-events-none opacity-40' : ''}" href="?epoch={data.epoch + 1}">{data.epoch + 1} <ChevronRight class="h-4 w-4" /></a>
	{/snippet}
</PageHeader>

<section class="mb-6 grid gap-4 md:grid-cols-3">
	<Stat
		label="Active stake"
		value={data.total != null ? `₳${adaCompact(data.total)}` : '—'}
		sub={change != null ? `${change >= 0 ? '+' : ''}${change.toFixed(2)}% vs epoch ${data.epoch - 1}` : 'total delegated to pools'}
		trend={data.trend.map((t) => t.stake)}
	>
		{#snippet icon()}<Layers class="h-4 w-4" />{/snippet}
	</Stat>
	<Stat
		label="Pools with stake"
		value={num(data.distribution.pools.length)}
		sub={data.distribution.complete ? 'every registered pool' : 'among recently registered pools'}
	/>
	<form class="y-panel flex flex-col gap-2 p-4" onsubmit={find}>
		<span class="y-label">Look up in this epoch</span>
		<div class="flex gap-2">
			<input class="y-input h-9 flex-1 font-mono text-[13px] placeholder:font-sans" placeholder="pool1… or stake1…" bind:value={lookup} spellcheck="false" aria-label="Pool id or stake address" />
			<button class="y-icon-btn" type="submit" aria-label="Look up"><Search class="h-4 w-4" /></button>
		</div>
		{#if problem}<span class="text-xs text-danger">{problem}</span>{/if}
	</form>
</section>

<div class="grid gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(0,1.2fr)]">
	<Panel title="Stake by pool" sub={data.distribution.complete ? 'All pools' : 'Recently registered pools only: there is no full ranking endpoint'}>
		{#snippet icon()}<ChartColumn class="h-4 w-4" />{/snippet}
		{#if top.length}
			<ul class="flex flex-col gap-3">
				{#each top as p (p.poolHash)}
					<li>
						<a href="/epoch-stakes/pools?id={p.poolId ?? poolBech32(p.poolHash)}&epoch={data.epoch}" class="group block">
							<div class="mb-1 flex items-baseline justify-between gap-3 text-sm">
								<span class="mono truncate text-[13px] group-hover:text-brand">{shorten(p.poolId ?? poolBech32(p.poolHash), 12, 6)}</span>
								<span class="shrink-0 tabular-nums"><b class="font-semibold">₳{adaCompact(p.stake)}</b>{#if data.total}<span class="text-faint"> · {percent(p.stake, data.total, 2)}</span>{/if}</span>
							</div>
							<div class="h-2 overflow-hidden rounded-full bg-line">
								<div class="h-full rounded-full bg-grad transition-[width] duration-700" style="width:{Math.max(1, (p.stake / max) * 100)}%"></div>
							</div>
						</a>
					</li>
				{/each}
			</ul>
			{#if data.total && !data.distribution.complete}
				<p class="mt-4 text-xs text-faint">These pools hold {percent(covered, data.total, 2)} of the active stake.</p>
			{/if}
		{:else}
			<Empty title="No pool stake yet" text="The first stake snapshot is taken at an epoch boundary." />
		{/if}
	</Panel>

	<Panel title="Stake by account" flush>
		{#if data.listError}
			<ErrorBox title="Couldn't load accounts" message={data.listError} />
		{:else}
			<DataTable
				rows={data.accounts}
				columns={[{ key: 'address', label: 'Stake address' }, { key: 'pool', label: 'Pool', minor: true }, { key: 'since', label: 'Since', minor: true }, { key: 'amount', label: 'Stake', align: 'right' }]}
				key={(r) => r.address}
				dense
				emptyTitle="No stake in this epoch"
			>
				{#snippet cell(r, key)}
					{#if key === 'address'}<a class="mono text-[13px] text-ink hover:text-brand" href="/epoch-stakes/accounts?address={r.address}&epoch={data.epoch}">{shorten(r.address, 12, 6)}</a>
					{:else if key === 'pool'}<a class="mono text-xs text-muted hover:text-brand" href="/epoch-stakes/pools?id={r.pool_id ?? r.pool_hash}&epoch={data.epoch}">{shorten(r.pool_id ?? r.pool_hash, 10, 6)}</a>
					{:else if key === 'since'}<span class="text-muted">epoch {r.delegation_epoch ?? '—'}</span>
					{:else}<Ada value={r.amount} class="font-semibold" />{/if}
				{/snippet}
			</DataTable>
			<Pager page={data.page} hasNext={data.accounts.length === data.count} />
		{/if}
	</Panel>
</div>
