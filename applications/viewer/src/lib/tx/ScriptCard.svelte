<script lang="ts">
	import type { Snippet } from 'svelte';
	import Hash from '$lib/ui/Hash.svelte';
	import Badge from '$lib/ui/Badge.svelte';
	import NameTag from '$lib/names/NameTag.svelte';
	import PlutusView from '$lib/ui/PlutusView.svelte';
	import { blueprints } from '$lib/julc/blueprint/store.svelte';
	import type { Schema } from '$lib/julc/blueprint/blueprint';
	import { purposeOf } from '$lib/julc/replay/build';
	import { num } from '$lib/format';
	import Cpu from '@lucide/svelte/icons/cpu';
	import MemoryStick from '@lucide/svelte/icons/memory-stick';

	/* A script run in this transaction: what it validated, its budget use, and its redeemer and datum. */
	interface Redeemer {
		tag?: string;
		index?: number;
		data?: string;
		ex_units?: { mem: number; steps: number };
	}
	let {
		script,
		maxMem = 14_000_000,
		maxSteps = 10_000_000_000,
		children = undefined
	}: {
		script: { script_hash: string; type?: string | null; redeemer?: string | Redeemer | null; datum?: string | null; datum_hash?: string | null };
		maxMem?: number;
		maxSteps?: number;
		/** Extra content at the bottom, e.g. a Script Lab re-run result. */
		children?: Snippet;
	} = $props();

	const r = $derived.by<Redeemer | null>(() => {
		if (!script.redeemer) return null;
		if (typeof script.redeemer === 'string') {
			try {
				return JSON.parse(script.redeemer);
			} catch {
				return null;
			}
		}
		return script.redeemer;
	});
	// A blueprint attached in this browser names the redeemer's and datum's types and fields.
	const bp = $derived(blueprints.find(script.script_hash, purposeOf(r?.tag)));
	const schemaOf = (s: Schema | null | undefined) =>
		bp && s ? { schema: s, definitions: bp.blueprint.definitions, source: `${bp.blueprint.title} · ${bp.validator.title}` } : null;

	const version = $derived((script.type ?? '').replace('PLUTUS_', 'Plutus ').replace('_', ' ').replace('NATIVE_SCRIPT', 'Native'));
	const memPct = $derived(r?.ex_units ? (r.ex_units.mem / maxMem) * 100 : 0);
	const stepPct = $derived(r?.ex_units ? (r.ex_units.steps / maxSteps) * 100 : 0);
	const purposeTone = { spend: 'brand', mint: 'violet', cert: 'cyan', reward: 'ok', vote: 'info', propose: 'warn' } as const;
</script>

<article class="rounded-2xl border border-line bg-surface p-4">
	<header class="flex flex-wrap items-center gap-2">
		{#if r?.tag}<Badge tone={purposeTone[r.tag as keyof typeof purposeTone] ?? 'neutral'}>{r.tag}{r.index != null ? ` #${r.index}` : ''}</Badge>{/if}
		{#if version}<Badge tone="neutral">{version}</Badge>{/if}
		<NameTag kind="script" id={script.script_hash} />
		<span class="ml-auto text-sm"><Hash value={script.script_hash} href="/scripts/{script.script_hash}" head={10} tail={8} /></span>
	</header>

	{#if r?.ex_units}
		<div class="mt-4 grid gap-3 sm:grid-cols-2">
			{#each [{ label: 'Memory', icon: MemoryStick, used: r.ex_units.mem, pct: memPct }, { label: 'CPU steps', icon: Cpu, used: r.ex_units.steps, pct: stepPct }] as u (u.label)}
				<div>
					<div class="mb-1 flex items-center justify-between text-xs">
						<span class="flex items-center gap-1.5 text-muted"><u.icon class="h-3.5 w-3.5" />{u.label}</span>
						<span class="tabular-nums"><b class="font-semibold">{num(u.used)}</b> <span class="text-faint">· {u.pct.toFixed(2)}% of tx limit</span></span>
					</div>
					<div class="h-1.5 overflow-hidden rounded-full bg-line">
						<div class="h-full rounded-full bg-grad" style="width:{Math.max(1, Math.min(100, u.pct))}%"></div>
					</div>
				</div>
			{/each}
		</div>
	{/if}

	<div class="mt-4 grid items-start gap-3 {script.datum ? 'xl:grid-cols-2' : ''}">
		{#if r?.data}<PlutusView cbor={r.data} label="Redeemer" schema={schemaOf(bp?.validator.redeemer)} />{/if}
		{#if script.datum}<PlutusView cbor={script.datum} label="Datum {script.datum_hash ? '· ' + script.datum_hash.slice(0, 8) + '…' : ''}" schema={schemaOf(bp?.validator.datum)} />{/if}
	</div>
	{#if children}{@render children()}{/if}
</article>
