<script lang="ts">
	import CopyButton from './CopyButton.svelte';

	/* Pretty, highlighted JSON with copy and an expand toggle for long documents. */
	let { value, maxHeight = 420, label = undefined }: { value: unknown; maxHeight?: number; label?: string } = $props();

	let expanded = $state(false);

	const text = $derived(typeof value === 'string' ? tryPretty(value) : JSON.stringify(value, null, 2) ?? 'null');
	const lines = $derived(text.split('\n').length);

	function tryPretty(s: string) {
		try {
			return JSON.stringify(JSON.parse(s), null, 2);
		} catch {
			return s;
		}
	}

	const esc = (s: string) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

	const html = $derived(
		esc(text).replace(
			/("(?:\\u[a-fA-F0-9]{4}|\\[^u]|[^\\"])*"(\s*:)?|\b(?:true|false|null)\b|-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)/g,
			(m) => {
				let cls = 'text-cyan';
				if (m.startsWith('"')) cls = m.trimEnd().endsWith(':') ? 'text-violet' : 'text-mint';
				else if (m === 'true' || m === 'false') cls = 'text-brand';
				else if (m === 'null') cls = 'text-faint';
				return `<span class="${cls}">${m}</span>`;
			}
		)
	);
</script>

<div class="overflow-hidden rounded-xl border border-line bg-bg-2">
	<div class="flex items-center justify-between border-b border-line px-3 py-1.5">
		<span class="y-label">{label ?? 'JSON'}</span>
		<div class="flex items-center gap-1">
			{#if lines > 18}
				<button class="rounded-md px-2 py-0.5 text-xs text-muted hover:bg-surface-2 hover:text-ink" onclick={() => (expanded = !expanded)}
					>{expanded ? 'Collapse' : `Expand (${lines} lines)`}</button
				>
			{/if}
			<CopyButton value={text} label="JSON copied" />
		</div>
	</div>
	<pre
		class="overflow-auto p-4 font-mono text-[12.5px] leading-relaxed text-ink"
		style="max-height:{expanded ? 'none' : `${maxHeight}px`}">{@html html}</pre>
</div>
