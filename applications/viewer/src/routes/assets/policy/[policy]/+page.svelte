<script lang="ts">
	import { num, parseUnit } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Pager from '$lib/ui/Pager.svelte';
	import AssetChip from '$lib/ui/AssetChip.svelte';
	import ErrorBox from '$lib/ui/ErrorBox.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import AssetAvatar from '$lib/assets/AssetAvatar.svelte';
	import MintHistory from '$lib/assets/MintHistory.svelte';
	import { scriptTypeLabel } from '$lib/assets/script';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import FileCode from '@lucide/svelte/icons/file-code';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';

	let { data } = $props();

	// Assets seen on this page of history, newest first.
	const units = $derived([...new Set(data.rows.map((r) => r.unit))]);
	const kind = $derived(data.script?.type ? scriptTypeLabel(data.script.type) : null);
</script>

<svelte:head><title>Policy {data.policy.slice(0, 10)}… · Yaci Viewer</title></svelte:head>

<PageHeader kind="Minting policy" title="{data.policy.slice(0, 12)}…{data.policy.slice(-8)}">
	{#snippet icon()}<ShieldCheck class="h-5 w-5" />{/snippet}
	<div class="flex items-start gap-1.5">
		<span class="mono break-all text-xs text-faint">{data.policy}</span>
		<CopyButton value={data.policy} label="Policy id copied" />
	</div>
	{#snippet actions()}
		{#if kind}<Badge tone={kind.startsWith('Plutus') ? 'violet' : 'cyan'}><FileCode class="h-3.5 w-3.5" />{kind}</Badge>{/if}
	{/snippet}
</PageHeader>

<section class="mb-6 grid gap-4 lg:grid-cols-[minmax(0,1fr)_minmax(0,2fr)]">
	<div class="y-panel relative flex items-center gap-5 overflow-hidden p-5">
		<div class="pointer-events-none absolute -right-16 -top-16 h-48 w-48 rounded-full bg-brand/10 blur-2xl"></div>
		<AssetAvatar policy={data.policy} name={units[0] ? parseUnit(units[0]).name : 'P'} size={64} />
		<div class="min-w-0">
			<div class="y-label">Total supply</div>
			<div class="mt-1 truncate font-display text-3xl font-semibold tabular-nums tracking-tight">{data.supply.value != null ? num(data.supply.value) : '—'}</div>
			<div class="mt-1 text-xs text-muted">
				{data.supply.value != null ? 'units across every asset under this policy' : data.supply.error ? 'The store took too long to total this policy.' : 'Not reported'}
			</div>
		</div>
	</div>
	<Panel title="Assets in recent activity">
		{#snippet actions()}
			<a class="y-btn h-8 text-xs" href="/scripts/{data.policy}">Policy script <ArrowRight class="h-3.5 w-3.5" /></a>
		{/snippet}
		{#if units.length}
			<div class="flex flex-wrap gap-1.5">
				{#each units.slice(0, 18) as u (u)}<AssetChip unit={u} />{/each}
				{#if units.length > 18}<span class="self-center text-xs text-faint">+{units.length - 18} more</span>{/if}
			</div>
		{:else}
			<p class="text-sm text-muted">No mint or burn events on this page of history.</p>
		{/if}
	</Panel>
</section>

<section class="y-panel overflow-hidden">
	<header class="y-panel-head">
		<h2 class="font-display text-[15px] font-semibold">Mint & burn history</h2>
	</header>
	{#if data.listError}
		<ErrorBox title="Couldn't load the history" message="{data.listError}. Busy policies can be too slow for the store; open an asset to see its own history." />
	{:else}
		<MintHistory rows={data.rows} showAsset />
		<Pager page={data.page} hasNext={data.rows.length === data.count} />
	{/if}
</section>
