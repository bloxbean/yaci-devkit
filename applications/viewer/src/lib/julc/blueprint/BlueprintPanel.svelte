<script lang="ts">
	import Panel from '$lib/ui/Panel.svelte';
	import { blueprints } from './store.svelte';
	import { typeNameOf } from './blueprint';
	import FileJson from '@lucide/svelte/icons/file-braces';
	import Upload from '@lucide/svelte/icons/upload';
	import X from '@lucide/svelte/icons/x';
	import CircleCheck from '@lucide/svelte/icons/circle-check';

	/*
	 * Attach a CIP-57 blueprint (plutus.json, e.g. from `aiken build`). It is kept in this browser; datums and
	 * redeemers of its validators are then shown with type, variant and field names across the viewer.
	 */
	let { hash }: { hash: string } = $props();

	const matched = $derived(blueprints.list.flatMap((b) => b.validators.filter((v) => v.hash === hash).map((v) => ({ blueprint: b, validator: v }))));
	let error = $state<string | null>(null);
	let added = $state<string | null>(null);
	let input = $state<HTMLInputElement>();

	async function attach(file: File | undefined) {
		error = null;
		added = null;
		if (!file) return;
		try {
			const bp = blueprints.add(await file.text());
			added = bp.validators.some((v) => v.hash === hash) ? null : `Saved “${bp.title}”, but none of its validators has this script’s hash.`;
		} catch (e) {
			error = (e as Error).message;
		}
	}


</script>

<Panel title="Blueprint" sub={matched.length ? `From “${matched[0].blueprint.title}”${matched[0].blueprint.version ? ` ${matched[0].blueprint.version}` : ''}` : 'Name this script’s data with its CIP-57 blueprint (plutus.json)'}>
	{#snippet icon()}<FileJson class="h-4 w-4" />{/snippet}
	{#snippet actions()}
		<input bind:this={input} type="file" accept=".json,application/json" class="hidden" onchange={(e) => attach(e.currentTarget.files?.[0])} />
		<button class="y-btn h-8 px-2.5 text-xs" onclick={() => input?.click()}><Upload class="h-3.5 w-3.5" />Attach plutus.json</button>
	{/snippet}
	{#if matched.length}
		<ul class="space-y-2 text-sm">
			{#each matched as m (m.blueprint.title + m.validator.title)}
				<li class="flex flex-wrap items-center gap-x-3 gap-y-1">
					<CircleCheck class="h-4 w-4 text-mint" />
					<span class="mono font-medium">{m.validator.title}</span>
					{#if m.validator.purpose}<span class="y-pill border-line text-muted">{m.validator.purpose}</span>{/if}
					{#if m.validator.redeemer}<span class="text-xs text-muted">redeemer <b class="font-medium text-ink">{typeNameOf(m.validator.redeemer, m.blueprint.definitions)}</b></span>{/if}
					{#if m.validator.datum}<span class="text-xs text-muted">datum <b class="font-medium text-ink">{typeNameOf(m.validator.datum, m.blueprint.definitions)}</b></span>{/if}
					{#if m.validator.parameters.length}<span class="text-xs text-muted">{m.validator.parameters.length} parameter{m.validator.parameters.length === 1 ? '' : 's'}</span>{/if}
				</li>
			{/each}
		</ul>
		<p class="mt-3 text-xs text-faint">Redeemers and datums of this script show a Typed view on transaction pages, and its activity names redeemer variants.</p>
	{:else}
		<p class="text-sm text-muted">No attached blueprint describes this script. Attach the <span class="mono">plutus.json</span> your contract build produces; it stays in this browser.</p>
	{/if}
	{#if error}<p class="mt-2 text-sm text-danger">{error}</p>{/if}
	{#if added}<p class="mt-2 text-sm text-amber">{added}</p>{/if}
	{#if blueprints.list.length}
		<div class="mt-4 border-t border-line pt-3">
			<div class="y-label mb-2">Attached in this browser</div>
			<div class="flex flex-wrap gap-2">
				{#each blueprints.list as b (b.title)}
					<span class="y-pill border-line text-muted">{b.title} · {b.validators.length} validator{b.validators.length === 1 ? '' : 's'}
						<button class="ml-0.5 text-faint hover:text-danger" title="Remove" aria-label="Remove blueprint {b.title}" onclick={() => blueprints.remove(b.title)}><X class="h-3 w-3" /></button></span
					>
				{/each}
			</div>
		</div>
	{/if}
</Panel>
