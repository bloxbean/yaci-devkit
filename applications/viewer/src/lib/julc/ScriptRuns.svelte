<script lang="ts">
	import { onMount } from 'svelte';
	import { num } from '$lib/format';
	import ScriptCard from '$lib/tx/ScriptCard.svelte';
	import PlutusView from '$lib/ui/PlutusView.svelte';
	import { julc, wasmSupported } from './engine.svelte';
	import { purposeOf, type RunPlan, type StoreScriptRun } from './replay/build';
	import type { PageTx } from './replay/load';
	import { prepareRuns, runAll, type RunResult } from './replay/run';
	import Sparkles from '@lucide/svelte/icons/sparkles';
	import LoaderCircle from '@lucide/svelte/icons/loader-circle';
	import CircleCheck from '@lucide/svelte/icons/circle-check';
	import CircleX from '@lucide/svelte/icons/circle-x';
	import CircleSlash from '@lucide/svelte/icons/circle-slash';
	import Gauge from '@lucide/svelte/icons/gauge';
	import BadgeCheck from '@lucide/svelte/icons/badge-check';
	import Info from '@lucide/svelte/icons/info';
	import RotateCcw from '@lucide/svelte/icons/rotate-ccw';
	import Bug from '@lucide/svelte/icons/bug';
	import GitFork from '@lucide/svelte/icons/git-fork';

	/*
	 * The transaction's script runs, each re-executable in the browser with julc: the transaction is rebuilt from
	 * Yaci Store data and every redeemer runs with the cost model of the epoch it ran in. Results are compared with
	 * the budget declared on chain. Nothing is downloaded until the user asks (or opted in to automatic runs).
	 */
	let {
		page,
		invalid = false,
		maxMem,
		maxSteps
	}: { page: PageTx; invalid?: boolean; maxMem: number; maxSteps: number } = $props();

	type Result = RunResult;

	let installed = $state<boolean | null>(null);
	let supported = $state(true);
	let auto = $state(false);
	let phase = $state<'idle' | 'loading' | 'running' | 'done'>('idle');
	let failure = $state<string | null>(null);
	let results = $state<Record<string, Result>>({});

	const keyOf = (s: StoreScriptRun) => {
		const r = typeof s.redeemer === 'string' ? safeJson(s.redeemer) : s.redeemer;
		return r?.tag ? `${purposeOf(r.tag) ?? String(r.tag).toLowerCase()}#${Number(r.index ?? 0)}` : null;
	};
	function safeJson(text: string) {
		try {
			return JSON.parse(text);
		} catch {
			return null;
		}
	}

	const runnable = $derived(page.scripts.filter((s) => keyOf(s)).length);
	const done = $derived(Object.values(results));
	const passed = (r: Result) => r.state === 'done' && !!r.evaluation && (r.evaluation.accepted ?? r.evaluation.status === 'success');
	const exact = (r: Result) => passed(r) && r.plan.declared != null && r.evaluation!.cpu === r.plan.declared.steps && r.evaluation!.mem === r.plan.declared.mem;
	/**
	 * Transaction builders often declare the evaluated budget plus a safety margin (e.g. +20%). When memory and CPU
	 * are both scaled by the same factor, the run is a reproduction too; returns the margin in percent.
	 */
	function margin(r: Result): number | null {
		if (!passed(r) || !r.plan.declared || exact(r) || !r.evaluation!.mem || !r.evaluation!.cpu) return null;
		const m = Number(r.plan.declared.mem) / Number(r.evaluation!.mem);
		const c = Number(r.plan.declared.steps) / Number(r.evaluation!.cpu);
		return m > 1.001 && Math.abs(m - c) < 0.0005 ? Math.round((m - 1) * 1000) / 10 : null;
	}
	const summary = $derived({
		exact: done.filter(exact).length,
		margin: done.filter((r) => margin(r) != null).length,
		passed: done.filter(passed).length,
		failed: done.filter((r) => r.state === 'done' && !passed(r)).length,
		other: done.filter((r) => r.state === 'error' || r.state === 'skipped').length
	});

	onMount(async () => {
		auto = julc.auto;
		supported = wasmSupported();
		installed = !!(await julc.available());
		if (installed && supported && auto && runnable) run();
	});

	async function run() {
		failure = null;
		results = {};
		phase = 'loading';
		let plans: RunPlan[];
		try {
			plans = await prepareRuns(page);
		} catch (e) {
			failure = `Couldn’t load what the re-run needs from Yaci Store: ${(e as Error)?.message ?? e}`;
			phase = 'idle';
			return;
		}
		phase = 'running';
		failure = await runAll(plans, (key, r) => (results[key] = r));
		phase = 'done';
	}

	function toggleAuto(on: boolean) {
		auto = on;
		julc.auto = on;
		if (on && phase === 'idle') run();
	}

	const pct = (a: bigint, b: bigint) => (b === 0n ? 0 : Number((a * 10000n) / b) / 100);
</script>

{#if runnable && installed !== false}
	<div class="mb-4 rounded-2xl border border-brand/25 bg-gradient-to-br from-brand/[0.06] to-violet/[0.04] p-4">
		<div class="flex flex-wrap items-start gap-3">
			<span class="grid h-9 w-9 shrink-0 place-items-center rounded-xl bg-brand/10 text-brand"><Sparkles class="h-4 w-4" /></span>
			<div class="min-w-0 flex-1">
				<h3 class="font-display font-semibold">Re-run in your browser</h3>
				<p class="text-sm text-muted">
					{#if !supported}
						This browser can’t run the julc engine (it needs WebAssembly GC and exception handling). Use a current Chrome, Edge, Firefox or Safari.
					{:else}
						Execute {runnable === 1 ? 'the script' : `all ${runnable} scripts`} again with julc, using the cost model of epoch {page.epoch ?? 'latest'}, and compare with the budget declared on chain.
					{/if}
				</p>
				{#if invalid}
					<p class="mt-1.5 text-sm"><CircleX class="mr-1 inline h-4 w-4 align-[-3px] text-danger" />This transaction failed script validation on chain. A re-run shows which script failed and why.</p>
				{/if}
			</div>
			{#if supported}
				<div class="flex flex-col items-end gap-2">
					{#if phase === 'loading' || phase === 'running'}
						<span class="flex h-9 items-center gap-2 text-sm text-muted" role="status">
							<LoaderCircle class="h-4 w-4 animate-spin text-brand" />
							{phase === 'loading' ? 'Rebuilding the transaction…' : julc.status === 'loading' ? 'Starting the julc engine…' : 'Running…'}
						</span>
					{:else}
						<button class="y-btn {phase === 'done' ? '' : 'y-btn-primary'}" disabled={installed === null} onclick={run}>
							{#if phase === 'done'}<RotateCcw class="h-4 w-4" />Run again{:else}<Sparkles class="h-4 w-4" />Re-run {runnable === 1 ? 'script' : `${runnable} scripts`}{/if}
						</button>
					{/if}
					<label class="flex items-center gap-2 text-xs text-muted">
						<input type="checkbox" class="accent-[rgb(var(--c-brand))]" checked={auto} onchange={(e) => toggleAuto(e.currentTarget.checked)} />
						Run Script Lab automatically
					</label>
					<a class="flex items-center gap-1.5 text-xs font-medium text-brand hover:underline" href="/transactions/{page.hash}/fork"><GitFork class="h-3.5 w-3.5" />Fork this transaction</a>
				</div>
			{/if}
		</div>
		{#if phase === 'done' && done.length}
			<div class="mt-3 flex flex-wrap gap-2 border-t border-brand/15 pt-3 text-xs">
				{#if summary.exact}<span class="y-pill border-mint/30 bg-mint/10 text-mint"><BadgeCheck class="h-3.5 w-3.5" />{summary.exact} reproduced exactly</span>{/if}
				{#if summary.margin}<span class="y-pill border-mint/30 bg-mint/10 text-mint"><BadgeCheck class="h-3.5 w-3.5" />{summary.margin} reproduced with the builder’s margin</span>{/if}
				{#if summary.passed - summary.exact - summary.margin}<span class="y-pill border-mint/30 text-mint"><CircleCheck class="h-3.5 w-3.5" />{summary.passed - summary.exact - summary.margin} passed</span>{/if}
				{#if summary.failed}<span class="y-pill border-danger/30 bg-danger/5 text-danger"><CircleX class="h-3.5 w-3.5" />{summary.failed} failed</span>{/if}
				{#if summary.other}<span class="y-pill border-line text-muted"><CircleSlash class="h-3.5 w-3.5" />{summary.other} couldn’t run</span>{/if}
			</div>
		{/if}
		{#if failure}<p class="mt-3 border-t border-brand/15 pt-3 text-sm text-danger">{failure}</p>{/if}
	</div>
{/if}

<div class="grid gap-4">
	{#each page.scripts as s, i (s.script_hash + i)}
		{@const key = keyOf(s)}
		{@const r = key ? results[key] : undefined}
		<ScriptCard script={s} {maxMem} {maxSteps}>
			{#if r}
				<div class="mt-4 rounded-xl border p-3.5 {r.state === 'running' ? 'border-line' : passed(r) ? 'border-mint/30 bg-mint/[0.04]' : r.state === 'done' ? 'border-danger/30 bg-danger/[0.04]' : 'border-line bg-surface-2/50'}">
					<div class="flex flex-wrap items-center gap-2 text-sm">
						<span class="y-label mr-1">julc re-run</span>
						{#if r.state === 'running'}
							<LoaderCircle class="h-4 w-4 animate-spin text-brand" /><span class="text-muted">Waiting…</span>
						{:else if r.state === 'skipped' || r.state === 'error'}
							<CircleSlash class="h-4 w-4 text-faint" /><span class="font-medium">Couldn’t run</span>
						{:else if passed(r)}
							<CircleCheck class="h-4 w-4 text-mint" /><span class="font-medium text-mint">Passed</span>
							{@const m = margin(r)}
							{#if exact(r)}
								<span class="y-pill border-mint/30 bg-mint/10 text-mint"><BadgeCheck class="h-3.5 w-3.5" />Reproduced exactly</span>
							{:else if m != null}
								<span class="y-pill border-mint/30 bg-mint/10 text-mint" title="The declared budget is this run’s budget plus {m}%, for both memory and CPU: the transaction builder added a safety margin."
									><BadgeCheck class="h-3.5 w-3.5" />Reproduced · declared with a {m}% margin</span
								>
							{/if}
						{:else}
							<CircleX class="h-4 w-4 text-danger" /><span class="font-medium text-danger">{r.evaluation?.status === 'budgetExhausted' ? 'Out of budget' : 'Failed'}</span>
						{/if}
						<span class="ml-auto flex items-center gap-3">
							{#if r.ms != null}<span class="text-xs tabular-nums text-faint">{r.ms} ms</span>{/if}
							{#if r.args && r.state === 'done'}
								<a
									class="y-btn h-7 gap-1.5 px-2.5 text-xs"
									href="/transactions/{page.hash}/debug/{r.plan.purpose}/{r.plan.index}{passed(r) ? '' : '?at=failure'}"
									title="Step through this run in the debugger"><Bug class="h-3.5 w-3.5" />{passed(r) ? 'Debug' : 'Debug the failure'}</a
								>
							{/if}
						</span>
					</div>

					{#if r.error}<p class="mt-2 break-words text-sm text-muted">{r.error}</p>{/if}

					{#if r.evaluation}
						{@const ev = r.evaluation}
						{#if r.plan.declared}
							<div class="mt-3 grid gap-2 text-xs sm:grid-cols-2">
								{#each [{ label: 'Memory', got: ev.mem, want: r.plan.declared.mem }, { label: 'CPU steps', got: ev.cpu, want: r.plan.declared.steps }] as b (b.label)}
									<div class="flex items-center gap-2 rounded-lg border border-line bg-surface px-2.5 py-1.5">
										<Gauge class="h-3.5 w-3.5 text-faint" />
										<span class="text-muted">{b.label}</span>
										<span class="ml-auto tabular-nums"><b>{num(b.got)}</b> <span class="text-faint">/ {num(b.want)} declared</span></span>
										{#if b.got !== b.want}
											<span class="tabular-nums {b.got > b.want ? 'text-amber' : 'text-faint'}">{pct(b.got, b.want)}%</span>
										{/if}
									</div>
								{/each}
							</div>
						{/if}
						{#if !passed(r) && ev.message}<p class="mt-2 break-words text-sm">{ev.message}</p>{/if}
						{#if invalid}
							<p class="mt-2 text-xs text-muted">
								{passed(r) ? 'Passed here, though the transaction failed on chain: another script in it may have failed, or the rebuilt context differs.' : 'Failed on chain too.'}
							</p>
						{/if}
						{#if ev.traces?.length}
							<div class="mt-3">
								<div class="y-label mb-1">Traces</div>
								<ol class="mono max-h-40 overflow-auto rounded-lg border border-line bg-bg-2 p-2 text-[12px] leading-5">
									{#each ev.traces as t, n}<li><span class="mr-2 select-none text-faint">{n + 1}</span>{t}</li>{/each}
								</ol>
							</div>
						{/if}
						{#if !passed(r) && ev.lastBuiltins?.length}
							<div class="mt-3 flex flex-wrap items-center gap-1.5 text-xs">
								<span class="y-label mr-1">Last builtins</span>
								{#each ev.lastBuiltins as b}<span class="mono rounded-md border border-line bg-surface-2 px-1.5 py-0.5 text-cyan">{b}</span>{/each}
							</div>
						{/if}
						{#if ev.scriptContextCbor}
							<details class="group mt-3">
								<summary class="cursor-pointer text-xs font-medium text-brand hover:underline">What the validator saw (ScriptContext)</summary>
								<div class="mt-2"><PlutusView cbor={ev.scriptContextCbor} label="ScriptContext · {ev.language ?? ''}" /></div>
							</details>
						{/if}
					{/if}

					{#if r.plan.notes.length && r.state !== 'skipped'}
						<ul class="mt-3 space-y-1 border-t border-line pt-2 text-xs text-muted">
							{#each r.plan.notes as note}<li class="flex gap-1.5"><Info class="mt-0.5 h-3.5 w-3.5 shrink-0 text-faint" />{note}</li>{/each}
						</ul>
					{/if}
				</div>
			{/if}
		</ScriptCard>
	{/each}
</div>
