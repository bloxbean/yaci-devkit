<script lang="ts">
	import { onMount, tick } from 'svelte';
	import { replaceState } from '$app/navigation';
	import { page as appPage } from '$app/state';
	import { tryApi } from '$lib/api/client';
	import { ada, num, shorten } from '$lib/format';
	import { copyText } from '$lib/ui/toast.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import Debugger from '../debug/Debugger.svelte';
	import { julc, wasmSupported } from '../engine.svelte';
	import { outcome, passed } from '../replay/verdict';
	import type { PageTx } from '../replay/load';
	import type { RunResult } from '../replay/run';
	import { PARAMS, paramByName, toInt, toRational, type ParamDef } from './action';
	import { checkEachParam, checkGuardrail, type EpochParams, type GuardrailScript } from './guardrail';
	import { actionOf, isEmpty, queryOf, type Draft } from './draft';
	import ShieldCheck from '@lucide/svelte/icons/shield-check';
	import ShieldX from '@lucide/svelte/icons/shield-x';
	import LoaderCircle from '@lucide/svelte/icons/loader-circle';
	import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
	import Plus from '@lucide/svelte/icons/plus';
	import X from '@lucide/svelte/icons/x';
	import Bug from '@lucide/svelte/icons/bug';
	import Link from '@lucide/svelte/icons/link';
	import FileCode from '@lucide/svelte/icons/file-code';
	import RotateCcw from '@lucide/svelte/icons/rotate-ccw';

	/*
	 * Draft a parameter change or treasury withdrawal and see, as you type, whether the guardrail script accepts it —
	 * before anyone pays a 100,000 ada deposit to find out. Each changed parameter is also checked on its own, so a
	 * rejection points at the value the guardrail doesn't allow (guardrail scripts usually emit no traces).
	 */
	type Recent = { id: string; type: string; epoch: number | null; policyHash: string | null; summary: string; draft: Draft };
	let {
		initial,
		params,
		recent,
		defaultScript,
		constitutionScript
	}: {
		initial: Draft;
		params: EpochParams | null;
		recent: Recent[];
		/** The script the page opened with (from ?script=, the proposal or the constitution). */
		defaultScript: { hash: string; type: string | null; cbor: string | null; constitutional: boolean } | null;
		constitutionScript: string | null;
	} = $props();

	// svelte-ignore state_referenced_locally
	let draft = $state<Draft>(structuredClone(initial));
	// svelte-ignore state_referenced_locally
	let script = $state.raw<GuardrailScript | null>(defaultScript?.cbor ? { hash: defaultScript.hash, cbor: defaultScript.cbor } : null);
	// svelte-ignore state_referenced_locally
	let scriptNote = $state<string | null>(defaultScript && !defaultScript.cbor ? `The store has no code for script ${shorten(defaultScript.hash, 8, 6)}.` : null);
	let customHash = $state('');
	let customCbor = $state('');
	let customError = $state<string | null>(null);

	let phase = $state<'starting' | 'ready' | 'error'>('starting');
	let failure = $state<string | null>(null);
	let result = $state.raw<RunResult | null>(null);
	let each = $state.raw<Record<string, 'ok' | 'rejected' | 'invalid'>>({});
	let running = $state(false);
	let debugging = $state(false);
	let generation = $state(0);

	onMount(async () => {
		if (!wasmSupported()) return fail('This browser can’t run the julc engine (it needs WebAssembly GC and exception handling). Use a current Chrome, Edge, Firefox or Safari.');
		if (!(await julc.available())) return fail('Script Lab is not installed with this viewer, so the guardrail can’t be run here.');
		phase = 'ready';
	});

	function fail(message: string) {
		failure = message;
		phase = 'error';
	}

	// Check shortly after every change; a newer change supersedes a check in progress.
	let timer: ReturnType<typeof setTimeout> | undefined;
	let latest = 0;
	$effect(() => {
		const d = $state.snapshot(draft) as Draft;
		const s = script;
		if (phase !== 'ready') return;
		clearTimeout(timer);
		timer = setTimeout(() => check(d, s), 300);
	});

	// Keep the draft in the URL, so the check can be shared.
	$effect(() => {
		const d = $state.snapshot(draft) as Draft;
		const custom = script && script.hash !== constitutionScript ? script.hash : null;
		const q = queryOf(d, custom);
		if (q === appPage.url.search.slice(1)) return;
		try {
			replaceState(`${appPage.url.pathname}${q ? `?${q}` : ''}`, {});
		} catch {
			/* not ready yet */
		}
	});

	async function check(d: Draft, s: GuardrailScript | null) {
		const mine = ++latest;
		if (!s || isEmpty(d)) {
			result = null;
			each = {};
			return;
		}
		running = true;
		const action = actionOf(d);
		const r = await checkGuardrail(action, s, params);
		// Which change does the guardrail object to? Only worth asking when there is more than one.
		const groups = new Set(Object.keys(d.values).map((n) => paramByName[n]?.key)).size + (d.costModels ? 1 : 0);
		const verdicts = action.type === 'PARAMETER_CHANGE_ACTION' && groups > 1 && r.state === 'done' ? await checkEachParam(action.change, s, params) : {};
		if (mine !== latest) return;
		result = r;
		each = verdicts;
		running = false;
		generation++;
	}

	// --- editing ---
	const current = (def: ParamDef) => params?.[def.current];
	const changed = $derived(PARAMS.filter((p) => draft.values[p.name] != null));
	const groupsShown = ['Network', 'Economic', 'Technical', 'Governance'] as const;
	let adding = $state('');

	function addParam(name: string) {
		const def = paramByName[name];
		if (!def) return;
		const c = current(def);
		const r = def.kind === 'rational' ? toRational(c) : toInt(c);
		draft.values[name] = r == null ? '' : typeof r === 'bigint' ? String(r) : r.d === 1n ? String(r.n) : `${r.n}/${r.d}`;
		adding = '';
	}

	function startFrom(id: string) {
		const p = recent.find((r) => r.id === id);
		if (p) draft = structuredClone($state.snapshot(p.draft) as Draft);
	}

	function reset(kind: Draft['kind']) {
		draft = { kind, values: {}, withdrawals: kind === 'treasury' ? [{ address: '', amount: '' }] : [], from: null };
	}

	/** Value hint under an input: ada for lovelace, the decimal of a fraction, an error for anything else. */
	function hint(def: ParamDef, value: string): { text: string; bad?: boolean } | null {
		if (!value.trim()) return { text: 'Enter a value', bad: true };
		if (def.kind === 'rational') {
			const r = toRational(value);
			if (!r) return { text: 'Not a number or fraction', bad: true };
			if (r.d === 1n) return null;
			// Show the other form: the fraction the ledger stores for a decimal, the decimal of a fraction.
			return value.includes('/') ? { text: `= ${Number((Number(r.n) / Number(r.d)).toPrecision(6))}` } : { text: `= ${r.n}/${r.d}` };
		}
		const n = toInt(value);
		if (n == null) return { text: 'Not a whole number', bad: true };
		return def.unit === 'lovelace' ? { text: `= ${ada(n, 'auto')} ada` } : null;
	}

	function showCurrent(def: ParamDef) {
		const c = current(def);
		if (c == null) return '—';
		if (def.unit === 'lovelace') return `${ada(String(c), 'auto')} ada`;
		return typeof c === 'number' && !Number.isInteger(c) ? String(c) : num(String(c));
	}

	async function useCustom() {
		customError = null;
		const hex = customCbor.trim().toLowerCase();
		if (hex) {
			if (!/^([0-9a-f]{2})+$/.test(hex)) return (customError = 'The script must be CBOR hex.');
			try {
				const h = await julc.run((c) => c.vm.hash({ script: { script: hex, language: 'V3' } }));
				script = { hash: h.scriptHash, cbor: hex };
				scriptNote = null;
			} catch (e) {
				customError = (e as Error)?.message ?? 'julc could not read this script.';
			}
			return;
		}
		const h = customHash.trim().toLowerCase();
		if (!/^[0-9a-f]{56}$/.test(h)) return (customError = 'Enter a 56-character script hash or paste the script’s CBOR.');
		const [info, body] = await Promise.all([tryApi<{ type?: string }>(`/scripts/${h}`), tryApi<{ cbor?: string }>(`/scripts/${h}/cbor`)]);
		if (!body?.cbor) return (customError = 'This chain’s store has no code for that script.');
		if (info?.type && !/v3/i.test(info.type)) return (customError = `That is a ${info.type} script; guardrails are Plutus V3.`);
		script = { hash: h, cbor: body.cbor };
		scriptNote = null;
	}

	const verdict = $derived(result ? outcome(result) : null);
	const accepted = $derived(passed(result));
	const limits = $derived({ cpu: Number(params?.max_tx_ex_steps ?? 0), mem: Number(params?.max_tx_ex_mem ?? 0) });
	const share = (n: number, of: number) => (of ? `${((n / of) * 100).toFixed(2)}%` : '');

	const fakePage: PageTx = { hash: '', tx: { fees: 0, inputs: [], outputs: [] }, scripts: [], withdrawals: [], mints: [], epoch: null };

	async function openDebugger() {
		debugging = true;
		await tick();
		document.getElementById('guardrail-debugger')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
	}

	const field = 'y-input h-8 text-xs mono';
</script>

<div class="grid items-start gap-5 xl:grid-cols-[minmax(0,1.3fr)_minmax(0,1fr)]">
	<!-- The draft -->
	<div class="min-w-0 space-y-4">
		<Panel title="Proposal" sub="The action the guardrail checks">
			{#snippet actions()}
				<div class="flex items-center gap-0.5 rounded-lg bg-surface-2 p-0.5 text-xs" role="tablist">
					{#each [['param', 'Parameter change'], ['treasury', 'Treasury withdrawal']] as [id, name] (id)}
						<button role="tab" aria-selected={draft.kind === id} class="rounded-md px-2.5 py-1 transition {draft.kind === id ? 'bg-surface text-ink shadow-sm' : 'text-muted hover:text-ink'}" onclick={() => draft.kind !== id && reset(id as Draft['kind'])}>{name}</button>
					{/each}
				</div>
			{/snippet}

			{#if recent.length}
				<label class="mb-4 flex flex-wrap items-center gap-2 text-sm">
					<span class="text-muted">Start from a proposal</span>
					<select class="y-input h-8 min-w-0 flex-1 text-xs" value={draft.from ?? ''} onchange={(e) => startFrom(e.currentTarget.value)} aria-label="Start from a proposal">
						<option value="" disabled>Choose a recent proposal…</option>
						{#each recent as r (r.id)}<option value={r.id}>{r.type === 'TREASURY_WITHDRAWALS_ACTION' ? 'Treasury' : 'Parameters'} · {r.summary}{r.epoch != null ? ` · epoch ${r.epoch}` : ''}</option>{/each}
					</select>
				</label>
			{/if}

			{#if draft.kind === 'param'}
				{#if changed.length || draft.costModels}
					<div class="overflow-x-auto">
						<table class="w-full text-sm">
							<thead><tr class="border-b border-line text-left"><th class="y-label py-2 pr-3">Parameter</th><th class="y-label py-2 pr-3">Now</th><th class="y-label py-2 pr-3">Proposed</th><th class="py-2"></th></tr></thead>
							<tbody>
								{#each changed as def (def.name)}
									{@const h = hint(def, draft.values[def.name] ?? '')}
									{@const v = each[def.name]}
									<tr class="border-b border-line/60 align-top last:border-0">
										<td class="py-2 pr-3">
											<div class="font-medium">{def.label}</div>
											<div class="mono text-[11px] text-faint">{def.name.replace(/^.*\./, '')} · key {def.key}</div>
										</td>
										<td class="mono py-2 pr-3 text-xs text-muted tabular-nums">{showCurrent(def)}</td>
										<td class="py-2 pr-3">
											<input class="{field} w-40 {h?.bad ? 'border-danger/60' : ''}" value={draft.values[def.name]} oninput={(e) => (draft.values[def.name] = e.currentTarget.value)} aria-label="New value for {def.label}" spellcheck="false" />
											{#if h}<div class="mt-0.5 text-[11px] {h.bad ? 'text-danger' : 'text-faint'}">{h.text}</div>{/if}
										</td>
										<td class="py-2 text-right whitespace-nowrap">
											{#if v === 'ok'}<Badge tone="ok">allowed</Badge>{:else if v === 'rejected'}<Badge tone="bad">rejected</Badge>{:else if v === 'invalid'}<Badge tone="warn">invalid</Badge>{/if}
											<button class="y-icon-btn ml-1 h-7 w-7" title="Remove" aria-label="Remove {def.label}" onclick={() => delete draft.values[def.name]}><X class="h-3.5 w-3.5" /></button>
										</td>
									</tr>
								{/each}
								{#if draft.costModels}
									<tr class="border-b border-line/60 align-top last:border-0">
										<td class="py-2 pr-3"><div class="font-medium">Cost models</div><div class="mono text-[11px] text-faint">key 18</div></td>
										<td class="py-2 pr-3 text-xs text-muted">—</td>
										<td class="py-2 pr-3 text-xs">Plutus {Object.keys(draft.costModels).map((l) => `V${Number(l) + 1}`).join(', ')} <span class="text-faint">from the proposal</span></td>
										<td class="py-2 text-right whitespace-nowrap">
											{#if each.costModels === 'ok'}<Badge tone="ok">allowed</Badge>{:else if each.costModels === 'rejected'}<Badge tone="bad">rejected</Badge>{/if}
											<button class="y-icon-btn ml-1 h-7 w-7" title="Remove" aria-label="Remove cost models" onclick={() => (draft.costModels = undefined)}><X class="h-3.5 w-3.5" /></button>
										</td>
									</tr>
								{/if}
							</tbody>
						</table>
					</div>
				{:else}
					<p class="text-sm text-muted">Add the parameters the proposal changes. Each starts at its current value.</p>
				{/if}
				<div class="mt-3 flex items-center gap-2">
					<select class="y-input h-8 min-w-0 flex-1 text-xs" bind:value={adding} onchange={() => addParam(adding)} aria-label="Add a parameter">
						<option value="" disabled>Add a parameter…</option>
						{#each groupsShown as g (g)}
							<optgroup label={g}>
								{#each PARAMS.filter((p) => p.group === g && draft.values[p.name] == null) as p (p.name)}<option value={p.name}>{p.label}</option>{/each}
							</optgroup>
						{/each}
					</select>
				</div>
				<p class="mt-3 text-xs text-faint">Fractions like <span class="mono">3/10</span> or decimals for ratios; lovelace for amounts. Prices, execution limits and voting thresholds are set as a whole by the ledger, so values you don't change keep their current value.</p>
			{:else}
				<div class="space-y-2">
					{#each draft.withdrawals as w, i (i)}
						{@const amount = toInt(w.amount)}
						<div class="flex flex-wrap items-start gap-2">
							<input class="{field} min-w-0 flex-[2]" placeholder="stake1… or reward account hex" value={w.address} oninput={(e) => (draft.withdrawals[i].address = e.currentTarget.value)} aria-label="Withdrawal {i + 1} stake address" spellcheck="false" />
							<div class="min-w-36 flex-1">
								<input class="{field} w-full" placeholder="lovelace" value={w.amount} oninput={(e) => (draft.withdrawals[i].amount = e.currentTarget.value)} aria-label="Withdrawal {i + 1} amount in lovelace" />
								{#if amount != null}<div class="mt-0.5 text-[11px] text-faint">= {ada(amount, 'auto')} ada</div>{/if}
							</div>
							<button class="y-icon-btn h-8 w-8" title="Remove" aria-label="Remove withdrawal {i + 1}" onclick={() => draft.withdrawals.splice(i, 1)}><X class="h-3.5 w-3.5" /></button>
						</div>
					{/each}
					<button class="y-btn h-8 text-xs" onclick={() => draft.withdrawals.push({ address: '', amount: '' })}><Plus class="h-3.5 w-3.5" />Add a withdrawal</button>
				</div>
			{/if}
		</Panel>

		<Panel title="Guardrail script">
			{#snippet icon()}<FileCode class="h-4 w-4" />{/snippet}
			{#if script}
				<div class="flex flex-wrap items-center gap-2 text-sm">
					<a class="mono y-link" href="/scripts/{script.hash}">{shorten(script.hash, 12, 8)}</a>
					{#if script.hash === constitutionScript}<Badge tone="info">the constitution’s guardrail</Badge>{:else}<Badge tone="violet">custom script</Badge>{/if}
					{#if script.hash !== defaultScript?.hash && defaultScript?.cbor}
						<button class="y-btn h-7 px-2 text-xs" onclick={() => (script = { hash: defaultScript!.hash, cbor: defaultScript!.cbor! })}><RotateCcw class="h-3 w-3" />Back to {defaultScript.constitutional ? 'the constitution’s' : 'the first'}</button>
					{/if}
				</div>
			{:else}
				<p class="text-sm text-muted">{scriptNote ?? 'This chain’s constitution has no guardrail script, so proposals aren’t checked on chain.'} Use a script on this chain or paste one to try it.</p>
			{/if}
			<details class="mt-3 text-sm" open={!script}>
				<summary class="cursor-pointer text-xs font-medium text-muted hover:text-ink">Check against another script</summary>
				<div class="mt-2 space-y-2">
					<input class="{field} w-full" placeholder="Script hash on this chain" bind:value={customHash} aria-label="Guardrail script hash" spellcheck="false" />
					<textarea class="y-input mono min-h-16 w-full p-2 text-[11px]" placeholder="…or paste a compiled Plutus V3 script (CBOR hex)" bind:value={customCbor} aria-label="Guardrail script CBOR" spellcheck="false"></textarea>
					<button class="y-btn h-8 text-xs" onclick={useCustom} disabled={phase !== 'ready'}>Use this script</button>
					{#if customError}<p class="text-xs text-danger">{customError}</p>{/if}
				</div>
			</details>
		</Panel>
	</div>

	<!-- The verdict -->
	<div class="min-w-0 space-y-4 xl:sticky xl:top-20">
		{#if phase === 'error'}
			<div class="flex items-start gap-3 rounded-2xl border border-danger/30 bg-danger/5 p-5 text-sm">
				<TriangleAlert class="mt-0.5 h-5 w-5 shrink-0 text-danger" />
				<div><p class="font-medium">Can’t run the guardrail</p><p class="mt-1 text-muted">{failure}</p></div>
			</div>
		{:else}
			<Panel title="Verdict">
				{#snippet actions()}{#if running || phase === 'starting'}<LoaderCircle class="h-4 w-4 animate-spin text-brand" aria-label="Checking" />{/if}{/snippet}
				{#if !script}
					<p class="text-sm text-muted">Choose a guardrail script to check against.</p>
				{:else if isEmpty(draft)}
					<p class="text-sm text-muted">{draft.kind === 'param' ? 'Add a parameter to check.' : 'Add a withdrawal to check.'}</p>
				{:else if !result}
					<p class="flex items-center gap-2 text-sm text-muted" role="status">{julc.status === 'loading' ? 'Starting the julc engine…' : 'Checking…'}</p>
				{:else if result.state !== 'done'}
					<div class="flex items-start gap-3 text-sm" data-verdict="invalid">
						<TriangleAlert class="mt-0.5 h-5 w-5 shrink-0 text-warn" />
						<div><p class="font-medium">Can’t check this proposal yet</p><p class="mt-1 text-muted">{result.error}</p></div>
					</div>
				{:else}
					{#key generation}
						<div class="flex items-start gap-3" data-verdict={accepted ? 'accepted' : 'rejected'}>
							{#if accepted}<ShieldCheck class="h-9 w-9 shrink-0 text-ok" />{:else}<ShieldX class="h-9 w-9 shrink-0 text-danger" />{/if}
							<div>
								<p class="font-display text-lg font-semibold">{accepted ? 'The guardrail accepts this proposal' : verdict === 'out of budget' ? 'The guardrail ran out of budget' : 'The guardrail rejects this proposal'}</p>
								<p class="mt-0.5 text-sm text-muted">
									{#if accepted}It could be submitted with this guardrail as its policy.{:else if Object.values(each).includes('rejected')}Not allowed: {changed.filter((d) => each[d.name] === 'rejected').map((d) => d.label).concat(each.costModels === 'rejected' ? ['Cost models'] : []).join(', ')}.{:else if Object.keys(each).length}Each change passes on its own, but not together.{:else}The ledger would refuse the transaction that proposes it.{/if}
								</p>
							</div>
						</div>
					{/key}
					<dl class="mt-4 grid grid-cols-2 gap-2.5">
						<div class="rounded-xl border border-line bg-surface-2/60 px-3 py-2"><dt class="y-label">CPU</dt><dd class="font-medium tabular-nums">{num(result.evaluation!.cpu)}</dd><dd class="text-[11px] text-faint">{share(Number(result.evaluation!.cpu), limits.cpu)} of a tx</dd></div>
						<div class="rounded-xl border border-line bg-surface-2/60 px-3 py-2"><dt class="y-label">Memory</dt><dd class="font-medium tabular-nums">{num(result.evaluation!.mem)}</dd><dd class="text-[11px] text-faint">{share(Number(result.evaluation!.mem), limits.mem)} of a tx</dd></div>
					</dl>
					{#if result.evaluation?.traces?.length}
						<div class="y-label mt-4 mb-1">Traces</div>
						<ul class="mono space-y-0.5 text-xs">{#each result.evaluation.traces as t, i (i)}<li class="truncate">{t}</li>{/each}</ul>
					{/if}
					<div class="mt-4 flex flex-wrap gap-2">
						{#if result.args}<button class="y-btn h-8 text-xs" onclick={openDebugger}><Bug class="h-3.5 w-3.5" />{accepted ? 'Step through it' : 'Debug the rejection'}</button>{/if}
						<button class="y-btn h-8 text-xs" onclick={() => copyText(appPage.url.href, 'Link to this check copied')}><Link class="h-3.5 w-3.5" />Copy link</button>
					</div>
				{/if}
			</Panel>
			<p class="px-1 text-xs text-faint">
				The guardrail sees the proposal as the ledger would show it to the script; the deposit is the current {params?.gov_action_deposit ? `${ada(String(params.gov_action_deposit), 0)} ada` : 'one'}, and the rest of the
				transaction is a placeholder. Run with this epoch’s cost model{params?.protocol_major_ver ? ` and protocol ${Math.min(11, Math.max(10, Number(params.protocol_major_ver)))} rules` : ''}.
			</p>
		{/if}
	</div>
</div>

{#if debugging && result?.args}
	<section id="guardrail-debugger" class="mt-6 scroll-mt-20">
		<div class="mb-3 flex items-center justify-between">
			<h2 class="font-display text-[15px] font-semibold">Guardrail run</h2>
			<button class="y-btn h-8 text-xs" onclick={() => (debugging = false)}><X class="h-3.5 w-3.5" />Close</button>
		</div>
		{#key generation}
			<Debugger page={fakePage} purpose="propose" index={0} prepared={{ plan: result.plan, result }} syncUrl={false} />
		{/key}
	</section>
{/if}
