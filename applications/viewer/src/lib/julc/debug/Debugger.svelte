<script lang="ts">
	import { onDestroy, onMount } from 'svelte';
	import { page as appPage } from '$app/state';
	import { replaceState } from '$app/navigation';
	import { num } from '$lib/format';
	import { copyText } from '$lib/ui/toast.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import Skeleton from '$lib/ui/Skeleton.svelte';
	import CodeView from '../CodeView.svelte';
	import Timeline from './Timeline.svelte';
	import { julc, wasmSupported } from '../engine.svelte';
	import { prepareRuns, runAll, vmRequest, type RunResult } from '../replay/run';
	import type { RunPlan } from '../replay/build';
	import type { PageTx } from '../replay/load';
	import type { Breakpoints, DebugReply, DebugSession, Snapshot, Timeline as RunTimeline } from '../types';
	import SkipBack from '@lucide/svelte/icons/skip-back';
	import StepBack from '@lucide/svelte/icons/step-back';
	import StepForward from '@lucide/svelte/icons/step-forward';
	import Redo from '@lucide/svelte/icons/redo';
	import CornerLeftUp from '@lucide/svelte/icons/corner-left-up';
	import FastForward from '@lucide/svelte/icons/fast-forward';
	import SkipForward from '@lucide/svelte/icons/skip-forward';
	import Flag from '@lucide/svelte/icons/flag';
	import Link from '@lucide/svelte/icons/link';
	import LoaderCircle from '@lucide/svelte/icons/loader-circle';
	import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
	import CircleCheck from '@lucide/svelte/icons/circle-check';
	import CircleX from '@lucide/svelte/icons/circle-x';
	import Info from '@lucide/svelte/icons/info';

	/*
	 * Step through one script run of a transaction. The run is rebuilt exactly as on the Scripts tab (same corrected
	 * context), then julc's VM replays it: every step is addressable, so ?step=N links to an exact machine state.
	 */
	let {
		page,
		purpose,
		index,
		prepared = undefined,
		syncUrl: keepUrl = true
	}: {
		page: PageTx;
		purpose: string;
		index: number;
		/** A run that is already rebuilt and evaluated (e.g. a forked one): debug it as is. Plain objects, not state. */
		prepared?: { plan: RunPlan; result: RunResult };
		/** Keep ?step= in the URL (off when the run isn't addressable by URL, like a fork). */
		syncUrl?: boolean;
	} = $props();

	const key = $derived(`${purpose}#${index}`);
	let phase = $state<'checking' | 'preparing' | 'ready' | 'error'>('checking');
	let failure = $state<string | null>(null);
	let run = $state<RunResult | null>(null);
	let uplc = $state('');
	let builtins = $state<string[]>([]);
	let session: DebugSession | null = null;
	let timeline = $state<RunTimeline | null>(null);
	let snap = $state<Snapshot | null>(null);
	let busy = $state(false);

	let lineBreaks = $state(new Set<number>());
	let builtinBreaks = $state(new Set<string>());
	let onTrace = $state(false);
	let onError = $state(true);

	/** The last step: steps run from 0 to totalSteps, where totalSteps is the finished state (result, full budget). */
	const last = $derived(timeline ? Number(timeline.totalSteps) : 0);
	const step = $derived(snap ? Number(snap.step) : 0);
	const errorStep = $derived(timeline?.errorStep != null ? Number(timeline.errorStep) : null);
	const traceSteps = $derived((timeline?.traceSteps ?? []).map(Number));
	const breakpoints = $derived<Breakpoints>({ lines: [...lineBreaks], builtins: [...builtinBreaks], onTrace, onError });

	onMount(async () => {
		if (!wasmSupported()) return fail('This browser can’t run the julc engine (it needs WebAssembly GC and exception handling). Use a current Chrome, Edge, Firefox or Safari.');
		if (!(await julc.available())) return fail('Script Lab is not installed with this viewer, so scripts can’t be debugged here.');
		await open();
	});

	onDestroy(() => {
		session?.close().catch(() => {});
	});

	function fail(message: string) {
		failure = message;
		phase = 'error';
	}

	async function open() {
		phase = 'preparing';
		failure = null;
		try {
			let plan: RunPlan | undefined;
			let result: RunResult | undefined;
			if (prepared) ({ plan, result } = prepared);
			else {
				const plans = await prepareRuns(page);
				plan = plans.find((p) => p.key === key);
				if (!plan) return fail(`This transaction has no ${purpose} script run #${index}.`);
				if (!plan.request) return fail(plan.skip ?? 'This run can’t be rebuilt.');
				const results: Record<string, RunResult> = {};
				const engineFailure = await runAll(plans, (k, r) => (results[k] = r));
				if (engineFailure) return fail(engineFailure);
				result = results[key];
			}
			if (!plan?.request) return fail(plan?.skip ?? 'This run can’t be rebuilt.');
			// Keep the plain object for engine requests: reactive state is a proxy, which can't be posted to a worker.
			run = result;
			if (!result?.args) return fail(result?.error ?? 'julc could not build the context for this run.');

			const decoded = await julc.run((c) => c.vm.decode({ script: plan!.request!.script }));
			uplc = decoded.uplcText ?? '';
			builtins = decoded.info?.builtins ?? [];

			const opened = await julc.run((c) => c.vm.debug(vmRequest(plan!, result!.args!)));
			if (opened.ok === false) return fail(opened.error ?? 'julc could not open a debug session.');
			session = opened;
			// A new session hasn't run yet; the `timeline` action runs it once (total steps, traces, failure).
			const ran = await julc.run((c) => (c.debug ? c.debug.act({ sessionId: opened.sessionId, action: 'timeline', step: 0n }) : opened.snapshot()));
			if (ran.timeline) timeline = ran.timeline;
			apply(await session.snapshot());

			// Start where the link points: ?step=N, or ?at=failure for the failing step.
			const wanted = keepUrl ? appPage.url.searchParams.get('step') : null;
			if (wanted && /^\d+$/.test(wanted)) await act((s) => s.goto(BigInt(Math.min(Number(wanted), last))));
			else if ((keepUrl ? appPage.url.searchParams.get('at') === 'failure' : prepared?.result.evaluation?.status !== 'success') && errorStep != null) await act((s) => s.goto(BigInt(errorStep)));
			phase = 'ready';
		} catch (e) {
			fail((e as Error)?.message ?? String(e));
		}
	}

	function apply(reply: DebugReply) {
		if (!reply.ok && reply.error) throw new Error(reply.error);
		if (reply.timeline) timeline = reply.timeline;
		if (reply.snapshot) snap = reply.snapshot;
	}

	let pending: ((s: DebugSession) => Promise<DebugReply>) | null = null;

	/** Run a session action; while one runs, only the latest request waits (so dragging the timeline stays live). */
	async function act(fn: (s: DebugSession) => Promise<DebugReply>) {
		if (!session) return;
		if (busy) {
			pending = fn;
			return;
		}
		busy = true;
		try {
			apply(await fn(session));
			while (pending) {
				const next = pending;
				pending = null;
				apply(await next(session));
			}
			syncUrl();
		} catch (e) {
			const message = (e as Error)?.message ?? String(e);
			failure = /closed|not.found|worker/i.test(message) ? 'The debug session was lost (the engine restarted). Reopen it to continue.' : message;
		} finally {
			busy = false;
		}
	}

	const goto = (n: number) => act((s) => s.goto(BigInt(Math.max(0, Math.min(n, last)))));
	const back = () => goto(step - 1);
	const forward = () => act((s) => s.step());
	const over = () => act((s) => s.over(breakpoints));
	const out = () => act((s) => s.out(breakpoints));
	const resume = () => act((s) => s.continue(breakpoints));
	const toFailure = () => errorStep != null && goto(errorStep);

	let urlTimer: ReturnType<typeof setTimeout> | undefined;
	function syncUrl() {
		if (!keepUrl) return;
		clearTimeout(urlTimer);
		urlTimer = setTimeout(() => {
			const u = new URL(appPage.url);
			u.searchParams.set('step', String(step));
			u.searchParams.delete('at');
			replaceState(u, {});
		}, 250);
	}

	function shareLink() {
		const u = new URL(appPage.url);
		u.searchParams.set('step', String(step));
		u.searchParams.delete('at');
		copyText(u.href, `Link to step ${num(step)} copied`);
	}

	function toggleLine(line: number) {
		const next = new Set(lineBreaks);
		if (next.has(line)) next.delete(line);
		else next.add(line);
		lineBreaks = next;
	}
	function toggleBuiltin(name: string) {
		const next = new Set(builtinBreaks);
		if (next.has(name)) next.delete(name);
		else next.add(name);
		builtinBreaks = next;
	}

	function onKey(e: KeyboardEvent) {
		if (phase !== 'ready' || e.metaKey || e.ctrlKey || e.altKey) return;
		const el = e.target as HTMLElement | null;
		if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA' || el.isContentEditable)) return;
		const map: Record<string, () => unknown> = {
			ArrowRight: forward,
			ArrowLeft: back,
			ArrowDown: over,
			ArrowUp: out,
			c: resume,
			f: toFailure,
			Home: () => goto(0),
			End: () => goto(last)
		};
		const fn = map[e.key];
		if (fn) {
			e.preventDefault();
			fn();
		}
	}

	const pctOf = (a: bigint | undefined, b: bigint | undefined) => (a == null || !b ? 0 : Math.min(100, Number((a * 10000n) / b) / 100));
	const declared = $derived(run?.plan.declared ?? null);
	const finalStatus = $derived(timeline?.status ?? null);
	const kbd = 'y-kbd';
</script>

<svelte:window onkeydown={onKey} />

{#if phase === 'checking' || phase === 'preparing'}
	<Panel>
		<div class="flex items-center gap-2 text-sm text-muted" role="status">
			<LoaderCircle class="h-4 w-4 animate-spin text-brand" />
			{phase === 'checking' ? 'Checking the julc engine…' : julc.status === 'loading' ? 'Starting the julc engine…' : 'Rebuilding the transaction and replaying the run…'}
		</div>
		<Skeleton rows={10} />
	</Panel>
{:else if phase === 'error'}
	<div class="flex items-start gap-3 rounded-2xl border border-danger/30 bg-danger/5 p-5 text-sm">
		<TriangleAlert class="mt-0.5 h-5 w-5 shrink-0 text-danger" />
		<div>
			<p class="font-medium">Can’t debug this run</p>
			<p class="mt-1 text-muted">{failure}</p>
			<a class="mt-3 inline-block text-xs font-medium text-brand hover:underline" href="/transactions/{page.hash}?tab=scripts">Back to the transaction’s scripts</a>
		</div>
	</div>
{:else}
	<!-- Controls and timeline stay at the top while the code scrolls. -->
	<div class="sticky top-16 z-20 -mx-1 mb-5 rounded-2xl border border-line bg-surface/95 p-3 shadow-card backdrop-blur">
		<div class="flex flex-wrap items-center gap-1.5">
			<button class="y-icon-btn" title="Start (Home)" aria-label="Go to the first step" disabled={busy || step === 0} onclick={() => goto(0)}><SkipBack class="h-4 w-4" /></button>
			<button class="y-icon-btn" title="Step back (←)" aria-label="Step back" disabled={busy || step === 0} onclick={back}><StepBack class="h-4 w-4" /></button>
			<button class="y-btn y-btn-primary h-9 px-3" title="Step (→)" aria-label="Step" disabled={busy || step >= last} onclick={forward}><StepForward class="h-4 w-4" />Step</button>
			<button class="y-icon-btn" title="Step over (↓)" aria-label="Step over" disabled={busy} onclick={over}><Redo class="h-4 w-4" /></button>
			<button class="y-icon-btn" title="Step out (↑)" aria-label="Step out" disabled={busy} onclick={out}><CornerLeftUp class="h-4 w-4" /></button>
			<button class="y-icon-btn" title="Continue to the next breakpoint (C)" aria-label="Continue" disabled={busy} onclick={resume}><FastForward class="h-4 w-4" /></button>
			<button class="y-icon-btn" title="Last step (End)" aria-label="Go to the last step" disabled={busy || step >= last} onclick={() => goto(last)}><SkipForward class="h-4 w-4" /></button>
			{#if errorStep != null}
				<button class="y-btn h-9 border-danger/40 px-3 text-danger" title="Jump to the failure (F)" disabled={busy} onclick={toFailure}><Flag class="h-4 w-4" />Failure</button>
			{/if}
			<span class="ml-2 text-sm tabular-nums">Step <b>{num(step)}</b> <span class="text-faint">/ {num(last)}</span></span>
			{#if busy}<LoaderCircle class="h-4 w-4 animate-spin text-brand" />{/if}
			{#if keepUrl}<button class="y-btn ml-auto h-9 px-3" title="Copy a link to this step" onclick={shareLink}><Link class="h-4 w-4" />Link to this step</button>{/if}
		</div>
		<div class="mt-3"><Timeline total={last + 1} current={step} traces={traceSteps} error={errorStep} onseek={goto} /></div>
		<div class="mt-1 hidden flex-wrap gap-x-4 gap-y-1 text-[11px] text-faint sm:flex">
			<span><kbd class={kbd}>→</kbd> step</span><span><kbd class={kbd}>←</kbd> back</span><span><kbd class={kbd}>↓</kbd> over</span><span><kbd class={kbd}>↑</kbd> out</span>
			<span><kbd class={kbd}>C</kbd> continue</span>{#if errorStep != null}<span><kbd class={kbd}>F</kbd> failure</span>{/if}<span><kbd class={kbd}>Home</kbd>/<kbd class={kbd}>End</kbd> start/end</span>
		</div>
		{#if failure}<p class="mt-2 text-sm text-danger">{failure} <button class="font-medium underline" onclick={() => { session = null; open(); }}>Reopen</button></p>{/if}
	</div>

	<div class="grid gap-5 xl:grid-cols-[minmax(0,1.65fr)_minmax(0,1fr)]">
		<div class="min-w-0 space-y-4">
			<CodeView text={uplc} language="uplc" label="UPLC" filename="{run?.plan.scriptHash.slice(0, 16)}.uplc" height={620} active={snap?.span ?? null} breakpoints={lineBreaks} ontoggle={toggleLine} />
			<Panel title="Breakpoints" sub="Click a line number to break there. Continue, over and out stop at breakpoints.">
				<div class="flex flex-wrap gap-4 text-sm">
					<label class="flex items-center gap-2"><input type="checkbox" class="accent-[rgb(var(--c-brand))]" bind:checked={onError} />On failure</label>
					<label class="flex items-center gap-2"><input type="checkbox" class="accent-[rgb(var(--c-brand))]" bind:checked={onTrace} />On trace</label>
					{#if lineBreaks.size}
						<span class="text-muted">Lines {[...lineBreaks].sort((a, b) => a - b).join(', ')} <button class="ml-1 text-xs font-medium text-brand hover:underline" onclick={() => (lineBreaks = new Set())}>clear</button></span>
					{/if}
				</div>
				{#if builtins.length}
					<div class="y-label mb-2 mt-4">Break on builtin</div>
					<div class="flex flex-wrap gap-1.5">
						{#each builtins as b}
							<button
								class="mono rounded-md border px-2 py-0.5 text-[11.5px] transition {builtinBreaks.has(b) ? 'border-violet/60 bg-violet/15 text-violet' : 'border-line bg-surface-2 text-cyan hover:border-cyan/40'}"
								aria-pressed={builtinBreaks.has(b)}
								onclick={() => toggleBuiltin(b)}>{b}</button
							>
						{/each}
					</div>
				{/if}
			</Panel>
		</div>

		<div class="min-w-0 space-y-4">
			{#if run?.plan.notes.length}
				<ul class="space-y-1 rounded-xl border border-line bg-surface-2/50 p-3 text-xs text-muted">
					{#each run.plan.notes as note}<li class="flex gap-1.5"><Info class="mt-0.5 h-3.5 w-3.5 shrink-0 text-faint" />{note}</li>{/each}
				</ul>
			{/if}
			<Panel title="Now">
				{#if snap}
					<dl class="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
						<div><dt class="y-label">Phase</dt><dd class="mt-0.5 font-medium capitalize">{snap.phase ?? '—'}</dd></div>
						<div><dt class="y-label">Term</dt><dd class="mt-0.5 font-medium">{snap.termKind ?? '—'}</dd></div>
						<div><dt class="y-label">Stopped by</dt><dd class="mt-0.5">{snap.stopReason ?? '—'}</dd></div>
						<div><dt class="y-label">Stack depth</dt><dd class="mt-0.5 tabular-nums">{snap.stackDepth}</dd></div>
					</dl>
					{#if snap.value}
						<div class="y-label mb-1 mt-4">Value</div>
						<pre class="mono max-h-40 overflow-auto whitespace-pre-wrap break-all rounded-lg border border-line bg-bg-2 p-2.5 text-[12px] leading-5">{snap.value}</pre>
					{/if}
					{#if snap.finished}
						<div class="mt-4 flex items-start gap-2 rounded-xl border p-3 text-sm {snap.status === 'success' ? 'border-mint/30 bg-mint/5' : 'border-danger/30 bg-danger/5'}">
							{#if snap.status === 'success'}<CircleCheck class="mt-0.5 h-4 w-4 shrink-0 text-mint" />{:else}<CircleX class="mt-0.5 h-4 w-4 shrink-0 text-danger" />{/if}
							<span>{snap.status === 'success' ? 'The script finished successfully.' : snap.error || 'The script failed.'}</span>
						</div>
					{/if}
				{/if}
			</Panel>

			<Panel title="Budget" sub={finalStatus ? `This run ends in ${finalStatus === 'success' ? 'success' : finalStatus === 'budgetExhausted' ? 'running out of budget' : 'failure'} after ${num(timeline?.cpu)} CPU` : undefined}>
				{#if snap && timeline}
					{#each [{ label: 'CPU', used: snap.cpu, delta: snap.cpuDelta, end: timeline.cpu, want: declared?.steps }, { label: 'Memory', used: snap.mem, delta: snap.memDelta, end: timeline.mem, want: declared?.mem }] as b (b.label)}
						<div class="mb-3 last:mb-0">
							<div class="mb-1 flex items-baseline justify-between text-xs">
								<span class="text-muted">{b.label}</span>
								<span class="tabular-nums"><b>{num(b.used)}</b> <span class="text-faint">/ {num(b.end)} this run{b.want ? ` · ${num(b.want)} declared` : ''}</span></span>
							</div>
							<div class="h-1.5 overflow-hidden rounded-full bg-line"><div class="h-full rounded-full bg-grad" style="width:{pctOf(b.used, b.end)}%"></div></div>
							<div class="mt-1 text-[11px] tabular-nums text-faint">+{num(b.delta)} this step</div>
						</div>
					{/each}
				{/if}
			</Panel>

			<Panel title="Environment" sub={snap?.environment?.length ? `${snap.environment.length} binding${snap.environment.length === 1 ? '' : 's'} in scope (nearest first)` : 'Nothing bound yet'} flush>
				{#if snap?.environment?.length}
					<ul class="max-h-72 divide-y divide-line overflow-auto">
						{#each snap.environment as v}
							<li class="mono px-4 py-1.5 text-[12px] leading-5"><span class="text-violet">{v.name}</span> <span class="text-faint">=</span> <span class="break-all text-muted">{v.value}</span></li>
						{/each}
					</ul>
				{/if}
			</Panel>

			<Panel title="Frames" sub={snap?.frames?.length ? `${snap.frames.length} waiting` : 'The stack is empty'} flush>
				{#if snap?.frames?.length}
					<ol class="max-h-56 divide-y divide-line overflow-auto">
						{#each snap.frames as f, i}
							<li class="px-4 py-1.5 text-[12px] leading-5">
								<span class="mono text-faint">{i}</span>
								<span class="font-medium">{f.kind}</span>
								<span class="text-muted">{f.detail}</span>
								{#if f.span}<span class="mono text-faint">· line {f.span.startLine}</span>{/if}
							</li>
						{/each}
					</ol>
				{/if}
			</Panel>

			<Panel title="Traces" sub={snap?.traces?.length ? `${snap.traces.length} so far` : 'None yet'} flush>
				{#if snap?.traces?.length}
					<ol class="mono max-h-48 overflow-auto px-4 py-2 text-[12px] leading-5">
						{#each snap.traces as t, i}<li><span class="mr-2 select-none text-faint">{i + 1}</span>{t}</li>{/each}
					</ol>
				{/if}
			</Panel>

		</div>
	</div>
{/if}
