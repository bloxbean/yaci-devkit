<script lang="ts">
	import CborTree from './CborTree.svelte';
	import { bytesToHex, type CborItem } from './cbor-item';
	import { addressText } from './decode';
	import ChevronRight from '@lucide/svelte/icons/chevron-right';

	/*
	 * One node of a transaction's CBOR, labelled with the ledger's field names where the position is known (body
	 * keys, witness set keys, output keys), with its byte range. Children render only when expanded.
	 */
	type Context = 'tx' | 'body' | 'witnesses' | 'outputs' | 'output' | 'legacyOutput' | 'none';
	let { item, label = null, context = 'none', depth = 0 }: { item: CborItem; label?: string | null; context?: Context; depth?: number } = $props();

	const BODY: Record<number, [string, Context?]> = {
		0: ['inputs'],
		1: ['outputs', 'outputs'],
		2: ['fee'],
		3: ['ttl (valid until slot)'],
		4: ['certificates'],
		5: ['withdrawals'],
		6: ['update'],
		7: ['auxiliary data hash'],
		8: ['validity start slot'],
		9: ['mint'],
		11: ['script data hash'],
		13: ['collateral inputs'],
		14: ['required signers'],
		15: ['network id'],
		16: ['collateral return', 'output'],
		17: ['total collateral'],
		18: ['reference inputs'],
		19: ['voting procedures'],
		20: ['proposal procedures'],
		21: ['current treasury value'],
		22: ['donation']
	};
	const WITNESS: Record<number, string> = { 0: 'vkey witnesses', 1: 'native scripts', 2: 'bootstrap witnesses', 3: 'Plutus V1 scripts', 4: 'Plutus data (datums)', 5: 'redeemers', 6: 'Plutus V2 scripts', 7: 'Plutus V3 scripts' };
	const OUTPUT: Record<number, string> = { 0: 'address', 1: 'value', 2: 'datum', 3: 'script reference' };
	const TX = ['transaction body', 'witness set', 'is valid', 'auxiliary data'];

	// svelte-ignore state_referenced_locally
	let open = $state(depth < 2);

	const children = $derived.by((): { item: CborItem; label: string | null; context: Context }[] => {
		if (item.type === 'array')
			return item.items.map((x, i) => {
				if (context === 'tx') return { item: x, label: TX[i] ?? `[${i}]`, context: i === 0 ? 'body' : i === 1 ? 'witnesses' : 'none' };
				if (context === 'outputs') return { item: x, label: `output #${i}`, context: x.type === 'array' ? 'legacyOutput' : 'output' };
				if (context === 'legacyOutput') return { item: x, label: ['address', 'value', 'datum hash'][i] ?? `[${i}]`, context: 'none' };
				return { item: x, label: `[${i}]`, context: 'none' };
			});
		if (item.type === 'map')
			return item.entries.map((e) => {
				const k = e.k.type === 'uint' ? Number(e.k.value) : null;
				if (context === 'body' && k != null && BODY[k]) return { item: e.v, label: `${k} · ${BODY[k][0]}`, context: BODY[k][1] ?? 'none' };
				if (context === 'witnesses' && k != null && WITNESS[k]) return { item: e.v, label: `${k} · ${WITNESS[k]}`, context: 'none' };
				if (context === 'output' && k != null && OUTPUT[k]) return { item: e.v, label: `${k} · ${OUTPUT[k]}`, context: 'none' };
				return { item: e.v, label: keyText(e.k), context: 'none' };
			});
		if (item.type === 'tag') return [{ item: item.item, label: null, context }];
		return [];
	});

	function keyText(k: CborItem): string {
		if (k.type === 'uint' || k.type === 'nint') return String(k.value);
		if (k.type === 'text') return JSON.stringify(k.value);
		if (k.type === 'bytes') return short(bytesToHex(k.value));
		if (k.type === 'array') return `[${k.items.map(keyText).join(', ')}]`;
		return k.type;
	}
	const short = (h: string) => (h.length > 24 ? `${h.slice(0, 16)}…${h.slice(-8)}` : h);
	const isAddress = $derived((label?.endsWith('address') ?? false) && item.type === 'bytes');

	const TAGS: Record<number, string> = { 24: 'embedded CBOR', 30: 'rational', 258: 'set', 259: 'auxiliary data', 121: 'Constr 0', 122: 'Constr 1', 123: 'Constr 2' };
	const summary = $derived.by(() => {
		switch (item.type) {
			case 'uint':
			case 'nint':
				return { kind: 'int', value: String(item.value) };
			case 'bytes':
				return { kind: `bytes · ${item.value.length}`, value: isAddress ? safeAddress(item.value) : short(bytesToHex(item.value)) };
			case 'text':
				return { kind: 'text', value: JSON.stringify(item.value.length > 80 ? item.value.slice(0, 80) + '…' : item.value) };
			case 'array':
				return { kind: `array · ${item.items.length}${item.indefinite ? ' (indefinite)' : ''}`, value: '' };
			case 'map':
				return { kind: `map · ${item.entries.length}${item.indefinite ? ' (indefinite)' : ''}`, value: '' };
			case 'tag':
				return { kind: `tag ${item.tag}${TAGS[item.tag] ? ` · ${TAGS[item.tag]}` : ''}`, value: '' };
			case 'simple':
				return { kind: 'simple', value: String(item.value) };
			default:
				return { kind: 'float', value: String(item.value) };
		}
	});
	function safeAddress(b: Uint8Array) {
		try {
			return addressText(b);
		} catch {
			return short(bytesToHex(b));
		}
	}
	const expandable = $derived(children.length > 0);
</script>

<div class="text-[12.5px] leading-6">
	<div class="flex min-w-0 items-baseline gap-2">
		{#if expandable}
			<button class="flex shrink-0 items-center text-muted hover:text-ink" onclick={() => (open = !open)} aria-expanded={open} aria-label="{open ? 'Collapse' : 'Expand'} {label ?? summary.kind}">
				<ChevronRight class="h-3.5 w-3.5 transition {open ? 'rotate-90' : ''}" />
			</button>
		{:else}
			<span class="w-3.5 shrink-0"></span>
		{/if}
		{#if label}<span class="shrink-0 font-medium">{label}</span>{/if}
		<span class="shrink-0 text-faint">{summary.kind}</span>
		{#if summary.value}<span class="mono min-w-0 truncate {isAddress ? 'text-brand' : 'text-ink'}" title={summary.value}>{summary.value}</span>{/if}
		<span class="mono ml-auto shrink-0 pl-2 text-[10.5px] text-faint tabular-nums" title="Byte range in the transaction">{item.start}–{item.end}</span>
	</div>
	{#if open && expandable}
		<div class="ml-[7px] border-l border-line pl-3">
			{#each children as c, i (i)}<CborTree item={c.item} label={c.label} context={c.context} depth={depth + 1} />{/each}
		</div>
	{/if}
</div>
