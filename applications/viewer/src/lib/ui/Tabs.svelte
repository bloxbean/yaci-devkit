<script module lang="ts">
export interface Tab {
	id: string;
	label: string;
	count?: number | null;
}
</script>

<script lang="ts">
	import { goto } from '$app/navigation';
	import { page } from '$app/state';


	/* Underlined tabs. With `param`, the active tab is kept in the URL so it survives reloads and can be shared. */
	let {
		tabs,
		active = $bindable(),
		param = undefined
	}: { tabs: Tab[]; active: string; param?: string } = $props();

	function select(id: string) {
		active = id;
		if (param) {
			const u = new URL(page.url);
			u.searchParams.set(param, id);
			goto(u.pathname + u.search, { replaceState: true, noScroll: true, keepFocus: true });
		}
	}
</script>

<div class="-mb-px flex gap-1 overflow-x-auto border-b border-line px-3" role="tablist">
	{#each tabs as t (t.id)}
		<button
			role="tab"
			aria-selected={active === t.id}
			class="relative flex shrink-0 items-center gap-2 whitespace-nowrap px-3 py-3 text-sm font-medium transition-colors {active === t.id
				? 'text-ink'
				: 'text-muted hover:text-ink'}"
			onclick={() => select(t.id)}
		>
			{t.label}
			{#if t.count != null}
				<span class="rounded-full px-1.5 text-[11px] tabular-nums {active === t.id ? 'bg-brand/15 text-brand' : 'bg-surface-2 text-faint'}">{t.count}</span>
			{/if}
			{#if active === t.id}<span class="absolute inset-x-2 -bottom-px h-0.5 rounded-full bg-grad"></span>{/if}
		</button>
	{/each}
</div>
