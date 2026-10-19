<script lang="ts">
	import { actionTitle, drepName, poolName, scriptName } from './names.svelte';

	/*
	 * The name of an id, shown next to it (never instead of it), on Mainnet when one is known; nothing otherwise.
	 *  - script: a script hash, or an address / stake address whose payment (stake) part is a script. Labels come
	 *    from curated dApp registries.
	 *  - drep, pool, action: names the owners chose in their own metadata. They match the hash on chain, but anyone
	 *    can pick any name, so they are labelled as self-declared.
	 * `block` puts an action title on its own line and `wrap` lets it wrap (titles are long); the other kinds are small pills.
	 */
	let {
		kind,
		id,
		block = false,
		wrap = false,
		class: klass = ''
	}: { kind: 'script' | 'drep' | 'pool' | 'action'; id: string | null | undefined; block?: boolean; wrap?: boolean; class?: string } = $props();

	const tag = $derived.by(() => {
		if (kind === 'script') {
			const s = scriptName(id);
			const from = s?.source === 'strica' ? "Strica's Cardano Contracts Registry" : "Cardano Fans' dApp registry";
			return s ? { text: s.label, title: `${s.project}${s.category ? ` · ${s.category.toLowerCase().replaceAll('_', ' ')}` : ''} — dApp label from ${from}` } : null;
		}
		if (kind === 'drep') {
			const n = drepName(id);
			return n ? { text: n, title: `${n} — name from the DRep's own metadata (CIP-119); self-declared, not verified` } : null;
		}
		if (kind === 'pool') {
			const p = poolName(id);
			const text = p?.ticker ?? p?.name;
			return text ? { text, title: `${[p?.ticker, p?.name].filter(Boolean).join(' · ')} — from the pool's own metadata; self-declared, not verified` } : null;
		}
		const t = actionTitle(id);
		return t ? { text: t, title: `${t} — title from the action's own metadata (CIP-108)` } : null;
	});
	const tone = { script: 'border-violet/25 bg-violet/10 text-violet', drep: 'border-blue/25 bg-blue/10 text-blue', pool: 'border-cyan/25 bg-cyan/10 text-cyan', action: '' };
</script>

{#if tag}
	{#if kind === 'action'}
		<span class="{block ? 'block' : 'inline'} min-w-0 {wrap ? '' : 'truncate'} text-sm text-ink {klass}" title={tag.title} data-name="action">{tag.text}</span>
	{:else}
		<span
			class="inline-flex max-w-[16rem] shrink-0 items-center truncate rounded-md border px-1.5 py-px align-middle text-[11px] font-medium leading-4 {tone[kind]} {klass}"
			title={tag.title}
			data-name={kind}>{tag.text}</span
		>
	{/if}
{/if}
