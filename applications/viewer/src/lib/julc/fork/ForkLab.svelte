<script lang="ts">
	import { onMount, tick } from 'svelte';
	import { num, shorten } from '$lib/format';
	import Panel from '$lib/ui/Panel.svelte';
	import Skeleton from '$lib/ui/Skeleton.svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import DataEditor from './DataEditor.svelte';
	import Debugger from '../debug/Debugger.svelte';
	import { julc, wasmSupported } from '../engine.svelte';
	import { buildRuns, type ReplayInput, type RunPlan } from '../replay/build';
	import { loadParams, loadReplayInput, type PageTx } from '../replay/load';
	import { runAll, type RunResult } from '../replay/run';
	import { applyEdits, countEdits, refOf, type ForkEdits } from '../replay/fork';
	import { downloadJson, fixtureOf } from '../replay/fixture';
	import { slotToPosixMs } from '../replay/slots';
	import GitFork from '@lucide/svelte/icons/git-fork';
	import LoaderCircle from '@lucide/svelte/icons/loader-circle';
	import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
	import CircleCheck from '@lucide/svelte/icons/circle-check';
	import CircleX from '@lucide/svelte/icons/circle-x';
	import CircleSlash from '@lucide/svelte/icons/circle-slash';
	import RotateCcw from '@lucide/svelte/icons/rotate-ccw';
	import Bug from '@lucide/svelte/icons/bug';
	import Download from '@lucide/svelte/icons/download';
	import Plus from '@lucide/svelte/icons/plus';
	import X from '@lucide/svelte/icons/x';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';

	/*
	 * The what-if lab: edit a transaction (redeemers, datums, signers, validity, outputs, mint, withdrawals, fee, the
	 * rules it runs under) and every script runs again as you type, side by side with the on-chain original. Edits are
	 * applied to the store data before the transaction is rebuilt (replay/fork.ts), so ordering and the context fixes
	 * stay consistent for every script.
	 */
	let { page }: { page: PageTx } = $props();

	let phase = $state<'loading' | 'ready' | 'error'>('loading');
	let failure = $state<string | null>(null);
	// Plain data for engine requests (reactive state is a proxy and can't be posted to the worker).
	let base: ReplayInput | null = null;
	let plans = $state.raw<RunPlan[]>([]);
	let original = $state.raw<Record<string, RunResult>>({});
	let forked = $state.raw<Record<string, RunResult>>({});
	let running = $state(false);

	let edits = $state<ForkEdits>({});
	let epochText = $state('');
	let otherParams = $state.raw<{ epoch: number; params: ReplayInput['params'] } | null>(null);
	let epochError = $state<string | null>(null);
	let epochNote = $state<string | null>(null);
	let debugging = $state<string | null>(null);
	let generation = $state(0);

	const editCount = $derived(countEdits(edits) + (otherParams ? 1 : 0));

	onMount(async () => {
		if (!wasmSupported()) return fail('This browser can’t run the julc engine (it needs WebAssembly GC and exception handling). Use a current Chrome, Edge, Firefox or Safari.');
		if (!(await julc.available())) return fail('Script Lab is not installed with this viewer.');
		try {
			base = await loadReplayInput(page);
			plans = buildRuns(base);
			if (!plans.some((p) => p.request)) return fail(plans[0]?.skip ?? 'This transaction has no script runs that can be rebuilt.');
			const results: Record<string, RunResult> = {};
			const engineFailure = await runAll(plans, (k, r) => (results[k] = r));
			if (engineFailure) return fail(engineFailure);
			original = { ...results };
			forked = { ...results };
			epochText = String(page.epoch ?? '');
			phase = 'ready';
		} catch (e) {
			fail((e as Error)?.message ?? String(e));
		}
	});

	function fail(message: string) {
		failure = message;
		phase = 'error';
	}

	// Re-run the fork shortly after every edit; a newer edit supersedes a run in progress.
	let timer: ReturnType<typeof setTimeout> | undefined;
	let latest = 0;
	$effect(() => {
		const snapshot = $state.snapshot(edits);
		const params = otherParams?.params;
		if (phase !== 'ready') return;
		clearTimeout(timer);
		timer = setTimeout(() => rerun(snapshot, params), 250);
	});

	async function rerun(e: ForkEdits, params?: ReplayInput['params']) {
		if (!base) return;
		const mine = ++latest;
		running = true;
		const input = applyEdits(base, e, params);
		const results: Record<string, RunResult> = {};
		await runAll(buildRuns(input), (k, r) => (results[k] = r));
		if (mine !== latest) return;
		forked = results;
		generation++;
		running = false;
	}

	// --- edit helpers (an edit equal to the on-chain value is dropped) ---
	function setIn<K extends keyof ForkEdits>(field: K, key: string | number, value: unknown) {
		const map = { ...((edits[field] as Record<string | number, unknown>) ?? {}) };
		if (value == null) delete map[key];
		else map[key] = value;
		(edits as Record<string, unknown>)[field] = Object.keys(map).length ? map : undefined;
	}
	function resetAll() {
		edits = {};
		otherParams = null;
		epochText = String(page.epoch ?? '');
		epochError = null;
	}

	const spendInputs = $derived(
		plans
			.filter((p) => p.purpose === 'spend' && p.request && p.datum)
			.map((p) => {
				const i = p.request!.transaction.inputs[p.index];
				return { plan: p, ref: `${i.txId}#${i.index}`, datum: p.datum! };
			})
	);

	const tx = $derived(page.tx);
	const signers = $derived(edits.signers ?? tx.required_signers ?? []);
	let newSigner = $state('');
	function addSigner() {
		const h = newSigner.trim().toLowerCase();
		if (!/^[0-9a-f]{56}$/.test(h) || signers.includes(h)) return;
		edits.signers = [...signers, h];
		newSigner = '';
	}

	const slotTime = (slot: number | null | undefined) => {
		if (slot == null || !base?.slots) return null;
		return new Date(Number(slotToPosixMs(slot, base.slots))).toISOString().replace('T', ' ').replace('.000Z', ' UTC');
	};
	const validFrom = $derived(edits.validFrom !== undefined ? edits.validFrom : tx.validity_interval_start || null);
	const validTo = $derived(edits.validTo !== undefined ? edits.validTo : tx.ttl || null);
	function setBound(which: 'validFrom' | 'validTo', text: string, none: boolean) {
		const onChain = which === 'validFrom' ? tx.validity_interval_start || null : tx.ttl || null;
		const next = none ? null : /^\d+$/.test(text) ? Number(text) : undefined;
		if (next === undefined) return;
		edits[which] = next === onChain ? undefined : next;
	}

	let newMint = $state({ unit: '', quantity: '' });
	function addMint() {
		const unit = newMint.unit.trim().toLowerCase();
		if (!/^[0-9a-f]{56}([0-9a-f]{2}){0,32}$/.test(unit) || !/^-?\d+$/.test(newMint.quantity.trim())) return;
		setIn('mints', unit, newMint.quantity.trim());
		newMint = { unit: '', quantity: '' };
	}

	function outputEdit(index: number, patch: Record<string, unknown>) {
		const cur = { ...(edits.outputs?.[index] ?? {}), ...patch } as Record<string, unknown>;
		for (const k of Object.keys(cur)) if (cur[k] === undefined) delete cur[k];
		if (cur.assets && !Object.keys(cur.assets as object).length) delete cur.assets;
		setIn('outputs', index, Object.keys(cur).length ? cur : null);
	}
	function assetEdit(index: number, unit: string, quantity: string, onChain: string) {
		const assets = { ...(edits.outputs?.[index]?.assets ?? {}) };
		if (quantity === onChain) delete assets[unit];
		else assets[unit] = quantity;
		outputEdit(index, { assets: Object.keys(assets).length ? assets : undefined });
	}

	async function applyEpoch() {
		epochError = null;
		epochNote = null;
		const n = Number(epochText);
		if (!/^\d+$/.test(epochText.trim())) return (epochError = 'Enter an epoch number.');
		if (n === page.epoch) return (otherParams = null);
		try {
			const params = await loadParams(n);
			if (!params?.cost_models_raw) return (epochError = `The store has no cost models for epoch ${n}.`);
			otherParams = { epoch: n, params };
			// Older cost models are shorter: julc (protocol 10/11 rules) sets the missing builtin costs to the maximum.
			const shorter = [...new Set(plans.map((p) => p.language).filter(Boolean))]
				.map((v) => ({ v, then: params.cost_models_raw?.[`Plutus${v}`]?.length ?? 0, now: base?.params?.cost_models_raw?.[`Plutus${v}`]?.length ?? 0 }))
				.filter((x) => x.then && x.now && x.then < x.now);
			if (shorter.length)
				epochNote = `${shorter.map((x) => `Plutus ${x.v}: ${x.then} cost parameters (now ${x.now})`).join('; ')}. julc runs protocol 10/11 rules, so the costs of builtins added since are set to the maximum: a script that uses them runs out of budget.`;
		} catch (e) {
			epochError = (e as Error)?.message ?? `Couldn’t load epoch ${n}.`;
		}
	}

	// --- results ---
	const passed = (r?: RunResult) => !!r && r.state === 'done' && !!r.evaluation && (r.evaluation.accepted ?? r.evaluation.status === 'success');
	const outcome = (r?: RunResult) => (!r ? 'none' : r.state === 'done' ? (passed(r) ? 'passed' : r.evaluation?.status === 'budgetExhausted' ? 'out of budget' : 'failed') : 'couldn’t run');
	const delta = (a?: bigint, b?: bigint) => (a == null || b == null || a === 0n ? null : Number(((b - a) * 10000n) / a) / 100);
	const changedOutcomes = $derived(plans.filter((p) => outcome(original[p.key]) !== outcome(forked[p.key])).length);

	async function openDebugger(key: string) {
		debugging = key;
		await tick();
		document.getElementById('fork-debugger')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
	}

	function exportFixture(key: string, which: 'original' | 'fork') {
		const r = which === 'fork' ? forked[key] : original[key];
		if (!r?.args) return;
		const fork = which === 'fork' ? { edits: $state.snapshot(edits), epoch: otherParams?.epoch ?? null } : null;
		downloadJson(`${page.hash.slice(0, 12)}-${key.replace('#', '-')}${which === 'fork' ? '-fork' : ''}.json`, fixtureOf(page.hash, r, fork));
	}

	const field = 'y-input h-8 text-xs mono';
</script>

{#if phase === 'loading'}
	<Panel>
		<div class="flex items-center gap-2 text-sm text-muted" role="status"><LoaderCircle class="h-4 w-4 animate-spin text-brand" />{julc.status === 'loading' ? 'Starting the julc engine…' : 'Rebuilding the transaction and running its scripts…'}</div>
		<Skeleton rows={10} />
	</Panel>
{:else if phase === 'error'}
	<div class="flex items-start gap-3 rounded-2xl border border-danger/30 bg-danger/5 p-5 text-sm">
		<TriangleAlert class="mt-0.5 h-5 w-5 shrink-0 text-danger" />
		<div><p class="font-medium">Can’t fork this transaction</p><p class="mt-1 text-muted">{failure}</p></div>
	</div>
{:else}
	<div class="grid items-start gap-5 xl:grid-cols-[minmax(0,1.25fr)_minmax(0,1fr)]">
		<!-- Editor -->
		<div class="min-w-0 space-y-4">
			<Panel title="Redeemers" sub="What each script was asked to do. Edit as JSON (detailed schema) or CBOR.">
				<div class="space-y-3">
					{#each plans.filter((p) => p.redeemer) as p (p.key)}
						<DataEditor
							label="{p.purpose} #{p.index} · {shorten(p.scriptHash, 8, 6)}"
							value={edits.redeemers?.[p.key] ?? p.redeemer!}
							original={p.redeemer!}
							onchange={(cbor) => setIn('redeemers', p.key, cbor)}
						/>
					{/each}
				</div>
			</Panel>

			{#if spendInputs.length}
				<Panel title="Datums" sub="The datums of the inputs being spent by scripts.">
					<div class="space-y-3">
						{#each spendInputs as s (s.ref)}
							<DataEditor label="spend #{s.plan.index} · input {shorten(s.ref, 8, 4)}" value={edits.datums?.[s.ref] ?? s.datum} original={s.datum} onchange={(cbor) => setIn('datums', s.ref, cbor)} />
						{/each}
					</div>
				</Panel>
			{/if}

			<Panel title="Signers and validity" sub="Who signed, and the slot range the transaction is valid in.">
				<div class="y-label mb-2">Required signers</div>
				<div class="space-y-1.5">
					{#each signers as s (s)}
						<div class="flex items-center gap-2">
							<span class="mono min-w-0 flex-1 truncate rounded-lg border border-line bg-bg-2 px-2.5 py-1.5 text-xs">{s}</span>
							<button class="y-icon-btn h-8 w-8" title="Remove this signer" aria-label="Remove signer {s}" onclick={() => (edits.signers = signers.filter((x) => x !== s))}><X class="h-3.5 w-3.5" /></button>
						</div>
					{:else}
						<p class="text-xs text-muted">No required signers.</p>
					{/each}
					<div class="flex gap-2">
						<input class="{field} flex-1" placeholder="Add a key hash (56 hex)" bind:value={newSigner} onkeydown={(e) => e.key === 'Enter' && addSigner()} />
						<button class="y-btn h-8 px-2.5 text-xs" onclick={addSigner}><Plus class="h-3.5 w-3.5" />Add</button>
					</div>
				</div>
				<div class="mt-4 grid gap-3 sm:grid-cols-2">
					{#each [{ key: 'validFrom' as const, label: 'Valid from (slot)', value: validFrom }, { key: 'validTo' as const, label: 'Valid until (slot, TTL)', value: validTo }] as b (b.key)}
						<div>
							<div class="y-label mb-1.5 flex items-center gap-2">
								{b.label}
								{#if edits[b.key] !== undefined}<span class="rounded bg-brand/15 px-1.5 text-[10px] font-semibold uppercase text-brand">edited</span>{/if}
							</div>
							<input class={field} inputmode="numeric" placeholder="no bound" value={b.value ?? ''} oninput={(e) => setBound(b.key, e.currentTarget.value.trim(), e.currentTarget.value.trim() === '')} />
							<div class="mt-1 text-[11px] text-faint">{b.value != null ? slotTime(b.value) : 'No bound'}</div>
						</div>
					{/each}
				</div>
			</Panel>

			<Panel title="Outputs" sub="Change where value goes. Scripts often check these.">
				<div class="space-y-3">
					{#each tx.outputs as o (o.output_index)}
						{@const e = edits.outputs?.[o.output_index]}
						<div class="rounded-xl border p-3 {e?.removed ? 'border-danger/30 opacity-60' : e ? 'border-brand/40' : 'border-line'}">
							<div class="mb-2 flex items-center gap-2 text-xs">
								<span class="font-semibold">#{o.output_index}</span>
								{#if e && !e.removed}<span class="rounded bg-brand/15 px-1.5 text-[10px] font-semibold uppercase text-brand">edited</span>{/if}
								<button class="ml-auto text-xs font-medium {e?.removed ? 'text-brand' : 'text-muted hover:text-danger'}" onclick={() => outputEdit(o.output_index, { removed: e?.removed ? undefined : true })}>{e?.removed ? 'Restore' : 'Remove'}</button>
							</div>
							{#if !e?.removed}
								<input class="{field} mb-2" aria-label="Address of output {o.output_index}" value={e?.address ?? o.address} oninput={(ev) => outputEdit(o.output_index, { address: ev.currentTarget.value.trim() === o.address ? undefined : ev.currentTarget.value.trim() })} />
								<div class="grid gap-2 sm:grid-cols-2">
									{#each o.amount as a (a.unit)}
										{@const onChain = String(a.quantity)}
										<label class="flex items-center gap-2 text-xs">
											<span class="mono w-28 shrink-0 truncate text-muted" title={a.unit}>{a.unit === 'lovelace' ? 'lovelace' : shorten(a.unit, 6, 6)}</span>
											<input
												class="{field} flex-1"
												inputmode="numeric"
												value={a.unit === 'lovelace' ? (e?.lovelace ?? onChain) : (e?.assets?.[a.unit] ?? onChain)}
												oninput={(ev) => {
													const v = ev.currentTarget.value.trim();
													if (!/^\d+$/.test(v)) return;
													if (a.unit === 'lovelace') outputEdit(o.output_index, { lovelace: v === onChain ? undefined : v });
													else assetEdit(o.output_index, a.unit, v, onChain);
												}}
											/>
										</label>
									{/each}
								</div>
							{/if}
						</div>
					{/each}
				</div>
			</Panel>

			<Panel title="Mint, withdrawals and fee">
				<div class="y-label mb-2">Mint (negative burns, 0 removes)</div>
				<div class="space-y-1.5">
					{#each [...new Set([...page.mints.map((m) => m.unit), ...Object.keys(edits.mints ?? {})])] as unit (unit)}
						{@const onChain = String(page.mints.find((m) => m.unit === unit)?.quantity ?? '0')}
						<label class="flex items-center gap-2 text-xs">
							<span class="mono w-40 shrink-0 truncate text-muted" title={unit}>{shorten(unit, 10, 8)}</span>
							<input class="{field} flex-1" value={edits.mints?.[unit] ?? onChain} oninput={(ev) => /^-?\d+$/.test(ev.currentTarget.value.trim()) && setIn('mints', unit, ev.currentTarget.value.trim() === onChain ? null : ev.currentTarget.value.trim())} />
						</label>
					{/each}
					<div class="flex gap-2">
						<input class="{field} flex-[2]" placeholder="Policy id + asset name (hex)" bind:value={newMint.unit} />
						<input class="{field} flex-1" placeholder="Quantity" bind:value={newMint.quantity} />
						<button class="y-btn h-8 px-2.5 text-xs" onclick={addMint}><Plus class="h-3.5 w-3.5" />Add</button>
					</div>
				</div>
				{#if page.withdrawals.length}
					<div class="y-label mb-2 mt-4">Withdrawals (lovelace)</div>
					{#each page.withdrawals as w (w.address)}
						<label class="mb-1.5 flex items-center gap-2 text-xs">
							<span class="mono w-40 shrink-0 truncate text-muted" title={w.address}>{shorten(w.address, 10, 6)}</span>
							<input class="{field} flex-1" value={edits.withdrawals?.[w.address] ?? String(w.amount)} oninput={(ev) => /^\d+$/.test(ev.currentTarget.value.trim()) && setIn('withdrawals', w.address, ev.currentTarget.value.trim() === String(w.amount) ? null : ev.currentTarget.value.trim())} />
						</label>
					{/each}
				{/if}
				<div class="y-label mb-2 mt-4">Fee (lovelace)</div>
				<input class={field} value={edits.fee ?? String(tx.fees)} oninput={(ev) => /^\d+$/.test(ev.currentTarget.value.trim()) && (edits.fee = ev.currentTarget.value.trim() === String(tx.fees) ? undefined : ev.currentTarget.value.trim())} />
			</Panel>

			<Panel title="Rules" sub="Run under another epoch’s cost model or another protocol version, e.g. to see what a cost model update changes.">
				<div class="grid gap-3 sm:grid-cols-2">
					<div>
						<div class="y-label mb-1.5">Cost model from epoch</div>
						<div class="flex gap-2">
							<input class="{field} flex-1" inputmode="numeric" bind:value={epochText} onkeydown={(e) => e.key === 'Enter' && applyEpoch()} />
							<button class="y-btn h-8 px-2.5 text-xs" onclick={applyEpoch}>Apply</button>
						</div>
						<div class="mt-1 text-[11px] {epochError ? 'text-danger' : 'text-faint'}">{epochError ?? (otherParams ? `Using epoch ${otherParams.epoch}; the transaction ran in epoch ${page.epoch}.` : `The transaction ran in epoch ${page.epoch ?? '—'}.`)}</div>
						{#if epochNote && otherParams}<div class="mt-1 text-[11px] text-amber">{epochNote}</div>{/if}
					</div>
					<div>
						<div class="y-label mb-1.5">Protocol version</div>
						<select class="y-input h-8 text-xs" value={edits.protocol ?? ''} onchange={(e) => (edits.protocol = e.currentTarget.value ? (Number(e.currentTarget.value) as 10 | 11) : undefined)}>
							<option value="">As on chain</option>
							<option value="10">10 (Chang / Plomin)</option>
							<option value="11">11</option>
						</select>
					</div>
				</div>
			</Panel>
		</div>

		<!-- Results -->
		<div class="min-w-0 space-y-4 xl:sticky xl:top-20">
			<div class="rounded-2xl border border-brand/25 bg-gradient-to-br from-brand/[0.06] to-violet/[0.04] p-4">
				<div class="flex flex-wrap items-center gap-2">
					<span class="grid h-8 w-8 place-items-center rounded-xl bg-brand/10 text-brand"><GitFork class="h-4 w-4" /></span>
					<div class="min-w-0">
						<h3 class="font-display font-semibold">Fork vs on chain</h3>
						<p class="text-xs text-muted">{editCount ? `${editCount} change${editCount === 1 ? '' : 's'}` : 'No changes yet: edit anything on the left'}{changedOutcomes ? ` · ${changedOutcomes} outcome${changedOutcomes === 1 ? '' : 's'} changed` : ''}</p>
					</div>
					<span class="ml-auto flex items-center gap-2">
						{#if running}<LoaderCircle class="h-4 w-4 animate-spin text-brand" />{/if}
						{#if editCount}<button class="y-btn h-8 px-2.5 text-xs" onclick={resetAll}><RotateCcw class="h-3.5 w-3.5" />Reset</button>{/if}
					</span>
				</div>
			</div>

			{#each plans as p (p.key)}
				{@const o = original[p.key]}
				{@const f = forked[p.key]}
				{@const flipped = outcome(o) !== outcome(f)}
				<article class="rounded-2xl border bg-surface p-4 {flipped ? (passed(f) ? 'border-mint/50' : 'border-danger/50') : 'border-line'}">
					<header class="mb-3 flex flex-wrap items-center gap-2 text-sm">
						<span class="font-semibold">{p.purpose} #{p.index}</span>
						<span class="text-xs text-muted">Plutus {p.language ?? '?'} · <Hash value={p.scriptHash} href="/scripts/{p.scriptHash}" head={6} tail={4} /></span>
						{#if flipped}
							<span class="y-pill ml-auto {passed(f) ? 'border-mint/40 bg-mint/10 text-mint' : 'border-danger/40 bg-danger/10 text-danger'}">{passed(f) ? 'Now passes' : outcome(f) === 'couldn’t run' ? 'Can’t run now' : 'Now fails'}</span>
						{/if}
					</header>
					{#if !p.request}
						<p class="text-sm text-muted"><CircleSlash class="mr-1 inline h-4 w-4 align-[-3px]" />{p.skip}</p>
					{:else}
						<div class="grid grid-cols-[auto_1fr_1fr_auto] items-center gap-x-3 gap-y-1.5 text-xs tabular-nums">
							<span></span><span class="y-label">CPU</span><span class="y-label">Memory</span><span></span>
							{#each [{ name: 'On chain', r: o }, { name: 'Fork', r: f }] as row (row.name)}
								<span class="flex items-center gap-1.5 font-medium">
									{#if passed(row.r)}<CircleCheck class="h-3.5 w-3.5 text-mint" />{:else if row.r?.state === 'done'}<CircleX class="h-3.5 w-3.5 text-danger" />{:else}<CircleSlash class="h-3.5 w-3.5 text-faint" />{/if}
									{row.name}
								</span>
								<span>{row.r?.evaluation ? num(row.r.evaluation.cpu) : '—'}</span>
								<span>{row.r?.evaluation ? num(row.r.evaluation.mem) : '—'}</span>
								<span class="text-faint">{row.r?.ms != null ? `${row.r.ms} ms` : ''}</span>
							{/each}
							{#if o?.evaluation && f?.evaluation}
								{@const dc = delta(o.evaluation.cpu, f.evaluation.cpu)}
								{@const dm = delta(o.evaluation.mem, f.evaluation.mem)}
								<span class="text-faint">Change</span>
								<!-- A cheaper run only counts as an improvement when the fork still passes. -->
								<span class={!dc || !passed(f) ? 'text-faint' : dc > 0 ? 'text-amber' : 'text-mint'}>{dc ? `${dc > 0 ? '+' : ''}${dc}%` : '='}</span>
								<span class={!dm || !passed(f) ? 'text-faint' : dm > 0 ? 'text-amber' : 'text-mint'}>{dm ? `${dm > 0 ? '+' : ''}${dm}%` : '='}</span>
								<span></span>
							{/if}
						</div>
						{#if f && !passed(f)}
							<p class="mt-2 break-words text-xs text-danger">{f.error ?? f.evaluation?.message ?? f.evaluation?.error ?? ''}</p>
							{#if f.evaluation?.traces?.length}<p class="mono mt-1 break-words text-[11px] text-muted">Last trace: {f.evaluation.traces.at(-1)}</p>{/if}
						{/if}
						<div class="mt-3 flex flex-wrap gap-2">
							{#if f?.args}<button class="y-btn h-7 px-2.5 text-xs" onclick={() => openDebugger(p.key)}><Bug class="h-3.5 w-3.5" />Debug the fork</button>{/if}
							{#if f?.args}<button class="y-btn h-7 px-2.5 text-xs" onclick={() => exportFixture(p.key, editCount ? 'fork' : 'original')}><Download class="h-3.5 w-3.5" />Export fixture</button>{/if}
							<a class="y-btn h-7 px-2.5 text-xs" href="/transactions/{page.hash}/debug/{p.purpose}/{p.index}">On-chain debugger<ArrowRight class="h-3.5 w-3.5" /></a>
						</div>
					{/if}
				</article>
			{/each}
		</div>
	</div>

	{#if debugging && forked[debugging]?.args}
		<section class="mt-8" id="fork-debugger">
			<div class="mb-3 flex items-center gap-2">
				<Bug class="h-5 w-5 text-brand" />
				<h2 class="font-display text-lg font-semibold">Debugging the fork · {debugging.replace('#', ' #')}</h2>
				<span class="text-xs text-muted">{editCount ? 'with your changes' : 'unchanged'} · reopens after each edit</span>
				<button class="y-btn ml-auto h-8 px-2.5 text-xs" onclick={() => (debugging = null)}><X class="h-3.5 w-3.5" />Close</button>
			</div>
			{#key `${generation}/${debugging}`}
				{@const r = forked[debugging]}
				<Debugger page={page} purpose={r.plan.purpose} index={r.plan.index} prepared={{ plan: r.plan, result: r }} syncUrl={false} />
			{/key}
		</section>
	{/if}
{/if}
