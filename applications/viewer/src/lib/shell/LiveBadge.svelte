<script lang="ts">
	import { chain } from '$lib/live/chain.svelte';
	import { apiBase } from '$lib/api/client';
	import { num } from '$lib/format';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';

	/* Network + live status + tip height. Opens a small panel with the details. */
	let open = $state(false);
	let root: HTMLDivElement;

	const tone = $derived.by(() => {
		if (chain.error && chain.status !== 'live') return { dot: 'bg-danger', label: 'Store unreachable' };
		switch (chain.status) {
			case 'live':
				return { dot: 'bg-mint', label: 'Live' };
			case 'polling':
				return { dot: 'bg-blue', label: 'Polling' };
			default:
				return { dot: 'bg-amber', label: 'Connecting' };
		}
	});

	const netClass: Record<string, string> = {
		mainnet: 'text-blue',
		preprod: 'text-violet',
		preview: 'text-cyan',
		sanchonet: 'text-pink',
		devnet: 'text-brand',
		custom: 'text-muted'
	};

	function onWindowClick(e: MouseEvent) {
		if (open && root && !root.contains(e.target as Node)) open = false;
	}
</script>

<svelte:window onclick={onWindowClick} onkeydown={(e) => e.key === 'Escape' && (open = false)} />

<div class="relative" bind:this={root}>
	<button
		class="flex h-9 items-center gap-2.5 rounded-xl border border-line bg-surface pl-3 pr-3.5 text-sm transition hover:border-brand/40"
		onclick={() => (open = !open)}
		aria-expanded={open}
		aria-label="Network status"
	>
		<span class="relative flex h-2 w-2">
			{#if chain.status === 'live'}<span class="absolute inline-flex h-full w-full animate-ping rounded-full {tone.dot} opacity-60"></span>{/if}
			<span class="relative inline-flex h-2 w-2 rounded-full {tone.dot}"></span>
		</span>
		<span class="font-semibold {netClass[chain.network?.kind ?? 'custom']}">{chain.network?.name ?? '…'}</span>
		{#if chain.tip}
			<span class="hidden h-4 w-px bg-line sm:block"></span>
			{#key chain.tip.number}
				<span class="mono hidden animate-fade-up text-[13px] tabular-nums text-muted sm:inline">#{num(chain.tip.number)}</span>
			{/key}
		{/if}
	</button>

	{#if open}
		<div class="absolute right-0 top-11 z-50 w-72 animate-fade-up rounded-2xl border border-line bg-surface p-4 text-sm shadow-card">
			<div class="mb-3 flex items-center justify-between">
				<span class="font-display font-semibold">{chain.network?.name ?? 'Network'}</span>
				<span class="flex items-center gap-1.5 text-xs text-muted"><span class="h-1.5 w-1.5 rounded-full {tone.dot}"></span>{tone.label}</span>
			</div>
			<dl class="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-[13px]">
				<dt class="text-faint">Tip</dt>
				<dd class="mono text-right">{chain.tip ? '#' + num(chain.tip.number) : '—'}</dd>
				<dt class="text-faint">Last block</dt>
				<dd class="text-right">{#if chain.tip}<TimeAgo ts={chain.tip.time} />{:else}—{/if}</dd>
				<dt class="text-faint">Epoch</dt>
				<dd class="text-right">{chain.tip?.epoch ?? chain.epoch?.epoch ?? '—'}</dd>
				<dt class="text-faint">Era</dt>
				<dd class="text-right">{chain.network?.era ?? '—'}</dd>
				<dt class="text-faint">Store sync</dt>
				<dd class="text-right">
					{#if chain.network?.syncPercent != null}{chain.network.syncPercent.toFixed(chain.network.syncPercent === 100 ? 0 : 2)}%{:else}—{/if}
				</dd>
				<dt class="text-faint">Protocol magic</dt>
				<dd class="mono text-right">{chain.network?.magic ?? '—'}</dd>
				<dt class="text-faint">Updates</dt>
				<dd class="text-right">{chain.status === 'live' ? 'WebSocket' : chain.status === 'polling' ? 'Polling every 4s' : '—'}</dd>
			</dl>
			<div class="mt-3 truncate rounded-lg bg-surface-2 px-2.5 py-1.5 font-mono text-[11px] text-muted" title={apiBase()}>{apiBase()}</div>
			{#if chain.error}<p class="mt-2 text-xs text-danger">{chain.error}</p>{/if}
		</div>
	{/if}
</div>
