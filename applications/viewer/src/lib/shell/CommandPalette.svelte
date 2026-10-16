<script lang="ts">
	import { goto } from '$app/navigation';
	import { tick } from 'svelte';
	import { classify, KIND_LABEL, SEARCH_HINT, type Candidate } from '$lib/search/classify';
	import { tryApi } from '$lib/api/client';
	import { shorten } from '$lib/format';
	import { ALL_PAGES } from './nav';
	import { shell } from './ui.svelte';
	import KindIcon from './KindIcon.svelte';
	import Search from '@lucide/svelte/icons/search';
	import CornerDownLeft from '@lucide/svelte/icons/corner-down-left';
	import LoaderCircle from '@lucide/svelte/icons/loader-circle';
	import Clock from '@lucide/svelte/icons/clock';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';

	/*
	 * ⌘K / Ctrl+K / "/": paste anything and jump to it. Shapes that are certain go straight through; ambiguous hex
	 * hashes are checked against the store and only the ones that exist are offered.
	 */
	interface Item {
		id: string;
		label: string;
		detail?: string;
		href: string;
		kind?: Candidate['kind'];
		page?: (typeof ALL_PAGES)[number];
		recent?: boolean;
	}

	const RECENT_KEY = 'viewer:recent-search';

	let input = $state<HTMLInputElement>();
	let query = $state('');
	let checking = $state(false);
	let found = $state<Candidate[]>([]);
	let selected = $state(0);
	let recent = $state<{ label: string; href: string; kind?: Candidate['kind'] }[]>([]);
	let seq = 0;

	$effect(() => {
		if (shell.paletteOpen) {
			query = shell.paletteQuery;
			selected = 0;
			try {
				recent = JSON.parse(localStorage.getItem(RECENT_KEY) ?? '[]');
			} catch {
				recent = [];
			}
			tick().then(() => input?.focus());
		}
	});

	// Classify, then confirm ambiguous candidates against the API.
	$effect(() => {
		const q = query;
		const mine = ++seq;
		const candidates = classify(q);
		const certain = candidates.filter((c) => c.certain || !c.check);
		const toCheck = candidates.filter((c) => !c.certain && c.check);
		found = candidates.length === 1 || toCheck.length === 0 ? candidates : certain;
		if (!toCheck.length) {
			checking = false;
			return;
		}
		checking = true;
		const timer = setTimeout(async () => {
			const results = await Promise.all(toCheck.map(async (c) => ((await tryApi(c.check!, { timeoutMs: 8000 })) ? c : null)));
			if (mine !== seq) return;
			const ok = results.filter((c): c is Candidate => c !== null);
			// Keep the classifier's order; unchecked shapes (like a hex pool id) go last.
			found = [...ok, ...certain.filter((c) => !ok.includes(c))];
			checking = false;
		}, 180);
		return () => clearTimeout(timer);
	});

	const items = $derived.by<Item[]>(() => {
		const q = query.trim().toLowerCase();
		const entities: Item[] = found.map((c) => ({
			id: `${c.kind}:${c.value}`,
			label: KIND_LABEL[c.kind],
			detail: c.value,
			href: c.href,
			kind: c.kind
		}));
		const pages: Item[] = ALL_PAGES.filter(
			(p) => !q || p.label.toLowerCase().includes(q) || (p.keywords ?? '').includes(q)
		).map((p) => ({ id: `page:${p.href}`, label: p.label, href: p.href, page: p }));
		const rec: Item[] = q ? [] : recent.map((r) => ({ id: `recent:${r.href}`, label: r.label, href: r.href, kind: r.kind, recent: true }));
		return [...entities, ...rec, ...(q && entities.length ? [] : pages).slice(0, q ? 6 : 8)];
	});

	$effect(() => {
		items.length;
		if (selected >= items.length) selected = 0;
	});

	function close() {
		shell.paletteOpen = false;
		query = '';
		found = [];
	}

	function choose(item: Item) {
		if (item.kind) {
			const entry = { label: item.detail ? `${item.label} ${shorten(item.detail, 10, 6)}` : item.label, href: item.href, kind: item.kind };
			const next = [entry, ...recent.filter((r) => r.href !== item.href)].slice(0, 6);
			try {
				localStorage.setItem(RECENT_KEY, JSON.stringify(next));
			} catch {
				/* storage unavailable */
			}
		}
		close();
		goto(item.href);
	}

	function onKey(e: KeyboardEvent) {
		if (e.key === 'ArrowDown') {
			e.preventDefault();
			selected = (selected + 1) % Math.max(1, items.length);
		} else if (e.key === 'ArrowUp') {
			e.preventDefault();
			selected = (selected - 1 + items.length) % Math.max(1, items.length);
		} else if (e.key === 'Enter') {
			e.preventDefault();
			if (items[selected]) choose(items[selected]);
		} else if (e.key === 'Escape') {
			close();
		}
	}

	function onGlobalKey(e: KeyboardEvent) {
		const target = e.target as HTMLElement;
		const typing = target.closest('input,textarea,select,[contenteditable="true"]');
		if ((e.key === 'k' || e.key === 'K') && (e.metaKey || e.ctrlKey)) {
			e.preventDefault();
			shell.paletteOpen ? close() : shell.openPalette();
		} else if (e.key === '/' && !typing && !shell.paletteOpen) {
			e.preventDefault();
			shell.openPalette();
		}
	}

	// Pasting a hash anywhere on the page (outside inputs) opens the palette with it.
	function onPaste(e: ClipboardEvent) {
		const target = e.target as HTMLElement;
		if (shell.paletteOpen || target.closest('input,textarea,[contenteditable="true"]')) return;
		const text = e.clipboardData?.getData('text')?.trim();
		if (text && classify(text).length) shell.openPalette(text);
	}
</script>

<svelte:window onkeydown={onGlobalKey} onpaste={onPaste} />

{#if shell.paletteOpen}
	<div class="fixed inset-0 z-[70] flex items-start justify-center px-4 pt-[12vh]" role="dialog" aria-modal="true" aria-label="Search">
		<button class="absolute inset-0 animate-[fade-up_0.15s_ease-out] bg-black/40 backdrop-blur-sm" onclick={close} aria-label="Close search"></button>
		<div class="relative w-full max-w-2xl animate-fade-up overflow-hidden rounded-2xl border border-line bg-surface shadow-[0_40px_120px_-30px_rgba(0,0,0,0.6)]">
			<div class="flex items-center gap-3 border-b border-line px-4">
				{#if checking}<LoaderCircle class="h-5 w-5 shrink-0 animate-spin text-brand" />{:else}<Search class="h-5 w-5 shrink-0 text-faint" />{/if}
				<input
					bind:this={input}
					bind:value={query}
					onkeydown={onKey}
					class="h-14 w-full bg-transparent font-mono text-[15px] text-ink placeholder:font-sans placeholder:text-faint focus:outline-none"
					placeholder="Paste a hash, address, block number…"
					spellcheck="false"
					autocomplete="off"
					aria-label="Search"
				/>
				<kbd class="y-kbd">Esc</kbd>
			</div>

			<div class="max-h-[56vh] overflow-y-auto p-2">
				{#if query.trim() && !found.length && !checking && !items.length}
					<p class="px-3 py-8 text-center text-sm text-muted">No match. Try {SEARCH_HINT.toLowerCase()}.</p>
				{/if}
				{#if query.trim() && !found.length && checking}
					<p class="px-3 py-6 text-center text-sm text-muted">Looking it up…</p>
				{/if}
				{#each items as item, i (item.id)}
					{#if i === 0 || items[i - 1].recent !== item.recent || !!items[i - 1].page !== !!item.page}
						<div class="y-label px-3 pb-1.5 pt-3">{item.page ? 'Pages' : item.recent ? 'Recent' : 'Go to'}</div>
					{/if}
					<button
						class="flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left transition-colors {i === selected ? 'bg-brand/10' : 'hover:bg-surface-2'}"
						onmouseenter={() => (selected = i)}
						onclick={() => choose(item)}
					>
						<span class="grid h-8 w-8 shrink-0 place-items-center rounded-lg border border-line bg-surface-2 {i === selected ? 'text-brand' : 'text-muted'}">
							{#if item.page}<item.page.icon class="h-4 w-4" />{:else if item.recent}<Clock class="h-4 w-4" />{:else if item.kind}<KindIcon kind={item.kind} />{/if}
						</span>
						<span class="min-w-0 flex-1">
							<span class="block text-sm font-medium">{item.label}</span>
							{#if item.detail}<span class="mono block truncate text-xs text-muted">{item.detail}</span>{/if}
						</span>
						{#if i === selected}<CornerDownLeft class="h-4 w-4 shrink-0 text-faint" />{:else}<ArrowRight class="h-4 w-4 shrink-0 text-transparent" />{/if}
					</button>
				{/each}
			</div>

			<div class="flex items-center justify-between gap-3 border-t border-line bg-surface-2/50 px-4 py-2.5 text-xs text-faint">
				<span class="truncate">{SEARCH_HINT}</span>
				<span class="hidden shrink-0 items-center gap-1 sm:flex"><kbd class="y-kbd">↑</kbd><kbd class="y-kbd">↓</kbd> to move</span>
			</div>
		</div>
	</div>
{/if}
