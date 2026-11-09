<script lang="ts">
	import SourceBadge from '$lib/julc/SourceBadge.svelte';
	import { onDestroy, onMount, untrack } from 'svelte';
	import { chain } from '$lib/live/chain.svelte';
	import { num, shorten } from '$lib/format';
	import PageHeader from '$lib/ui/PageHeader.svelte';
	import Stat from '$lib/ui/Stat.svelte';
	import TimeAgo from '$lib/ui/TimeAgo.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import { julc, wasmSupported } from '$lib/julc/engine.svelte';
	import { NEAR_LIMIT, ScriptMonitor, type MonitoredRun } from '$lib/julc/monitor/monitor.svelte';
	import Activity from '@lucide/svelte/icons/activity';
	import Play from '@lucide/svelte/icons/play';
	import Pause from '@lucide/svelte/icons/pause';
	import Trash from '@lucide/svelte/icons/trash';
	import LoaderCircle from '@lucide/svelte/icons/loader-circle';
	import CircleCheck from '@lucide/svelte/icons/circle-check';
	import CircleX from '@lucide/svelte/icons/circle-x';
	import CircleSlash from '@lucide/svelte/icons/circle-slash';
	import BadgeCheck from '@lucide/svelte/icons/badge-check';
	import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
	import Bug from '@lucide/svelte/icons/bug';
	import GitFork from '@lucide/svelte/icons/git-fork';

	/*
	 * Re-runs the scripts of every new transaction as blocks arrive. Made for devnets: submit a transaction and see
	 * its scripts run, with their budget against the per-transaction limits, the moment it lands.
	 */
	const monitor = new ScriptMonitor();
	let installed = $state<boolean | null>(null);
	let supported = $state(true);
	const devnet = $derived(chain.network?.kind === 'devnet' || chain.network?.kind === 'custom');

	onMount(async () => {
		supported = wasmSupported();
		installed = !!(await julc.available());
		if (!supported || !installed) return;
		monitor.cap = devnet ? Infinity : 3;
		monitor.start();
	});
	onDestroy(() => monitor.stop());

	// Feed new blocks to the monitor, oldest first: every block since the last one seen (fast devnets can make several
	// between updates); when starting, the last two blocks fill the feed.
	$effect(() => {
		const blocks = chain.blocks;
		if (!monitor.running) return;
		untrack(() => {
			const last = monitor.lastBlock;
			const fresh = last == null ? blocks.slice(0, 2) : blocks.filter((b) => b.number > last).slice(0, 20);
			for (const b of [...fresh].reverse()) monitor.block(b);
		});
	});

	const caps = [
		{ label: '1 per block', value: 1 },
		{ label: '3 per block', value: 3 },
		{ label: '10 per block', value: 10 },
		{ label: 'All', value: Infinity }
	];

	const tone = (r: MonitoredRun) =>
		r.outcome === 'passed' ? (Math.max(r.cpuPct, r.memPct) >= NEAR_LIMIT ? 'border-amber/40 bg-amber/10' : 'border-mint/30 bg-mint/[0.06]') : r.outcome === 'couldn’t run' ? 'border-line bg-surface-2' : 'border-danger/40 bg-danger/10';
	const compact = (n: bigint | null) => (n == null ? '—' : n >= 1_000_000n ? `${(Number(n) / 1e6).toFixed(n >= 100_000_000n ? 0 : 1)}M` : n >= 1000n ? `${(Number(n) / 1e3).toFixed(0)}K` : String(n));
</script>

<svelte:head><title>Script monitor · Yaci Viewer</title></svelte:head>

<PageHeader kind="Script Lab" title="Script monitor">
	{#snippet icon()}<Activity class="h-5 w-5" />{/snippet}
	<p class="max-w-2xl text-sm text-muted">
		Every new transaction that runs scripts is re-run in your browser with julc as its block arrives: pass or fail, the budget used, and how close it came to the
		per-transaction limits.
	</p>
	{#snippet actions()}
		{#if monitor.running}
			<button class="y-btn" onclick={() => monitor.stop()}><Pause class="h-4 w-4" />Pause</button>
		{:else}
			<button class="y-btn y-btn-primary" disabled={!installed || !supported} onclick={() => monitor.start()}><Play class="h-4 w-4" />Watch</button>
		{/if}
		{#if monitor.entries.length}<button class="y-icon-btn" title="Clear the feed" aria-label="Clear the feed" onclick={() => monitor.clear()}><Trash class="h-4 w-4" /></button>{/if}
	{/snippet}
</PageHeader>

{#if !supported}
	<Empty title="This browser can’t run the julc engine" text="The monitor needs WebAssembly GC and exception handling. Use a current Chrome, Edge, Firefox or Safari." />
{:else if installed === false}
	<Empty title="Script Lab is not installed" text="This viewer was built without the julc engine." />
{:else}
	<div class="mb-4 flex flex-wrap items-center gap-3 rounded-2xl border border-line bg-surface px-4 py-3 text-sm">
		{#if monitor.running}
			<span class="relative flex h-2.5 w-2.5"><span class="absolute inline-flex h-full w-full animate-ping rounded-full bg-mint opacity-60"></span><span class="relative inline-flex h-2.5 w-2.5 rounded-full bg-mint"></span></span>
			<span>Watching{monitor.lastBlock ? ` · block #${num(monitor.lastBlock)}` : ' for blocks…'}</span>
		{:else}
			<span class="h-2.5 w-2.5 rounded-full bg-faint"></span><span class="text-muted">Paused</span>
		{/if}
		{#if monitor.busy || monitor.queued}<span class="flex items-center gap-1.5 text-muted"><LoaderCircle class="h-4 w-4 animate-spin text-brand" />{monitor.queued ? `${monitor.queued} waiting` : 'Running'}</span>{/if}
		<label class="ml-auto flex items-center gap-2 text-xs text-muted">
			Re-run
			<select class="y-input h-8 w-auto text-xs" value={String(monitor.cap)} onchange={(e) => (monitor.cap = Number(e.currentTarget.value))}>
				{#each caps as c (c.label)}<option value={String(c.value)}>{c.label}</option>{/each}
			</select>
		</label>
	</div>
	{#if !devnet && monitor.cap > 3}
		<p class="-mt-2 mb-4 text-xs text-amber">Each re-run takes several store requests; on a busy public network, re-running every script transaction puts real load on the store.</p>
	{/if}
	{#if monitor.failure}<p class="mb-4 text-sm text-danger">{monitor.failure}</p>{/if}

	<div class="mb-6 grid grid-cols-2 gap-3 sm:grid-cols-3 xl:grid-cols-6">
		<Stat label="Blocks watched" value={num(monitor.stats.blocks)} />
		<Stat label="Script transactions" value={num(monitor.stats.scriptTxs)} sub={monitor.stats.skipped ? `${num(monitor.stats.skipped)} not re-run` : undefined} />
		<Stat label="Script runs" value={num(monitor.stats.runs)} />
		<Stat label="Reproduced" value={num(monitor.stats.reproduced)} sub="exactly, or with the builder’s margin" />
		<Stat label="Failed" value={num(monitor.stats.failed + monitor.stats.couldnt)} sub={monitor.stats.couldnt ? `${num(monitor.stats.couldnt)} couldn’t run` : undefined} />
		<Stat label="Near the limits" value={num(monitor.stats.near)} sub="peak {monitor.stats.peak.toFixed(1)}% of a tx limit" />
	</div>

	{#if !monitor.entries.length}
		<Empty title={monitor.running ? 'Waiting for script transactions' : 'Not watching'} text={monitor.running ? 'Transactions that run Plutus scripts appear here as their blocks arrive.' : 'Press Watch to re-run new script transactions as they arrive.'} />
	{:else}
		<ol class="space-y-2.5">
			{#each monitor.entries as e (e.hash)}
				<li class="animate-fade-up rounded-2xl border border-line bg-surface p-3.5">
					<div class="mb-2.5 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm">
						<a class="mono font-medium text-brand hover:underline" href="/transactions/{e.hash}?tab=scripts">{shorten(e.hash, 10, 8)}</a>
						<a class="text-xs text-muted hover:text-ink" href="/blocks/{e.block}">block #{num(e.block)}</a>
						<span class="text-xs text-faint"><TimeAgo ts={e.time} /></span>
						<SourceBadge source={e.source} compact />
						<span class="ml-auto text-xs tabular-nums text-faint">{e.ms} ms</span>
						<a class="flex items-center gap-1 text-xs font-medium text-muted hover:text-brand" href="/transactions/{e.hash}/fork"><GitFork class="h-3.5 w-3.5" />Fork</a>
					</div>
					<div class="flex flex-wrap gap-2">
						{#each e.runs as r (r.purpose + r.index)}
							<a
								class="group flex min-w-0 items-center gap-2 rounded-xl border px-2.5 py-1.5 text-xs transition hover:brightness-110 {tone(r)}"
								href={r.outcome === 'couldn’t run' ? `/transactions/${e.hash}?tab=scripts` : `/transactions/${e.hash}/debug/${r.purpose}/${r.index}${r.outcome === 'passed' ? '' : '?at=failure'}`}
								title={r.error ?? `Plutus ${r.language ?? '?'} · script ${r.scriptHash}`}
							>
								{#if r.outcome === 'passed'}<CircleCheck class="h-3.5 w-3.5 text-mint" />{:else if r.outcome === 'couldn’t run'}<CircleSlash class="h-3.5 w-3.5 text-faint" />{:else}<CircleX class="h-3.5 w-3.5 text-danger" />{/if}
								<span class="font-semibold">{r.purpose} #{r.index}</span>
								{#if r.cpu != null}
									<span class="tabular-nums text-muted">{compact(r.cpu)} CPU · {compact(r.mem)} mem</span>
									<span class="tabular-nums {Math.max(r.cpuPct, r.memPct) >= NEAR_LIMIT ? 'font-semibold text-amber' : 'text-faint'}">{Math.max(r.cpuPct, r.memPct).toFixed(1)}%</span>
								{:else if r.error}
									<span class="max-w-56 truncate text-muted">{r.error}</span>
								{/if}
								{#if r.verdict}<BadgeCheck class="h-3.5 w-3.5 text-mint" aria-label="Reproduced" />{/if}
								{#if Math.max(r.cpuPct, r.memPct) >= NEAR_LIMIT}<TriangleAlert class="h-3.5 w-3.5 text-amber" aria-label="Near the limit" />{/if}
								<Bug class="h-3 w-3 text-faint opacity-0 transition group-hover:opacity-100" />
							</a>
						{/each}
					</div>
				</li>
			{/each}
		</ol>
	{/if}
{/if}
