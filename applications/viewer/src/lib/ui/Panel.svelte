<script lang="ts">
	import type { Snippet } from 'svelte';

	/* A titled card. `flush` drops the body padding, for tables and lists. */
	let {
		title = undefined,
		sub = undefined,
		icon = undefined,
		actions = undefined,
		flush = false,
		class: klass = '',
		children
	}: {
		title?: string;
		sub?: string;
		icon?: Snippet;
		actions?: Snippet;
		flush?: boolean;
		class?: string;
		children: Snippet;
	} = $props();
</script>

<section class="y-panel min-w-0 overflow-hidden {klass}">
	{#if title || actions}
		<header class="y-panel-head">
			<div class="flex min-w-0 items-center gap-2.5">
				{#if icon}<span class="grid h-7 w-7 shrink-0 place-items-center rounded-lg bg-brand/10 text-brand">{@render icon()}</span>{/if}
				<div class="min-w-0">
					{#if title}<h2 class="truncate font-display text-[15px] font-semibold">{title}</h2>{/if}
					{#if sub}<p class="truncate text-xs text-muted">{sub}</p>{/if}
				</div>
			</div>
			{#if actions}<div class="flex shrink-0 items-center gap-2">{@render actions()}</div>{/if}
		</header>
	{/if}
	<div class={flush ? '' : 'p-5'}>
		{@render children()}
	</div>
</section>
