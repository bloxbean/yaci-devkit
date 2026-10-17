<script lang="ts">
	import { chain } from '$lib/live/chain.svelte';
	import { bech32Encode } from '$lib/bech32';
	import { num, shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import KV from '$lib/ui/KV.svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import JsonView from '$lib/ui/JsonView.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import NativeScriptTree from '$lib/assets/NativeScriptTree.svelte';
	import { isPlutus, plutusVersionOf, scriptTypeLabel } from '$lib/assets/script';
	import ScriptCode from '$lib/julc/ScriptCode.svelte';
	import ScriptActivity from '$lib/julc/ScriptActivity.svelte';
	import BlueprintPanel from '$lib/julc/blueprint/BlueprintPanel.svelte';
	import { blueprints } from '$lib/julc/blueprint/store.svelte';
	import { variantOf } from '$lib/julc/blueprint/blueprint';
	import { tryDecodePlutusData } from '$lib/cbor';
	import Skeleton from '$lib/ui/Skeleton.svelte';
	import FileCode from '@lucide/svelte/icons/file-code';
	import Coins from '@lucide/svelte/icons/coins';
	import Wallet from '@lucide/svelte/icons/wallet';

	let { data } = $props();

	const label = $derived(scriptTypeLabel(data.type));
	const plutus = $derived(isPlutus(data.type));
	const plutusVersion = $derived(plutusVersionOf(data.type));
	const bytes = $derived(data.size ?? (data.cbor ? data.cbor.length / 2 : null));
	let showCbor = $state(false);

	// With a blueprint attached, redeemer variants get their names (e.g. Action.Swap).
	const variantName = $derived.by(() => {
		const found = blueprints.find(data.hash);
		if (!found?.validator.redeemer) return undefined;
		return (cbor: string) => {
			const d = tryDecodePlutusData(cbor);
			return d ? variantOf(d, found.validator.redeemer, found.blueprint.definitions) : null;
		};
	});

	// The script's enterprise address (no stake part): where funds locked by this validator usually sit.
	const scriptAddress = $derived.by(() => {
		const magic = chain.network?.magic;
		if (magic == null) return null;
		const mainnet = magic === 764824073;
		const header = 0x70 | (mainnet ? 1 : 0);
		const hashBytes = data.hash.match(/../g)!.map((h) => parseInt(h, 16));
		return bech32Encode(mainnet ? 'addr' : 'addr_test', new Uint8Array([header, ...hashBytes]));
	});
</script>

<svelte:head><title>Script {shorten(data.hash, 8, 6)} · Yaci Viewer</title></svelte:head>

<PageHeader kind="Script" title={shorten(data.hash, 14, 10)}>
	{#snippet icon()}<FileCode class="h-5 w-5" />{/snippet}
	<div class="flex items-start gap-1.5">
		<span class="mono break-all text-xs text-faint">{data.hash}</span>
		<CopyButton value={data.hash} label="Script hash copied" />
	</div>
	{#snippet actions()}
		<Badge tone={plutus ? 'violet' : 'cyan'}>{label}</Badge>
	{/snippet}
</PageHeader>

<div class="mb-6 grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(0,1fr)]">
	<Panel title="Overview">
		<dl class="grid gap-x-6 gap-y-5 sm:grid-cols-2">
			<KV label="Type">{label}</KV>
			<KV label="Size">{#if bytes}<span class="tabular-nums">{num(bytes)}</span> <span class="text-faint">bytes</span>{:else}<span class="text-muted">—</span>{/if}</KV>
			<KV label="Script hash" wide><Hash value={data.hash} head={16} tail={12} /></KV>
			{#if scriptAddress}
				<KV label="Script address" wide><Hash value={scriptAddress} href="/addresses/{scriptAddress}" head={16} tail={10} /></KV>
			{/if}
		</dl>
	</Panel>
	<Panel title="Used as">
		<div class="flex flex-col gap-2">
			<a href="/assets/policy/{data.hash}" class="group flex items-center gap-3 rounded-xl border border-line p-3 transition hover:border-brand/40">
				<span class="grid h-9 w-9 place-items-center rounded-xl bg-brand/10 text-brand"><Coins class="h-4 w-4" /></span>
				<span class="min-w-0"><span class="block text-sm font-medium group-hover:text-brand">Minting policy</span><span class="block text-xs text-muted">Tokens minted under this hash</span></span>
			</a>
			{#if scriptAddress}
				<a href="/addresses/{scriptAddress}" class="group flex items-center gap-3 rounded-xl border border-line p-3 transition hover:border-brand/40">
					<span class="grid h-9 w-9 place-items-center rounded-xl bg-violet/10 text-violet"><Wallet class="h-4 w-4" /></span>
					<span class="min-w-0"><span class="block text-sm font-medium group-hover:text-brand">Spending validator</span><span class="block text-xs text-muted">Outputs locked at its address (without a stake part)</span></span>
				</a>
			{/if}
		</div>
	</Panel>
</div>

{#if !plutus && data.json}
	<Panel title="Rules" sub="Who must sign, and when this native script allows spending or minting." class="mb-6">
		<NativeScriptTree script={data.json} />
		<div class="mt-5"><JsonView value={data.json} label="Script JSON" maxHeight={280} /></div>
	</Panel>
{/if}

{#if plutus && data.cbor}
	<ScriptCode hash={data.hash} cbor={data.cbor} language={plutusVersion} />
	<div class="mt-6"><BlueprintPanel hash={data.hash} /></div>
	{#if data.activity}
		<div class="mt-6">
			{#await data.activity}
				<Panel title="Activity" sub="Loading recent executions…"><Skeleton rows={4} /></Panel>
			{:then activity}
				{#if activity}<ScriptActivity {activity} {variantName} />{/if}
			{/await}
		</div>
	{/if}
{:else}
	<Panel title="CBOR">
		{#snippet actions()}{#if data.cbor}<CopyButton value={data.cbor} label="CBOR copied" />{/if}{/snippet}
		{#if data.cbor}
			<code class="block overflow-auto break-all rounded-xl border border-line bg-bg-2 p-4 text-[12px] leading-relaxed text-muted" style="max-height:{showCbor ? 'none' : '160px'}">{data.cbor}</code>
			{#if data.cbor.length > 600}
				<button class="mt-2 text-xs font-medium text-brand hover:underline" onclick={() => (showCbor = !showCbor)}>{showCbor ? 'Show less' : 'Show all'}</button>
			{/if}
		{:else}
			<Empty title="No CBOR" text="The store didn't return the script body." />
		{/if}
	</Panel>
{/if}
