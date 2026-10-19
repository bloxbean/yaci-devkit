<script lang="ts">
	import { goto } from '$app/navigation';
	import { shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import KV from '$lib/ui/KV.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import Sparkline from '$lib/ui/Sparkline.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import { poolBech32 } from '$lib/staking/pool';
	import KeyRound from '@lucide/svelte/icons/key-round';
	import Search from '@lucide/svelte/icons/search';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';
	import NameTag from '$lib/names/NameTag.svelte';

	let { data } = $props();

	let addressInput = $state('');
	let epochInput = $state('');
	let problem = $state<string | null>(null);
	function find(e: Event) {
		e.preventDefault();
		problem = null;
		const a = addressInput.trim() || data.address || '';
		const ep = epochInput.trim() || String(data.epoch);
		if (!/^(stake|stake_test)1/.test(a)) return (problem = 'Stake addresses start with stake1 or stake_test1.');
		if (!/^\d+$/.test(ep)) return (problem = 'Enter an epoch number.');
		goto(`/epoch-stakes/accounts?address=${a}&epoch=${ep}`);
	}

	const series = $derived([...data.history].reverse());
	const poolId = $derived(data.stake ? (data.stake.pool_id ?? poolBech32(data.stake.pool_hash)) : null);
</script>

<svelte:head><title>Account stake · epoch {data.epoch} · Yaci Viewer</title></svelte:head>

<PageHeader kind="Epoch stakes" title={data.address && data.valid ? `Account stake in epoch ${data.epoch}` : 'Account stake by epoch'}>
	{#snippet icon()}<KeyRound class="h-5 w-5" />{/snippet}
	{#if data.address && data.valid}<a class="y-link mono text-[13px]" href="/stake/{data.address}">{shorten(data.address, 14, 8)}</a>{:else}A stake account's stake in one epoch's snapshot.{/if}
</PageHeader>

<form class="y-panel mb-6 flex flex-col gap-2 p-4 sm:flex-row sm:items-center" onsubmit={find}>
	<input class="y-input flex-1 font-mono text-[13px] placeholder:font-sans" placeholder={data.address ?? 'stake1… or stake_test1…'} bind:value={addressInput} spellcheck="false" aria-label="Stake address" />
	<input class="y-input sm:w-32" inputmode="numeric" placeholder="Epoch {data.epoch}" bind:value={epochInput} aria-label="Epoch" />
	<button class="y-btn y-btn-primary" type="submit"><Search class="h-4 w-4" /> Show</button>
</form>
{#if problem || !data.valid}<p class="-mt-4 mb-6 text-sm text-danger">{problem ?? 'That is not a stake address.'}</p>{/if}

{#if data.address && data.valid}
	{#if data.stake}
		<div class="grid gap-6 lg:grid-cols-[minmax(0,1.3fr)_minmax(0,1fr)]">
			<Panel title="Snapshot">
				<dl class="grid gap-x-6 gap-y-5 sm:grid-cols-2">
					<KV label="Stake"><span class="font-display text-2xl font-semibold"><Ada value={data.stake.amount} decimals={6} /></span></KV>
					<KV label="Epoch"><span class="font-medium">{data.stake.epoch}</span></KV>
					<KV label="Pool">{#if poolId}<a class="y-link mono text-[13px]" href="/pools/{poolId}">{shorten(poolId, 12, 8)}</a> <NameTag kind="pool" id={poolId} />{:else}—{/if}</KV>
					<KV label="Delegating since"><span>epoch {data.stake.delegation_epoch ?? '—'}</span></KV>
				</dl>
				<a class="y-btn mt-6" href="/stake/{data.address}">Rewards and withdrawals <ArrowRight class="h-4 w-4" /></a>
			</Panel>
			<Panel title="Recent epochs">
				{#if series.length > 1}<div class="mb-3"><Sparkline values={series.map((s) => Number(s.amount))} width={320} height={56} /></div>{/if}
				<ul class="divide-y divide-line/70 text-sm">
					{#each data.history as h (h.epoch)}
						<li class="flex items-center justify-between py-2">
							<a class="text-muted hover:text-brand" href="/epoch-stakes/accounts?address={data.address}&epoch={h.epoch}">epoch {h.epoch}</a>
							<Ada value={h.amount} class="font-medium" />
						</li>
					{/each}
				</ul>
			</Panel>
		</div>
	{:else}
		<div class="y-panel"><Empty title="No stake in this epoch" text="The account wasn't delegated in this snapshot, or the epoch hasn't been snapshotted yet." /></div>
	{/if}
{:else if !data.address}
	<div class="y-panel"><Empty title="Pick a stake account" text="Enter a stake address above, or choose one from the epoch stake overview." /></div>
{/if}
