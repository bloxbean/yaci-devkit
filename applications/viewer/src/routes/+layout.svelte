<script lang="ts">
	import '@fontsource-variable/inter';
	import '@fontsource-variable/space-grotesk';
	import '@fontsource-variable/jetbrains-mono';
	import '../app.postcss';
	import { onMount, type Snippet } from 'svelte';
	import { onNavigate } from '$app/navigation';
	import { env } from '$env/dynamic/public';
	import { chain } from '$lib/live/chain.svelte';
	import { shell } from '$lib/shell/ui.svelte';
	import Sidebar from '$lib/shell/Sidebar.svelte';
	import Topbar from '$lib/shell/Topbar.svelte';
	import CommandPalette from '$lib/shell/CommandPalette.svelte';
	import NavProgress from '$lib/shell/NavProgress.svelte';
	import Toaster from '$lib/ui/Toaster.svelte';
	import TriangleAlert from '@lucide/svelte/icons/triangle-alert';

	let { children }: { children: Snippet } = $props();

	onMount(() => {
		shell.restore();
		chain.start();
	});

	// Cross-fade between pages where the browser supports view transitions.
	onNavigate((navigation) => {
		const doc = document as Document & { startViewTransition?: (update: () => Promise<void>) => unknown };
		if (!doc.startViewTransition || navigation.from?.url.pathname === navigation.to?.url.pathname) return;
		return new Promise((resolve) => {
			doc.startViewTransition!(async () => {
				resolve();
				await navigation.complete;
			});
		});
	});
</script>

<svelte:head>
	<title>Yaci Viewer</title>
</svelte:head>

<NavProgress />

<!-- Soft brand glow behind the content -->
<div class="pointer-events-none fixed inset-0 -z-10 overflow-hidden" aria-hidden="true">
	<div class="absolute -top-40 right-[-10%] h-[520px] w-[620px] rounded-full blur-3xl" style="background: var(--glow-1)"></div>
	<div class="absolute top-[30%] -left-40 h-[480px] w-[520px] rounded-full blur-3xl" style="background: var(--glow-2)"></div>
</div>

<Sidebar />

<div class="flex min-h-screen flex-col transition-[padding] duration-200 {shell.collapsed ? 'lg:pl-[76px]' : 'lg:pl-[248px]'}">
	<Topbar />

	{#if env.PUBLIC_MAINTENANCE_MSG}
		<div class="mx-auto mt-4 w-full max-w-[1480px] px-4 sm:px-6 lg:px-8">
			<div class="flex items-center gap-3 rounded-xl border border-amber/30 bg-amber/10 px-4 py-3 text-sm text-ink">
				<TriangleAlert class="h-4 w-4 shrink-0 text-amber" />
				{env.PUBLIC_MAINTENANCE_MSG}
			</div>
		</div>
	{/if}

	<main class="mx-auto w-full max-w-[1480px] flex-1 px-4 py-6 sm:px-6 lg:px-8 lg:py-8">
		{@render children()}
	</main>

	<footer class="border-t border-line/70">
		<div class="mx-auto flex max-w-[1480px] flex-col items-center justify-between gap-2 px-4 py-5 text-xs text-faint sm:flex-row sm:px-6 lg:px-8">
			<span>Yaci Viewer · part of <a class="y-link" href="https://devkit.yaci.xyz" target="_blank" rel="noopener noreferrer">Yaci DevKit</a></span>
			<span>Open source by <a class="y-link" href="https://github.com/bloxbean" target="_blank" rel="noopener noreferrer">BloxBean</a> · MIT</span>
		</div>
	</footer>
</div>

<CommandPalette />
<Toaster />
