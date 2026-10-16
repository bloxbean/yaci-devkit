<script lang="ts">
	import { shorten } from '$lib/format';
	import LookupHero from '$lib/ui/LookupHero.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import Ada from '$lib/ui/Ada.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import { isPoolId, marginPct, poolBech32 } from '$lib/staking/pool';
	import Server from '@lucide/svelte/icons/server';
	import UserPlus from '@lucide/svelte/icons/user-plus';
	import Power from '@lucide/svelte/icons/power';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';

	let { data } = $props();

	const examples = $derived(
		[...new Map(data.registrations.map((r) => [r.pool_id, r])).values()]
			.slice(0, 2)
			.map((r) => ({ label: shorten(r.pool_id_bech32 ?? poolBech32(r.pool_id), 10, 4), value: r.pool_id_bech32 ?? poolBech32(r.pool_id) }))
	);

	function resolve(v: string) {
		return isPoolId(v) ? `/pools/${v}` : { error: 'Pool ids start with pool1, or are a 56-character hex hash.' };
	}
</script>

<svelte:head><title>Stake pools · Yaci Viewer</title></svelte:head>

<LookupHero
	kind="Staking"
	title="Find a stake pool"
	text="Pledge, fees, owners, relays, stake and delegators for any pool on this chain."
	placeholder="pool1… or pool hash"
	icon={Server}
	{resolve}
	{examples}
/>

<div class="grid gap-6 xl:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)]">
	<Panel title="Recent registrations and updates" flush>
		{#snippet icon()}<UserPlus class="h-4 w-4" />{/snippet}
		{#snippet actions()}<a href="/certificates/pool-registrations" class="y-btn h-8 text-xs">All <ArrowRight class="h-3.5 w-3.5" /></a>{/snippet}
		{#if data.registrations.length}
			<ul class="divide-y divide-line/70">
				{#each data.registrations as r (r.tx_hash + r.cert_index)}
					{@const id = r.pool_id_bech32 ?? poolBech32(r.pool_id)}
					<li>
						<a href="/pools/{id}" class="group flex items-center gap-3 px-5 py-3 transition-colors hover:bg-surface-2/70">
							<span class="grid h-9 w-9 shrink-0 place-items-center rounded-xl border border-line bg-surface-2 text-faint group-hover:border-brand/40 group-hover:text-brand"><Server class="h-4 w-4" /></span>
							<span class="min-w-0 flex-1">
								<span class="mono block truncate text-[13px] font-medium group-hover:text-brand">{shorten(id, 14, 8)}</span>
								<span class="block text-xs text-muted">margin {marginPct(r.margin)} · cost <Ada value={r.cost} decimals={0} /></span>
							</span>
							<span class="shrink-0 text-right">
								<span class="block text-[13px]">pledge <Ada value={r.pledge} decimals={0} class="font-semibold" /></span>
								<TimeAgo ts={r.block_time} class="text-[11px] text-faint" />
							</span>
						</a>
					</li>
				{/each}
			</ul>
		{:else}
			<Empty title="No pool registrations" />
		{/if}
	</Panel>

	<Panel title="Retirements" flush>
		{#snippet icon()}<Power class="h-4 w-4" />{/snippet}
		{#snippet actions()}<a href="/certificates/pool-retirements" class="y-btn h-8 text-xs">All <ArrowRight class="h-3.5 w-3.5" /></a>{/snippet}
		{#if data.retirements.length}
			<ul class="divide-y divide-line/70">
				{#each data.retirements as r (r.tx_hash + r.cert_index)}
					{@const id = r.pool_id_bech32 ?? poolBech32(r.pool_id)}
					<li class="flex items-center gap-3 px-5 py-3">
						<a href="/pools/{id}" class="mono min-w-0 flex-1 truncate text-[13px] font-medium hover:text-brand">{shorten(id, 12, 6)}</a>
						{#if data.epoch != null && r.retirement_epoch <= data.epoch}<Badge tone="neutral">retired {r.retirement_epoch}</Badge>{:else}<Badge tone="warn">retires {r.retirement_epoch}</Badge>{/if}
					</li>
				{/each}
			</ul>
		{:else}
			<Empty title="No retirements" text="No pool has announced it is retiring." />
		{/if}
	</Panel>
</div>
