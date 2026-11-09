<script lang="ts">
	import { onMount, tick, untrack } from 'svelte';
	import { replaceState } from '$app/navigation';
	import { page as appPage } from '$app/state';
	import { ada, num, shorten } from '$lib/format';
	import { decodePlutusData, type PlutusData } from '$lib/cbor';
	import { copyText } from '$lib/ui/toast.svelte';
	import Panel from '$lib/ui/Panel.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import PlutusView from '$lib/ui/PlutusView.svelte';
	import CodeView from '../CodeView.svelte';
	import DataEditor from '../fork/DataEditor.svelte';
	import Debugger from '../debug/Debugger.svelte';
	import JavaEditor from './JavaEditor.svelte';
	import JavaDebugger from './JavaDebugger.svelte';
	import { julc, wasmSupported } from '../engine.svelte';
	import { runAll, type RunResult } from '../replay/run';
	import { outcome, passed } from '../replay/verdict';
	import { slotToPosixMs, slotConfigFor, type Genesis } from '../replay/slots';
	import { downloadJson, fixtureOf } from '../replay/fixture';
	import type { StoreUtxo } from '../replay/build';
	import type { PageTx } from '../replay/load';
	import type { CheckResponse, CompileResponse, PlutusVersion } from '../types';
	import { parseBlueprint, type Blueprint } from '../blueprint/blueprint';
	import { EXAMPLES } from './examples';
	import { labPlan, labPurposeOf, paramData, paymentKeyHash, scriptAddress, templateOf, type LabPurpose, type LabScript, type LabTx } from './lab';
	import { datumByHash, findUtxos } from './utxo';
	import { javaFailureLine } from './source-debug';
	import CircleCheck from '@lucide/svelte/icons/circle-check';
	import CircleX from '@lucide/svelte/icons/circle-x';
	import LoaderCircle from '@lucide/svelte/icons/loader-circle';
	import TriangleAlert from '@lucide/svelte/icons/triangle-alert';
	import Plus from '@lucide/svelte/icons/plus';
	import X from '@lucide/svelte/icons/x';
	import Bug from '@lucide/svelte/icons/bug';
	import Coffee from '@lucide/svelte/icons/coffee';
	import Download from '@lucide/svelte/icons/download';
	import Search from '@lucide/svelte/icons/search';
	import Copy from '@lucide/svelte/icons/copy';

	/*
	 * The validator lab: bring a validator (julc Java compiled in the browser, a compiled script, or one on this
	 * chain), point it at real chain state (a UTxO it spends as if locked at the script, a mint, a withdrawal) and see
	 * whether it accepts, with its budget and traces, as you type. Runs can be stepped through in the UPLC debugger,
	 * or at the Java source level, and exported as test fixtures.
	 */
	let {
		params,
		genesis,
		tipSlot,
		initialScript,
		scriptMissing,
		initialUtxo
	}: {
		params: Record<string, unknown> & { max_tx_ex_mem?: number | string | null; max_tx_ex_steps?: number | string | null };
		genesis: Genesis | null;
		tipSlot: number | null;
		initialScript: { hash: string; type: string | null; cbor: string } | null;
		scriptMissing: boolean;
		initialUtxo: StoreUtxo | null;
	} = $props();

	const SOURCE_KEY = 'viewer:lab:java';
	const slots = $derived(slotConfigFor(genesis?.network_magic, genesis));
	const mainnet = $derived(genesis?.network_magic === 764824073);

	// --- the validator ---
	// svelte-ignore state_referenced_locally
	let mode = $state<'java' | 'cbor' | 'chain'>(initialScript ? 'chain' : 'java');
	let source = $state(EXAMPLES[0].source);
	let exampleId = $state(EXAMPLES[0].id);
	let check = $state.raw<CheckResponse | null>(null);
	let compiled = $state.raw<CompileResponse | null>(null);
	let compiling = $state(false);
	let compileError = $state<string | null>(null);
	let paramTexts = $state<string[]>([]);
	let cborText = $state('');
	let cborLanguage = $state<PlutusVersion>('V3');
	// svelte-ignore state_referenced_locally
	let chainHash = $state(initialScript?.hash ?? '');
	// svelte-ignore state_referenced_locally
	let chainScript = $state.raw<{ hash: string; type: string | null; cbor: string } | null>(initialScript);
	// svelte-ignore state_referenced_locally
	let chainError = $state<string | null>(scriptMissing ? 'The store has no code for that script.' : null);
	let script = $state.raw<LabScript | null>(null);
	let scriptError = $state<string | null>(null);

	// --- the transaction ---
	let purpose = $state<LabPurpose>('spend');
	// svelte-ignore state_referenced_locally
	let utxo = $state.raw<StoreUtxo | null>(initialUtxo);
	let utxoQuery = $state('');
	let utxoChoices = $state.raw<StoreUtxo[]>([]);
	let utxoError = $state<string | null>(null);
	let utxoDatum = $state<string | null>(null);
	let datum = $state(templateOf(0, []));
	let redeemer = $state(templateOf(0, []));
	let signers = $state<string[]>([]);
	let newSigner = $state('');
	let validFrom = $state('');
	let validTo = $state('');
	let outputs = $state<{ address: string; lovelace: string }[]>([]);
	let mint = $state<{ tokenName: string; quantity: string }[]>([{ tokenName: '', quantity: '1' }]);
	let withdrawal = $state('0');
	let withdrawals = $state<{ credential: string; amount: string }[]>([]);
	let fee = $state('200000');

	// --- results ---
	let phase = $state<'starting' | 'ready' | 'error'>('starting');
	let failure = $state<string | null>(null);
	let result = $state.raw<RunResult | null>(null);
	let runError = $state<string | null>(null);
	let failedAt = $state.raw<{ line: number; fragment: string | null } | null>(null);
	let editor: JavaEditor | undefined = $state();
	let running = $state(false);
	let debugging = $state<'uplc' | 'java' | null>(null);
	let generation = $state(0);

	onMount(async () => {
		try {
			const saved = localStorage.getItem(SOURCE_KEY);
			if (saved && !initialScript) {
				source = saved;
				exampleId = EXAMPLES.find((e) => e.source === saved)?.id ?? '';
			}
		} catch {
			/* no storage */
		}
		if (initialUtxo) await useUtxo(initialUtxo);
		if (!wasmSupported()) return fail('This browser can’t run the julc engine (it needs WebAssembly GC and exception handling). Use a current Chrome, Edge, Firefox or Safari.');
		if (!(await julc.available())) return fail('Script Lab is not installed with this viewer, so validators can’t be run here.');
		phase = 'ready';
	});

	function fail(message: string) {
		failure = message;
		phase = 'error';
	}

	// Compile the Java source shortly after it changes, and keep it in this browser.
	let compileTimer: ReturnType<typeof setTimeout> | undefined;
	let compileGen = 0;
	$effect(() => {
		const src = source;
		if (phase !== 'ready' || mode !== 'java') return;
		try {
			localStorage.setItem(SOURCE_KEY, src);
		} catch {
			/* no storage */
		}
		clearTimeout(compileTimer);
		compileTimer = setTimeout(() => compile(src), 450);
	});

	async function compile(src: string) {
		const mine = ++compileGen;
		compiling = true;
		try {
			const [c, r] = await julc.run(async (cl) => {
				if (!cl.compiler) throw new Error('This julc engine can’t compile (VM-only build).');
				return [await cl.compiler.check({ source: src, language: 'java' }), await cl.compiler.compile({ source: src, language: 'java', blueprint: true })] as const;
			});
			if (mine !== compileGen) return;
			check = c;
			compiled = r;
			compileError = r.compiledCode ? null : (r.diagnostics ?? c.diagnostics ?? []).length ? null : 'julc couldn’t compile this source and gave no reason. Constructs it doesn’t support yet fail this way (for example `x instanceof Type` without a variable: write `x instanceof Type t`).';
			const p = labPurposeOf(c.purpose);
			if (p && p !== purpose) setPurpose(p);
			const want = r.params?.length ?? 0;
			if (paramTexts.length !== want) paramTexts = Array.from({ length: want }, (_, i) => paramTexts[i] ?? '');
		} catch (e) {
			if (mine === compileGen) compileError = (e as Error)?.message ?? String(e);
		} finally {
			if (mine === compileGen) compiling = false;
		}
	}

	function loadExample(id: string) {
		const e = EXAMPLES.find((x) => x.id === id);
		if (!e) return;
		source = e.source;
		exampleId = id;
		redeemer = templateOf(0, []);
		paramTexts = [];
	}

	async function loadChainScript() {
		chainError = null;
		const h = chainHash.trim().toLowerCase();
		if (!/^[0-9a-f]{56}$/.test(h)) return (chainError = 'Enter a 56-character script hash.');
		const { tryApi } = await import('$lib/api/client');
		const [info, body] = await Promise.all([tryApi<{ type?: string }>(`/scripts/${h}`), tryApi<{ cbor?: string }>(`/scripts/${h}/cbor`)]);
		if (!body?.cbor) return (chainError = 'The store has no code for that script.');
		chainScript = { hash: h, type: info?.type ?? null, cbor: body.cbor };
	}

	const versionOf = (type: string | null | undefined): PlutusVersion => (/v1/i.test(type ?? '') ? 'V1' : /v2/i.test(type ?? '') ? 'V2' : 'V3');

	// The script to run: compiled Java with its parameters applied, pasted CBOR, or the chain's.
	$effect(() => {
		const m = mode;
		const comp = compiled;
		const texts = [...paramTexts];
		const pasted = cborText.trim().toLowerCase();
		const lang = cborLanguage;
		const chain = chainScript;
		if (phase !== 'ready') return;
		untrack(() => resolveScript(m, comp, texts, pasted, lang, chain));
	});

	let scriptGen = 0;
	async function resolveScript(m: typeof mode, comp: CompileResponse | null, texts: string[], pasted: string, lang: PlutusVersion, chain: typeof chainScript) {
		const mine = ++scriptGen;
		scriptError = null;
		let next: Omit<LabScript, 'hash'> | null = null;
		try {
			if (m === 'java') {
				if (!comp?.compiledCode) return void (script = null);
				const ps = (comp.params ?? []).map((p, i) => {
					if (!texts[i]?.trim()) throw new Error(`Set the parameter “${p.name}”.`);
					try {
						return paramData(p.type, texts[i], (t) => fromJson(t));
					} catch (e) {
						throw new Error(`Parameter “${p.name}”: ${(e as Error).message}.`);
					}
				});
				next = { cbor: comp.compiledCode, language: 'V3', params: ps };
			} else if (m === 'cbor') {
				if (!pasted) return void (script = null);
				if (!/^([0-9a-f]{2})+$/.test(pasted)) throw new Error('The script must be CBOR hex.');
				next = { cbor: pasted, language: lang, params: [] };
			} else {
				if (!chain) return void (script = null);
				next = { cbor: chain.cbor, language: versionOf(chain.type), params: [] };
			}
			const n = next;
			const h = await julc.run((c) => c.vm.hash({ script: { script: n.cbor, language: n.language, ...(n.params.length ? { params: n.params.map((value) => ({ format: 'cbor' as const, value })) } : {}) } }));
			if (mine !== scriptGen) return;
			script = { ...n, hash: h.scriptHash };
		} catch (e) {
			if (mine !== scriptGen) return;
			script = null;
			scriptError = (e as Error)?.message ?? String(e);
		}
	}

	/** Plutus data from the editor's JSON (detailed schema) or CBOR hex. */
	function fromJson(text: string): PlutusData {
		if (/^([0-9a-f]{2})+$/i.test(text)) return decodePlutusData(text);
		const toData = (x: unknown): PlutusData => {
			const o = x as Record<string, unknown>;
			if ('int' in o) return { int: String(BigInt(String(o.int))) };
			if ('bytes' in o) return { bytes: String(o.bytes) };
			if ('list' in o) return { list: (o.list as unknown[]).map(toData) };
			if ('map' in o) return { map: (o.map as { k: unknown; v: unknown }[]).map((e) => ({ k: toData(e.k), v: toData(e.v) })) };
			return { constructor: Number(o.constructor), fields: ((o.fields as unknown[]) ?? []).map(toData) };
		};
		return toData(JSON.parse(text));
	}

	// --- chain state ---
	async function searchUtxos() {
		utxoError = null;
		utxoChoices = [];
		const r = await findUtxos(utxoQuery);
		if (r.error) utxoError = r.error;
		if (r.utxos.length === 1) await useUtxo(r.utxos[0]);
		else utxoChoices = r.utxos;
	}

	async function useUtxo(u: StoreUtxo) {
		utxo = u;
		utxoChoices = [];
		utxoDatum = u.inline_datum ?? (u.data_hash ? await datumByHash(u.data_hash) : null);
		if (utxoDatum) datum = utxoDatum;
		const lovelace = u.amount.find((a) => a.unit === 'lovelace')?.quantity ?? '0';
		if (!outputs.length && u.address) outputs = [{ address: u.address, lovelace: String(BigInt(lovelace) > 2000000n ? BigInt(lovelace) - BigInt(fee || '0') : BigInt(lovelace)) }];
		const owner = u.address ? paymentKeyHash(u.address) : null;
		if (owner && !signers.includes(owner) && purpose !== 'spend') signers = [...signers, owner];
	}

	function setPurpose(p: LabPurpose) {
		purpose = p;
	}

	function addSigner(h = newSigner) {
		const k = h.trim().toLowerCase();
		if (!/^[0-9a-f]{56}$/.test(k) || signers.includes(k)) return;
		signers = [...signers, k];
		newSigner = '';
	}

	const slotNum = (t: string) => (/^\d+$/.test(t.trim()) ? Number(t.trim()) : null);
	const posixOf = (t: string) => {
		const n = slotNum(t);
		return n != null && slots ? slotToPosixMs(n, slots) : null;
	};

	// --- run as anything changes ---
	let runTimer: ReturnType<typeof setTimeout> | undefined;
	let runGen = 0;
	$effect(() => {
		const tx: LabTx = {
			purpose,
			utxo,
			atScript: true,
			datum: purpose === 'spend' ? datum : null,
			redeemer,
			referenceInputs: [],
			outputs: $state.snapshot(outputs),
			mint: $state.snapshot(mint),
			withdrawal,
			withdrawals: $state.snapshot(withdrawals),
			signers: [...signers],
			validFrom: slotNum(validFrom),
			validTo: slotNum(validTo),
			fee
		};
		const s = script;
		if (phase !== 'ready') return;
		clearTimeout(runTimer);
		runTimer = setTimeout(() => run(s, tx), 300);
	});

	async function run(s: LabScript | null, tx: LabTx) {
		const mine = ++runGen;
		if (!s) {
			result = null;
			runError = null;
			return;
		}
		running = true;
		try {
			const plan = labPlan(s, tx, { params, slots });
			const results: Record<string, RunResult> = {};
			const engineFailure = await runAll([plan], (k, r) => (results[k] = r));
			if (mine !== runGen) return;
			if (engineFailure) return fail(engineFailure);
			result = results[plan.key];
			runError = null;
			failedAt = null;
			generation++;
			// A Java validator that rejects: find the Java line it failed on.
			if (mode === 'java' && result.state === 'done' && !passed(result)) {
				const at = await javaFailureLine(source, s.params, plan).catch(() => null);
				if (mine === runGen) failedAt = at;
			}
		} catch (e) {
			if (mine !== runGen) return;
			result = null;
			runError = (e as Error)?.message ?? String(e);
		} finally {
			if (mine === runGen) running = false;
		}
	}

	// Keep the script and UTxO in the link (Java source stays in this browser).
	$effect(() => {
		const q = new URLSearchParams();
		if (mode === 'chain' && chainScript) q.set('script', chainScript.hash);
		if (utxo) q.set('utxo', `${utxo.tx_hash}#${utxo.output_index}`);
		const next = q.toString();
		if (next === appPage.url.search.slice(1)) return;
		try {
			replaceState(`${appPage.url.pathname}${next ? `?${next}` : ''}`, {});
		} catch {
			/* not ready */
		}
	});

	const blueprint = $derived.by<Blueprint | null>(() => {
		try {
			if (mode !== 'java' || !compiled?.blueprintJson) return null;
			const json = JSON.parse(compiled.blueprintJson);
			// julc titles every blueprint "Playground"; name it after the contract.
			const title = check?.contractName ?? json.preamble?.title;
			return parseBlueprint({ ...json, preamble: { ...json.preamble, title }, validators: json.validators.map((v: object) => ({ ...v, hash: script?.hash ?? '00'.repeat(28) })) });
		} catch {
			return null;
		}
	});
	const schemaOf = (which: 'datum' | 'redeemer') => {
		const v = blueprint?.validators[0];
		const s = v?.[which];
		return s ? { schema: s, definitions: blueprint!.definitions, source: blueprint!.title } : null;
	};

	const accepted = $derived(passed(result));
	const limits = $derived({ cpu: Number(params?.max_tx_ex_steps ?? 0), mem: Number(params?.max_tx_ex_mem ?? 0) });
	const share = (n: bigint | undefined, of: number) => (n != null && of ? `${((Number(n) / of) * 100).toFixed(2)}% of a tx` : '');
	const fakePage: PageTx = { hash: '', tx: { fees: 0, inputs: [], outputs: [] }, scripts: [], withdrawals: [], mints: [], epoch: null };

	async function openDebugger(kind: 'uplc' | 'java') {
		debugging = kind;
		await tick();
		document.getElementById('lab-debugger')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
	}

	function exportFixture() {
		if (!result?.args) return;
		downloadJson(`lab-${script?.hash.slice(0, 12)}-${purpose}.json`, { ...fixtureOf('', result), source: { lab: true, purpose, utxo: utxo ? `${utxo.tx_hash}#${utxo.output_index}` : null } });
	}

	function downloadBlueprint() {
		if (!compiled?.blueprintJson) return;
		const json = JSON.parse(compiled.blueprintJson);
		if (check?.contractName) json.preamble = { ...json.preamble, title: check.contractName };
		downloadJson('plutus.json', json);
	}

	const lovelaceOf = (u: StoreUtxo) => u.amount.find((a) => a.unit === 'lovelace')?.quantity ?? '0';
	const field = 'y-input h-8 text-xs mono';
	const seg = (on: boolean) => `rounded-md px-2.5 py-1 transition ${on ? 'bg-surface text-ink shadow-sm' : 'text-muted hover:text-ink'}`;
</script>

{#if phase === 'error'}
	<div class="flex items-start gap-3 rounded-2xl border border-danger/30 bg-danger/5 p-5 text-sm">
		<TriangleAlert class="mt-0.5 h-5 w-5 shrink-0 text-danger" />
		<div><p class="font-medium">Can’t run validators here</p><p class="mt-1 text-muted">{failure}</p></div>
	</div>
{:else}
	<div class="grid items-start gap-5 xl:grid-cols-[minmax(0,1.35fr)_minmax(0,1fr)]">
		<div class="min-w-0 space-y-4">
			<!-- 1. The validator -->
			<Panel title="Validator">
				{#snippet actions()}
					<div class="flex items-center gap-0.5 rounded-lg bg-surface-2 p-0.5 text-xs" role="tablist">
						{#each [['java', 'Java'], ['cbor', 'Compiled'], ['chain', 'On chain']] as [id, name] (id)}
							<button role="tab" aria-selected={mode === id} class={seg(mode === id)} onclick={() => (mode = id as typeof mode)}>{name}</button>
						{/each}
					</div>
				{/snippet}

				{#if mode === 'java'}
					<div class="mb-3 flex flex-wrap items-center gap-2 text-sm">
						<span class="text-muted">Start from</span>
						{#each EXAMPLES as e (e.id)}
							<button class="rounded-full border px-2.5 py-0.5 text-xs transition {exampleId === e.id ? 'border-brand/50 bg-brand/10 text-brand' : 'border-line text-muted hover:text-ink'}" title={e.summary} onclick={() => loadExample(e.id)}>{e.name}</button>
						{/each}
					</div>
					<JavaEditor bind:this={editor} bind:value={source} diagnostics={[...(compiled?.diagnostics ?? check?.diagnostics ?? []), ...(failedAt ? [{ level: 'ERROR', message: 'The validator failed here', startLine: failedAt.line }] : [])]} />
					<div class="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted" role="status">
						{#if phase === 'starting' || (compiling && !compiled)}<span class="flex items-center gap-1.5"><LoaderCircle class="h-3.5 w-3.5 animate-spin text-brand" />{julc.status === 'loading' ? 'Starting the julc engine…' : 'Compiling…'}</span>
						{:else if compiled?.compiledCode}
							<span class="flex items-center gap-1 text-ok"><CircleCheck class="h-3.5 w-3.5" />Compiled</span>
							<span>{check?.contractName} · {String(check?.purpose ?? '').toLowerCase()} · {num(compiled.scriptSizeBytes)} bytes</span>
							{#if compiling}<LoaderCircle class="h-3.5 w-3.5 animate-spin text-brand" />{/if}
							<button class="ml-auto flex items-center gap-1 hover:text-ink" onclick={downloadBlueprint}><Download class="h-3.5 w-3.5" />plutus.json</button>
						{:else if compiled || compileError}
							<span class="flex items-center gap-1 text-danger"><CircleX class="h-3.5 w-3.5" />Doesn’t compile</span>
						{/if}
					</div>
					{#if compileError}<p class="mt-2 text-xs text-danger">{compileError}</p>{/if}
					{#each (compiled?.diagnostics ?? []).concat(compiled?.compiledCode ? [] : (check?.diagnostics ?? [])) as d, i (i)}
						<p class="mt-1.5 text-xs {d.level === 'ERROR' ? 'text-danger' : 'text-warn'}"><span class="mono">line {d.startLine ?? '?'}</span> · {d.message}{#if d.suggestion}<span class="text-muted"> — {d.suggestion}</span>{/if}</p>
					{/each}
					{#if compiled?.params?.length}
						<div class="mt-4">
							<div class="y-label mb-2">Parameters</div>
							<div class="space-y-2">
								{#each compiled.params as p, i (p.name)}
									<label class="flex flex-wrap items-center gap-2 text-sm">
										<span class="mono w-36 shrink-0 truncate">{p.name} <span class="text-[11px] text-faint">{p.type}</span></span>
										<input class="{field} min-w-0 flex-1" bind:value={paramTexts[i]} placeholder={/byte/i.test(p.type ?? '') ? 'hex, e.g. a key hash' : /int/i.test(p.type ?? '') ? 'a whole number' : 'Plutus data (JSON or CBOR)'} spellcheck="false" />
									</label>
								{/each}
							</div>
						</div>
					{/if}
					{#if compiled?.uplcText}
						<details class="mt-4 text-sm">
							<summary class="cursor-pointer text-xs font-medium text-muted hover:text-ink">Compiled UPLC</summary>
							<div class="mt-2"><CodeView text={compiled.uplcText} language="uplc" filename="validator.uplc" height={320} label="Compiled UPLC" /></div>
						</details>
					{/if}
				{:else if mode === 'cbor'}
					<textarea class="y-input mono min-h-28 w-full p-2 text-[11px]" bind:value={cborText} placeholder="A compiled Plutus script as CBOR hex, e.g. compiledCode from Aiken’s plutus.json" aria-label="Compiled script CBOR" spellcheck="false"></textarea>
					<div class="mt-2 flex items-center gap-2 text-xs">
						<span class="text-muted">Plutus</span>
						<div class="flex items-center gap-0.5 rounded-lg bg-surface-2 p-0.5">{#each ['V1', 'V2', 'V3'] as v (v)}<button class={seg(cborLanguage === v)} onclick={() => (cborLanguage = v as PlutusVersion)}>{v}</button>{/each}</div>
					</div>
				{:else}
					<div class="flex items-center gap-2">
						<input class="{field} min-w-0 flex-1" bind:value={chainHash} placeholder="Script hash on this chain" aria-label="Script hash" spellcheck="false" onkeydown={(e) => e.key === 'Enter' && loadChainScript()} />
						<button class="y-btn h-8 text-xs" onclick={loadChainScript}>Load</button>
					</div>
					{#if chainError}<p class="mt-2 text-xs text-danger">{chainError}</p>{/if}
					{#if chainScript}<p class="mt-2 text-xs text-muted">Plutus {versionOf(chainScript.type).slice(1)} · <a class="y-link mono" href="/scripts/{chainScript.hash}">{shorten(chainScript.hash, 10, 6)}</a></p>{/if}
				{/if}
				{#if scriptError}<p class="mt-3 text-xs text-danger">{scriptError}</p>{/if}
				{#if script}
					<div class="mt-4 flex flex-wrap items-center gap-2 border-t border-line pt-3 text-xs text-muted">
						<span>Hash{script.params.length ? ' with parameters' : ''}</span>
						<span class="mono text-ink">{shorten(script.hash, 10, 8)}</span>
						<button class="hover:text-ink" title="Copy the hash" aria-label="Copy the script hash" onclick={() => copyText(script!.hash, 'Script hash copied')}><Copy class="h-3.5 w-3.5" /></button>
						{#if purpose === 'spend'}<span class="ml-auto truncate" title={scriptAddress(script.hash, mainnet)}>address <span class="mono">{shorten(scriptAddress(script.hash, mainnet), 12, 6)}</span></span>{/if}
					</div>
				{/if}
			</Panel>

			<!-- 2. Chain state -->
			<Panel title="Chain state" sub={purpose === 'spend' ? 'A real UTxO, spent as if it were locked at the validator' : 'A real UTxO that pays for the transaction'}>
				{#snippet actions()}
					<div class="flex items-center gap-0.5 rounded-lg bg-surface-2 p-0.5 text-xs" role="tablist" aria-label="Purpose">
						{#each [['spend', 'Spend'], ['mint', 'Mint'], ['reward', 'Withdraw']] as [id, name] (id)}
							<button role="tab" aria-selected={purpose === id} class={seg(purpose === id)} onclick={() => setPurpose(id as LabPurpose)}>{name}</button>
						{/each}
					</div>
				{/snippet}
				<div class="flex items-center gap-2">
					<input class="{field} min-w-0 flex-1" bind:value={utxoQuery} placeholder="<tx hash>#<index>, or an address to list its UTxOs" aria-label="Find a UTxO" spellcheck="false" onkeydown={(e) => e.key === 'Enter' && searchUtxos()} />
					<button class="y-btn h-8 text-xs" onclick={searchUtxos}><Search class="h-3.5 w-3.5" />Find</button>
				</div>
				{#if utxoError}<p class="mt-2 text-xs text-danger">{utxoError}</p>{/if}
				{#if utxoChoices.length}
					<ul class="mt-2 max-h-56 divide-y divide-line overflow-auto rounded-xl border border-line">
						{#each utxoChoices as u (u.tx_hash + u.output_index)}
							<li><button class="flex w-full items-center justify-between gap-3 px-3 py-2 text-left text-xs hover:bg-surface-2" onclick={() => useUtxo(u)}><span class="mono">{shorten(u.tx_hash, 10, 6)}#{u.output_index}</span><span class="tabular-nums">{ada(lovelaceOf(u), 'auto')} ada{u.amount.length > 1 ? ` + ${u.amount.length - 1} token${u.amount.length > 2 ? 's' : ''}` : ''}{u.inline_datum || u.data_hash ? ' · datum' : ''}</span></button></li>
						{/each}
					</ul>
				{/if}
				{#if utxo}
					<div class="mt-3 rounded-xl border border-line bg-surface-2/60 px-3 py-2.5 text-xs">
						<div class="flex flex-wrap items-center gap-2">
							<a class="mono y-link" href="/transactions/{utxo.tx_hash}">{shorten(utxo.tx_hash, 10, 6)}#{utxo.output_index}</a>
							<span class="tabular-nums">{ada(lovelaceOf(utxo), 'auto')} ada</span>
							{#if utxo.amount.length > 1}<Badge tone="violet">+{utxo.amount.length - 1} token{utxo.amount.length > 2 ? 's' : ''}</Badge>{/if}
							{#if utxoDatum}<Badge tone="cyan">datum</Badge>{/if}
							<button class="ml-auto text-muted hover:text-ink" aria-label="Clear the UTxO" onclick={() => ((utxo = null), (utxoDatum = null))}><X class="h-3.5 w-3.5" /></button>
						</div>
						{#if utxo.address}<div class="mono mt-1 truncate text-faint">from {utxo.address}</div>{/if}
					</div>
				{:else}
					<p class="mt-3 text-xs text-muted">No UTxO chosen: a sample 10 ada one is used.</p>
				{/if}

				{#if purpose === 'spend'}
					<div class="mt-4">
						<div class="mb-2 flex flex-wrap items-center gap-2">
							<span class="y-label">Datum</span>
							{#if utxoDatum && datum !== utxoDatum}<button class="text-xs text-brand hover:underline" onclick={() => (datum = utxoDatum!)}>Use the UTxO’s datum</button>{/if}
							{#if check?.datumFields?.length}<button class="text-xs text-brand hover:underline" onclick={() => (datum = templateOf(0, check!.datumFields))}>Template for {check.datumName}</button>{/if}
						</div>
						<DataEditor label="Datum the validator sees" value={datum} original={datum} onchange={(c) => c && (datum = c)} />
						{#if schemaOf('datum')}<div class="mt-2"><PlutusView cbor={datum} label="Datum" schema={schemaOf('datum')} /></div>{/if}
					</div>
				{:else if purpose === 'mint'}
					<div class="mt-4">
						<div class="y-label mb-2">Mint under this policy</div>
						{#each mint as m, i (i)}
							<div class="mb-2 flex items-center gap-2">
								<input class="{field} min-w-0 flex-1" placeholder="token name (hex)" bind:value={m.tokenName} aria-label="Token name {i + 1}" spellcheck="false" />
								<input class="{field} w-28" placeholder="quantity" bind:value={m.quantity} aria-label="Quantity {i + 1}" />
								<button class="y-icon-btn h-8 w-8" aria-label="Remove token {i + 1}" onclick={() => mint.splice(i, 1)}><X class="h-3.5 w-3.5" /></button>
							</div>
						{/each}
						<button class="y-btn h-8 text-xs" onclick={() => mint.push({ tokenName: '', quantity: '1' })}><Plus class="h-3.5 w-3.5" />Add a token</button>
					</div>
				{:else}
					<label class="mt-4 flex items-center gap-2 text-sm">
						<span class="text-muted">Withdraw from the script’s stake credential</span>
						<input class="{field} w-32" bind:value={withdrawal} aria-label="Withdrawal amount" />
						<span class="text-xs text-faint">lovelace (0 for “withdraw zero”)</span>
					</label>
				{/if}
			</Panel>

			<!-- 3. The transaction -->
			<Panel title="Transaction" sub="What the validator is asked to accept">
				<div class="mb-2 flex flex-wrap items-center gap-2">
					<span class="y-label">Redeemer</span>
					{#each check?.redeemerVariants ?? [] as v (v.tag)}
						<button class="rounded-full border border-line px-2 py-0.5 text-xs text-muted hover:border-brand/50 hover:text-brand" onclick={() => (redeemer = templateOf(v.tag, v.fields))}>{v.name}</button>
					{/each}
				</div>
				<DataEditor label="Redeemer" value={redeemer} original={redeemer} onchange={(c) => c && (redeemer = c)} />
				{#if schemaOf('redeemer')}<div class="mt-2"><PlutusView cbor={redeemer} label="Redeemer" schema={schemaOf('redeemer')} /></div>{/if}

				<div class="y-label mt-5 mb-2">Signers</div>
				<div class="space-y-1.5">
					{#each signers as s (s)}
						<div class="flex items-center gap-2">
							<span class="mono min-w-0 flex-1 truncate rounded-lg border border-line bg-bg-2 px-2.5 py-1.5 text-xs">{s}</span>
							<button class="y-icon-btn h-8 w-8" aria-label="Remove signer {s}" onclick={() => (signers = signers.filter((x) => x !== s))}><X class="h-3.5 w-3.5" /></button>
						</div>
					{/each}
					<div class="flex items-center gap-2">
						<input class="{field} min-w-0 flex-1" bind:value={newSigner} placeholder="key hash (56 hex)" aria-label="Add a signer" spellcheck="false" onkeydown={(e) => e.key === 'Enter' && addSigner()} />
						<button class="y-btn h-8 text-xs" onclick={() => addSigner()}><Plus class="h-3.5 w-3.5" />Add</button>
						{#if utxo?.address && paymentKeyHash(utxo.address) && !signers.includes(paymentKeyHash(utxo.address)!)}<button class="y-btn h-8 text-xs" onclick={() => addSigner(paymentKeyHash(utxo!.address)!)}>UTxO owner</button>{/if}
					</div>
				</div>

				<div class="y-label mt-5 mb-2">Valid between slots</div>
				<div class="grid gap-2 sm:grid-cols-2">
					{#each [['from', 'Valid from'], ['to', 'Valid until']] as [which, name] (which)}
						{@const text = which === 'from' ? validFrom : validTo}
						{@const posix = posixOf(text)}
						<div>
							<div class="flex items-center gap-2">
								<input class="{field} min-w-0 flex-1" value={text} oninput={(e) => (which === 'from' ? (validFrom = e.currentTarget.value) : (validTo = e.currentTarget.value))} placeholder="{name} (slot, empty: no bound)" aria-label="{name} slot" />
								{#if tipSlot != null}<button class="y-btn h-8 px-2 text-xs" onclick={() => (which === 'from' ? (validFrom = String(tipSlot)) : (validTo = String(tipSlot! + 3600)))}>{which === 'from' ? 'tip' : 'tip + 1h'}</button>{/if}
							</div>
							{#if posix != null}<div class="mono mt-0.5 text-[11px] text-faint">{posix} ms · {new Date(Number(posix)).toISOString().replace('.000Z', 'Z')}</div>{/if}
						</div>
					{/each}
				</div>

				<div class="y-label mt-5 mb-2">Other withdrawals <span class="normal-case tracking-normal text-faint">· e.g. a withdraw-zero stake script the validator expects</span></div>
				{#each withdrawals as w, i (i)}
					<div class="mb-2 flex items-center gap-2">
						<input class="{field} min-w-0 flex-[2]" bind:value={w.credential} placeholder="stake address, or script:<hash>" aria-label="Withdrawal {i + 1} credential" spellcheck="false" />
						<input class="{field} min-w-0 flex-1" bind:value={w.amount} placeholder="lovelace" aria-label="Withdrawal {i + 1} amount" />
						<button class="y-icon-btn h-8 w-8" aria-label="Remove withdrawal {i + 1}" onclick={() => withdrawals.splice(i, 1)}><X class="h-3.5 w-3.5" /></button>
					</div>
				{/each}
				<button class="y-btn h-8 text-xs" onclick={() => withdrawals.push({ credential: '', amount: '0' })}><Plus class="h-3.5 w-3.5" />Add a withdrawal</button>

				<div class="y-label mt-5 mb-2">Outputs</div>
				{#each outputs as o, i (i)}
					<div class="mb-2 flex items-center gap-2">
						<input class="{field} min-w-0 flex-[2]" bind:value={o.address} placeholder="address" aria-label="Output {i + 1} address" spellcheck="false" />
						<input class="{field} min-w-0 flex-1" bind:value={o.lovelace} placeholder="lovelace" aria-label="Output {i + 1} lovelace" />
						<button class="y-icon-btn h-8 w-8" aria-label="Remove output {i + 1}" onclick={() => outputs.splice(i, 1)}><X class="h-3.5 w-3.5" /></button>
					</div>
				{/each}
				<div class="flex flex-wrap items-center gap-3">
					<button class="y-btn h-8 text-xs" onclick={() => outputs.push({ address: utxo?.address ?? '', lovelace: '2000000' })}><Plus class="h-3.5 w-3.5" />Add an output</button>
					<label class="ml-auto flex items-center gap-2 text-xs text-muted">Fee <input class="{field} w-28" bind:value={fee} aria-label="Fee in lovelace" /></label>
				</div>
			</Panel>
		</div>

		<!-- The verdict -->
		<div class="min-w-0 space-y-4 xl:sticky xl:top-20">
			<Panel title="Result">
				{#snippet actions()}{#if running || compiling || phase === 'starting'}<LoaderCircle class="h-4 w-4 animate-spin text-brand" aria-label="Running" />{/if}{/snippet}
				{#if runError}
					<div class="flex items-start gap-2 text-sm" data-verdict="invalid"><TriangleAlert class="mt-0.5 h-4 w-4 shrink-0 text-warn" /><span>{runError}</span></div>
				{:else if !script}
					<p class="text-sm text-muted">{mode === 'java' ? 'The result appears once the validator compiles.' : mode === 'cbor' ? 'Paste a compiled script.' : 'Load a script from this chain.'}</p>
				{:else if !result}
					<p class="text-sm text-muted" role="status">Running…</p>
				{:else if result.state !== 'done'}
					<div class="flex items-start gap-2 text-sm" data-verdict="invalid"><TriangleAlert class="mt-0.5 h-4 w-4 shrink-0 text-warn" /><span>{result.error}</span></div>
				{:else}
					{#key generation}
						<div class="flex items-start gap-3" data-verdict={accepted ? 'accepted' : 'rejected'}>
							{#if accepted}<CircleCheck class="h-9 w-9 shrink-0 text-ok" />{:else}<CircleX class="h-9 w-9 shrink-0 text-danger" />{/if}
							<div>
								<p class="font-display text-lg font-semibold">{accepted ? 'The validator accepts it' : outcome(result) === 'out of budget' ? 'Out of budget' : 'The validator rejects it'}</p>
								{#if !accepted && result.evaluation?.error}<p class="mt-0.5 text-sm text-muted">{result.evaluation.error}</p>{/if}
								{#if !accepted && failedAt}<button class="mt-1 text-left text-sm text-muted hover:text-ink" onclick={() => editor?.goto(failedAt!.line)}>Failed on <span class="font-medium text-danger">line {failedAt.line}</span>{#if failedAt.fragment}: <span class="mono text-xs">{failedAt.fragment.split('\n')[0]}</span>{/if}</button>{/if}
							</div>
						</div>
					{/key}
					<dl class="mt-4 grid grid-cols-2 gap-2.5">
						<div class="rounded-xl border border-line bg-surface-2/60 px-3 py-2"><dt class="y-label">CPU</dt><dd class="font-medium tabular-nums">{num(result.evaluation!.cpu)}</dd><dd class="text-[11px] text-faint">{share(result.evaluation!.cpu, limits.cpu)}</dd></div>
						<div class="rounded-xl border border-line bg-surface-2/60 px-3 py-2"><dt class="y-label">Memory</dt><dd class="font-medium tabular-nums">{num(result.evaluation!.mem)}</dd><dd class="text-[11px] text-faint">{share(result.evaluation!.mem, limits.mem)}</dd></div>
					</dl>
					{#if result.evaluation?.traces?.length}
						<div class="y-label mt-4 mb-1">Traces</div>
						<ul class="mono space-y-0.5 text-xs">{#each result.evaluation.traces as t, i (i)}<li>{t}</li>{/each}</ul>
					{/if}
					{#if result.plan.notes.length}<ul class="mt-3 space-y-1 text-xs text-faint">{#each result.plan.notes as n (n)}<li>{n}</li>{/each}</ul>{/if}
					<div class="mt-4 flex flex-wrap gap-2">
						{#if result.args}<button class="y-btn h-8 text-xs" onclick={() => openDebugger('uplc')}><Bug class="h-3.5 w-3.5" />Debug UPLC</button>{/if}
						{#if mode === 'java' && compiled?.compiledCode}<button class="y-btn h-8 text-xs" onclick={() => openDebugger('java')}><Coffee class="h-3.5 w-3.5" />Debug the Java</button>{/if}
						{#if result.args}<button class="y-btn h-8 text-xs" onclick={exportFixture}><Download class="h-3.5 w-3.5" />Test fixture</button>{/if}
					</div>
				{/if}
			</Panel>
			<p class="px-1 text-xs text-faint">Runs in your browser with this chain’s current cost model and limits. The transaction is built from your inputs; it isn’t signed or submitted.</p>
		</div>
	</div>

	{#if debugging && result?.args}
		<section id="lab-debugger" class="mt-6 scroll-mt-20">
			<div class="mb-3 flex items-center justify-between">
				<h2 class="font-display text-[15px] font-semibold">{debugging === 'java' ? 'Java source' : 'UPLC'} debugger</h2>
				<button class="y-btn h-8 text-xs" onclick={() => (debugging = null)}><X class="h-3.5 w-3.5" />Close</button>
			</div>
			{#key generation}
				{#if debugging === 'java'}
					<JavaDebugger {source} params={script?.params ?? []} plan={result.plan} />
				{:else}
					<Debugger page={fakePage} purpose={result.plan.purpose} index={result.plan.index} prepared={{ plan: result.plan, result }} syncUrl={false} />
				{/if}
			{/key}
		</section>
	{/if}
{/if}
