<script lang="ts">
	import { untrack } from 'svelte';
	import { tokenize, splitMatches, type CodeLanguage, type TokenKind } from './highlight';
	import type { Span } from './types';
	import { copyText } from '$lib/ui/toast.svelte';
	import Search from '@lucide/svelte/icons/search';
	import ChevronUp from '@lucide/svelte/icons/chevron-up';
	import ChevronDown from '@lucide/svelte/icons/chevron-down';
	import Copy from '@lucide/svelte/icons/copy';
	import Download from '@lucide/svelte/icons/download';
	import WrapText from '@lucide/svelte/icons/wrap-text';

	/*
	 * A read-only code viewer for large generated code (UPLC of a 16 KB script is tens of thousands of lines).
	 * Only the lines on screen are rendered and highlighted. Search jumps between matching lines.
	 * For the debugger: `active` highlights the current term's lines and keeps them in view, and with `ontoggle`
	 * the line numbers set and clear breakpoints.
	 */
	let {
		text,
		language = 'plain',
		filename = 'code.txt',
		height = 520,
		label = 'Code',
		query = $bindable(''),
		active = null,
		breakpoints = undefined,
		ontoggle = undefined
	}: {
		text: string;
		language?: CodeLanguage;
		filename?: string;
		height?: number;
		label?: string;
		query?: string;
		active?: Span | null;
		breakpoints?: Set<number>;
		ontoggle?: (line: number) => void;
	} = $props();

	const LINE = 20;
	const OVERSCAN = 30;

	const lines = $derived(text.split('\n'));
	const widest = $derived(Math.min(4000, lines.reduce((m, l) => Math.max(m, l.length), 0)));
	const gutter = $derived(String(lines.length).length);
	const viewHeight = $derived(Math.min(height, lines.length * LINE + 16));

	let viewport = $state<HTMLDivElement>();
	let scrollTop = $state(0);
	let wrap = $state(false);

	const first = $derived(Math.max(0, Math.floor(scrollTop / LINE) - OVERSCAN));
	const last = $derived(Math.min(lines.length, Math.ceil((scrollTop + viewHeight) / LINE) + OVERSCAN));
	const visible = $derived(lines.slice(first, last).map((line, i) => ({ n: first + i, tokens: tokenize(line, language) })));

	let current = $state(0);
	const needle = $derived(query.trim().toLowerCase());
	const hits = $derived.by(() => {
		if (needle.length < 2) return [] as number[];
		const out: number[] = [];
		for (let i = 0; i < lines.length && out.length < 5000; i++) if (lines[i].toLowerCase().includes(needle)) out.push(i);
		return out;
	});
	const hitSet = $derived(new Set(hits));

	function jump(index: number) {
		if (!hits.length || !viewport) return;
		current = (index + hits.length) % hits.length;
		viewport.scrollTop = Math.max(0, hits[current] * LINE - viewHeight / 3);
	}

	$effect(() => {
		// A new search starts at the first hit. Only `hits` is tracked: jump() reads `current`.
		if (hits.length) untrack(() => jump(0));
	});

	let probe = $state<HTMLSpanElement>();

	// Keep the debugger's current term in view: scroll only when it is off screen (or near the edges), vertically and,
	// as UPLC is deeply indented, horizontally too.
	$effect(() => {
		const line = active?.startLine;
		const column = active?.startColumn ?? 1;
		if (!line || !viewport) return;
		untrack(() => {
			const v = viewport!;
			const y = (line - 1) * LINE;
			if (y < v.scrollTop + LINE * 2 || y > v.scrollTop + viewHeight - LINE * 3) v.scrollTop = Math.max(0, y - viewHeight / 3);
			const ch = probe ? probe.getBoundingClientRect().width / 10 : 7.5;
			const gutterPx = (gutter + 3) * ch + 36;
			const x = (column - 1) * ch;
			if (x < v.scrollLeft + ch * 4 || x > v.scrollLeft + v.clientWidth - gutterPx - ch * 30) v.scrollLeft = Math.max(0, x - ch * 8);
		});
	});

	const inActive = (n: number) => !!active && n + 1 >= active.startLine && n + 1 <= active.endLine;

	function onKey(e: KeyboardEvent) {
		if (e.key === 'Enter') {
			e.preventDefault();
			jump(current + (e.shiftKey ? -1 : 1));
		} else if (e.key === 'Escape') {
			query = '';
		}
	}

	function download() {
		const url = URL.createObjectURL(new Blob([text], { type: 'text/plain' }));
		const a = Object.assign(document.createElement('a'), { href: url, download: filename });
		a.click();
		setTimeout(() => URL.revokeObjectURL(url), 1000);
	}

	const tone: Record<TokenKind, string> = {
		kw: 'text-violet',
		builtin: 'text-cyan',
		type: 'text-amber',
		num: 'text-mint',
		bytes: 'text-mint',
		str: 'text-pink',
		punct: 'text-faint',
		plain: ''
	};
</script>

<div class="overflow-hidden rounded-xl border border-line bg-bg-2">
	<div class="flex flex-wrap items-center gap-2 border-b border-line bg-surface px-3 py-2">
		<label class="relative flex min-w-[180px] flex-1 items-center sm:max-w-xs">
			<Search class="pointer-events-none absolute left-2.5 h-3.5 w-3.5 text-faint" />
			<input
				class="y-input h-8 pl-8 text-xs"
				placeholder="Search {label.toLowerCase()}"
				aria-label="Search {label.toLowerCase()}"
				bind:value={query}
				onkeydown={onKey}
			/>
		</label>
		{#if needle.length >= 2}
			<span class="text-xs tabular-nums text-muted">{hits.length ? `${current + 1} / ${hits.length}${hits.length >= 5000 ? '+' : ''}` : 'No matches'}</span>
			<button class="y-icon-btn h-8 w-8" aria-label="Previous match" disabled={!hits.length} onclick={() => jump(current - 1)}><ChevronUp class="h-4 w-4" /></button>
			<button class="y-icon-btn h-8 w-8" aria-label="Next match" disabled={!hits.length} onclick={() => jump(current + 1)}><ChevronDown class="h-4 w-4" /></button>
		{/if}
		<span class="ml-auto text-xs tabular-nums text-faint">{lines.length.toLocaleString()} lines</span>
		<button class="y-icon-btn h-8 w-8 {wrap ? 'text-brand' : ''}" aria-pressed={wrap} aria-label="Wrap long lines" title="Wrap long lines" onclick={() => (wrap = !wrap)}><WrapText class="h-4 w-4" /></button>
		<button class="y-icon-btn h-8 w-8" aria-label="Copy {label}" title="Copy" onclick={() => copyText(text, `${label} copied`)}><Copy class="h-4 w-4" /></button>
		<button class="y-icon-btn h-8 w-8" aria-label="Download {label}" title="Download" onclick={download}><Download class="h-4 w-4" /></button>
	</div>

	{#if wrap}
		<!-- Wrapped lines have no fixed height, so this view renders everything; fine for reading, not for huge scripts. -->
		<pre class="mono overflow-auto p-3 text-[12.5px] leading-5 whitespace-pre-wrap break-all text-ink" style="max-height:{height}px">{text}</pre>
	{:else}
		<div
			bind:this={viewport}
			class="mono relative overflow-auto text-[12.5px] leading-5"
			style="height:{viewHeight}px"
			onscroll={(e) => (scrollTop = e.currentTarget.scrollTop)}
			role="region"
			aria-label={label}
			tabindex="-1"
		>
			<span bind:this={probe} class="invisible absolute" aria-hidden="true">0000000000</span>
			<div style="height:{lines.length * LINE + 16}px; min-width:calc({widest + gutter + 4}ch + 3rem)">
				<div style="transform:translateY({first * LINE + 8}px)">
					{#each visible as line (line.n)}
						{@const isActive = inActive(line.n)}
						{@const isStart = isActive && active?.startLine === line.n + 1}
						{@const isBreak = breakpoints?.has(line.n + 1)}
						<div
							class="flex h-5 {isStart ? 'bg-brand/20' : isActive ? 'bg-brand/[0.07]' : hitSet.has(line.n) ? (hits[current] === line.n ? 'bg-amber/20' : 'bg-amber/[0.07]') : ''}"
							data-line={line.n + 1}
						>
							{#if ontoggle}
								<button
									class="sticky left-0 flex shrink-0 select-none items-center justify-end gap-1.5 border-r border-line bg-bg-2 pl-2 pr-3 text-right {isStart ? 'text-brand' : 'text-faint'} hover:text-ink"
									style="width:calc({gutter}ch + 2.25rem)"
									title={isBreak ? 'Remove breakpoint' : 'Break on this line'}
									aria-label="{isBreak ? 'Remove breakpoint at' : 'Break at'} line {line.n + 1}"
									onclick={() => ontoggle(line.n + 1)}
								>
									<span class="h-2 w-2 shrink-0 rounded-full {isBreak ? 'bg-danger' : 'bg-transparent'}"></span>{line.n + 1}
								</button>
							{:else}
								<span class="sticky left-0 shrink-0 select-none border-r border-line bg-bg-2 pl-3 pr-3 text-right text-faint" style="width:calc({gutter}ch + 1.5rem)">{line.n + 1}</span>
							{/if}
							<span class="whitespace-pre pl-3 pr-4 text-ink">{#each line.tokens as t}{#if needle.length >= 2 && hitSet.has(line.n)}{#each splitMatches(t.text, needle) as part}<span class="{tone[t.kind]} {part.hit ? 'rounded-sm bg-amber/40 text-ink' : ''}">{part.text}</span>{/each}{:else}<span class={tone[t.kind]}>{t.text}</span>{/if}{/each}</span>
						</div>
					{/each}
				</div>
			</div>
		</div>
	{/if}
</div>
