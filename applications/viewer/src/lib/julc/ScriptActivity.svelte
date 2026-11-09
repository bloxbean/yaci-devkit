<script lang="ts">
	import { num, shorten } from '$lib/format';
	import { tryDecodePlutusData, type PlutusData } from '$lib/cbor';
	import Panel from '$lib/ui/Panel.svelte';
	import PlutusView from '$lib/ui/PlutusView.svelte';
	import { purposeOf } from './replay/build';
	import type { ScriptActivity } from './activity';
	import ActivityIcon from '@lucide/svelte/icons/activity';
	import Bug from '@lucide/svelte/icons/bug';

	/*
	 * How a script is used: its recent executions with their declared budgets, and which redeemer variants call it.
	 * `variantName` lets a blueprint name the variants.
	 */
	let { activity, variantName = undefined }: { activity: ScriptActivity; variantName?: (cbor: string) => string | null } = $props();

	const runs = $derived(activity.executions);
	const steps = $derived(runs.map((r) => Number(r.unit_steps)));
	const mems = $derived(runs.map((r) => Number(r.unit_mem)));
	const median = (xs: number[]) => {
		if (!xs.length) return 0;
		const s = [...xs].sort((a, b) => a - b);
		return s[Math.floor(s.length / 2)];
	};
	const peakPct = $derived(Math.max(0, ...runs.map((r) => Math.max((Number(r.unit_steps) / activity.limits.steps) * 100, (Number(r.unit_mem) / activity.limits.mem) * 100))));
	const purposes = $derived(
		Object.entries(runs.reduce<Record<string, number>>((m, r) => ((m[purposeOf(r.purpose) ?? r.purpose] = (m[purposeOf(r.purpose) ?? r.purpose] ?? 0) + 1), m), {})).sort((a, b) => b[1] - a[1])
	);

	/** The redeemer's top-level shape: its constructor (named by a blueprint when one is attached), or its kind. */
	function shapeOf(cbor: string | undefined): string {
		if (!cbor) return 'unknown';
		const named = variantName?.(cbor);
		if (named) return named;
		const d: PlutusData | null = tryDecodePlutusData(cbor);
		if (!d) return 'unknown';
		if (typeof (d as { constructor: unknown }).constructor === 'number') return `Constr ${(d as { constructor: number }).constructor}`;
		return Object.keys(d)[0];
	}
	const variants = $derived.by(() => {
		const groups = new Map<string, { count: number; sample: string | undefined }>();
		for (const r of runs) {
			const cbor = r.redeemer_data_hash ? activity.redeemers[r.redeemer_data_hash] : undefined;
			const name = shapeOf(cbor);
			const g = groups.get(name) ?? { count: 0, sample: cbor };
			g.count++;
			groups.set(name, g);
		}
		return [...groups.entries()].sort((a, b) => b[1].count - a[1].count);
	});
	let openVariant = $state<string | null>(null);

	// Declared CPU per execution, oldest on the left.
	const chart = $derived([...runs].reverse());
	const maxSteps = $derived(Math.max(1, ...steps));
</script>

<Panel title="Activity" sub={runs.length ? `The last ${runs.length} execution${runs.length === 1 ? '' : 's'}, newest first` : 'No executions recorded by this store'}>
	{#snippet icon()}<ActivityIcon class="h-4 w-4" />{/snippet}
	{#if runs.length}
		<div class="mb-5 grid grid-cols-2 gap-2.5 sm:grid-cols-4">
			{#each [{ l: 'Executions', v: num(runs.length), s: purposes.map(([p, n]) => `${n} ${p}`).join(' · ') }, { l: 'Median CPU', v: num(median(steps)), s: `max ${num(Math.max(...steps))}` }, { l: 'Median memory', v: num(median(mems)), s: `max ${num(Math.max(...mems))}` }, { l: 'Peak of a tx limit', v: `${peakPct.toFixed(1)}%`, s: 'CPU or memory, declared' }] as f (f.l)}
				<div class="rounded-xl border border-line bg-surface-2/60 px-3.5 py-2.5">
					<div class="y-label">{f.l}</div>
					<div class="mt-0.5 font-medium tabular-nums">{f.v}</div>
					<div class="truncate text-[11px] text-faint">{f.s}</div>
				</div>
			{/each}
		</div>

		<div class="y-label mb-2">Declared CPU per execution</div>
		<svg class="mb-5 h-20 w-full" viewBox="0 0 {chart.length * 6} 80" preserveAspectRatio="none" role="img" aria-label="Declared CPU of each execution">
			{#each chart as r, i}
				{@const h = Math.max(2, (Number(r.unit_steps) / maxSteps) * 76)}
				<rect x={i * 6 + 1} y={80 - h} width="4" height={h} rx="1" class="fill-brand/70 hover:fill-brand"><title>{num(r.unit_steps)} CPU · {num(r.unit_mem)} mem · tx {shorten(r.tx_hash, 8, 6)}</title></rect>
			{/each}
		</svg>

		<div class="y-label mb-2">Redeemer variants</div>
		<ul class="mb-5 space-y-1.5">
			{#each variants as [name, g] (name)}
				<li>
					<button class="flex w-full items-center gap-3 text-left text-sm" onclick={() => (openVariant = openVariant === name ? null : name)}>
						<span class="mono w-40 shrink-0 truncate font-medium">{name}</span>
						<span class="h-2 flex-1 overflow-hidden rounded-full bg-line"><span class="block h-full rounded-full bg-violet" style="width:{(g.count / runs.length) * 100}%"></span></span>
						<span class="w-20 shrink-0 text-right text-xs tabular-nums text-muted">{g.count} · {Math.round((g.count / runs.length) * 100)}%</span>
					</button>
					{#if openVariant === name && g.sample}<div class="mt-2"><PlutusView cbor={g.sample} label="Example redeemer" /></div>{/if}
				</li>
			{/each}
		</ul>

		<div class="y-label mb-2">Recent executions</div>
		<div class="overflow-x-auto">
			<table class="w-full text-sm">
				<thead><tr class="border-b border-line text-left"><th class="y-label py-2 pr-4">Transaction</th><th class="y-label py-2 pr-4">Purpose</th><th class="y-label py-2 pr-4">Redeemer</th><th class="y-label py-2 pr-4 text-right">CPU</th><th class="y-label py-2 pr-4 text-right">Memory</th><th class="py-2"></th></tr></thead>
				<tbody>
					{#each runs.slice(0, 25) as r (r.tx_hash + r.purpose + r.tx_index)}
						{@const purpose = purposeOf(r.purpose) ?? r.purpose}
						<tr class="border-b border-line/60 last:border-0">
							<td class="py-2 pr-4"><a class="mono y-link" href="/transactions/{r.tx_hash}?tab=scripts">{shorten(r.tx_hash, 8, 6)}</a></td>
							<td class="py-2 pr-4">{purpose} #{r.tx_index}</td>
							<td class="mono py-2 pr-4 text-xs text-muted">{shapeOf(r.redeemer_data_hash ? activity.redeemers[r.redeemer_data_hash] : undefined)}</td>
							<td class="py-2 pr-4 text-right tabular-nums">{num(r.unit_steps)}</td>
							<td class="py-2 pr-4 text-right tabular-nums">{num(r.unit_mem)}</td>
							<td class="py-2 text-right"><a class="inline-flex items-center gap-1 text-xs text-muted hover:text-brand" href="/transactions/{r.tx_hash}/debug/{purpose}/{r.tx_index}" title="Debug this execution"><Bug class="h-3.5 w-3.5" />Debug</a></td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
		<p class="mt-3 text-xs text-faint">Budgets are as declared in each transaction. Re-run one from its Scripts tab, or open it in the debugger, to see what it actually used.</p>
	{/if}
</Panel>
