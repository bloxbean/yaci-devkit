<script lang="ts">
	/*
	 * A plain code editor for julc Java: a textarea with line numbers, Tab indents, and compiler diagnostics marked in
	 * the gutter. Kept dependency-free on purpose (the viewer doesn't ship an editor library).
	 */
	export interface Diagnostic {
		level: string | null;
		message: string | null;
		startLine: number | null;
		suggestion?: string | null;
	}

	let { value = $bindable(''), diagnostics = [], label = 'Java source' }: { value: string; diagnostics?: Diagnostic[]; label?: string } = $props();

	let area: HTMLTextAreaElement | undefined = $state();
	let gutter: HTMLDivElement | undefined = $state();
	const lines = $derived(value.split('\n').length);
	const marks = $derived(new Map(diagnostics.filter((d) => d.startLine).map((d) => [d.startLine!, d])));

	function keydown(e: KeyboardEvent) {
		if (e.key !== 'Tab' || !area) return;
		e.preventDefault();
		const { selectionStart: s, selectionEnd: end } = area;
		value = value.slice(0, s) + '    ' + value.slice(end);
		requestAnimationFrame(() => area && (area.selectionStart = area.selectionEnd = s + 4));
	}

	/** Put the caret on a line (from a diagnostic). */
	export function goto(line: number) {
		if (!area) return;
		const at = value.split('\n').slice(0, line - 1).join('\n').length + (line > 1 ? 1 : 0);
		area.focus();
		area.setSelectionRange(at, at);
		area.scrollTop = Math.max(0, (line - 4) * 20);
	}
</script>

<div class="flex max-h-[34rem] min-h-80 overflow-hidden rounded-xl border border-line bg-bg-2 font-mono text-[12.5px] leading-5">
	<div bind:this={gutter} class="shrink-0 overflow-hidden border-r border-line bg-surface-2/50 py-2 text-right text-faint select-none" aria-hidden="true">
		{#each { length: lines } as _, i (i)}
			{@const d = marks.get(i + 1)}
			<div class="h-5 px-2 {d ? (d.level === 'ERROR' ? 'bg-danger/15 text-danger' : 'bg-warn/15 text-warn') : ''}" title={d?.message ?? undefined}>{i + 1}</div>
		{/each}
	</div>
	<textarea
		bind:this={area}
		bind:value
		onkeydown={keydown}
		onscroll={() => gutter && area && (gutter.scrollTop = area.scrollTop)}
		class="min-w-0 flex-1 resize-none bg-transparent px-3 py-2 whitespace-pre text-ink outline-none"
		spellcheck="false"
		autocapitalize="off"
		autocomplete="off"
		wrap="off"
		aria-label={label}
	></textarea>
</div>
