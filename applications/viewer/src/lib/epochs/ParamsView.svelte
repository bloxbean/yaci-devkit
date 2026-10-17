<script lang="ts">
	import { ada, num } from '$lib/format';
	import JsonView from '$lib/ui/JsonView.svelte';
	import Empty from '$lib/ui/Empty.svelte';
	import CopyButton from '$lib/ui/CopyButton.svelte';
	import { groupParams, type ParamRow } from './params';
	import Search from '@lucide/svelte/icons/search';
	import ChevronDown from '@lucide/svelte/icons/chevron-down';

	/* Protocol parameters in readable groups, with a filter and the cost models tucked away. */
	let { params }: { params: Record<string, unknown> } = $props();

	let filter = $state('');
	let openModels = $state<string | null>(null);

	const groups = $derived(groupParams(params));
	const q = $derived(filter.trim().toLowerCase());
	const shown = $derived(
		groups
			.map((grp) => ({
				...grp,
				rows: q ? grp.rows.filter((r) => r.label.toLowerCase().includes(q) || r.key.includes(q) || grp.label.toLowerCase().includes(q)) : grp.rows
			}))
			.filter((grp) => grp.rows.length)
	);
	const models = $derived(
		params.cost_models && typeof params.cost_models === 'object' ? Object.entries(params.cost_models as Record<string, unknown>) : []
	);

	function show(r: ParamRow): string {
		const v = r.value;
		switch (r.unit) {
			case 'lovelace':
				return `₳${ada(v as string | number, 'auto')}`;
			case 'percent':
				return `${(Number(v) * 100).toFixed(Number(v) * 100 % 1 ? 1 : 0)}%`;
			case 'bytes':
				return `${num(v as number)} B`;
			case 'epochs':
				return `${num(v as number)} epoch${Number(v) === 1 ? '' : 's'}`;
			case 'number':
				return typeof v === 'number' && !Number.isInteger(v) ? String(v) : num(v as number | string);
			default:
				return String(v);
		}
	}

	const count = (m: unknown) => (Array.isArray(m) ? m.length : m && typeof m === 'object' ? Object.keys(m).length : 0);
</script>

<div class="mb-4 flex items-center gap-2">
	<div class="relative w-full max-w-sm">
		<Search class="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-faint" />
		<input class="y-input pl-9" placeholder="Filter parameters…" bind:value={filter} aria-label="Filter parameters" />
	</div>
	<span class="text-xs text-faint">{shown.reduce((s, grp) => s + grp.rows.length, 0)} shown</span>
</div>

{#if !shown.length && !models.length}
	<Empty title="No matching parameters" />
{:else}
	<div class="grid gap-4 lg:grid-cols-2">
		{#each shown as grp (grp.label)}
			<section class="rounded-2xl border border-line bg-surface">
				<h3 class="border-b border-line px-4 py-2.5 font-display text-sm font-semibold">{grp.label}</h3>
				<dl class="divide-y divide-line/70">
					{#each grp.rows as r (r.key)}
						<div class="group flex items-center justify-between gap-4 px-4 py-2.5">
							<dt class="min-w-0">
								<div class="truncate text-[13px] text-ink">{r.label}</div>
								<div class="mono truncate text-[10.5px] text-faint">{r.key}{r.hint ? ` · ${r.hint}` : ''}</div>
							</dt>
							<dd class="flex shrink-0 items-center gap-1">
								<span class="mono max-w-[16rem] truncate text-[13px] font-medium tabular-nums" title={String(r.value)}>{show(r)}</span>
								<span class="opacity-0 transition group-hover:opacity-100"><CopyButton value={String(r.value)} size="xs" /></span>
							</dd>
						</div>
					{/each}
				</dl>
			</section>
		{/each}
	</div>
{/if}

{#if models.length && (!q || 'cost models'.includes(q) || models.some(([k]) => k.toLowerCase().includes(q)))}
	<section class="mt-4 rounded-2xl border border-line bg-surface">
		<h3 class="border-b border-line px-4 py-2.5 font-display text-sm font-semibold">Plutus cost models</h3>
		<ul class="divide-y divide-line/70">
			{#each models as [lang, model] (lang)}
				<li>
					<button class="flex w-full items-center justify-between px-4 py-2.5 text-left text-[13px] hover:bg-surface-2/60" onclick={() => (openModels = openModels === lang ? null : lang)} aria-expanded={openModels === lang}>
						<span class="font-medium">{lang}</span>
						<span class="flex items-center gap-2 text-xs text-faint">{count(model)} entries <ChevronDown class="h-3.5 w-3.5 transition {openModels === lang ? 'rotate-180' : ''}" /></span>
					</button>
					{#if openModels === lang}<div class="px-4 pb-4"><JsonView value={model} label={lang} maxHeight={360} /></div>{/if}
				</li>
			{/each}
		</ul>
	</section>
{/if}
