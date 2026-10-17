<script lang="ts">
	import { onDestroy, onMount, tick } from 'svelte';
	import { num } from '$lib/format';
	import Panel from '$lib/ui/Panel.svelte';
	import { julc } from '../engine.svelte';
	import type { RunPlan } from '../replay/build';
	import type { Snapshot, Timeline } from '../types';
	import SkipBack from '@lucide/svelte/icons/skip-back';
	import ArrowDownToLine from '@lucide/svelte/icons/arrow-down-to-line';
	import FastForward from '@lucide/svelte/icons/fast-forward';
	import Flag from '@lucide/svelte/icons/flag';
	import LoaderCircle from '@lucide/svelte/icons/loader-circle';
	import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
	import FlaskConical from '@lucide/svelte/icons/flask-conical';

	/*
	 * Step through a julc Java validator at the source level: julc compiles a debug build (optimizer off) that maps
	 * machine steps back to Java lines, and reports locals where it can. Experimental in julc 0.1.0-pre18: only some
	 * lines can be stopped on and many locals aren't available yet, which is shown rather than hidden.
	 */
	let { source, params, plan }: { source: string; params: string[]; plan: RunPlan } = $props();

	interface LocalVar {
		name: string | null;
		declaredType: string | null;
		availability: string | null;
		reason: string | null;
		value: { summary: string | null; childrenHandle: string | null; childCount: number | null } | null;
	}
	interface SourceSession {
		sessionId: string;
		ok: boolean;
		error: string | null;
		warning: string | null;
		executableJavaLines: number[] | null;
		timeline: Timeline | null;
		snapshot: Snapshot | null;
		continue(b?: unknown): Promise<{ ok: boolean; error: string | null; snapshot: Snapshot | null }>;
		goto(step: bigint): Promise<{ ok: boolean; error: string | null; snapshot: Snapshot | null }>;
		locals(): Promise<{ availability: string | null; reason: string | null; scopes: { kind: string | null; variables: LocalVar[] | null }[] | null }>;
		children(handle: string, start?: number, count?: number): Promise<{ children: { name: string | null; value: LocalVar['value'] }[] | null }>;
		close(): Promise<unknown>;
	}

	let session: SourceSession | null = null;
	let phase = $state<'opening' | 'ready' | 'error'>('opening');
	let failure = $state<string | null>(null);
	let warning = $state<string | null>(null);
	let timeline = $state.raw<Timeline | null>(null);
	let snap = $state.raw<Snapshot | null>(null);
	let executable = $state<number[]>([]);
	let breaks = $state(new Set<number>());
	let locals = $state.raw<{ kind: string; variables: LocalVar[] }[]>([]);
	let localsNote = $state<string | null>(null);
	let busy = $state(false);
	/** julc mapped none of this run's steps to a Java line (common for passing runs in 0.1.0-pre18). */
	let unmapped = $state(false);
	let lineEls: HTMLElement[] = $state([]);

	const javaLine = $derived(snap?.javaLocation?.line ?? null);
	const lines = $derived(source.split('\n'));

	onMount(async () => {
		try {
			const req = plan.request!;
			const opened = (await julc.run((c) => {
				const sd = (c as unknown as { sourceDebug?: { open(r: unknown): Promise<SourceSession> } }).sourceDebug;
				if (!sd) throw new Error('This julc engine has no source debugger.');
				return sd.open({ source, params: params.map((value) => ({ format: 'cbor', value })), transaction: req.transaction, target: req.target, costModel: req.costModel, maxCpu: req.maxCpu, maxMem: req.maxMem, locals: true });
			})) as SourceSession;
			if (!opened.ok || !opened.sessionId) throw new Error(opened.error ?? 'julc could not open a source debug session.');
			session = opened;
			warning = opened.warning;
			timeline = opened.timeline;
			snap = opened.snapshot;
			executable = opened.executableJavaLines ?? [];
			phase = 'ready';
			// Start where it failed, else on the first Java line julc maps.
			// One step before the error term, where the failing line's locals are still in scope.
			if (opened.timeline?.errorStep != null) await act((s) => s.goto(BigInt(Math.max(0, Number(opened.timeline!.errorStep!) - 1))));
			else await act((s) => s.continue({ javaLines: [...executable], onError: true }));
			if (snap?.finished && !javaLine) unmapped = true;
		} catch (e) {
			failure = (e as Error)?.message ?? String(e);
			phase = 'error';
		}
	});

	onDestroy(() => {
		session?.close().catch(() => {});
	});

	async function act(fn: (s: SourceSession) => Promise<{ ok: boolean; error: string | null; snapshot: Snapshot | null }>) {
		if (!session || busy) return;
		busy = true;
		try {
			const r = await fn(session);
			if (!r.ok && r.error) throw new Error(r.error);
			if (r.snapshot) snap = r.snapshot;
			await loadLocals();
			await tick();
			if (javaLine) lineEls[javaLine - 1]?.scrollIntoView({ block: 'nearest' });
		} catch (e) {
			failure = (e as Error)?.message ?? String(e);
		} finally {
			busy = false;
		}
	}

	async function loadLocals() {
		locals = [];
		localsNote = null;
		if (!session || snap?.localsAvailability !== 'available') {
			localsNote = snap?.finished ? 'The run has ended.' : 'No Java locals at this step.';
			return;
		}
		const l = await session.locals();
		if (l.availability !== 'available') localsNote = l.reason ?? 'Locals aren’t available here.';
		locals = (l.scopes ?? []).map((s) => ({ kind: s.kind ?? 'scope', variables: s.variables ?? [] }));
	}

	let expanded = $state.raw<Record<string, { name: string | null; value: LocalVar['value'] }[]>>({});
	async function expand(handle: string) {
		if (!session) return;
		if (expanded[handle]) {
			const { [handle]: _, ...rest } = expanded;
			expanded = rest;
			return;
		}
		const r = await session.children(handle, 0, 50);
		expanded = { ...expanded, [handle]: r.children ?? [] };
	}

	function toggle(line: number) {
		if (!executable.includes(line)) return;
		const next = new Set(breaks);
		if (next.has(line)) next.delete(line);
		else next.add(line);
		breaks = next;
	}

	const nextLine = () => act((s) => s.continue({ javaLines: [...executable], onError: true }));
	const cont = () => act((s) => s.continue({ javaLines: [...breaks], onError: true }));
	const restart = () => act(async (s) => (await s.goto(0n), s.continue({ javaLines: [...executable], onError: true })));
	const toEnd = () => act((s) => s.goto(BigInt(timeline?.errorStep ?? timeline?.totalSteps ?? 0)));
	const AVAIL: Record<string, string> = { unsupportedLowering: 'not tracked by julc yet', optimizedOut: 'optimized out', notInScope: 'not in scope yet' };
</script>

{#if phase === 'opening'}
	<Panel><p class="flex items-center gap-2 text-sm text-muted" role="status"><LoaderCircle class="h-4 w-4 animate-spin text-brand" />Compiling a debug build and running it…</p></Panel>
{:else if phase === 'error'}
	<div class="flex items-start gap-3 rounded-2xl border border-danger/30 bg-danger/5 p-5 text-sm">
		<TriangleAlert class="mt-0.5 h-5 w-5 shrink-0 text-danger" />
		<div><p class="font-medium">Can’t debug the Java source</p><p class="mt-1 text-muted">{failure}</p></div>
	</div>
{:else}
	<div class="mb-3 flex items-start gap-2 rounded-xl border border-warn/30 bg-warn/5 px-3 py-2 text-xs text-muted">
		<FlaskConical class="mt-0.5 h-3.5 w-3.5 shrink-0 text-warn" />
		<span><b class="font-medium text-ink">Experimental (julc 0.1.0-pre18).</b> {warning ?? 'This debug build is compiled without the optimizer, so its budget differs from your script’s.'} Only some steps map to Java lines (reliably the one a validator fails on), and many locals aren’t tracked yet.</span>
	</div>
	{#if unmapped}<p class="mb-3 text-sm text-muted">julc mapped none of this run’s steps to a Java line, so there is nothing to stop on. The UPLC debugger steps through every term.</p>{/if}
	<div class="grid items-start gap-4 lg:grid-cols-[minmax(0,1.5fr)_minmax(0,1fr)]">
		<section class="y-panel min-w-0 overflow-hidden">
			<div class="flex flex-wrap items-center gap-1.5 border-b border-line px-3 py-2">
				<button class="y-icon-btn h-8 w-8" title="Restart" aria-label="Restart" onclick={restart} disabled={busy}><SkipBack class="h-4 w-4" /></button>
				<button class="y-btn y-btn-primary h-8 text-xs" onclick={nextLine} disabled={busy || snap?.finished}><ArrowDownToLine class="h-3.5 w-3.5" />Next line</button>
				<button class="y-btn h-8 text-xs" onclick={cont} disabled={busy || snap?.finished} title="Run to the next breakpoint or failure"><FastForward class="h-3.5 w-3.5" />Continue</button>
				<button class="y-btn h-8 text-xs" onclick={toEnd} disabled={busy}><Flag class="h-3.5 w-3.5" />{timeline?.errorStep != null ? 'To the failure' : 'To the end'}</button>
				<span class="ml-auto text-xs text-muted tabular-nums">step {num(String(snap?.step ?? 0))} / {num(String(timeline?.totalSteps ?? 0))}</span>
			</div>
			<ol class="mono max-h-[30rem] overflow-auto py-2 text-[12.5px] leading-5" aria-label="Java source">
				{#each lines as text, i (i)}
					{@const n = i + 1}
					{@const can = executable.includes(n)}
					<li bind:this={lineEls[i]} class="flex {javaLine === n ? 'bg-brand/15' : ''}" data-current={javaLine === n || undefined}>
						<button class="w-12 shrink-0 pr-2 text-right {can ? 'cursor-pointer text-muted hover:text-ink' : 'cursor-default text-faint/60'}" onclick={() => toggle(n)} disabled={!can} title={can ? 'Toggle a breakpoint' : 'julc can’t stop on this line'} aria-label="Line {n}{breaks.has(n) ? ', breakpoint' : ''}">
							{#if breaks.has(n)}<span class="mr-1 inline-block h-2 w-2 rounded-full bg-danger"></span>{:else if can}<span class="mr-1 inline-block h-1.5 w-1.5 rounded-full bg-line"></span>{/if}{n}
						</button>
						<span class="px-3 whitespace-pre text-ink">{text || ' '}</span>
					</li>
				{/each}
			</ol>
		</section>
		<div class="min-w-0 space-y-4">
			<Panel title="Now">
				<dl class="grid grid-cols-2 gap-3 text-sm">
					<div><dt class="y-label">Java line</dt><dd>{javaLine ?? '—'}</dd></div>
					<div><dt class="y-label">Stopped by</dt><dd>{snap?.stopReason ?? '—'}</dd></div>
					<div><dt class="y-label">CPU</dt><dd class="tabular-nums">{num(String(snap?.cpu ?? 0))}</dd></div>
					<div><dt class="y-label">Memory</dt><dd class="tabular-nums">{num(String(snap?.mem ?? 0))}</dd></div>
				</dl>
				{#if snap?.finished}<p class="mt-3 text-sm {snap.status === 'success' ? 'text-ok' : 'text-danger'}">{snap.status === 'success' ? 'The validator returned true.' : (snap.error ?? 'The validator failed.')}</p>{/if}
				{#if snap?.traces?.length}<div class="y-label mt-3 mb-1">Traces</div><ul class="mono space-y-0.5 text-xs">{#each snap.traces as t, i (i)}<li>{t}</li>{/each}</ul>{/if}
			</Panel>
			<Panel title="Locals">
				{#if localsNote && !locals.length}<p class="text-sm text-muted">{localsNote}</p>{/if}
				{#each locals as scope (scope.kind + scope.variables.length)}
					<div class="y-label mt-1 mb-1.5">{scope.kind}</div>
					<ul class="mb-3 space-y-1 text-[12.5px]">
						{#each scope.variables as v (v.name)}
							<li>
								<div class="flex flex-wrap items-baseline gap-x-2">
									<span class="mono font-medium">{v.name}</span><span class="text-[11px] text-faint">{v.declaredType}</span>
									{#if v.value}
										<span class="mono text-ink">{v.value.summary}</span>
										{#if v.value.childrenHandle}<button class="text-[11px] text-brand hover:underline" onclick={() => expand(v.value!.childrenHandle!)}>{expanded[v.value.childrenHandle] ? 'hide' : `${v.value.childCount ?? ''} fields`}</button>{/if}
									{:else}
										<span class="text-[11px] text-faint" title={v.reason ?? undefined}>{AVAIL[v.availability ?? ''] ?? v.availability}</span>
									{/if}
								</div>
								{#if v.value?.childrenHandle && expanded[v.value.childrenHandle]}
									<ul class="mt-1 ml-4 space-y-0.5 border-l border-line pl-2">
										{#each expanded[v.value.childrenHandle] as c, i (i)}<li><span class="mono">{c.name}</span> <span class="mono text-muted">{c.value?.summary}</span></li>{/each}
									</ul>
								{/if}
							</li>
						{/each}
					</ul>
				{/each}
			</Panel>
		</div>
	</div>
	{#if failure}<p class="mt-2 text-xs text-danger">{failure}</p>{/if}
{/if}
