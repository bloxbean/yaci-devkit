<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/state';
	import { num } from '$lib/format';
	import Panel from '$lib/ui/Panel.svelte';
	import Tabs from '$lib/ui/Tabs.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import Skeleton from '$lib/ui/Skeleton.svelte';
	import CodeView from './CodeView.svelte';
	import { julc, wasmSupported } from './engine.svelte';
	import type { DecodeResponse, PlutusVersion } from './types';
	import Braces from '@lucide/svelte/icons/braces';
	import Sparkles from '@lucide/svelte/icons/sparkles';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import ShieldAlert from '@lucide/svelte/icons/shield-alert';
	import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
	import LoaderCircle from '@lucide/svelte/icons/loader-circle';

	/*
	 * The code of a Plutus script: readable UPLC and the raw CBOR. Decoding runs in the browser with the julc engine,
	 * which is only downloaded when the user asks (or has chosen to always decode).
	 */
	let { hash, cbor, language }: { hash: string; cbor: string; language: PlutusVersion | null } = $props();

	type View = 'uplc' | 'cbor';
	const tabs = [
		{ id: 'uplc', label: 'UPLC' },
		{ id: 'cbor', label: 'CBOR' }
	];
	let view = $state<View>(page.url.searchParams.get('view') === 'cbor' ? 'cbor' : 'uplc');

	let installed = $state<boolean | null>(null);
	let supported = $state(true);
	let started = $state(false);
	let auto = $state(false);
	let decoded = $state<DecodeResponse | null>(null);
	let failure = $state<string | null>(null);
	let search = $state('');

	const info = $derived(decoded?.info ?? null);
	const verified = $derived(info?.scriptHash ? info.scriptHash === hash : null);

	onMount(async () => {
		auto = julc.auto;
		supported = wasmSupported();
		installed = !!(await julc.available());
		if (installed && supported && auto) decode();
	});

	async function decode() {
		started = true;
		failure = null;
		decoded = null;
		try {
			decoded = await julc.run((c) => c.vm.decode({ script: { script: cbor, ...(language ? { language } : {}) } }));
			if (!decoded.ok) failure = decoded.error || 'The engine could not decode this script.';
		} catch (e) {
			failure = (e as Error)?.message || 'The julc engine failed.';
		}
	}

	function toggleAuto(on: boolean) {
		auto = on;
		julc.auto = on;
		if (on && !started) decode();
	}

	const fact = 'rounded-xl border border-line bg-surface-2/60 px-3.5 py-2.5';
</script>

<Panel title="Code" sub={started ? `Decoded in your browser by julc ${julc.manifest?.julcVersion ?? ''}` : 'Compiled Plutus code'}>
	{#snippet icon()}<Braces class="h-4 w-4" />{/snippet}
	{#snippet actions()}
		{#if view === 'cbor'}<CopyButton value={cbor} label="CBOR copied" />{/if}
	{/snippet}

	<div class="-mx-5 -mt-5 mb-5"><Tabs {tabs} bind:active={view} param="view" /></div>

	{#if view === 'cbor'}
		<code class="block max-h-[420px] overflow-auto break-all rounded-xl border border-line bg-bg-2 p-4 text-[12px] leading-relaxed text-muted">{cbor}</code>
	{:else if installed === false}
		<Empty title="Script Lab is not installed" text="This viewer was built without the julc engine, so the code can't be decoded here. The raw CBOR is in the CBOR tab." />
	{:else if !supported}
		<Empty title="This browser can’t run the julc engine" text="Decoding needs WebAssembly GC and exception handling. Use a current Chrome, Edge, Firefox or Safari. The raw CBOR is in the CBOR tab." />
	{:else if !started}
		<div class="grid place-items-center rounded-2xl border border-dashed border-line bg-bg-2/60 px-6 py-10 text-center">
			<span class="mb-3 grid h-11 w-11 place-items-center rounded-2xl bg-brand/10 text-brand"><Sparkles class="h-5 w-5" /></span>
			<h3 class="font-display text-lg font-semibold">Read this script</h3>
			<p class="mt-1 max-w-md text-sm text-muted">
				Decode the compiled code into readable UPLC, see which builtins it uses and check its hash. It runs in your browser with the julc engine,
				a few megabytes downloaded once.
			</p>
			<button class="y-btn y-btn-primary mt-5" disabled={installed === null} onclick={decode}>
				<Sparkles class="h-4 w-4" />Decode script
			</button>
			<label class="mt-3 flex items-center gap-2 text-xs text-muted">
				<input type="checkbox" class="accent-[rgb(var(--c-brand))]" checked={auto} onchange={(e) => toggleAuto(e.currentTarget.checked)} />
				Always decode scripts automatically
			</label>
		</div>
	{:else if failure}
		<div class="flex items-start gap-3 rounded-xl border border-danger/30 bg-danger/5 p-4 text-sm">
			<TriangleAlert class="mt-0.5 h-4 w-4 shrink-0 text-danger" />
			<div class="min-w-0">
				<p class="font-medium">{julc.status === 'unsupported' ? 'This browser can’t run the julc engine' : 'Couldn’t read this script'}</p>
				<p class="mt-0.5 break-words text-muted">{failure}</p>
				{#if julc.status !== 'unsupported'}
					<button class="mt-3 text-xs font-medium text-brand hover:underline" onclick={decode}>Try again</button>
				{/if}
			</div>
		</div>
	{:else if !decoded}
		<div class="flex items-center gap-2 pb-4 text-sm text-muted" role="status">
			<LoaderCircle class="h-4 w-4 animate-spin text-brand" />
			{julc.status === 'loading' ? 'Starting the julc engine… (first time only)' : 'Decoding…'}
		</div>
		<Skeleton rows={8} />
	{:else if info}
		<div class="mb-4 grid grid-cols-2 gap-2.5 sm:grid-cols-3 xl:grid-cols-6">
			<div class={fact}><div class="y-label">Language</div><div class="mt-0.5 font-medium">Plutus {info.language ?? '—'}</div></div>
			<div class={fact}><div class="y-label">Plutus Core</div><div class="mt-0.5 font-medium tabular-nums">{info.programVersion ?? '—'}</div></div>
			<div class={fact}><div class="y-label">Size</div><div class="mt-0.5 font-medium tabular-nums">{num(info.flatBytes)} <span class="text-xs font-normal text-faint">bytes</span></div></div>
			<div class={fact}><div class="y-label">Terms</div><div class="mt-0.5 font-medium tabular-nums">{num(info.termCount)}</div></div>
			<div class={fact}><div class="y-label">Parameters applied</div><div class="mt-0.5 font-medium tabular-nums">{info.paramsApplied}</div></div>
			<div class="{fact} {verified === false ? 'border-danger/40 bg-danger/5' : ''}">
				<div class="y-label">Hash</div>
				{#if verified}
					<div class="mt-0.5 flex items-center gap-1.5 font-medium text-mint"><ShieldCheck class="h-4 w-4" />Verified</div>
				{:else if verified === false}
					<div class="mt-0.5 flex items-center gap-1.5 font-medium text-danger" title="The code hashes to {info.scriptHash}"><ShieldAlert class="h-4 w-4" />Mismatch</div>
				{:else}
					<div class="mt-0.5 text-muted">—</div>
				{/if}
			</div>
		</div>

		{#if verified === false}
			<p class="mb-4 rounded-xl border border-danger/30 bg-danger/5 p-3 text-sm">
				This code hashes to <span class="mono break-all">{info.scriptHash}</span>, not to this script's hash. The store may hold the wrong body, or the language was misread.
			</p>
		{/if}
		{#if info.warnings?.length}
			<ul class="mb-4 space-y-1 rounded-xl border border-amber/30 bg-amber/5 p-3 text-sm">
				{#each info.warnings as w}<li class="flex gap-2"><TriangleAlert class="mt-0.5 h-4 w-4 shrink-0 text-amber" />{w}</li>{/each}
			</ul>
		{/if}
		{#if info.builtins?.length}
			<div class="mb-4">
				<div class="y-label mb-2">Builtins used <span class="tabular-nums">({info.builtins.length})</span></div>
				<div class="flex flex-wrap gap-1.5">
					{#each info.builtins as b}
						<button
							class="mono rounded-md border px-2 py-0.5 text-[11.5px] transition {search === b ? 'border-cyan/60 bg-cyan/10 text-cyan' : 'border-line bg-surface-2 text-cyan hover:border-cyan/40'}"
							title="Find {b} in the code"
							onclick={() => (search = search === b ? '' : b)}>{b}</button
						>
					{/each}
				</div>
			</div>
		{/if}
		<CodeView text={decoded.uplcText ?? ''} language="uplc" label="UPLC" filename="{hash.slice(0, 16)}.uplc" bind:query={search} />
	{/if}
</Panel>
