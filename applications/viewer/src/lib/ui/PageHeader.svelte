<script lang="ts">
	import type { Snippet } from 'svelte';

	/* The top of every page: a small kind label with an icon, the title, and optional actions and meta. */
	let {
		kind = undefined,
		title,
		icon = undefined,
		actions = undefined,
		children = undefined
	}: { kind?: string; title: string; icon?: Snippet; actions?: Snippet; children?: Snippet } = $props();
</script>

<header class="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
	<div class="flex min-w-0 items-start gap-3.5">
		{#if icon}
			<div class="grid h-11 w-11 shrink-0 place-items-center rounded-2xl border border-brand/20 bg-brand/10 text-brand">
				{@render icon()}
			</div>
		{/if}
		<div class="min-w-0">
			{#if kind}<div class="y-label mb-0.5 text-brand">{kind}</div>{/if}
			<h1 class="truncate text-2xl font-semibold sm:text-[28px]">{title}</h1>
			{#if children}<div class="mt-1.5 min-w-0 text-sm text-muted">{@render children()}</div>{/if}
		</div>
	</div>
	{#if actions}<div class="flex shrink-0 flex-wrap items-center gap-2">{@render actions()}</div>{/if}
</header>
