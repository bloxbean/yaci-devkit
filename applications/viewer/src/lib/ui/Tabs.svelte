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


	/*
	 * Underlined tabs. With `param`, the active tab is kept in the URL so it survives reloads and can be shared.
	 * With `link`, tabs are plain links to ?param=id, for pages whose server load depends on the tab.
	 */
	let {
		tabs,
		active = $bindable(),
		param = undefined,
		link = false
	}: { tabs: Tab[]; active: string; param?: string; link?: boolean } = $props();

	function hrefFor(id: string) {
		const u = new URL(page.url);
		u.searchParams.set(param ?? 'tab', id);
		u.searchParams.delete('page');
		return u.pathname + u.search;
	}

	function select(id: string) {
		active = id;
		if (param) {
			const u = new URL(page.url);
			u.searchParams.set(param, id);
			goto(u.pathname + u.search, { replaceState: true, noScroll: true, keepFocus: true });
		}
	}
</script>

<div class="-mb-px flex gap-1 overflow-x-auto overflow-y-hidden border-b border-line px-3 [scrollbar-width:none]" role="tablist">
	{#each tabs as t (t.id)}
		{@const on = active === t.id}
		<svelte:element
			this={link ? 'a' : 'button'}
			href={link ? hrefFor(t.id) : undefined}
			data-sveltekit-noscroll={link ? '' : undefined}
			role="tab"
			tabindex="0"
			aria-selected={on}
			class="relative flex shrink-0 items-center gap-2 whitespace-nowrap px-3 py-3 text-sm font-medium transition-colors {on ? 'text-ink' : 'text-muted hover:text-ink'}"
			onclick={link ? undefined : () => select(t.id)}
		>
			{t.label}
			{#if t.count != null}
				<span class="rounded-full px-1.5 text-[11px] tabular-nums {on ? 'bg-brand/15 text-brand' : 'bg-surface-2 text-faint'}">{t.count}</span>
			{/if}
			{#if on}<span class="absolute inset-x-2 -bottom-px h-0.5 rounded-full bg-grad"></span>{/if}
		</svelte:element>
	{/each}
</div>
