<script lang="ts">
	import { goto } from '$app/navigation';
	import type { Component, Snippet } from 'svelte';
	import ArrowRight from '@lucide/svelte/icons/arrow-right';

	/*
	 * A big lookup box for pages that start from an id (addresses, assets, pools). `resolve` turns the input into a
	 * URL, or returns an error message.
	 */
	let {
		kind,
		title,
		text,
		placeholder,
		icon: Icon,
		resolve,
		examples = [],
		children = undefined
	}: {
		kind: string;
		title: string;
		text: string;
		placeholder: string;
		icon: Component;
		resolve: (value: string) => string | { error: string };
		examples?: { label: string; value: string }[];
		children?: Snippet;
	} = $props();

	let value = $state('');
	let problem = $state<string | null>(null);

	function submit(e?: Event) {
		e?.preventDefault();
		const v = value.trim();
		if (!v) return;
		const r = resolve(v);
		if (typeof r === 'string') goto(r);
		else problem = r.error;
	}
</script>

<section class="relative mx-auto max-w-3xl py-10 text-center sm:py-16">
	<div class="mx-auto mb-5 grid h-14 w-14 place-items-center rounded-2xl border border-brand/25 bg-brand/10 text-brand shadow-glow">
		<Icon class="h-6 w-6" />
	</div>
	<div class="y-label mb-2 text-brand">{kind}</div>
	<h1 class="text-3xl font-semibold sm:text-4xl">{title}</h1>
	<p class="mx-auto mt-3 max-w-xl text-muted">{text}</p>

	<form class="mx-auto mt-8 flex max-w-2xl gap-2" onsubmit={submit}>
		<input
			class="y-input h-12 flex-1 font-mono text-[14px] placeholder:font-sans"
			{placeholder}
			bind:value
			oninput={() => (problem = null)}
			spellcheck="false"
			autocomplete="off"
			aria-label={placeholder}
		/>
		<button class="y-btn y-btn-primary h-12 px-5" type="submit">Go <ArrowRight class="h-4 w-4" /></button>
	</form>
	{#if problem}<p class="mt-3 text-sm text-danger">{problem}</p>{/if}
	{#if examples.length}
		<div class="mt-5 flex flex-wrap items-center justify-center gap-2 text-xs text-faint">
			Try
			{#each examples as ex (ex.value)}
				<button class="y-pill border-line bg-surface text-muted hover:border-brand/40 hover:text-ink" onclick={() => ((value = ex.value), submit())}>{ex.label}</button>
			{/each}
		</div>
	{/if}
	{#if children}<div class="mt-10 text-left">{@render children()}</div>{/if}
</section>
