<script lang="ts">
	import { decodePlutusData, encodePlutusData, type PlutusData } from '$lib/cbor';
	import RotateCcw from '@lucide/svelte/icons/rotate-ccw';

	/*
	 * Edit Plutus data (a redeemer or datum) as JSON in the detailed schema ({constructor, fields} | {int} | {bytes} |
	 * {list} | {map: [{k, v}]}) or as raw CBOR hex. Valid input is reported as CBOR through `onchange`.
	 */
	let { value, original, label, onchange }: { value: string; original: string; label: string; onchange: (cbor: string | null) => void } = $props();

	let mode = $state<'json' | 'cbor'>('json');
	let text = $state('');
	let error = $state<string | null>(null);
	let shown = '';

	/** JSON for editing: integers as numbers where that is exact. */
	function toEditable(d: PlutusData): unknown {
		if ('int' in d) {
			const n = Number(d.int);
			return { int: Number.isSafeInteger(n) ? n : d.int };
		}
		if ('bytes' in d) return d;
		if ('list' in d) return { list: d.list.map(toEditable) };
		if ('map' in d) return { map: d.map.map((e) => ({ k: toEditable(e.k), v: toEditable(e.v) })) };
		return { constructor: d.constructor, fields: d.fields.map(toEditable) };
	}

	function fromEditable(x: unknown, path = '$'): PlutusData {
		if (!x || typeof x !== 'object' || Array.isArray(x)) throw new Error(`${path}: expected an object`);
		const o = x as Record<string, unknown>;
		const keys = Object.keys(o);
		if ('int' in o) {
			if (typeof o.int !== 'number' && typeof o.int !== 'string') throw new Error(`${path}.int: expected a number`);
			if (!/^-?\d+$/.test(String(o.int))) throw new Error(`${path}.int: “${o.int}” is not an integer`);
			return { int: String(BigInt(String(o.int))) };
		}
		if ('bytes' in o) {
			if (typeof o.bytes !== 'string' || !/^([0-9a-fA-F]{2})*$/.test(o.bytes)) throw new Error(`${path}.bytes: expected hex`);
			return { bytes: o.bytes.toLowerCase() };
		}
		if ('list' in o) {
			if (!Array.isArray(o.list)) throw new Error(`${path}.list: expected an array`);
			return { list: o.list.map((v, i) => fromEditable(v, `${path}.list[${i}]`)) };
		}
		if ('map' in o) {
			if (!Array.isArray(o.map)) throw new Error(`${path}.map: expected an array of {k, v}`);
			return { map: o.map.map((e, i) => ({ k: fromEditable((e as { k: unknown })?.k, `${path}.map[${i}].k`), v: fromEditable((e as { v: unknown })?.v, `${path}.map[${i}].v`) })) };
		}
		if (keys.includes('constructor')) {
			const c = o.constructor;
			if (typeof c !== 'number' || !Number.isInteger(c) || c < 0) throw new Error(`${path}.constructor: expected a non-negative integer`);
			if (!Array.isArray(o.fields)) throw new Error(`${path}.fields: expected an array`);
			return { constructor: c, fields: o.fields.map((v, i) => fromEditable(v, `${path}.fields[${i}]`)) };
		}
		throw new Error(`${path}: expected constructor, int, bytes, list or map`);
	}

	function render(cbor: string) {
		try {
			text = mode === 'json' ? JSON.stringify(toEditable(decodePlutusData(cbor)), null, 1) : cbor;
		} catch {
			text = cbor;
			mode = 'cbor';
		}
		shown = cbor;
		error = null;
	}

	// Show the value when it changes from outside (reset, or first render).
	$effect(() => {
		if (value !== shown) render(value);
	});

	function input(next: string) {
		text = next;
		try {
			const cbor = mode === 'json' ? encodePlutusData(fromEditable(JSON.parse(next))) : (decodePlutusData(next.trim()), next.trim().toLowerCase());
			error = null;
			shown = cbor;
			onchange(cbor === original ? null : cbor);
		} catch (e) {
			error = (e as Error).message.replace(/^JSON\.parse: /, '');
		}
	}

	function switchTo(m: 'json' | 'cbor') {
		if (error) return;
		mode = m;
		render(shown || value);
	}

	const edited = $derived(value !== original);
</script>

<div class="overflow-hidden rounded-xl border {error ? 'border-danger/50' : edited ? 'border-brand/50' : 'border-line'} bg-bg-2">
	<div class="flex items-center gap-2 border-b border-line px-3 py-1.5">
		<span class="y-label">{label}</span>
		{#if edited}<span class="rounded bg-brand/15 px-1.5 text-[10px] font-semibold uppercase text-brand">edited</span>{/if}
		<div class="ml-auto flex items-center gap-1">
			{#if edited}
				<button class="flex items-center gap-1 rounded-md px-1.5 py-0.5 text-xs text-muted hover:text-ink" title="Back to the on-chain value" onclick={() => { onchange(null); render(original); }}><RotateCcw class="h-3 w-3" />Reset</button>
			{/if}
			<div class="flex items-center gap-0.5 rounded-lg bg-surface-2 p-0.5 text-xs">
				{#each [['json', 'JSON'], ['cbor', 'CBOR']] as [id, name] (id)}
					<button class="rounded-md px-2 py-0.5 transition {mode === id ? 'bg-surface text-ink shadow-sm' : 'text-muted hover:text-ink'}" disabled={!!error && mode !== id} onclick={() => switchTo(id as 'json' | 'cbor')}>{name}</button>
				{/each}
			</div>
		</div>
	</div>
	<textarea
		class="mono block max-h-72 min-h-20 w-full resize-y bg-transparent p-3 text-[12px] leading-5 text-ink outline-none"
		rows={Math.min(14, Math.max(3, text.split('\n').length))}
		spellcheck="false"
		aria-label={label}
		value={text}
		oninput={(e) => input(e.currentTarget.value)}
	></textarea>
	{#if error}<p class="border-t border-danger/30 bg-danger/5 px-3 py-1.5 text-xs text-danger">{error}</p>{/if}
</div>
