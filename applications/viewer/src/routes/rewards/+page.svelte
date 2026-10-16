<script lang="ts">
	import type { Snippet } from 'svelte';
	import { goto } from '$app/navigation';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import { isPoolId } from '$lib/staking/pool';
	import Gift from '@lucide/svelte/icons/gift';
	import Timer from '@lucide/svelte/icons/timer';
	import KeyRound from '@lucide/svelte/icons/key-round';
	import Server from '@lucide/svelte/icons/server';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';

	let { data } = $props();

	let epoch = $state('');
	let account = $state('');
	let pool = $state('');
	let poolEpoch = $state('');
	let problem = $state<{ form: string; text: string } | null>(null);

	const epochOk = (v: string) => /^\d+$/.test(v.trim());

	function go(form: string, e: Event) {
		e.preventDefault();
		problem = null;
		if (form === 'epoch') {
			if (!epochOk(epoch)) return (problem = { form, text: 'Enter an epoch number.' });
			goto(`/rewards/epoch/${epoch.trim()}`);
		} else if (form === 'account') {
			if (!/^(stake|stake_test)1/.test(account.trim())) return (problem = { form, text: 'Stake addresses start with stake1 or stake_test1.' });
			goto(`/stake/${account.trim()}?tab=rewards`);
		} else {
			if (!isPoolId(pool.trim())) return (problem = { form, text: 'Pool ids start with pool1, or are a 56-character hex hash.' });
			const e2 = poolEpoch.trim() || String(data.epoch ?? '');
			if (!epochOk(e2)) return (problem = { form, text: 'Enter an epoch number.' });
			goto(`/rewards/pool/${pool.trim()}/epoch/${e2}`);
		}
	}

	const recent = $derived(data.epoch != null ? [data.epoch, data.epoch - 1, data.epoch - 2].filter((e) => e >= 0) : []);
</script>

<svelte:head><title>Rewards · Yaci Viewer</title></svelte:head>

<PageHeader kind="Staking" title="Rewards">
	{#snippet icon()}<Gift class="h-5 w-5" />{/snippet}
	Staking rewards are paid at each epoch boundary, two epochs after they are earned.
</PageHeader>

{#if recent.length}
	<div class="mb-6 flex flex-wrap items-center gap-2 text-sm">
		<span class="text-muted">Jump to epoch</span>
		{#each recent as e (e)}
			<a class="y-pill border-line bg-surface py-1 text-ink hover:border-brand/40 hover:text-brand" href="/rewards/epoch/{e}">{e}{e === data.epoch ? ' · current' : ''}</a>
		{/each}
	</div>
{/if}

<div class="grid gap-4 lg:grid-cols-3">
	{#snippet card(form: string, Icon: typeof Gift, title: string, text: string, body: Snippet)}
		<form class="y-panel flex flex-col gap-4 p-5" onsubmit={(e) => go(form, e)}>
			<div class="flex items-start gap-3">
				<span class="grid h-10 w-10 shrink-0 place-items-center rounded-xl border border-brand/20 bg-brand/10 text-brand"><Icon class="h-5 w-5" /></span>
				<div>
					<h2 class="font-display text-base font-semibold">{title}</h2>
					<p class="text-sm text-muted">{text}</p>
				</div>
			</div>
			{@render body()}
			{#if problem?.form === form}<p class="text-sm text-danger">{problem.text}</p>{/if}
			<button class="y-btn mt-auto self-start" type="submit">Show rewards <ArrowRight class="h-4 w-4" /></button>
		</form>
	{/snippet}

	{#snippet epochBody()}
		<input class="y-input" inputmode="numeric" placeholder="Epoch, e.g. {data.epoch ?? 12}" bind:value={epoch} aria-label="Epoch" />
	{/snippet}
	{#snippet accountBody()}
		<input class="y-input font-mono text-[13px] placeholder:font-sans" placeholder="stake1… or stake_test1…" bind:value={account} spellcheck="false" aria-label="Stake address" />
	{/snippet}
	{#snippet poolBody()}
		<div class="flex gap-2">
			<input class="y-input flex-1 font-mono text-[13px] placeholder:font-sans" placeholder="pool1…" bind:value={pool} spellcheck="false" aria-label="Pool id" />
			<input class="y-input w-24" inputmode="numeric" placeholder={String(data.epoch ?? 'epoch')} bind:value={poolEpoch} aria-label="Epoch" />
		</div>
	{/snippet}

	{@render card('epoch', Timer, 'By epoch', 'Every reward paid out in one epoch, with the pots it came from.', epochBody)}
	{@render card('account', KeyRound, 'By stake account', 'One account’s reward history across epochs.', accountBody)}
	{@render card('pool', Server, 'By pool and epoch', 'What a pool’s leader and members received in an epoch.', poolBody)}
</div>
