<script lang="ts">
	let { compact = false }: { compact?: boolean } = $props();
</script>

<a href="/" class="flex items-center gap-2.5" aria-label="Yaci Viewer home">
	<svg viewBox="0 0 64 64" class="h-8 w-8 shrink-0" aria-hidden="true">
		<rect x="2" y="2" width="60" height="60" rx="14" fill="#0E0F12" stroke="rgb(var(--c-line))" stroke-width="2" />
		<path d="M14 12 L24 12 L32 26 L40 12 L50 12 L36 36 L42 36 L24 56 L30 38 L22 38 Z" fill="#FF4E2D" />
	</svg>
	{#if !compact}
		<span class="font-mono text-[15px] font-extrabold tracking-tight"
			>Yaci<span class="mx-0.5 inline-block h-1.5 w-1.5 -translate-y-2 rounded-full bg-brand"></span><span class="font-medium text-muted">Viewer</span></span
		>
	{/if}
</a>
