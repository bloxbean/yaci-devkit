<script lang="ts">
	import type { Snippet } from 'svelte';

	export type Tone = 'neutral' | 'brand' | 'ok' | 'warn' | 'bad' | 'info' | 'violet' | 'cyan';

	let { tone = 'neutral', class: klass = '', children }: { tone?: Tone; class?: string; children: Snippet } = $props();

	const tones: Record<Tone, string> = {
		neutral: 'border-line bg-surface-2 text-muted',
		brand: 'border-brand/25 bg-brand/10 text-brand',
		ok: 'border-mint/25 bg-mint/10 text-mint',
		warn: 'border-amber/25 bg-amber/10 text-amber',
		bad: 'border-danger/25 bg-danger/10 text-danger',
		info: 'border-blue/25 bg-blue/10 text-blue',
		violet: 'border-violet/25 bg-violet/10 text-violet',
		cyan: 'border-cyan/25 bg-cyan/10 text-cyan'
	};
</script>

<span class="y-pill {tones[tone]} {klass}">{@render children()}</span>
